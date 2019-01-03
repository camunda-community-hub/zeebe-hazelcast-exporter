using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class WorkflowSubscriptionListener : IMessageListener<byte[]>
    {
        public delegate void Consumer(WorkflowInstanceSubscriptionRecord record);

        private Consumer MessageConsumer { get; set; }

        public WorkflowSubscriptionListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            WorkflowInstanceSubscriptionRecord value = WorkflowInstanceSubscriptionRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
