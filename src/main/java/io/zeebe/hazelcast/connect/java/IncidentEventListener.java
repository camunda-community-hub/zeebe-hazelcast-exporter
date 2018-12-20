package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.IncidentRecord;
import java.util.function.Consumer;

public class IncidentEventListener extends ZeebeHazelcastListener<IncidentRecord> {

  public IncidentEventListener(Consumer<IncidentRecord> consumer) {
    super(IncidentRecord.class, consumer);
  }
}
