using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class DeploymentListener: IMessageListener<byte[]>
    {
        public delegate void Consumer(DeploymentRecord value);

        private Consumer MessageConsumer { get; set; }

        public DeploymentListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            DeploymentRecord value = DeploymentRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
