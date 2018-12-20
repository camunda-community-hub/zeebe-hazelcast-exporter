package io.zeebe.hazelcast.protocol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public class DeploymentRecord extends BaseRecord implements DeploymentEvent {

  @JsonDeserialize(contentAs = WorkflowRecord.class)
  private List<WorkflowMetadata> deployedWorkflows;

  @Override
  public List<WorkflowMetadata> getDeployedWorkflows() {
    return deployedWorkflows;
  }

  public void setDeployedWorkflows(List<WorkflowMetadata> deployedWorkflows) {
    this.deployedWorkflows = deployedWorkflows;
  }
}
