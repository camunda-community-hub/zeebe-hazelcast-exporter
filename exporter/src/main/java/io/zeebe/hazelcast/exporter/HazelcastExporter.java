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
import org.slf4j.Logger;

import java.util.function.Function;

public class HazelcastExporter implements Exporter {

  private ExporterConfiguration config;
  private Logger logger;
  private Controller controller;

  private HazelcastInstance hazelcast;
  private Ringbuffer<byte[]> ringbuffer;

  private Function<Record, byte[]> recordTransformer;

  @Override
  public void configure(Context context) {
    logger = context.getLogger();
    config = context.getConfiguration().instantiate(ExporterConfiguration.class);

    logger.debug("Starting exporter with configuration: {}", config);

    final var filter = new HazelcastRecordFilter(config);
    context.setFilter(filter);

    configureFormat();
  }

  private void configureFormat() {
    final var format = config.getFormat();
    if (format.equalsIgnoreCase("protobuf")) {
      recordTransformer = this::recordToProtobuf;

    } else if (format.equalsIgnoreCase("json")) {
      recordTransformer = this::recordToJson;

    } else {
      throw new IllegalArgumentException(
              String.format(
                      "Expected the parameter 'format' to be one fo 'protobuf' or 'json' but was '%s'",
                      format));
    }
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;

    final Config cfg = buildHazelcastConfig();
    hazelcast = Hazelcast.newHazelcastInstance(cfg);

    ringbuffer = hazelcast.getRingbuffer(config.getName());
    if (ringbuffer == null) {
      throw new IllegalStateException(
              String.format("Failed to open ringbuffer with name '%s'", config.getName()));
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
    cfg.getNetworkConfig().setPort(config.getPort());
    cfg.setProperty("hazelcast.logging.type", "slf4j");

    final var ringbufferConfig = new RingbufferConfig(config.getName());

    if (config.getCapacity() > 0) {
      ringbufferConfig.setCapacity(config.getCapacity());
    }
    if (config.getTimeToLiveInSeconds() > 0) {
      ringbufferConfig.setTimeToLiveSeconds(config.getTimeToLiveInSeconds());
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
      final byte[] transformedRecord = recordTransformer.apply(record);
      ringbuffer.add(transformedRecord);
    }

    controller.updateLastExportedRecordPosition(record.getPosition());
  }

  private byte[] recordToProtobuf(Record record) {
    final Schema.Record dto = RecordTransformer.toGenericRecord(record);
    return dto.toByteArray();
  }

  private byte[] recordToJson(Record record) {
    final var json = record.toJson();
    return json.getBytes();
  }
}
