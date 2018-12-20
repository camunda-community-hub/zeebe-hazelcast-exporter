package io.zeebe.hazelcast.protocol;

import java.util.Map;

public class WorkflowInstanceRecord extends BaseRecord implements WorkflowInstanceEvent {

  private String bpmnProcessId;
  private String elementId;
  private int version;
  private long workflowKey;
  private long workflowInstanceKey;
  private long scopeInstanceKey;
  private Map<String, Object> payload;

  @Override
  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public void setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  @Override
  public String getElementId() {
    return elementId;
  }

  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  @Override
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public long getWorkflowKey() {
    return workflowKey;
  }

  public void setWorkflowKey(long workflowKey) {
    this.workflowKey = workflowKey;
  }

  @Override
  public long getWorkflowInstanceKey() {
    return workflowInstanceKey;
  }

  public void setWorkflowInstanceKey(long workflowInstanceKey) {
    this.workflowInstanceKey = workflowInstanceKey;
  }

  @Override
  public long getScopeInstanceKey() {
    return scopeInstanceKey;
  }

  public void setScopeInstanceKey(long scopeInstanceKey) {
    this.scopeInstanceKey = scopeInstanceKey;
  }

  @Override
  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }

  @Override
  public String toString() {
    return "WorkflowInstanceEvent [key="
        + getKey()
        + ", intent="
        + getIntent()
        + ", timestamp="
        + getTimestamp()
        + ", bpmnProcessId="
        + bpmnProcessId
        + ", elementId="
        + elementId
        + ", version="
        + version
        + ", workflowKey="
        + workflowKey
        + ", workflowInstanceKey="
        + workflowInstanceKey
        + ", scopeInstanceKey="
        + scopeInstanceKey
        + ", payload="
        + payload
        + "]";
  }
}
