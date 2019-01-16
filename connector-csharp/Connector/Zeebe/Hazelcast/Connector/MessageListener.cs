using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class MessageListener : IMessageListener<byte[]>
    {
        public delegate void Consumer(MessageRecord value);

        private Consumer MessageConsumer { get; set; }

        public MessageListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            MessageRecord value = MessageRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
