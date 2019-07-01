package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.zeebe.hazelcast.connect.java.*;

import java.util.concurrent.CountDownLatch;

public class ExampleApplication {

  public static void main(String[] args) {

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

    final ZeebeHazelcast zeebeHazelcast = new ZeebeHazelcast(hz);

    zeebeHazelcast.addJobListener(job -> System.out.println("> job: " + job));
    zeebeHazelcast.addWorkflowInstanceListener(workflowInstance -> System.out.println("> workflow instance: " + workflowInstance));
    zeebeHazelcast.addDeploymentListener(deployment -> System.out.println("> deployment: " + deployment));
    zeebeHazelcast.addIncidentListener(incident -> System.out.println("> incident: " + incident));

    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
