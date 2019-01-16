using ExporterProtocol;
using Hazelcast.Core;

namespace Zeebe.Hazelcast.Connector
{

    public class WorkflowInstanceListener : IMessageListener<byte[]>
    {
        public delegate void Consumer(WorkflowInstanceRecord value);

        private Consumer MessageConsumer { get; set; }

        public WorkflowInstanceListener(Consumer consumer)
        {
            MessageConsumer = consumer;
        }

        public void OnMessage(Message<byte[]> message)
        {
            WorkflowInstanceRecord value = WorkflowInstanceRecord.Parser.ParseFrom(message.GetMessageObject());
            MessageConsumer.Invoke(value);
        }
    }
}
