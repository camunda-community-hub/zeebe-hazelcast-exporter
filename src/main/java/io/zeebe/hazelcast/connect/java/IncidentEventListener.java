package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.IncidentEvent;
import io.zeebe.hazelcast.protocol.IncidentRecord;
import java.util.function.Consumer;

public class IncidentEventListener extends ZeebeHazelcastListener<IncidentEvent> {

  public IncidentEventListener(Consumer<IncidentEvent> consumer) {
    super(IncidentRecord.class, consumer);
  }
}
