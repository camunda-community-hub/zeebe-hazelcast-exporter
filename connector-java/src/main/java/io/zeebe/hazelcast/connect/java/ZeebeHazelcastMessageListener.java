package io.zeebe.hazelcast.connect.java;

import com.google.protobuf.GeneratedMessageV3;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.Arrays;
import java.util.function.Consumer;

public class ZeebeHazelcastMessageListener<T extends GeneratedMessageV3>
    implements MessageListener<byte[]> {

  interface MessageDeserializer<T> {
    T deserialize(byte[] message) throws Exception;
  }

  private final Consumer<T> consumer;
  private final MessageDeserializer<T> deserializer;

  public ZeebeHazelcastMessageListener(MessageDeserializer<T> deserializer, Consumer<T> consumer) {
    this.deserializer = deserializer;
    this.consumer = consumer;
  }

  @Override
  public void onMessage(Message<byte[]> message) {
    final byte[] messageObject = message.getMessageObject();
    try {
      final T protobufMessage = deserializer.deserialize(messageObject);
      consumer.accept(protobufMessage);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to deserialize Protobuf message.", ex);
    }
  }
}
