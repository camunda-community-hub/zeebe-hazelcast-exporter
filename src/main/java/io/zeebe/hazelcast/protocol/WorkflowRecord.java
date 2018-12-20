package io.zeebe.hazelcast.protocol;

public class WorkflowRecord implements WorkflowMetadata {

  private String bpmnProcessId;
  private long workflowKey;
  private int version;

  private String resourceName;
  private byte[] resource;

  @Override
public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public void setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  @Override
public long getWorkflowKey() {
    return workflowKey;
  }

  public void setWorkflowKey(long workflowKey) {
    this.workflowKey = workflowKey;
  }

  @Override
public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  @Override
public byte[] getResource() {
    return resource;
  }

  public void setResource(byte[] resource) {
    this.resource = resource;
  }
}
