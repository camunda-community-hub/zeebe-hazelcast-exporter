package io.zeebe.hazelcast.protocol;

import java.util.Map;

public class JobRecord extends BaseRecord {

  private String type;
  private String worker;
  private long deadline;
  private int retries;
  private String errorMessage;

  private Map<String, Object> customHeaders;
  private Map<String, Object> payload;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getWorker() {
    return worker;
  }

  public void setWorker(String worker) {
    this.worker = worker;
  }

  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }

  public Map<String, Object> getCustomHeaders() {
    return customHeaders;
  }

  public void setCustomHeaders(Map<String, Object> customHeaders) {
    this.customHeaders = customHeaders;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }
}
