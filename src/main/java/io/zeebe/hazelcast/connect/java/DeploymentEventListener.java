package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.DeploymentRecord;
import java.util.function.Consumer;

public class DeploymentEventListener extends ZeebeHazelcastListener<DeploymentRecord> {

  public DeploymentEventListener(Consumer<DeploymentRecord> consumer) {
    super(DeploymentRecord.class, consumer);
  }
}
