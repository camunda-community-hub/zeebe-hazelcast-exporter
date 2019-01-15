package io.zeebe.hazelcast.connect.java;

import io.zeebe.exporter.proto.Schema;
import java.util.function.Consumer;

public class JobEventListener extends ZeebeHazelcastListener<Schema.JobRecord> {

  public JobEventListener(Consumer<Schema.JobRecord> consumer) {
    super(consumer);
  }

  @Override
  protected Schema.JobRecord toProtobufMessage(byte[] message) throws Exception {
    return Schema.JobRecord.parseFrom(message);
  }
}
