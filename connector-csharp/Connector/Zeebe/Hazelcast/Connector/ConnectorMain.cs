using System;
using Hazelcast.Client;

using System.Threading;

namespace Zeebe.Hazelcast.Connector
{
    public class ConnectorMain
    {
        public static void Main(String[] args)
        {
            Console.WriteLine("Hello World!");
            // Start the Hazelcast Client and connect to an already running Hazelcast Cluster on 127.0.0.1
            var hz = HazelcastClient.NewHazelcastClient();
            // Get a Topic called "my-distributed-topic"
            var topic = hz.GetTopic<byte[]>("zeebe-deployments");

            // Add a Listener to the Topic
            DeploymentListener.Consumer consumer = (record) => Console.WriteLine(record.ToString());
            topic.AddMessageListener(new DeploymentListener(consumer));


            using (var signal = new EventWaitHandle(false, EventResetMode.AutoReset))
            {
                signal.WaitOne();
            }
        }
    }
}
