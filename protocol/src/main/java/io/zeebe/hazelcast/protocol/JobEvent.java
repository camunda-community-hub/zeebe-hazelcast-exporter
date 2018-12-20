package io.zeebe.hazelcast.protocol;

import java.util.Map;

public interface JobEvent extends ZeebeEvent {

  String getType();

  String getWorker();

  long getDeadline();

  Map<String, Object> getCustomHeaders();

  int getRetries();

  String getErrorMessage();

  Map<String, Object> getPayload();
}
