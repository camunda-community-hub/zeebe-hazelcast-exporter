package io.zeebe.hazelcast.connect.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.zeebe.hazelcast.protocol.ZeebeEvent;
import java.io.IOException;
import java.util.function.Consumer;

public class ZeebeHazelcastListener<T extends ZeebeEvent> implements MessageListener<String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final Class<? extends T> clazz;
  private final Consumer<T> consumer;

  public ZeebeHazelcastListener(Class<? extends T> clazz, Consumer<T> consumer) {
    this.clazz = clazz;
    this.consumer = consumer;
  }

  @Override
  public void onMessage(Message<String> message) {
    final String json = message.getMessageObject();

    try {
      final T event = objectMapper.readValue(json, clazz);

      consumer.accept(event);
    } catch (IOException e) {
      throw new RuntimeException("Fail to transform JSON event: " + json, e);
    }
  }
}
