package io.zeebe.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import io.zeebe.hazelcast.exporter.ExporterConfiguration;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ZeebeHazelcastClientTest {

  private static final ExporterConfiguration CONFIGURATION = new ExporterConfiguration();
  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

  private ZeebeHazelcast zeebeHazelcast;

  private HazelcastInstance hzInstance;
  private HazelcastInstance hzClient;

  @Before
  public void init() {
    final Config config = new Config();
    config.getNetworkConfig().setPort(5702);
    hzInstance = Hazelcast.newHazelcastInstance(config);

    final ClientConfig clientConfig = new ClientConfig();

    final var connectionRetryConfig =
        clientConfig.getConnectionStrategyConfig().getConnectionRetryConfig();
    connectionRetryConfig.setClusterConnectTimeoutMillis(CONNECTION_TIMEOUT.toMillis());

    clientConfig.getNetworkConfig().addAddress("127.0.0.1:5702");
    hzClient = HazelcastClient.newHazelcastClient(clientConfig);
  }

  @After
  public void cleanUp() throws Exception {
    zeebeHazelcast.close();
    hzClient.shutdown();
    hzInstance.shutdown();
  }

  @Test
  public void shouldCloseIfHazelcastIsUnavailable() {
    // given
    zeebeHazelcast = ZeebeHazelcast.newBuilder(hzClient).build();

    // when
    hzInstance.shutdown();

    // then
    Awaitility.await()
        .atMost(CONNECTION_TIMEOUT.multipliedBy(2))
        .untilAsserted(() -> assertThat(zeebeHazelcast.isClosed()).isTrue());
  }
}
