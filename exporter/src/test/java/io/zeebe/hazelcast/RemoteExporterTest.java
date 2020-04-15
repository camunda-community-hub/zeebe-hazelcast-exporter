package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.zeebe.client.ZeebeClient;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.ZeebeTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteExporterTest {

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
  public final ZeebeTestRule testRule = new ZeebeTestRule("application-remote.yaml", Properties::new);

  private ZeebeClient client;
  private HazelcastInstance hzInstance;
  private HazelcastInstance hzClient;

  @Before
  public void init() {
    client = testRule.getClient();

    final Config config = new Config();
    config.getNetworkConfig().setPort(5702);
    hzInstance = Hazelcast.newHazelcastInstance(config);

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5702");
    hzClient = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @After
  public void cleanUp() {
    hzClient.shutdown();
    hzInstance.shutdown();
  }

  @Test
  public void shouldExportEventsAsProtobuf() throws Exception {
    // given
    final Ringbuffer<byte[]> buffer = hzClient.getRingbuffer(CONFIGURATION.getName());

    var sequence = buffer.headSequence();

    // when
    client.newDeployCommand().addWorkflowModel(WORKFLOW, "process.bpmn").send().join();

    // then
    final var message = buffer.readOne(sequence);
    assertThat(message).isNotNull();

    final var record = Schema.Record.parseFrom(message);
    assertThat(record.getRecord().is(Schema.DeploymentRecord.class)).isTrue();

    final var deploymentRecord = record.getRecord().unpack(Schema.DeploymentRecord.class);
    final Schema.DeploymentRecord.Resource resource = deploymentRecord.getResources(0);
    assertThat(resource.getResourceName()).isEqualTo("process.bpmn");
  }
}
