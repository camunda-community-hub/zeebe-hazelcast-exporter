package io.zeebe.hazelcast.protocol;

import java.util.Map;

public class WorkflowInstanceRecord extends BaseRecord {

  private String bpmnProcessId;
  private String elementId;
  private int version;
  private long workflowKey;
  private long workflowInstanceKey;
  private long scopeInstanceKey;
  private Map<String, Object> payload;

  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public void setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  public String getElementId() {
    return elementId;
  }

  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public long getWorkflowKey() {
    return workflowKey;
  }

  public void setWorkflowKey(long workflowKey) {
    this.workflowKey = workflowKey;
  }

  public long getWorkflowInstanceKey() {
    return workflowInstanceKey;
  }

  public void setWorkflowInstanceKey(long workflowInstanceKey) {
    this.workflowInstanceKey = workflowInstanceKey;
  }

  public long getScopeInstanceKey() {
    return scopeInstanceKey;
  }

  public void setScopeInstanceKey(long scopeInstanceKey) {
    this.scopeInstanceKey = scopeInstanceKey;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }
}
