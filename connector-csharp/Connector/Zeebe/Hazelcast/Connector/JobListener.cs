using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class JobListener: IMessageListener<byte[]>
    {
        public delegate void Consumer(JobRecord value);

        private Consumer MessageConsumer { get; set; }

        public JobListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            JobRecord value = JobRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
