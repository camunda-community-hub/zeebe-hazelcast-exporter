package io.zeebe.hazelcast.protocol;

import java.util.Map;

public interface WorkflowInstanceEvent extends ZeebeEvent {

  String getBpmnProcessId();

  String getElementId();

  int getVersion();

  long getWorkflowKey();

  long getWorkflowInstanceKey();

  long getScopeInstanceKey();

  Map<String, Object> getPayload();
}
