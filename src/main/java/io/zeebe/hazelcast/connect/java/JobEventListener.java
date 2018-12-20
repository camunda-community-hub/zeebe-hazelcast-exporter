package io.zeebe.hazelcast.connect.java;

import io.zeebe.hazelcast.protocol.JobRecord;
import java.util.function.Consumer;

public class JobEventListener extends ZeebeHazelcastListener<JobRecord> {

  public JobEventListener(Consumer<JobRecord> consumer) {
    super(JobRecord.class, consumer);
  }
}
