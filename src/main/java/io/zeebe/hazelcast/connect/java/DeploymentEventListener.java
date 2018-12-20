package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.DeploymentEvent;
import io.zeebe.hazelcast.protocol.DeploymentRecord;
import java.util.function.Consumer;

public class DeploymentEventListener extends ZeebeHazelcastListener<DeploymentEvent> {

  public DeploymentEventListener(Consumer<DeploymentEvent> consumer) {
    super(DeploymentRecord.class, consumer);
  }
}
