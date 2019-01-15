package io.zeebe.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.client.ZeebeClient;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.ZeebeTestRule;
import io.zeebe.test.util.TestUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ExporterTest {

  private static final BpmnModelInstance WORKFLOW =
      Bpmn.createExecutableProcess("process")
          .startEvent("start")
          .sequenceFlowId("to-task")
          .serviceTask("task", s -> s.zeebeTaskType("test").zeebeInput("$.foo", "$.bar"))
          .sequenceFlowId("to-end")
          .endEvent("end")
          .done();

  private static final ExporterConfiguration CONFIGURATION = new ExporterConfiguration();

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("zeebe.test.cfg.toml", Properties::new);

  private ZeebeClient client;
  private HazelcastInstance hz;

  @Before
  public void init() {
    client = testRule.getClient();

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    hz = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @After
  public void cleanUp() {
    hz.shutdown();
  }

  @Test
  public void shouldExportEventsAsProtobuf() throws Exception {
    final List<byte[]> messages = new ArrayList<>();

    final ITopic<byte[]> topic = hz.getTopic(CONFIGURATION.deploymentTopic);
    topic.addMessageListener(m -> messages.add(m.getMessageObject()));

    client
        .workflowClient()
        .newDeployCommand()
        .addWorkflowModel(WORKFLOW, "process.bpmn")
        .send()
        .join();

    TestUtil.waitUntil(() -> messages.size() > 0);

    byte[] message = messages.get(0);

    final Schema.DeploymentRecord deploymentRecord = Schema.DeploymentRecord.parseFrom(message);
    final Schema.DeploymentRecord.Resource resource = deploymentRecord.getResources(0);
    assertThat(resource.getResourceName()).isEqualTo("process.bpmn");
  }
}
