package io.zeebe.hazelcast.protocol;

public class IncidentRecord extends BaseRecord implements IncidentEvent {

  private String errorType;
  private String errorMessage;
  private String bpmnProcessId;
  private String elementId;
  private long workflowInstanceKey;
  private long elementInstanceKey;
  private long jobKey;

  @Override
  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

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
  public long getWorkflowInstanceKey() {
    return workflowInstanceKey;
  }

  public void setWorkflowInstanceKey(long workflowInstanceKey) {
    this.workflowInstanceKey = workflowInstanceKey;
  }

  @Override
  public long getElementInstanceKey() {
    return elementInstanceKey;
  }

  public void setElementInstanceKey(long elementInstanceKey) {
    this.elementInstanceKey = elementInstanceKey;
  }

  @Override
  public long getJobKey() {
    return jobKey;
  }

  public void setJobKey(long jobKey) {
    this.jobKey = jobKey;
  }

  @Override
  public String toString() {
    return "IncidentEvent [key="
        + getKey()
        + ", intent="
        + getIntent()
        + ", timestamp="
        + getTimestamp()
        + ", errorType="
        + errorType
        + ", errorMessage="
        + errorMessage
        + ", bpmnProcessId="
        + bpmnProcessId
        + ", elementId="
        + elementId
        + ", workflowInstanceKey="
        + workflowInstanceKey
        + ", elementInstanceKey="
        + elementInstanceKey
        + ", jobKey="
        + jobKey
        + "]";
  }
}
