package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.test.ZeebeTestRule;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ExporterRecordTest {

  private static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("process")
          .startEvent("start")
          .parallelGateway("fork")
          .serviceTask("task", s -> s.zeebeJobType("test").zeebeInputExpression("key", "x"))
          .endEvent("end")
          .moveToNode("fork")
          .receiveTask("receive-task")
          .message(m -> m.name("message").zeebeCorrelationKeyExpression("key"))
          .boundaryEvent("timer", b -> b.timerWithDuration("PT1M"))
          .endEvent()
          .done();

  private static final BpmnModelInstance MESSAGE_PROCESS =
      Bpmn.createExecutableProcess("message-process")
          .startEvent()
          .message("start")
          .zeebeOutputExpression("x", "x")
          .endEvent()
          .done();

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("application.yaml", Properties::new);

  private final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
  private final List<Schema.IncidentRecord> incidentRecords = new ArrayList<>();
  private final List<Schema.JobBatchRecord> jobBatchRecords = new ArrayList<>();
  private final List<Schema.JobRecord> jobRecords = new ArrayList<>();
  private final List<Schema.MessageRecord> messageRecords = new ArrayList<>();
  private final List<Schema.MessageStartEventSubscriptionRecord>
      messageStartEventSubscriptionRecords = new ArrayList<>();
  private final List<Schema.MessageSubscriptionRecord> messageSubscriptionRecords =
      new ArrayList<>();
  private final List<Schema.ProcessEventRecord> processEventRecords = new ArrayList<>();
  private final List<Schema.ProcessInstanceCreationRecord> processInstanceCreationRecords =
      new ArrayList<>();
  private final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();
  private final List<Schema.ProcessMessageSubscriptionRecord> processMessageSubscriptionRecords =
      new ArrayList<>();
  private final List<Schema.ProcessRecord> processRecords = new ArrayList<>();
  private final List<Schema.TimerRecord> timerRecords = new ArrayList<>();
  private final List<Schema.VariableDocumentRecord> variableDocumentRecords = new ArrayList<>();
  private final List<Schema.VariableRecord> variableRecords = new ArrayList<>();

  private ZeebeClient client;
  private HazelcastInstance hz;
  private ZeebeHazelcast zeebeHazelcast;

  @Before
  public void init() {
    client = testRule.getClient();

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    hz = HazelcastClient.newHazelcastClient(clientConfig);

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addIncidentListener(incidentRecords::add)
            .addJobBatchListener(jobBatchRecords::add)
            .addJobListener(jobRecords::add)
            .addMessageListener(messageRecords::add)
            .addMessageStartEventSubscriptionListener(messageStartEventSubscriptionRecords::add)
            .addMessageSubscriptionListener(messageSubscriptionRecords::add)
            .addProcessEventListener(processEventRecords::add)
            .addProcessInstanceCreationListener(processInstanceCreationRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .addProcessMessageSubscriptionListener(processMessageSubscriptionRecords::add)
            .addProcessListener(processRecords::add)
            .addTimerListener(timerRecords::add)
            .addVariableDocumentListener(variableDocumentRecords::add)
            .addVariableListener(variableRecords::add)
            .build();
  }

  @After
  public void cleanUp() throws Exception {
    zeebeHazelcast.close();
    hz.shutdown();
  }

  @Test
  public void shouldExportRecords() {
    // given
    client
        .newDeployCommand()
        .addProcessModel(PROCESS, "process.bpmn")
        .addProcessModel(MESSAGE_PROCESS, "message-process.bpmn")
        .send()
        .join();

    // when
    final var processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Map.of("key", "key-1"))
            .send()
            .join();

    client
        .newSetVariablesCommand(processInstance.getProcessInstanceKey())
        .variables(Map.of("y", 2))
        .send()
        .join();

    client.newPublishMessageCommand().messageName("start").correlationKey("key-2").send().join();
    client
        .newPublishMessageCommand()
        .messageName("message")
        .correlationKey("key-1")
        .timeToLive(Duration.ofMinutes(1))
        .send()
        .join();

    final var jobsResponse =
        client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();
    jobsResponse.getJobs().forEach(job -> client.newCompleteCommand(job.getKey()).send().join());

    // then
    await()
        .untilAsserted(
            () -> {
              assertThat(deploymentRecords)
                  .hasSizeGreaterThanOrEqualTo(3)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.DEPLOYMENT))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATE", "CREATED", "FULLY_DISTRIBUTED");

              assertThat(incidentRecords)
                  .hasSizeGreaterThanOrEqualTo(1)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.INCIDENT))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED");

              assertThat(jobBatchRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.JOB_BATCH))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("ACTIVATE", "ACTIVATED");

              assertThat(jobRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.JOB))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED", "COMPLETED");

              assertThat(messageRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.MESSAGE))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("PUBLISH", "PUBLISHED");

              assertThat(messageStartEventSubscriptionRecords)
                  .hasSizeGreaterThanOrEqualTo(1)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(
                                  Schema.RecordMetadata.ValueType.MESSAGE_START_EVENT_SUBSCRIPTION))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED");

              assertThat(messageSubscriptionRecords)
                  .hasSizeGreaterThanOrEqualTo(3)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.MESSAGE_SUBSCRIPTION))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED", "CORRELATING", "CORRELATED");

              assertThat(processEventRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.PROCESS_EVENT))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("TRIGGERING", "TRIGGERED");

              assertThat(processInstanceCreationRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.PROCESS_INSTANCE_CREATION))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATE", "CREATED");

              assertThat(processInstanceRecords)
                  .hasSizeGreaterThanOrEqualTo(3)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.PROCESS_INSTANCE))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("ACTIVATE_ELEMENT", "ELEMENT_ACTIVATING", "ELEMENT_ACTIVATED");

              assertThat(processMessageSubscriptionRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(
                                  Schema.RecordMetadata.ValueType.PROCESS_MESSAGE_SUBSCRIPTION))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATING", "CORRELATED");

              assertThat(processRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.PROCESS))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED");

              assertThat(timerRecords)
                  .hasSizeGreaterThanOrEqualTo(1)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.TIMER))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED");

              assertThat(variableDocumentRecords)
                  .hasSizeGreaterThanOrEqualTo(1)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.VARIABLE_DOCUMENT))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("UPDATE", "UPDATED");

              assertThat(variableRecords)
                  .hasSizeGreaterThanOrEqualTo(2)
                  .allSatisfy(
                      r ->
                          assertThat(r.getMetadata().getValueType())
                              .isEqualTo(Schema.RecordMetadata.ValueType.VARIABLE))
                  .extracting(r -> r.getMetadata().getIntent())
                  .contains("CREATED");
            });
  }
}
