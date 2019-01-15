package io.zeebe.hazelcast.connect.java;

import io.zeebe.exporter.proto.Schema;
import java.util.function.Consumer;

public class DeploymentEventListener extends ZeebeHazelcastListener<Schema.DeploymentRecord> {

  public DeploymentEventListener(Consumer<Schema.DeploymentRecord> consumer) {
    super(consumer);
  }

  @Override
  protected Schema.DeploymentRecord toProtobufMessage(byte[] message) throws Exception {
    return Schema.DeploymentRecord.parseFrom(message);
  }
}
