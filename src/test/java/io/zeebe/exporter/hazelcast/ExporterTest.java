package io.zeebe.exporter.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.test.ZeebeTestRule;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;

public class ExporterTest {

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("zeebe.test.cfg.toml", Properties::new);

  private ZeebeClient client;

  @Test
  public void test() {

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

    final ITopic<String> topic = hz.getTopic(new ExporterConfiguration().workflowInstanceTopic);
    topic.addMessageListener(
        message -> {
          System.out.println("> " + message.getMessageObject());
        });

    client = testRule.getClient();

    client
        .workflowClient()
        .newDeployCommand()
        .addWorkflowModel(
            Bpmn.createExecutableProcess("process").startEvent("start").endEvent("end").done(),
            "process.bpmn")
        .send()
        .join();

    final WorkflowInstanceEvent workflowInstance =
        client
            .workflowClient()
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .send()
            .join();

    ZeebeTestRule.assertThat(workflowInstance).isEnded();
  }
}
