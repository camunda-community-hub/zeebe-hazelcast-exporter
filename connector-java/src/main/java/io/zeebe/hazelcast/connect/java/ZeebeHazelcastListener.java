package io.zeebe.hazelcast.connect.java;

import com.google.protobuf.GeneratedMessageV3;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.Arrays;
import java.util.function.Consumer;

public abstract class ZeebeHazelcastListener<T extends GeneratedMessageV3>
    implements MessageListener<byte[]> {

  private final Consumer<T> consumer;

  public ZeebeHazelcastListener(Consumer<T> consumer) {
    this.consumer = consumer;
  }

  protected abstract T toProtobufMessage(byte[] message) throws Exception;

  @Override
  public void onMessage(Message<byte[]> message) {
    final byte[] messageObject = message.getMessageObject();
    try {
      final T protobufMessage = toProtobufMessage(messageObject);
      consumer.accept(protobufMessage);
    } catch (Exception ex) {
      final String exceptionMessage = "Expected to transform %s into protobuf message, but failed.";
      throw new RuntimeException(
          String.format(exceptionMessage, Arrays.toString(messageObject)), ex);
    }
  }
}
