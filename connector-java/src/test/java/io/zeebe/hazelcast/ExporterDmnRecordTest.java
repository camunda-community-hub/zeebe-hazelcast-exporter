/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import io.zeebe.hazelcast.testcontainers.ZeebeTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
public final class ExporterDmnRecordTest {

  private static ZeebeClient ZEEBE_CLIENT;
  private static HazelcastInstance HAZELCAST_INSTANCE;

  @Container public static ZeebeTestContainer ZEEBE_TESTCONTAINER = new ZeebeTestContainer();

  @BeforeAll
  public static void init() {
    ZEEBE_CLIENT = ZEEBE_TESTCONTAINER.getClient();

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress(ZEEBE_TESTCONTAINER.getHazelcastAddress());
    HAZELCAST_INSTANCE = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @AfterAll
  public static void cleanUp() {
    HAZELCAST_INSTANCE.shutdown();
  }

  private static ZeebeHazelcast.Builder createZeebeHazelcastClient() {
    return ZeebeHazelcast.newBuilder(HAZELCAST_INSTANCE).readFromHead();
  }

  @Test
  void shouldExportDecisionRecords() throws Exception {
    final var decisions = new ArrayList<Schema.DecisionRecord>();
    final var zeebeHazelcast =
        createZeebeHazelcastClient().addDecisionListener(decisions::add).build();

    try (zeebeHazelcast) {
      ZEEBE_CLIENT.newDeployResourceCommand().addResourceFromClasspath("rating.dmn").send().join();

      await()
          .untilAsserted(
              () -> {
                assertThat(decisions)
                    .hasSize(2)
                    .extracting(Schema.DecisionRecord::getDecisionId)
                    .contains("decision_a", "decision_b");
              });
    }
  }

  @Test
  void shouldExportDecisionRequirementsRecords() throws Exception {
    final var decisionRequirements = new ArrayList<Schema.DecisionRequirementsRecord>();
    final var zeebeHazelcast =
        createZeebeHazelcastClient()
            .addDecisionRequirementsListener(decisionRequirements::add)
            .build();

    try (zeebeHazelcast) {
      ZEEBE_CLIENT.newDeployResourceCommand().addResourceFromClasspath("rating.dmn").send().join();

      await()
          .untilAsserted(
              () -> {
                assertThat(decisionRequirements)
                    .hasSize(1)
                    .extracting(
                        record ->
                            record.getDecisionRequirementsMetadata().getDecisionRequirementsId())
                    .contains("Ratings");
              });
    }
  }

  @Test
  void shouldExportDecisionEvaluationRecords() throws Exception {
    final var decisionEvaluations = new ArrayList<Schema.DecisionEvaluationRecord>();
    final var zeebeHazelcast =
        createZeebeHazelcastClient()
            .addDecisionEvaluationListener(decisionEvaluations::add)
            .build();

    try (zeebeHazelcast) {
      ZEEBE_CLIENT
          .newDeployResourceCommand()
          .addResourceFromClasspath("rating.dmn")
          .addProcessModel(
              Bpmn.createExecutableProcess("process")
                  .startEvent()
                  .businessRuleTask(
                      "task",
                      t -> t.zeebeCalledDecisionId("decision_a").zeebeResultVariable("result"))
                  .done(),
              "process.bpmn")
          .send()
          .join();

      ZEEBE_CLIENT
          .newCreateInstanceCommand()
          .bpmnProcessId("process")
          .latestVersion()
          .variables(Map.of("x", 7))
          .send()
          .join();

      await()
          .untilAsserted(
              () -> {
                assertThat(decisionEvaluations)
                    .hasSize(1)
                    .extracting(Schema.DecisionEvaluationRecord::getDecisionOutput)
                    .contains("\"A+\"");
              });
    }
  }
}
