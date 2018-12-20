package io.zeebe.hazelcast.protocol;

public interface ZeebeEvent {

  long getKey();

  long getTimestamp();

  String getIntent();
}