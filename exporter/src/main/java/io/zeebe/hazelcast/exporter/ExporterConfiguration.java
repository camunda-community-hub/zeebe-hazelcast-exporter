package io.zeebe.hazelcast.exporter;

public class ExporterConfiguration {

  public int port = 5701;

  public String name = "zeebe";

  public int capacity = -1;
  public int timeToLiveInSeconds = -1;

  public String enabledValueTypes = "JOB,WORKFLOW_INSTANCE,DEPLOYMENT,INCIDENT";
  public String enabledRecordTypes = "EVENT";

  @Override
  public String toString() {
    return "[port="
            + port
            + ", name="
            + name
            + ", enabledValueTypes="
            + enabledValueTypes
            + ", enabledRecordTypes="
            + enabledRecordTypes
            + ", capacity="
            + capacity
            + ", timeToLiveInSeconds="
            + timeToLiveInSeconds
            + "]";
  }
}
