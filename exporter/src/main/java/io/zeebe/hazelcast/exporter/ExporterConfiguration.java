package io.zeebe.hazelcast.exporter;

import io.zeebe.protocol.record.ValueType;

public class ExporterConfiguration {

  public int port = 5701;

  public String topicPrefix = "zeebe-";

  public String enabledValueTypes = "JOB,WORKFLOW_INSTANCE,DEPLOYMENT,INCIDENT";

  public String enabledRecordTypes = "EVENT";

  public boolean updatePosition = true;

  public String getTopicName(ValueType valueType) {
    return topicPrefix + valueType.name();
  }

  @Override
  public String toString() {
    return "[port="
        + port
        + ", topicPrefix="
        + topicPrefix
        + ", enabledValueTypes="
        + enabledValueTypes
        + ", enabledRecordTypes="
        + enabledRecordTypes
            + ", updatePosition="
            + updatePosition
        + "]";
  }
}
