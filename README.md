# zeebe-hazelcast-exporter

Export events from [Zeebe](https://github.com/zeebe-io/zeebe) to [Hazelcast](https://github.com/hazelcast/hazelcast/). Hazelcast is an in-memory data grid which is used here as a message topic.

![How it works](how-it-works.png)

The exporter provides an easy way to connect multiple applications to Zeebe. For example, an application can use the exporter to send a notification when a new incident is created. Without the exporter, the application needs to implement its own exporter. 

## Usage

### Java Application

Add the Maven dependency to your `pom.xml`

```
<dependency>
	<groupId>io.zeebe.hazelcast</groupId>
	<artifactId>zeebe-hazelcast-connector</artifactId>
	<version>%{VERSION}</version>
</dependency>
```

Connect to Hazelcast and register a listener 

```java
ClientConfig clientConfig = new ClientConfig();
clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

ITopic<String> topic = hz.getTopic("zeebe-workflow-instances");
topic.addMessageListener(new WorkflowInstanceEventListener(event -> {
    // do something ...
}));
```

You can use one of the following listeners:

* `WorkflowInstanceEventListener`
* `DeploymentEventListener`
* `JobEventListener`
* `IncidentEventListener`

### C# Application

Add the nuget package `zeebe hazelcast connector(???)` to your project.

Example usage:
```
            // Start the Hazelcast Client and connect to an already running Hazelcast Cluster on 127.0.0.1
            var hz = HazelcastClient.NewHazelcastClient();
            // Get a Topic called "zeebe-deployments"
            var topic = hz.GetTopic<byte[]>("zeebe-deployments");

             // Add a Listener to the Topic
            DeploymentListener.Consumer consumer = (record) => Console.WriteLine(record.ToString());
            topic.AddMessageListener(new DeploymentListener(consumer));

```

### Exporter

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

### Configuration

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

## How to build

Build with Maven

`mvn clean install`

## Code of Conduct

This project adheres to the Contributor Covenant [Code of
Conduct](/CODE_OF_CONDUCT.md). By participating, you are expected to uphold
this code. Please report unacceptable behavior to
code-of-conduct@zeebe.io.
