using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class MessageSubscriptionListener : IMessageListener<byte[]>
    {
        public delegate void Consumer(MessageSubscriptionRecord value);

        private Consumer MessageConsumer { get; set; }

        public MessageSubscriptionListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            MessageSubscriptionRecord value = MessageSubscriptionRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
