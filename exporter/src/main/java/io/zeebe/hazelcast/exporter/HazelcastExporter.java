package io.zeebe.hazelcast.exporter;

import com.google.protobuf.GeneratedMessageV3;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class HazelcastExporter implements Exporter {
  private final EnumMap<ValueType, ITopic<byte[]>> topics = new EnumMap<>(ValueType.class);

  private final List<ValueType> enabledValueTypes = new ArrayList<>();

  private ExporterConfiguration config;
  private Logger logger;
  private Controller controller;

  private HazelcastInstance hazelcast;

  @Override
  public void configure(Context context) {
    logger = context.getLogger();
    config = context.getConfiguration().instantiate(ExporterConfiguration.class);

    logger.debug("Starting exporter with configuration: {}", config);

    final List<RecordType> enabledRecordTypes =
        parseList(config.enabledRecordTypes).map(RecordType::valueOf).collect(Collectors.toList());

    parseList(config.enabledValueTypes).map(ValueType::valueOf).forEach(enabledValueTypes::add);

    context.setFilter(
        new Context.RecordFilter() {

          @Override
          public boolean acceptType(RecordType recordType) {
            return enabledRecordTypes.contains(recordType);
          }

          @Override
          public boolean acceptValue(ValueType valueType) {
            return enabledValueTypes.contains(valueType);
          }
        });

    if (!config.updatePosition) {
      logger.warn(
              "The exporter is configured to not update its position. "
                      + "Because of this, the broker can't delete data and may run out of disk space!");
    }
  }

  private Stream<String> parseList(String list) {
    return Arrays.stream(list.split(",")).map(String::trim).map(String::toUpperCase);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;

    final Config cfg = new Config();
    cfg.getNetworkConfig().setPort(config.port);
    cfg.setProperty("hazelcast.logging.type", "slf4j");

    hazelcast = Hazelcast.newHazelcastInstance(cfg);

    enabledValueTypes.forEach(
        valueType -> {
          final String topicName = config.getTopicName(valueType);
          topics.put(valueType, hazelcast.getTopic(topicName));

          logger.debug("Export records of type '{}' to Hazelcast topic '{}'", valueType, topicName);
        });
  }

  @Override
  public void close() {
    hazelcast.shutdown();
  }

  @Override
  public void export(Record record) {

    final ITopic<byte[]> topic = topics.get(record.getValueType());
    if (topic != null) {

      final byte[] protobuf = transformRecord(record);
      topic.publish(protobuf);
    }

    if (config.updatePosition) {
      controller.updateLastExportedRecordPosition(record.getPosition());
    }
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
