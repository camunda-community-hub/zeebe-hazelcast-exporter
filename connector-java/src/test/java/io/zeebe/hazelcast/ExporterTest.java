package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.zeebe.client.ZeebeClient;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.ZeebeTestRule;
import io.zeebe.test.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExporterTest {

  private static final BpmnModelInstance WORKFLOW =
      Bpmn.createExecutableProcess("process")
          .startEvent("start")
          .sequenceFlowId("to-task")
          .serviceTask("task", s -> s.zeebeTaskType("test").zeebeInput("foo", "bar"))
          .sequenceFlowId("to-end")
          .endEvent("end")
          .done();

  private static final ExporterConfiguration CONFIGURATION = new ExporterConfiguration();

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("zeebe.test.cfg.toml", Properties::new);

  private ZeebeClient client;
  private HazelcastInstance hz;
  private ZeebeHazelcast zeebeHazelcast;

  @Before
  public void init() {
    client = testRule.getClient();

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    hz = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @After
  public void cleanUp() throws Exception {
    zeebeHazelcast.close();
    hz.shutdown();
  }

  @Test
  public void shouldExportWorkflowInstanceEvents() {
    final List<Schema.WorkflowInstanceRecord> events = new ArrayList<>();

    zeebeHazelcast = ZeebeHazelcast.newBuilder(hz).addWorkflowInstanceListener(events::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    TestUtil.waitUntil(() -> events.size() >= 4);

    assertThat(events)
            .extracting(r -> tuple(r.getElementId(), r.getMetadata().getIntent()))
            .containsSequence(
                    tuple("process", "ELEMENT_ACTIVATING"),
                    tuple("process", "ELEMENT_ACTIVATED"),
                    tuple("start", "ELEMENT_ACTIVATING"),
                    tuple("start", "ELEMENT_ACTIVATED"));
  }

  @Test
  public void shouldExportDeploymentEvents() {
    final List<Schema.DeploymentRecord> events = new ArrayList<>();

    zeebeHazelcast = ZeebeHazelcast.newBuilder(hz).addDeploymentListener(events::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    TestUtil.waitUntil(() -> events.size() >= 2);

    assertThat(events)
            .hasSize(2)
            .extracting(r -> r.getMetadata().getIntent())
            .containsExactly("CREATED", "DISTRIBUTED");
  }

  @Test
  public void shouldExportJobEvents() {
    final List<Schema.JobRecord> events = new ArrayList<>();

    zeebeHazelcast = ZeebeHazelcast.newBuilder(hz).addJobListener(events::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Collections.singletonMap("foo", 123))
            .send()
            .join();

    TestUtil.waitUntil(() -> events.size() >= 1);

    assertThat(events)
        .hasSize(1)
        .extracting(r -> r.getMetadata().getIntent())
        .containsExactly("CREATED");
  }

  @Test
  public void shouldExportIncidentEvents() {
    final List<Schema.IncidentRecord> events = new ArrayList<>();

    zeebeHazelcast = ZeebeHazelcast.newBuilder(hz).addIncidentListener(events::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    TestUtil.waitUntil(() -> events.size() >= 1);

    assertThat(events)
            .hasSize(1)
            .extracting(r -> r.getMetadata().getIntent())
            .containsExactly("CREATED");
  }

  @Test
  public void shouldIncrementSequence() {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.WorkflowInstanceRecord> wfRecords = new ArrayList<>();

    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .addDeploymentListener(deploymentRecords::add)
                    .addWorkflowInstanceListener(wfRecords::add)
                    .build();

    final var sequence1 = zeebeHazelcast.getSequence();

    // when
    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    TestUtil.waitUntil(() -> deploymentRecords.size() >= 2);
    final var sequence2 = zeebeHazelcast.getSequence();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    TestUtil.waitUntil(() -> wfRecords.size() >= 4);
    final var sequence3 = zeebeHazelcast.getSequence();

    // then
    assertThat(sequence2).isGreaterThan(sequence1);
    assertThat(sequence3).isGreaterThan(sequence2);
  }

  @Test
  public void shouldReadFromHead() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.WorkflowInstanceRecord> wfRecords = new ArrayList<>();

    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .addDeploymentListener(deploymentRecords::add)
                    .readFromHead()
                    .build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();
    TestUtil.waitUntil(() -> deploymentRecords.size() >= 2);

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .addDeploymentListener(deploymentRecords::add)
                    .addWorkflowInstanceListener(wfRecords::add)
                    .readFromHead()
                    .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();
    TestUtil.waitUntil(() -> wfRecords.size() >= 4);

    // then
    assertThat(deploymentRecords).hasSize(2);
  }

  @Test
  public void shouldReadFromTail() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.WorkflowInstanceRecord> wfRecords = new ArrayList<>();

    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz).addDeploymentListener(deploymentRecords::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();
    TestUtil.waitUntil(() -> deploymentRecords.size() >= 2);

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .addDeploymentListener(deploymentRecords::add)
                    .addWorkflowInstanceListener(wfRecords::add)
                    .readFromTail()
                    .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();
    TestUtil.waitUntil(() -> wfRecords.size() >= 4);

    // then
    assertThat(deploymentRecords).hasSize(1);
  }

  @Test
  public void shouldReadFromSequence() throws Exception {
    // given
    final List<Schema.DeploymentRecord> deploymentRecords = new ArrayList<>();
    final List<Schema.WorkflowInstanceRecord> wfRecords = new ArrayList<>();

    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz).addDeploymentListener(deploymentRecords::add).build();

    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();
    TestUtil.waitUntil(() -> deploymentRecords.size() >= 2);

    final var sequence = zeebeHazelcast.getSequence();

    zeebeHazelcast.close();
    deploymentRecords.clear();

    // when
    zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .addDeploymentListener(deploymentRecords::add)
                    .addWorkflowInstanceListener(wfRecords::add)
                    .readFrom(sequence)
                    .build();

    client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();
    TestUtil.waitUntil(() -> wfRecords.size() >= 4);

    // then
    assertThat(deploymentRecords).isEmpty();
  }
}
