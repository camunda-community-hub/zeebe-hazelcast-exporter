package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;

import java.util.concurrent.CountDownLatch;

public class ExampleApplication {

  public static void main(String[] args) throws Exception {

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

    ZeebeHazelcast zeebeHazelcast =
            ZeebeHazelcast.newBuilder(hz)
                    .readFromTail()
                    .addDeploymentListener(deployment -> System.out.println("> deployment: " + deployment))
                    .addWorkflowInstanceListener(
                            workflowInstance -> System.out.println("> workflow instance: " + workflowInstance))
                    .addJobListener(job -> System.out.println("> job: " + job))
                    .addIncidentListener(incident -> System.out.println("> incident: " + incident))
                    .build();

    try {
      new CountDownLatch(1).await();

    } catch (InterruptedException e) {
      zeebeHazelcast.close();
      hz.shutdown();
    }
  }
}
