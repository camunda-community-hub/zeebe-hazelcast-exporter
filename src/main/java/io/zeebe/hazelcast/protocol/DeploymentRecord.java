package io.zeebe.hazelcast.protocol;

import java.util.List;

public class DeploymentRecord extends BaseRecord {

  private List<WorkflowRecord> deployedWorkflows;

  public List<WorkflowRecord> getDeployedWorkflows() {
    return deployedWorkflows;
  }

  public void setDeployedWorkflows(List<WorkflowRecord> deployedWorkflows) {
    this.deployedWorkflows = deployedWorkflows;
  }
}
