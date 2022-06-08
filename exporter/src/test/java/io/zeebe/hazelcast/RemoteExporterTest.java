package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.hazelcast.testcontainers.ZeebeTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
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

    @Container
    public ZeebeTestContainer zeebeContainer = ZeebeTestContainer.withHazelcastContainer();

    private HazelcastInstance hzClient;

    @BeforeEach
    public void init() {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(zeebeContainer.getHazelcastContainer().getHazelcastServerExternalAddress());
        hzClient = HazelcastClient.newHazelcastClient(clientConfig);
    }

    @AfterEach
    public void cleanUp() {
        hzClient.shutdown();
    }

    @Test
    public void shouldExportEventsAsProtobuf() throws Exception {
        // given
        final Ringbuffer<byte[]> buffer = hzClient.getRingbuffer(CONFIGURATION.getName());

        var sequence = buffer.headSequence();

        // when
        zeebeContainer.getClient().newDeployResourceCommand().addProcessModel(WORKFLOW, "process.bpmn").send().join();

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
