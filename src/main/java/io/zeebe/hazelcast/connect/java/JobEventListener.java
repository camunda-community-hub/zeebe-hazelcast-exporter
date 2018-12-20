package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.JobEvent;
import io.zeebe.hazelcast.protocol.JobRecord;
import java.util.function.Consumer;

public class JobEventListener extends ZeebeHazelcastListener<JobEvent> {

  public JobEventListener(Consumer<JobEvent> consumer) {
    super(JobRecord.class, consumer);
  }
}
