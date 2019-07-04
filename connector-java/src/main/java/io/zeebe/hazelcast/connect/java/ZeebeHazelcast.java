package io.zeebe.hazelcast.connect.java;

import com.google.protobuf.GeneratedMessageV3;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.protocol.record.ValueType;

import java.util.function.Consumer;

public class ZeebeHazelcast {

  private final HazelcastInstance hazelcastInstance;

  public ZeebeHazelcast(HazelcastInstance hazelcastInstance) {
    this.hazelcastInstance = hazelcastInstance;
  }

  public void addJobListener(Consumer<Schema.JobRecord> consumer) {
    addJobListener(getTopicName(ValueType.JOB), consumer);
  }

  public void addJobListener(String topic, Consumer<Schema.JobRecord> consumer) {
    addMessageListener(topic, Schema.JobRecord::parseFrom, consumer);
  }

  public void addDeploymentListener(Consumer<Schema.DeploymentRecord> consumer) {
    addDeploymentListener(getTopicName(ValueType.DEPLOYMENT), consumer);
  }

  public void addDeploymentListener(String topic, Consumer<Schema.DeploymentRecord> consumer) {
    addMessageListener(topic, Schema.DeploymentRecord::parseFrom, consumer);
  }

  public void addWorkflowInstanceListener(Consumer<Schema.WorkflowInstanceRecord> consumer) {
    addWorkflowInstanceListener(getTopicName(ValueType.WORKFLOW_INSTANCE), consumer);
  }

  public void addWorkflowInstanceListener(
      String topic, Consumer<Schema.WorkflowInstanceRecord> consumer) {
    addMessageListener(topic, Schema.WorkflowInstanceRecord::parseFrom, consumer);
  }

  public void addIncidentListener(Consumer<Schema.IncidentRecord> consumer) {
    addIncidentListener(getTopicName(ValueType.INCIDENT), consumer);
  }

  public void addIncidentListener(String topic, Consumer<Schema.IncidentRecord> consumer) {
    addMessageListener(topic, Schema.IncidentRecord::parseFrom, consumer);
  }

  public void addErrorListener(Consumer<Schema.ErrorRecord> consumer) {
    addErrorListener(getTopicName(ValueType.ERROR), consumer);
  }

  public void addErrorListener(String topic, Consumer<Schema.ErrorRecord> consumer) {
    addMessageListener(topic, Schema.ErrorRecord::parseFrom, consumer);
  }

  public void addJobBatchListener(Consumer<Schema.JobBatchRecord> consumer) {
    addJobBatchListener(getTopicName(ValueType.JOB_BATCH), consumer);
  }

  public void addJobBatchListener(String topic, Consumer<Schema.JobBatchRecord> consumer) {
    addMessageListener(topic, Schema.JobBatchRecord::parseFrom, consumer);
  }

  public void addMessageListener(Consumer<Schema.MessageRecord> consumer) {
    addMessageListener(getTopicName(ValueType.MESSAGE), consumer);
  }

  public void addMessageListener(String topic, Consumer<Schema.MessageRecord> consumer) {
    addMessageListener(topic, Schema.MessageRecord::parseFrom, consumer);
  }

  public void addMessageStartEventSubscriptionListener(
      Consumer<Schema.MessageStartEventSubscriptionRecord> consumer) {
    addMessageStartEventSubscriptionListener(
        getTopicName(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION), consumer);
  }

  public void addMessageStartEventSubscriptionListener(
      String topic, Consumer<Schema.MessageStartEventSubscriptionRecord> consumer) {
    addMessageListener(topic, Schema.MessageStartEventSubscriptionRecord::parseFrom, consumer);
  }

  public void addMessageSubscriptionListener(Consumer<Schema.MessageSubscriptionRecord> consumer) {
    addMessageSubscriptionListener(getTopicName(ValueType.MESSAGE_SUBSCRIPTION), consumer);
  }

  public void addMessageSubscriptionListener(
      String topic, Consumer<Schema.MessageSubscriptionRecord> consumer) {
    addMessageListener(topic, Schema.MessageSubscriptionRecord::parseFrom, consumer);
  }

  public void addTimerListener(Consumer<Schema.TimerRecord> consumer) {
    addTimerListener(getTopicName(ValueType.TIMER), consumer);
  }

  public void addTimerListener(String topic, Consumer<Schema.TimerRecord> consumer) {
    addMessageListener(topic, Schema.TimerRecord::parseFrom, consumer);
  }

  public void addVariableListener(Consumer<Schema.VariableRecord> consumer) {
    addVariableListener(getTopicName(ValueType.VARIABLE), consumer);
  }

  public void addVariableListener(String topic, Consumer<Schema.VariableRecord> consumer) {
    addMessageListener(topic, Schema.VariableRecord::parseFrom, consumer);
  }

  public void addVariableDocumentListener(Consumer<Schema.VariableDocumentRecord> consumer) {
    addVariableDocumentListener(getTopicName(ValueType.VARIABLE_DOCUMENT), consumer);
  }

  public void addVariableDocumentListener(
      String topic, Consumer<Schema.VariableDocumentRecord> consumer) {
    addMessageListener(topic, Schema.VariableDocumentRecord::parseFrom, consumer);
  }

  public void addWorkflowInstanceCreationListener(
      Consumer<Schema.WorkflowInstanceCreationRecord> consumer) {
    addWorkflowInstanceCreationListener(
        getTopicName(ValueType.WORKFLOW_INSTANCE_CREATION), consumer);
  }

  public void addWorkflowInstanceCreationListener(
      String topic, Consumer<Schema.WorkflowInstanceCreationRecord> consumer) {
    addMessageListener(topic, Schema.WorkflowInstanceCreationRecord::parseFrom, consumer);
  }

  public void addWorkflowInstanceSubscriptionListener(
      Consumer<Schema.WorkflowInstanceSubscriptionRecord> consumer) {
    addWorkflowInstanceSubscriptionListener(
        getTopicName(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION), consumer);
  }

  public void addWorkflowInstanceSubscriptionListener(
      String topic, Consumer<Schema.WorkflowInstanceSubscriptionRecord> consumer) {
    addMessageListener(topic, Schema.WorkflowInstanceSubscriptionRecord::parseFrom, consumer);
  }

  private <T extends GeneratedMessageV3> void addMessageListener(
      String topicName,
      ZeebeHazelcastMessageListener.MessageDeserializer<T> deserializer,
      Consumer<T> consumer) {

    final ITopic<byte[]> topic = hazelcastInstance.getTopic(topicName);
    final ZeebeHazelcastMessageListener<T> listener =
        new ZeebeHazelcastMessageListener<>(deserializer, consumer);

    topic.addMessageListener(listener);
  }

  private String getTopicName(ValueType valueType) {
    return "zeebe-" + valueType;
  }
}
