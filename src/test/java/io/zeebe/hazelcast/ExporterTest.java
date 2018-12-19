package io.zeebe.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import io.zeebe.hazelcast.protocol.WorkflowInstanceRecord;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.test.ZeebeTestRule;
import io.zeebe.test.util.TestUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;

public class ExporterTest {

  @Rule
  public final ZeebeTestRule testRule = new ZeebeTestRule("zeebe.test.cfg.toml", Properties::new);

  private ZeebeClient client;

  @Test
  public void test() {

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    final HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

    final List<String> messages = new ArrayList<>();

    final ITopic<String> topic = hz.getTopic(new ExporterConfiguration().workflowInstanceTopic);
    topic.addMessageListener(m -> messages.add(m.getMessageObject()));

    client = testRule.getClient();

    client
        .workflowClient()
        .newDeployCommand()
        .addWorkflowModel(
            Bpmn.createExecutableProcess("process")
                .startEvent("start")
                .sequenceFlowId("to-end")
                .endEvent("end")
                .done(),
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

    TestUtil.waitUntil(() -> messages.size() == 7);

    final ObjectMapper objectMapper = new ObjectMapper();

    final List<WorkflowInstanceRecord> records =
        messages
            .stream()
            .map(
                m -> {
                  try {
                    return objectMapper.readValue(m, WorkflowInstanceRecord.class);
                  } catch (IOException e1) {
                    e1.printStackTrace();
                    return null;
                  }
                })
            .collect(Collectors.toList());

    assertThat(records)
        .hasSize(7)
        .extracting(r -> tuple(r.getElementId(), r.getIntent()))
        .containsExactly(
            tuple("process", "ELEMENT_READY"),
            tuple("process", "ELEMENT_ACTIVATED"),
            tuple("start", "START_EVENT_OCCURRED"),
            tuple("to-end", "SEQUENCE_FLOW_TAKEN"),
            tuple("end", "END_EVENT_OCCURRED"),
            tuple("process", "ELEMENT_COMPLETING"),
            tuple("process", "ELEMENT_COMPLETED"));
  }
}
