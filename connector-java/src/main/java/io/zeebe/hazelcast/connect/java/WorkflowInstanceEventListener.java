package io.zeebe.hazelcast.connect.java;

import io.zeebe.exporter.proto.Schema;
import java.util.function.Consumer;

public class WorkflowInstanceEventListener
    extends ZeebeHazelcastListener<Schema.WorkflowInstanceRecord> {

  public WorkflowInstanceEventListener(Consumer<Schema.WorkflowInstanceRecord> consumer) {
    super(consumer);
  }

  @Override
  protected Schema.WorkflowInstanceRecord toProtobufMessage(byte[] message) throws Exception {
    return Schema.WorkflowInstanceRecord.parseFrom(message);
  }
}
