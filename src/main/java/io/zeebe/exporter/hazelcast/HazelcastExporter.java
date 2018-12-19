package io.zeebe.exporter.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.exporter.spi.Exporter;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import java.util.EnumMap;

public class HazelcastExporter implements Exporter {

  private ExporterConfiguration config;
  private Controller controller;

  private HazelcastInstance hz;
  private final EnumMap<ValueType, ITopic<String>> topics = new EnumMap<>(ValueType.class);

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
        topic.publish(record.toJson());
      }
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }
}
