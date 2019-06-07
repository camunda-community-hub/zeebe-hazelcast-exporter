package io.zeebe.hazelcast.exporter;

import com.google.protobuf.GeneratedMessageV3;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.api.record.Record;
import io.zeebe.exporter.api.record.RecordMetadata;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.protocol.RecordType;
import io.zeebe.protocol.ValueType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import org.slf4j.Logger;

public class HazelcastExporter implements Exporter {
  private final EnumMap<ValueType, ITopic<byte[]>> topics = new EnumMap<>(ValueType.class);

  private ExporterConfiguration config;
  private Logger logger;
  private Controller controller;

  private HazelcastInstance hazelcast;

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

    hazelcast = Hazelcast.newHazelcastInstance(cfg);

    topics.put(ValueType.DEPLOYMENT, hazelcast.getTopic(config.deploymentTopic));
    topics.put(ValueType.WORKFLOW_INSTANCE, hazelcast.getTopic(config.workflowInstanceTopic));
    topics.put(ValueType.JOB, hazelcast.getTopic(config.jobTopic));
    topics.put(ValueType.INCIDENT, hazelcast.getTopic(config.incidentTopic));
  }

  @Override
  public void close() {
    hazelcast.shutdown();
  }

  @Override
  public void export(Record record) {
    final RecordMetadata metadata = record.getMetadata();
    final ValueType valueType = metadata.getValueType();

    if (metadata.getRecordType() == RecordType.EVENT) {

      final ITopic<byte[]> topic = topics.get(valueType);
      if (topic != null) {

        final byte[] json = transformRecord(record);
        topic.publish(json);
      }
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }

  private byte[] transformRecord(Record record) {
    final GeneratedMessageV3 dto = RecordTransformer.toProtobufMessage(record);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      dto.writeTo(outputStream);
      return outputStream.toByteArray();
    } catch (IOException ioe) {
      final String exceptionMsg =
          String.format("Failed to write %s to byte array output stream.", dto.toString());
      logger.error(exceptionMsg, ioe);
      throw new RuntimeException(exceptionMsg, ioe);
    }
  }
}
