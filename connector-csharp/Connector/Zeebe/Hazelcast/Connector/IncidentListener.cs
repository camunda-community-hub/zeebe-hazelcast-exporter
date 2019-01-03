using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class IncidentListener: IMessageListener<byte[]>
    {
        public delegate void Consumer(IncidentRecord value);

        private Consumer MessageConsumer { get; set; }

        public IncidentListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            IncidentRecord value = IncidentRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
