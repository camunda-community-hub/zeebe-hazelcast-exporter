package io.zeebe.hazelcast.protocol;

public class WorkflowRecord {

  private String bpmnProcessId;
  private long workflowKey;
  private int version;

  private String resourceName;
  private byte[] resource;

  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public void setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  public long getWorkflowKey() {
    return workflowKey;
  }

  public void setWorkflowKey(long workflowKey) {
    this.workflowKey = workflowKey;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public byte[] getResource() {
    return resource;
  }

  public void setResource(byte[] resource) {
    this.resource = resource;
  }
}
