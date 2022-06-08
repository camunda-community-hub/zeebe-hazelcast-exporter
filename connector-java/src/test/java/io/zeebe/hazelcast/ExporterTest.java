package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import io.zeebe.hazelcast.testcontainers.ZeebeTestContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ExporterTest {

  private static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("process")
          .startEvent("start")
          .sequenceFlowId("to-task")
          .serviceTask("task", s -> s.zeebeJobType("test").zeebeInputExpression("foo", "bar"))
          .sequenceFlowId("to-end")
          .endEvent("end")
          .done();

  private ZeebeClient client;
  private HazelcastInstance hz;
  private ZeebeHazelcast zeebeHazelcast;

  @Container
  public ZeebeTestContainer zeebeContainer = new ZeebeTestContainer();

  @BeforeEach
  public void init() {
    client = zeebeContainer.getClient();

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress(zeebeContainer.getHazelcastAddress());
    hz = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @AfterEach
  public void cleanUp() throws Exception {
    zeebeHazelcast.close();
    hz.shutdown();
  }

  @Test
  public void shouldIncrementSequence() {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .build();

    final var sequence1 = zeebeHazelcast.getSequence();

    // when
    client.newDeployResourceCommand().addProcessModel(PROCESS, "process.bpmn").send().join();

    Awaitility.await("await until the deployment is fully distributed")
        .untilAsserted(
            () ->
                assertThat(deploymentRecords)
                    .extracting(r -> r.getMetadata().getIntent())
                    .contains(DeploymentIntent.FULLY_DISTRIBUTED.name()));

    final var sequence2 = zeebeHazelcast.getSequence();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    Awaitility.await("await until the service task is activated")
        .untilAsserted(
            () ->
                assertThat(processInstanceRecords)
                    .extracting(Schema.ProcessInstanceRecord::getBpmnElementType)
                    .contains(BpmnElementType.SERVICE_TASK.name()));

    final var sequence3 = zeebeHazelcast.getSequence();

    // then
    assertThat(sequence2).isGreaterThan(sequence1);
    assertThat(sequence3).isGreaterThan(sequence2);
  }

  @Test
  public void shouldInvokePostProcessListener() {
    // given
    final List<Long> invocations = new ArrayList<>();

    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();
    final List<Schema.JobRecord> jobRecords = new ArrayList<>();

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .addJobListener(jobRecords::add)
            .postProcessListener(invocations::add)
            .build();

    final var initialSequence = zeebeHazelcast.getSequence();

    // when
    client.newDeployResourceCommand().addProcessModel(PROCESS, "process.bpmn").send().join();
    client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("foo", "bar"))
        .send()
        .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              assertThat(jobRecords).hasSizeGreaterThanOrEqualTo(1);
              assertThat(invocations)
                  .hasSizeGreaterThanOrEqualTo(
                      deploymentRecords.size() + processInstanceRecords.size() + jobRecords.size());
            });

    final var lastSequence = zeebeHazelcast.getSequence();

    // then
    final var expectedSequence =
        LongStream.rangeClosed(initialSequence + 1, lastSequence)
            .boxed()
            .collect(Collectors.toList());

    assertThat(invocations).isEqualTo(expectedSequence);
  }

  @Test
  public void shouldReadFromHead() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .readFromHead()
            .build();

    client.newDeployResourceCommand().addProcessModel(PROCESS, "process.bpmn").send().join();

    Awaitility.await("await until the deployment is fully distributed")
        .untilAsserted(
            () ->
                assertThat(deploymentRecords)
                    .hasSize(3)
                    .extracting(r -> r.getMetadata().getIntent())
                    .contains("CREATE", "CREATED", "FULLY_DISTRIBUTED"));

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .readFromHead()
            .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    Awaitility.await("await until the service task is activated")
        .untilAsserted(
            () ->
                assertThat(processInstanceRecords)
                    .extracting(Schema.ProcessInstanceRecord::getBpmnElementType)
                    .contains(BpmnElementType.SERVICE_TASK.name()));

    // then
    assertThat(deploymentRecords)
        .hasSize(3)
        .extracting(r -> r.getMetadata().getIntent())
        .contains("CREATE", "CREATED", "FULLY_DISTRIBUTED");
  }

  @Test
  public void shouldReadFromTail() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz).addDeploymentListener(deploymentRecords::add).build();

    client.newDeployResourceCommand().addProcessModel(PROCESS, "process.bpmn").send().join();
    Awaitility.await("await until the deployment is fully distributed")
        .untilAsserted(
            () ->
                assertThat(deploymentRecords)
                    .extracting(r -> r.getMetadata().getIntent())
                    .contains(DeploymentIntent.FULLY_DISTRIBUTED.name()));

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .readFromTail()
            .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    Awaitility.await("await until the service task is activated")
        .untilAsserted(
            () ->
                assertThat(processInstanceRecords)
                    .extracting(Schema.ProcessInstanceRecord::getBpmnElementType)
                    .contains(BpmnElementType.SERVICE_TASK.name()));

    // then
    assertThat(deploymentRecords).hasSize(1);
  }

  @Test
  public void shouldReadFromSequence() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.ProcessInstanceRecord> processInstanceRecords = new ArrayList<>();

    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz).addDeploymentListener(deploymentRecords::add).build();

    client.newDeployResourceCommand().addProcessModel(PROCESS, "process.bpmn").send().join();

    Awaitility.await("await until the deployment is fully distributed")
        .untilAsserted(
            () ->
                assertThat(deploymentRecords)
                    .extracting(r -> r.getMetadata().getIntent())
                    .contains(DeploymentIntent.FULLY_DISTRIBUTED.name()));

    final var sequence = zeebeHazelcast.getSequence();

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
        ZeebeHazelcast.newBuilder(hz)
            .addDeploymentListener(deploymentRecords::add)
            .addProcessInstanceListener(processInstanceRecords::add)
            .readFrom(sequence)
            .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    Awaitility.await("await until the service task is activated")
        .untilAsserted(
            () ->
                assertThat(processInstanceRecords)
                    .extracting(Schema.ProcessInstanceRecord::getBpmnElementType)
                    .contains(BpmnElementType.SERVICE_TASK.name()));

    // then
    assertThat(deploymentRecords).isEmpty();
  }
}
