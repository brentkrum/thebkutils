package com.thebk.utils.pipe;

import com.denaliai.fw.utility.concurrent.ParamBag;
import com.denaliai.fw.utility.concurrent.PerpetualWork;
import com.denaliai.fw.utility.concurrent.RCFuture;
import com.denaliai.fw.utility.concurrent.RCPromise;
import com.thebk.utils.MPSCFixedQueue;
import com.thebk.utils.PrivateFixedQueue;

public class Pipe {
	private final Worker m_pipeProcessor = new Worker();
	private MPSCFixedQueue m_commandQueue = new MPSCFixedQueue(16);
	private final PrivateFixedQueue m_pipeBuffer = new PrivateFixedQueue(4096);

	public RCFuture<IConsumerHandle> connectConsumer(IBrokerConsumer consumer) {
		ParamDataBag
	}

	public RCFuture<IProducerHandle> connectProducer(IBrokerProducer producer) {

	}

	private class Worker extends PerpetualWork {
		private IBrokerConsumer m_consumer;
		private IBrokerProducer m_producer;

		protected void _doWork() {
			while(true) {
				ParamBag bag = (ParamBag)m_commandQueue.dequeue();
				if (bag == null) {
					break;
				}
				int cmd = RCParamBag.pi();
				RCPromise<Void> commandDone = bag.po();
				if (o == PRODUCER_CONNECT) {
				} else if (o == CONSUMER_CONNECT) {
					if (clearBuffer) {
						m_pipeBuffer.clear();
					}
				} else if (o == CONSUMER_DISCONNECT) {
					if (clearBuffer) {
						m_pipeBuffer.clear();
					}
					m_consumer = null;
				}

			}
			if (m_producer != null) {
				boolean removedFromInQueue = false;
				while(m_pipeBuffer.hasSpace()) {
					Object o = m_producer.dequeue();
					if (o == null) {
						break;
					}
					m_pipeBuffer.add(o);
				}
			}
			if (m_consumer != null) {
				while(true) {
					Object o = m_pipeBuffer.peek();
					if (o == null) {
						break;
					}
					if (!m_consumer.enqueue(o)) {
						break;
					}
					m_pipeBuffer.dequeue();
				}
			}
		}
	}

	public interface IBrokerConsumer {
		void enqueue(Object obj);
	}
	public interface IConsumerHandle {
		RCFuture<Void> disconnect();
	}

	public interface IBrokerProducer {
		boolean produce(Object o);
		RCFuture<Void> close();
	}
	public interface IProducerHandle {
		RCFuture<Void> disconnect();
	}

	public  abstract class Producer implements IBrokerProducer {
		protected abstract Object dequeue();
	}
}
