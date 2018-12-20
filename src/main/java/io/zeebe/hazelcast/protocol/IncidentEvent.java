package io.zeebe.hazelcast.protocol;

public interface IncidentEvent extends ZeebeEvent {

  String getErrorType();

  String getErrorMessage();

  String getBpmnProcessId();

  String getElementId();

  long getWorkflowInstanceKey();

  long getElementInstanceKey();

  long getJobKey();
}
