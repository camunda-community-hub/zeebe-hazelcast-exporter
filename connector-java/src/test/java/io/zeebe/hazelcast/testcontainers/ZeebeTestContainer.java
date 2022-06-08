package io.zeebe.hazelcast.testcontainers;

import io.camunda.zeebe.client.ZeebeClient;
import io.zeebe.containers.ZeebeContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class ZeebeTestContainer extends ZeebeContainer {

    public ZeebeTestContainer() {
        this(null);
    }

    public ZeebeTestContainer(Map<String, String> env) {
        super(DockerImageName.parse("ghcr.io/camunda-community-hub/zeebe-with-hazelcast-exporter"));
        withExposedPorts(26500,9600,5701);
        if (env != null && !env.isEmpty()) {
            withEnv(env);
        }
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
}
