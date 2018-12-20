package io.zeebe.hazelcast.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.exporter.record.RecordValue;
import io.zeebe.exporter.record.value.DeploymentRecordValue;
import io.zeebe.exporter.record.value.IncidentRecordValue;
import io.zeebe.exporter.record.value.JobRecordValue;
import io.zeebe.exporter.record.value.WorkflowInstanceRecordValue;
import io.zeebe.exporter.record.value.deployment.DeploymentResource;
import io.zeebe.exporter.spi.Exporter;
import io.zeebe.hazelcast.protocol.BaseRecord;
import io.zeebe.hazelcast.protocol.DeploymentRecord;
import io.zeebe.hazelcast.protocol.IncidentRecord;
import io.zeebe.hazelcast.protocol.JobRecord;
import io.zeebe.hazelcast.protocol.WorkflowInstanceRecord;
import io.zeebe.hazelcast.protocol.WorkflowMetadata;
import io.zeebe.hazelcast.protocol.WorkflowRecord;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class HazelcastExporter implements Exporter {

  private static final EnumMap<ValueType, Function<RecordValue, BaseRecord>> TRANSFORMERS =
      new EnumMap<>(ValueType.class);

  static {
    TRANSFORMERS.put(
        ValueType.WORKFLOW_INSTANCE,
        v -> transformWorkflowInstance((WorkflowInstanceRecordValue) v));
    TRANSFORMERS.put(ValueType.JOB, v -> transformJob((JobRecordValue) v));
    TRANSFORMERS.put(ValueType.INCIDENT, v -> transformIncident((IncidentRecordValue) v));
    TRANSFORMERS.put(ValueType.DEPLOYMENT, v -> transformDeployment((DeploymentRecordValue) v));
  }

  private final EnumMap<ValueType, ITopic<String>> topics = new EnumMap<>(ValueType.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  private ExporterConfiguration config;
  private Logger logger;
  private Controller controller;

  private HazelcastInstance hz;

  @Override
  public void configure(Context context) {
    logger = context.getLogger();
    config = context.getConfiguration().instantiate(ExporterConfiguration.class);

    logger.debug("Starting exporter with configuration: {}", config);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;

    final Config cfg = new Config();
    cfg.setProperty("hazelcast.logging.type", "slf4j");

    hz = Hazelcast.newHazelcastInstance(cfg);

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
    final ValueType valueType = metadata.getValueType();

    if (metadata.getRecordType() == RecordType.EVENT) {

      final ITopic<String> topic = topics.get(valueType);
      final Function<RecordValue, BaseRecord> transformer =
          TRANSFORMERS.get(metadata.getValueType());

      if (topic != null && transformer != null) {

        final String json = transformRecord(record, transformer);
        topic.publish(json);
      }
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }

  private String transformRecord(
      Record record, final Function<RecordValue, BaseRecord> transformer) {
    final BaseRecord dto = transformer.apply(record.getValue());
    transformMetadata(record, dto);

    try {
      return objectMapper.writeValueAsString(dto);

    } catch (JsonProcessingException e) {
      logger.error("Fail to transform record to JSON", e);

      throw new RuntimeException("Hazelcast exporter: fail to transform record", e);
    }
  }

  private static WorkflowInstanceRecord transformWorkflowInstance(
      WorkflowInstanceRecordValue value) {
    final WorkflowInstanceRecord record = new WorkflowInstanceRecord();
    record.setBpmnProcessId(value.getBpmnProcessId());
    record.setVersion(value.getVersion());
    record.setWorkflowKey(value.getWorkflowKey());
    record.setWorkflowInstanceKey(value.getWorkflowInstanceKey());
    record.setElementId(value.getElementId());
    record.setScopeInstanceKey(value.getScopeInstanceKey());
    record.setPayload(value.getPayloadAsMap());
    return record;
  }

  private static IncidentRecord transformIncident(IncidentRecordValue incident) {
    final IncidentRecord record = new IncidentRecord();
    record.setErrorType(incident.getErrorType());
    record.setErrorMessage(incident.getErrorMessage());
    record.setWorkflowInstanceKey(incident.getWorkflowInstanceKey());
    record.setElementInstanceKey(incident.getElementInstanceKey());
    record.setElementId(incident.getElementId());
    record.setJobKey(incident.getJobKey());
    return record;
  }

  private static DeploymentRecord transformDeployment(DeploymentRecordValue deployment) {
    final DeploymentRecord record = new DeploymentRecord();

    final Map<String, byte[]> resourcesByName =
        deployment
            .getResources()
            .stream()
            .collect(
                Collectors.toMap(
                    DeploymentResource::getResourceName, DeploymentResource::getResource));

    final List<WorkflowMetadata> workflowRecords =
        deployment
            .getDeployedWorkflows()
            .stream()
            .map(
                w -> {
                  final WorkflowRecord workflowRecord = new WorkflowRecord();
                  workflowRecord.setBpmnProcessId(w.getBpmnProcessId());
                  workflowRecord.setVersion(w.getVersion());
                  workflowRecord.setWorkflowKey(w.getWorkflowKey());
                  workflowRecord.setResourceName(w.getResourceName());
                  workflowRecord.setResource(resourcesByName.get(w.getResourceName()));
                  return workflowRecord;
                })
            .collect(Collectors.toList());
    record.setDeployedWorkflows(workflowRecords);

    return record;
  }

  private static JobRecord transformJob(JobRecordValue job) {
    final JobRecord record = new JobRecord();
    record.setType(job.getType());
    record.setWorker(job.getWorker());
    record.setRetries(job.getRetries());
    record.setErrorMessage(job.getErrorMessage());
    record.setCustomHeaders(job.getCustomHeaders());
    record.setPayload(job.getPayloadAsMap());

    final Instant deadline = job.getDeadline();
    if (deadline != null) {
      record.setDeadline(deadline.toEpochMilli());
    }

    return record;
  }

  private static void transformMetadata(Record<?> record, final BaseRecord dto) {
    dto.setKey(record.getKey());
    dto.setIntent(record.getMetadata().getIntent().name());
    dto.setTimestamp(record.getTimestamp().toEpochMilli());
  }
}
