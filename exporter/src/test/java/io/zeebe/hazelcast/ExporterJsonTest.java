package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.hazelcast.testcontainers.ZeebeTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
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

    @Container
    public ZeebeTestContainer zeebeContainer = ZeebeTestContainer.withJsonFormat();

    private HazelcastInstance hz;

    @BeforeEach
    public void init() {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(zeebeContainer.getHazelcastAddress());
        hz = HazelcastClient.newHazelcastClient(clientConfig);
    }

    @AfterEach
    public void cleanUp() {
        hz.shutdown();
    }

    @Test
    public void shouldExportEventsAsProtobuf() throws Exception {
        // given
        final Ringbuffer<byte[]> buffer = hz.getRingbuffer(CONFIGURATION.getName());

        var sequence = buffer.headSequence();

        // when
        zeebeContainer.getClient().newDeployResourceCommand().addProcessModel(WORKFLOW, "process.bpmn").send().join();

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
