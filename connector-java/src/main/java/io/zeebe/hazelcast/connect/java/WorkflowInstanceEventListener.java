package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.WorkflowInstanceEvent;
import io.zeebe.hazelcast.protocol.WorkflowInstanceRecord;
import java.util.function.Consumer;

public class WorkflowInstanceEventListener extends ZeebeHazelcastListener<WorkflowInstanceEvent> {

  public WorkflowInstanceEventListener(Consumer<WorkflowInstanceEvent> consumer) {
    super(WorkflowInstanceRecord.class, consumer);
  }
}
