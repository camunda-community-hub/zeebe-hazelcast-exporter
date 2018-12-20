package io.zeebe.hazelcast.protocol;

import java.util.List;

public interface DeploymentEvent extends ZeebeEvent {

  List<WorkflowMetadata> getDeployedWorkflows();
}
