using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class TimerListener : IMessageListener<byte[]>
    {
        public delegate void Consumer(TimerRecord value);

        private Consumer MessageConsumer { get; set; }

        public TimerListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            TimerRecord value = TimerRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
