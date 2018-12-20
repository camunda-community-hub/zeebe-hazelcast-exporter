# zeebe-hazelcast-exporter

Export events from [Zeebe](https://github.com/zeebe-io/zeebe) to [Hazelcast](https://github.com/hazelcast/hazelcast/). Hazelcast is an in-memory data grid which is used here as a message topic.

![How it works](how-it-works.png)

The exporter provides an easy way to connect multiple applications to Zeebe. For example, an application can use the exporter to send a notification when a new incident is created. Without the exporter, the application needs to implement its own exporter. 

# Usage

## Java Application

```java
ClientConfig clientConfig = new ClientConfig();
clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

ITopic<String> topic = hz.getTopic("zeebe-workflow-instances");
topic.addMessageListener(new WorkflowInstanceEventListener(event -> {
    // do something ...
}));
```

# How to run

Before you start the broker, copy the exporter JAR  into the lib folder of the broker.

```
cp exporter/target/zeebe-hazelcast-exporter-%{VERSION}.jar ~/zeebe-broker-%{VERSION}/lib/
```

Register the exporter in the Zeebe configuration file `~/zeebe-broker-%{VERSION}/config/zeebe.cfg.toml`.

```
[[exporters]]
id = "hazelcast"
className = "io.zeebe.hazelcast.exporter.HazelcastExporter"
```

Now start the broker and the applications.

# How to configure

In the Zeebe configuration file, you can change the topics where the events are published.

```
[[exporters]]
id = "hazelcast"
className = "io.zeebe.hazelcast.exporter.HazelcastExporter"

deploymentTopic = "zeebe-deployments"
workflowInstanceTopic = "zeebe-workflow-instances"
jobTopic = "zeebe-jobs"
incidentTopic = "zeebe-incidents"
```

# How to build

Build with Maven

`mvn clean install`

