package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.zeebe.hazelcast.connect.java.DeploymentEventListener;
import io.zeebe.hazelcast.connect.java.IncidentEventListener;
import io.zeebe.hazelcast.connect.java.JobEventListener;
import io.zeebe.hazelcast.connect.java.WorkflowInstanceEventListener;
import java.util.concurrent.CountDownLatch;

public class ExampleApplication {

  public static void main(String[] args) {

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
    HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

    ITopic<byte[]> topic = hz.getTopic("zeebe-workflow-instances");
    topic.addMessageListener(
        new WorkflowInstanceEventListener(
            event -> System.out.println("> " + event)));

    hz.<byte[]>getTopic("zeebe-deployments")
        .addMessageListener(
            new DeploymentEventListener(
                event -> System.out.println("> " + event)));

    hz.<byte[]>getTopic("zeebe-jobs")
        .addMessageListener(
            new JobEventListener(
                event -> System.out.println("> " + event)));

    hz.<byte[]>getTopic("zeebe-incidents")
        .addMessageListener(
            new IncidentEventListener(
                event -> System.out.println("> " + event)));

    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
