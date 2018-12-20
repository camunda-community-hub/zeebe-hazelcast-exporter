package io.zeebe.hazelcast.exporter;

public class ExporterConfiguration {

  public String deploymentTopic = "zeebe-deployments";
  public String workflowInstanceTopic = "zeebe-workflow-instances";
  public String jobTopic = "zeebe-jobs";
  public String incidentTopic = "zeebe-incidents";

  @Override
  public String toString() {
    return "[deploymentTopic="
        + deploymentTopic
        + ", workflowInstanceTopic="
        + workflowInstanceTopic
        + ", jobTopic="
        + jobTopic
        + ", incidentTopic="
        + incidentTopic
        + "]";
  }
}
