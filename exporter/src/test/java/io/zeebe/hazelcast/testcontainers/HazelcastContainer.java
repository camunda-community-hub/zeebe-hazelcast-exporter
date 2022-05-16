package io.zeebe.hazelcast.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static java.lang.String.format;

public class HazelcastContainer extends GenericContainer<HazelcastContainer> {

    private static final String IMAGE_AND_VERSION_FORMAT = "%s:%s";
    public static String HAZELCAST_DOCKER_IMAGE_NAME = "hazelcast/hazelcast";
    public static Integer PORT = 5701;
    public static String ALIAS = "hazelcast";
    public static String VERSION = "5.1.1";

    protected HazelcastContainer() {
        super(format(IMAGE_AND_VERSION_FORMAT, HAZELCAST_DOCKER_IMAGE_NAME, VERSION));
    }

    @Override
    protected void configure() {
        withNetwork(Network.SHARED);
        withNetworkAliases(ALIAS);
        withExposedPorts(PORT);
    }

    public String getHazelcastServerExternalAddress() {
        return this.getHost() + ":" + this.getMappedPort(PORT);
    }

    public String getHazelcastServerAddress() {
        return ALIAS + ":" + PORT;
    }
}
