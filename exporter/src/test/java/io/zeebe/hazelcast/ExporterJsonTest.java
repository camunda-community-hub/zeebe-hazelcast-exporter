package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.test.ZeebeTestRule;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ExporterJsonTest {

  private static final BpmnModelInstance WORKFLOW =
      Bpmn.createExecutableProcess("process")
          .startEvent("start")
          .sequenceFlowId("to-task")
          .serviceTask("task", s -> s.zeebeJobType("test"))
          .sequenceFlowId("to-end")
          .endEvent("end")
          .done();

  private static final ExporterConfiguration CONFIGURATION = new ExporterConfiguration();

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("application-json.yaml", Properties::new);

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
    // given
    final Ringbuffer<byte[]> buffer = hz.getRingbuffer(CONFIGURATION.getName());

    var sequence = buffer.headSequence();

    // when
    client.newDeployCommand().addProcessModel(WORKFLOW, "process.bpmn").send().join();

    // then
    final var message = buffer.readOne(sequence);
    assertThat(message).isNotNull();

    final var jsonRecord = new String(message);

    assertThat(jsonRecord)
        .startsWith("{")
        .endsWith("}")
        .contains("\"valueType\":\"DEPLOYMENT\"")
        .contains("\"recordType\":\"COMMAND\"")
        .contains("\"intent\":\"CREATE\"");
  }
}
