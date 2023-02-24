package io.zeebe.hazelcast.connect.java;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hazelcast.client.HazelcastClientNotActiveException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.ringbuffer.StaleSequenceException;
import io.zeebe.exporter.proto.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class ZeebeHazelcast implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeHazelcast.class);

  private static final List<Class<? extends com.google.protobuf.Message>> RECORD_MESSAGE_TYPES;

  static {
    RECORD_MESSAGE_TYPES = new ArrayList<>();
    RECORD_MESSAGE_TYPES.add(Schema.DeploymentRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.DeploymentDistributionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ErrorRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.IncidentRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.JobRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.JobBatchRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageStartEventSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ProcessRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ProcessEventRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ProcessInstanceRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ProcessInstanceCreationRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ProcessMessageSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.TimerRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.VariableRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.VariableDocumentRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.DecisionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.DecisionRequirementsRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.DecisionEvaluationRecord.class);
  }

  private final Ringbuffer<byte[]> ringbuffer;
  private final Map<Class<?>, List<Consumer<?>>> listeners;
  private final Consumer<Long> postProcessListener;

  private long sequence;

  private Future<?> future;
  private ExecutorService executorService;

  private volatile boolean isClosed = false;

  private ZeebeHazelcast(
      Ringbuffer<byte[]> ringbuffer,
      long sequence,
      Map<Class<?>, List<Consumer<?>>> listeners,
      Consumer<Long> postProcessListener) {
    this.ringbuffer = ringbuffer;
    this.sequence = sequence;
    this.listeners = listeners;
    this.postProcessListener = postProcessListener;
  }

  /** Returns a new builder to read from the ringbuffer. */
  public static Builder newBuilder(HazelcastInstance hazelcastInstance) {
    return new ZeebeHazelcast.Builder(hazelcastInstance);
  }

  private void start() {
    executorService = Executors.newSingleThreadExecutor();
    future = executorService.submit(this::readFromBuffer);
  }

  public boolean isClosed() {
    return isClosed;
  }

  /** Stop reading from the ringbuffer. */
  @Override
  public void close() throws Exception {
    LOGGER.info("Closing. Stop reading from ringbuffer. Current sequence: '{}'", getSequence());

    isClosed = true;

    if (future != null) {
      future.cancel(true);
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  /** Returns the current sequence. */
  public long getSequence() {
    return sequence;
  }

  private void readFromBuffer() {
    while (!isClosed) {
      readNext();
    }
  }

  private void readNext() {
    LOGGER.trace("Read from ring-buffer with sequence '{}'", sequence);

    try {
      final byte[] item = ringbuffer.readOne(sequence);

      final var genericRecord = Schema.Record.parseFrom(item);
      handleRecord(genericRecord);

      sequence += 1;

      postProcessListener.accept(sequence);

    } catch (InvalidProtocolBufferException e) {
      LOGGER.error("Failed to deserialize Protobuf message at sequence '{}'", sequence, e);

      sequence += 1;

    } catch (StaleSequenceException e) {
      // if the sequence is smaller than headSequence(). Because a Ringbuffer won't store all event
      // indefinitely, it can be that the data for the given sequence doesn't exist anymore and the
      // StaleSequenceException is thrown. It is up to the caller to deal with this particular
      // situation, e.g. throw an Exception or restart from the last known head. That is why the
      // StaleSequenceException contains the last known head.
      final var headSequence = e.getHeadSeq();
      LOGGER.warn(
          "Fail to read from ring-buffer at sequence '{}'. The sequence is reported as stale. Continue with new head sequence at '{}'",
          sequence,
          headSequence,
          e);

      sequence = headSequence;

    } catch (IllegalArgumentException e) {
      // if sequence is smaller than 0 or larger than tailSequence()+1
      final var headSequence = ringbuffer.headSequence();
      LOGGER.warn(
          "Fail to read from ring-buffer at sequence '{}'. Continue with head sequence at '{}'",
          sequence,
          headSequence,
          e);

      sequence = headSequence;

    } catch (HazelcastClientNotActiveException e) {
      LOGGER.warn("Lost connection to the Hazelcast server", e);

      try {
        close();
      } catch (Exception closingFailure) {
        LOGGER.debug("Failure while closing the client", closingFailure);
      }

    } catch (InterruptedException e) {
      LOGGER.debug("Interrupted while reading from ring-buffer with sequence '{}'", sequence);
      throw new RuntimeException("Interrupted while reading from ring-buffer", e);

    } catch (Exception e) {
      if (!isClosed) {
        LOGGER.error(
            "Fail to read from ring-buffer at sequence '{}'. Will try again.", sequence, e);
      }
    }
  }

  private void handleRecord(Schema.Record genericRecord) throws InvalidProtocolBufferException {
    for (Class<? extends com.google.protobuf.Message> type : RECORD_MESSAGE_TYPES) {
      final var handled = handleRecord(genericRecord, type);
      if (handled) {
        return;
      }
    }
  }

  private <T extends com.google.protobuf.Message> boolean handleRecord(
      Schema.Record genericRecord, Class<T> t) throws InvalidProtocolBufferException {

    if (genericRecord.getRecord().is(t)) {
      final var record = genericRecord.getRecord().unpack(t);

      listeners
          .getOrDefault(t, List.of())
          .forEach(listener -> ((Consumer<T>) listener).accept(record));

      return true;
    } else {
      return false;
    }
  }

  public static class Builder {

    private final HazelcastInstance hazelcastInstance;

    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    private String name = "zeebe";

    private long readFromSequence = -1;
    private boolean readFromHead = false;

    private Consumer<Long> postProcessListener = sequence -> {};

    private Builder(HazelcastInstance hazelcastInstance) {
      this.hazelcastInstance = hazelcastInstance;
    }

    /** Set the name of the ringbuffer to read from. */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /** Start reading from the given sequence. */
    public Builder readFrom(long sequence) {
      this.readFromSequence = sequence;
      readFromHead = false;
      return this;
    }

    /** Start reading from the oldest item of the ringbuffer. */
    public Builder readFromHead() {
      readFromSequence = -1;
      readFromHead = true;
      return this;
    }

    /** Start reading from the newest item of the ringbuffer. */
    public Builder readFromTail() {
      readFromSequence = -1;
      readFromHead = false;
      return this;
    }

    /**
     * Register a listener that is called when an item is read from the ringbuffer and consumed by
     * the registered listeners. The listener is called with the next sequence number of the
     * ringbuffer. It can be used to store the sequence number externally.
     */
    public Builder postProcessListener(Consumer<Long> listener) {
      postProcessListener = listener;
      return this;
    }

    private <T extends com.google.protobuf.Message> void addListener(
        Class<T> recordType, Consumer<T> listener) {
      final var recordListeners = listeners.getOrDefault(recordType, new ArrayList<>());
      recordListeners.add(listener);
      listeners.put(recordType, recordListeners);
    }

    public Builder addDeploymentListener(Consumer<Schema.DeploymentRecord> listener) {
      addListener(Schema.DeploymentRecord.class, listener);
      return this;
    }

    public Builder addDeploymentDistributionListener(
        Consumer<Schema.DeploymentDistributionRecord> listener) {
      addListener(Schema.DeploymentDistributionRecord.class, listener);
      return this;
    }

    public Builder addProcessListener(Consumer<Schema.ProcessRecord> listener) {
      addListener(Schema.ProcessRecord.class, listener);
      return this;
    }

    public Builder addProcessInstanceListener(Consumer<Schema.ProcessInstanceRecord> listener) {
      addListener(Schema.ProcessInstanceRecord.class, listener);
      return this;
    }

    public Builder addProcessEventListener(Consumer<Schema.ProcessEventRecord> listener) {
      addListener(Schema.ProcessEventRecord.class, listener);
      return this;
    }

    public Builder addVariableListener(Consumer<Schema.VariableRecord> listener) {
      addListener(Schema.VariableRecord.class, listener);
      return this;
    }

    public Builder addVariableDocumentListener(Consumer<Schema.VariableDocumentRecord> listener) {
      addListener(Schema.VariableDocumentRecord.class, listener);
      return this;
    }

    public Builder addJobListener(Consumer<Schema.JobRecord> listener) {
      addListener(Schema.JobRecord.class, listener);
      return this;
    }

    public Builder addJobBatchListener(Consumer<Schema.JobBatchRecord> listener) {
      addListener(Schema.JobBatchRecord.class, listener);
      return this;
    }

    public Builder addIncidentListener(Consumer<Schema.IncidentRecord> listener) {
      addListener(Schema.IncidentRecord.class, listener);
      return this;
    }

    public Builder addTimerListener(Consumer<Schema.TimerRecord> listener) {
      addListener(Schema.TimerRecord.class, listener);
      return this;
    }

    public Builder addMessageListener(Consumer<Schema.MessageRecord> listener) {
      addListener(Schema.MessageRecord.class, listener);
      return this;
    }

    public Builder addMessageSubscriptionListener(
        Consumer<Schema.MessageSubscriptionRecord> listener) {
      addListener(Schema.MessageSubscriptionRecord.class, listener);
      return this;
    }

    public Builder addMessageStartEventSubscriptionListener(
        Consumer<Schema.MessageStartEventSubscriptionRecord> listener) {
      addListener(Schema.MessageStartEventSubscriptionRecord.class, listener);
      return this;
    }

    public Builder addProcessMessageSubscriptionListener(
        Consumer<Schema.ProcessMessageSubscriptionRecord> listener) {
      addListener(Schema.ProcessMessageSubscriptionRecord.class, listener);
      return this;
    }

    public Builder addProcessInstanceCreationListener(
        Consumer<Schema.ProcessInstanceCreationRecord> listener) {
      addListener(Schema.ProcessInstanceCreationRecord.class, listener);
      return this;
    }

    public Builder addErrorListener(Consumer<Schema.ErrorRecord> listener) {
      addListener(Schema.ErrorRecord.class, listener);
      return this;
    }

    public Builder addDecisionListener(Consumer<Schema.DecisionRecord> listener) {
      addListener(Schema.DecisionRecord.class, listener);
      return this;
    }

    public Builder addDecisionRequirementsListener(
        Consumer<Schema.DecisionRequirementsRecord> listener) {
      addListener(Schema.DecisionRequirementsRecord.class, listener);
      return this;
    }

    public Builder addDecisionEvaluationListener(
        Consumer<Schema.DecisionEvaluationRecord> listener) {
      addListener(Schema.DecisionEvaluationRecord.class, listener);
      return this;
    }

    private long getSequence(Ringbuffer<?> ringbuffer) {

      final var headSequence = ringbuffer.headSequence();
      final var tailSequence = ringbuffer.tailSequence();

      if (readFromSequence > 0) {
        if (readFromSequence > (tailSequence + 1)) {
          LOGGER.info(
              "The given sequence '{}' is greater than the current tail-sequence '{}' of the ringbuffer. Using the head-sequence instead.",
              readFromSequence,
              tailSequence);
          return headSequence;
        } else {
          return readFromSequence;
        }

      } else if (readFromHead) {
        return headSequence;

      } else {
        return Math.max(headSequence, tailSequence);
      }
    }

    /**
     * Start a background task that reads from the ringbuffer and invokes the listeners. After an
     * item is read and the listeners are invoked, the sequence is incremented (at-least-once
     * semantic). <br>
     * The current sequence is returned by {@link #getSequence()}. <br>
     * Call {@link #close()} to stop reading.
     */
    public ZeebeHazelcast build() {

      LOGGER.debug("Read from ringbuffer with name '{}'", name);
      final Ringbuffer<byte[]> ringbuffer = hazelcastInstance.getRingbuffer(name);

      if (ringbuffer == null) {
        throw new IllegalArgumentException(
            String.format("No ring buffer found with name '%s'", name));
      }

      LOGGER.debug(
          "Ringbuffer status: [head: {}, tail: {}, size: {}, capacity: {}]",
          ringbuffer.headSequence(),
          ringbuffer.tailSequence(),
          ringbuffer.size(),
          ringbuffer.capacity());

      final long sequence = getSequence(ringbuffer);
      LOGGER.info("Read from ringbuffer '{}' starting from sequence '{}'", name, sequence);

      final var zeebeHazelcast =
          new ZeebeHazelcast(ringbuffer, sequence, listeners, postProcessListener);
      zeebeHazelcast.start();

      return zeebeHazelcast;
    }
  }
}
