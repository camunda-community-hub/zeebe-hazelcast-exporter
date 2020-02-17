package io.zeebe.hazelcast.exporter;

import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HazelcastExporter implements Exporter {

  private ExporterConfiguration config;
  private Logger logger;
  private Controller controller;

  private HazelcastInstance hazelcast;
  private Ringbuffer<byte[]> ringbuffer;

  @Override
  public void configure(Context context) {
    logger = context.getLogger();
    config = context.getConfiguration().instantiate(ExporterConfiguration.class);

    logger.debug("Starting exporter with configuration: {}", config);

    final List<RecordType> enabledRecordTypes =
            parseList(config.enabledRecordTypes).map(RecordType::valueOf).collect(Collectors.toList());

    final List<ValueType> enabledValueTypes =
            parseList(config.enabledValueTypes).map(ValueType::valueOf).collect(Collectors.toList());

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
  }

  private Stream<String> parseList(String list) {
    return Arrays.stream(list.split(",")).map(String::trim).map(String::toUpperCase);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;

    final Config cfg = buildHazelcastConfig();
    hazelcast = Hazelcast.newHazelcastInstance(cfg);

    ringbuffer = hazelcast.getRingbuffer(config.name);
    if (ringbuffer == null) {
      throw new IllegalStateException(
              String.format("Failed to open ringbuffer with name '%s'", config.name));
    }

    logger.info(
            "Export records to ringbuffer with name '{}' [head: {}, tail: {}, size: {}, capacity: {}]",
            ringbuffer.getName(),
            ringbuffer.headSequence(),
            ringbuffer.tailSequence(),
            ringbuffer.size(),
            ringbuffer.capacity());
  }

  private Config buildHazelcastConfig() {

    final Config cfg = new Config();
    cfg.getNetworkConfig().setPort(config.port);
    cfg.setProperty("hazelcast.logging.type", "slf4j");

    final var ringbufferConfig = new RingbufferConfig(config.name);

    if (config.capacity > 0) {
      ringbufferConfig.setCapacity(config.capacity);
    }
    if (config.timeToLiveInSeconds > 0) {
      ringbufferConfig.setTimeToLiveSeconds(config.timeToLiveInSeconds);
    }

    cfg.addRingBufferConfig(ringbufferConfig);

    return cfg;
  }

  @Override
  public void close() {
    hazelcast.shutdown();
  }

  @Override
  public void export(Record record) {

    if (ringbuffer != null) {
      final byte[] protobuf = transformRecord(record);
      ringbuffer.add(protobuf);
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }

  private byte[] transformRecord(Record record) {
    final Schema.Record dto = RecordTransformer.toGenericRecord(record);
    return dto.toByteArray();
  }
}
