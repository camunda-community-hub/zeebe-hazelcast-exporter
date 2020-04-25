package io.zeebe.hazelcast.exporter;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
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

    hazelcast =
            config
                    .getRemoteAddress()
                    .map(this::connectToHazelcast)
                    .orElseGet(this::createHazelcastInstance);

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

  private HazelcastInstance createHazelcastInstance() {
    final var port = this.config.getPort();

    final var hzConfig = new Config();
    hzConfig.getNetworkConfig().setPort(port);
    hzConfig.setProperty("hazelcast.logging.type", "slf4j");

    final var ringbufferConfig = new RingbufferConfig(this.config.getName());

    if (this.config.getCapacity() > 0) {
      ringbufferConfig.setCapacity(this.config.getCapacity());
    }
    if (this.config.getTimeToLiveInSeconds() > 0) {
      ringbufferConfig.setTimeToLiveSeconds(this.config.getTimeToLiveInSeconds());
    }

    hzConfig.addRingBufferConfig(ringbufferConfig);

    logger.info("Creating new in-memory Hazelcast instance [port: {}]", port);

    return Hazelcast.newHazelcastInstance(hzConfig);
  }

  private HazelcastInstance connectToHazelcast(String remoteAddress) {

    final var clientConfig = new ClientConfig();
    clientConfig.setProperty("hazelcast.logging.type", "slf4j");

    final var networkConfig = clientConfig.getNetworkConfig();
    networkConfig.addAddress(remoteAddress);

    logger.info("Connecting to remote Hazelcast instance [address: {}]", remoteAddress);

    return HazelcastClient.newHazelcastClient(clientConfig);
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
