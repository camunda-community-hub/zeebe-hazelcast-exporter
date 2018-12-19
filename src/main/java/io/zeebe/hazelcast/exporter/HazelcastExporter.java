package io.zeebe.hazelcast.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.exporter.record.value.WorkflowInstanceRecordValue;
import io.zeebe.exporter.spi.Exporter;
import io.zeebe.hazelcast.protocol.BaseRecord;
import io.zeebe.hazelcast.protocol.WorkflowInstanceRecord;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import java.util.EnumMap;

public class HazelcastExporter implements Exporter {

  private final EnumMap<ValueType, ITopic<String>> topics = new EnumMap<>(ValueType.class);

  private ExporterConfiguration config;
  private Controller controller;

  private HazelcastInstance hz;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void configure(Context context) {
    config = context.getConfiguration().instantiate(ExporterConfiguration.class);

    context.getLogger().debug("Starting exporter with configuration: {}", config);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;

    hz = Hazelcast.newHazelcastInstance();

    topics.put(ValueType.DEPLOYMENT, hz.getTopic(config.deploymentTopic));
    topics.put(ValueType.WORKFLOW_INSTANCE, hz.getTopic(config.workflowInstanceTopic));
    topics.put(ValueType.JOB, hz.getTopic(config.jobTopic));
    topics.put(ValueType.INCIDENT, hz.getTopic(config.incidentTopic));
  }

  @Override
  public void close() {
    hz.shutdown();
  }

  @Override
  public void export(Record record) {
    final RecordMetadata metadata = record.getMetadata();

    if (metadata.getRecordType() == RecordType.EVENT) {
      final ITopic<String> topic = topics.get(metadata.getValueType());
      if (topic != null) {

        final BaseRecord dto = transform(record);
        if (dto != null) {

          try {
            final String json = objectMapper.writeValueAsString(dto);
            topic.publish(json);

          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        }
      }
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }

  @SuppressWarnings("unchecked")
  private BaseRecord transform(Record<?> record) {
    switch (record.getMetadata().getValueType()) {
      case WORKFLOW_INSTANCE:
        return transformWorkflowInstance((Record<WorkflowInstanceRecordValue>) record);
      default:
        return null;
    }
  }

  private static WorkflowInstanceRecord transformWorkflowInstance(
      Record<WorkflowInstanceRecordValue> record) {
    final WorkflowInstanceRecord dto = new WorkflowInstanceRecord();
    dto.setKey(record.getKey());
    dto.setIntent(record.getMetadata().getIntent().name());
    dto.setTimestamp(record.getTimestamp().toEpochMilli());

    final WorkflowInstanceRecordValue value = record.getValue();
    dto.setBpmnProcessId(value.getBpmnProcessId());
    dto.setVersion(value.getVersion());
    dto.setWorkflowKey(value.getWorkflowKey());
    dto.setWorkflowInstanceKey(value.getWorkflowInstanceKey());
    dto.setElementId(value.getElementId());
    dto.setScopeInstanceKey(value.getScopeInstanceKey());
    dto.setPayload(value.getPayloadAsMap());
    return dto;
  }
}
