package io.zeebe.hazelcast.testcontainers;

import io.camunda.zeebe.client.ZeebeClient;
import io.zeebe.containers.ZeebeContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class ZeebeTestContainer extends ZeebeContainer {

    private HazelcastContainer hazelcastContainer;

    protected ZeebeTestContainer(HazelcastContainer hazelcastContainer) {
        super(DockerImageName.parse("ghcr.io/camunda-community-hub/zeebe-with-hazelcast-exporter"));
        withExposedPorts(26500,9600,5701);
        this.hazelcastContainer = hazelcastContainer;
    }

    public static ZeebeTestContainer withDefaultConfig() {
        return new ZeebeTestContainer(null);
    }

    public static ZeebeTestContainer withDefaultConfig(String clusterName) {
        ZeebeTestContainer container = withDefaultConfig();
        container.withEnv("ZEEBE_HAZELCAST_CLUSTER_NAME", clusterName);
        return container;
    }

    public static ZeebeTestContainer withJsonFormat() {
        ZeebeTestContainer container = withDefaultConfig();
        container.withEnv("ZEEBE_HAZELCAST_FORMAT", "json");
        return container;
    }

    public static ZeebeTestContainer withHazelcastContainer() {
        return new ZeebeTestContainer(new HazelcastContainer());
    }

    public static ZeebeTestContainer withHazelcastContainer(String clusterName) {
        HazelcastContainer hazelcast = new HazelcastContainer();
        hazelcast.withEnv("HZ_CLUSTERNAME", clusterName);
        ZeebeTestContainer container = new ZeebeTestContainer(hazelcast);
        container.withEnv("ZEEBE_HAZELCAST_CLUSTER_NAME", clusterName);
        return container;
    }

    public ZeebeClient getClient() {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(getExternalGatewayAddress())
                .usePlaintext()
                .build();
    }

    public String getHazelcastAddress() {
        return getExternalAddress(5701);
    }

    @Override
    public void start() {
        if (hazelcastContainer != null) {
            hazelcastContainer.start();
            withNetwork(hazelcastContainer.getNetwork());
            withEnv("ZEEBE_HAZELCAST_REMOTE_ADDRESS", hazelcastContainer.getHazelcastServerAddress());
        }
        super.start();
    }

    @Override
    public void stop() {
        if (hazelcastContainer != null) {
            hazelcastContainer.stop();
        }
        super.stop();
    }

    public HazelcastContainer getHazelcastContainer() {
        return hazelcastContainer;
    }
}
