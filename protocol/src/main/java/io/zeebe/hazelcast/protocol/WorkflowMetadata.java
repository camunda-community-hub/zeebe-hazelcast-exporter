package io.zeebe.hazelcast.protocol;

public interface WorkflowMetadata {

  String getBpmnProcessId();

  long getWorkflowKey();

  int getVersion();

  String getResourceName();

  byte[] getResource();
}