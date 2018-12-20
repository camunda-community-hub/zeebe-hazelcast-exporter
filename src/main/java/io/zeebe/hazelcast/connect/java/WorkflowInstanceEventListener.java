package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.WorkflowInstanceRecord;
import java.util.function.Consumer;

public class WorkflowInstanceEventListener extends ZeebeHazelcastListener<WorkflowInstanceRecord> {

  public WorkflowInstanceEventListener(Consumer<WorkflowInstanceRecord> consumer) {
    super(WorkflowInstanceRecord.class, consumer);
  }
}
