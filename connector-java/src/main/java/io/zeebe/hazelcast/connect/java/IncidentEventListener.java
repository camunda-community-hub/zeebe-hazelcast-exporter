package io.zeebe.hazelcast.connect.java;

import io.zeebe.exporter.proto.Schema;
import java.util.function.Consumer;

public class IncidentEventListener extends ZeebeHazelcastListener<Schema.IncidentRecord> {

  public IncidentEventListener(Consumer<Schema.IncidentRecord> consumer) {
    super(consumer);
  }

  @Override
  protected Schema.IncidentRecord toProtobufMessage(byte[] message) throws Exception {
    return Schema.IncidentRecord.parseFrom(message);
  }
}
