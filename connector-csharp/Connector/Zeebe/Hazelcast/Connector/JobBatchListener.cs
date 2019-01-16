using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class JobBatchListener: IMessageListener<byte[]>
    {
        public delegate void Consumer(JobBatchRecord value);

        private Consumer MessageConsumer { get; set; }

        public JobBatchListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            JobBatchRecord value = JobBatchRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
