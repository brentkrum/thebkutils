package com.thebk.utils.pipe;

import com.thebk.utils.queue.MPSCFixedQueue;
import com.thebk.utils.queue.PrivateFixedQueue;
import com.thebk.utils.rc.RCBoolean;
import com.thebk.utils.concurrent.*;
import com.thebk.utils.parambag.ParamBag;
import com.thebk.utils.parambag.ParamDataBag;

public class ConnectablePipe {
	private static final int CONSUMER_CONNECT = 1;
	private static final int CONSUMER_DISCONNECT = 2;
	private static final int PRODUCER_CONNECT = 3;
	private static final int PRODUCER_DISCONNECT = 4;

	private static final Object PRODUCER_HAS_CLOSED = new Object();
	private static final Object PRODUCER_HAS_OPENED = new Object();

	private final Worker m_pipeProcessor = new Worker();
	private final MPSCFixedQueue m_commandQueue;
	private final PrivateFixedQueue m_pipeBuffer;
	private final IExceptionHandler m_exceptionHandler;
	private boolean m_clearPipeOnConsumerConnect;

	public ConnectablePipe() {
		this(16, 4096, null);
	}

	public ConnectablePipe(IExceptionHandler exceptionHandler) {
		this(16, 4096, exceptionHandler);
	}

	public ConnectablePipe(int commandQueueMaxNumEntries, int pipeQueueMaxNumEntries, IExceptionHandler exceptionHandler) {
		m_commandQueue = new MPSCFixedQueue(commandQueueMaxNumEntries);
		m_pipeBuffer = new PrivateFixedQueue(pipeQueueMaxNumEntries);
		m_exceptionHandler = exceptionHandler;
	}

	private <H, I> RCFuture<H> sendToProcessor(int msg, I iface) {
		RCBoolean comitted = RCBoolean.create(false);
		try {
			RCPromise<H> p = RCPromise.create();
			// Pass one ref into bag
			ParamDataBag bag = ParamDataBag.create().po(iface).po(p.retain()).pi(msg);
			if (!m_commandQueue.enqueue(bag, comitted)) {
				// Release the create() ref
				p.release();
				// Release the bag (and the RCPromise ref inside it)
				bag.release();
				return RCFailedFuture.create(new IllegalStateException("Command queue full"));
			}
			if (comitted.value() == true) {
				// Only run the pipe processor if we are the comitting thread in the queue
				// A False value means there is another thread competing with us in the queue and it will call this
				m_pipeProcessor.runMoreWork();
			}
			// Return one ref out
			return p;
		} finally {
			comitted.release();
		}
	}

	public RCFuture<IConsumerHandle> connectConsumer(IBrokerConsumer consumer) {
		return sendToProcessor(CONSUMER_CONNECT, consumer);
	}

	public RCFuture<IProducerHandle> connectProducer(IBrokerProducer producer) {
		return sendToProcessor(PRODUCER_CONNECT, producer);
	}

	public void runProcessor() {
		m_pipeProcessor.runMoreWork();
	}

	public void notifyProcessor() {
		m_pipeProcessor.requestMoreWork();
	}

	private RCFuture<Void> consumerDisconnect(IBrokerConsumer consumer) {
		return sendToProcessor(CONSUMER_DISCONNECT, consumer);
	}

	private RCFuture<Void> producerDisconnect(IBrokerProducer producer) {
		return sendToProcessor(PRODUCER_DISCONNECT, producer);
	}

	private class ConsumerHandleImpl implements IConsumerHandle {
		private final IBrokerConsumer m_ref;

		private ConsumerHandleImpl(IBrokerConsumer consumerRef) {
			m_ref = consumerRef;
		}

		@Override
		public RCFuture<Void> disconnect() {
			return consumerDisconnect(m_ref);
		}
	}

	private class ProducerHandleImpl implements IConsumerHandle {
		private final IBrokerProducer m_ref;

		private ProducerHandleImpl(IBrokerProducer producerRef) {
			m_ref = producerRef;
		}

		@Override
		public RCFuture<Void> disconnect() {
			return producerDisconnect(m_ref);
		}
	}

	private final void handleException(Throwable t) {
		if (m_exceptionHandler != null) {
			m_exceptionHandler.handleException(t);
		}
	}

	private final <T> void setFailure(RCPromise<T> p, Throwable cause) {
		try {
			p.setFailure(cause);
		} catch(Throwable t) {
			handleException(t);
		} finally {
			p.release();
		}
	}

	private final <T> void setSuccess(RCPromise<T> p, T result) {
		try {
			p.setSuccess(result);
		} catch(Throwable t) {
			handleException(t);
		} finally {
			p.release();
		}
	}

	private class Worker extends PerpetualWork {
		private IBrokerConsumer m_consumer;
		private IBrokerProducer m_producer;

		private void processBag0(ParamBag bag) {
			final int cmd = bag.pi();
			final RCPromise<Object> commandDone = (RCPromise<Object>)bag.po();
			switch(cmd) {
				case CONSUMER_CONNECT:
					if (m_consumer != null) {
						setFailure(commandDone, new IllegalStateException("There is already a consumer registered: " + m_consumer));
						return;
					}
					m_consumer = (IBrokerConsumer)bag.po();
					setSuccess(commandDone, new ConsumerHandleImpl(m_consumer));
					if (m_clearPipeOnConsumerConnect) {
						m_pipeBuffer.clear();
					}
					break;
				case CONSUMER_DISCONNECT:
					IBrokerConsumer disconnectConsumer = (IBrokerConsumer)bag.po();
					if (m_consumer != disconnectConsumer) {
						setFailure(commandDone, new IllegalStateException("Consumer requested to be disconnected " + disconnectConsumer + " was not the currently registered consumer " + m_consumer));
						return;
					}
					m_consumer = null;
					setSuccess(commandDone, null);
					break;

				case PRODUCER_CONNECT:
					if (m_producer != null) {
						setFailure(commandDone, new IllegalStateException("There is already a producer registered: " + m_producer));
						return;
					}
					m_producer = (IBrokerProducer)bag.po();
					setSuccess(commandDone, new ProducerHandleImpl(m_producer));
					m_pipeBuffer.enqueue(PRODUCER_HAS_OPENED);
					break;
				case PRODUCER_DISCONNECT:
					IBrokerProducer disconnectProducer = (IBrokerProducer)bag.po();
					if (m_producer != disconnectProducer) {
						setFailure(commandDone, new IllegalStateException("Producer requested to be disconnected " + disconnectProducer + " was not the currently registered producer " + m_producer));
						return;
					}
					m_producer = null;
					setSuccess(commandDone, null);
					m_pipeBuffer.enqueue(PRODUCER_HAS_CLOSED);
					break;
			}
		}

		protected void _doWork() {
			while(true) {
				ParamBag bag = (ParamBag)m_commandQueue.dequeue();
				if (bag == null) {
					break;
				}
				try {
					processBag0(bag);
				} finally {
					bag.release();
				}
			}
			/*
				Move objects from the producer into the pipe's queue
			*/
			if (m_producer != null) {
				while(!m_pipeBuffer.isFull()) {
					Object o = m_producer.produce();
					if (o == null) {
						break;
					}
					m_pipeBuffer.enqueue(o);
				}
			}
			/*
				Move objects from the pipe's queue to the consumer
			*/
			if (m_consumer != null) {
				while(true) {
					Object o = m_pipeBuffer.peek();
					if (o == null) {
						break;
					}
					if (o == PRODUCER_HAS_OPENED) {
						m_consumer.producerOpened();
					} else if (o == PRODUCER_HAS_CLOSED) {
						m_consumer.producerClosed();
					} else if (!m_consumer.consume(o)) {
						break;
					}
					m_pipeBuffer.dequeue();
				}
			}
		}
	}

	public interface IExceptionHandler {
		void handleException(Throwable t);
	}

	public interface IBrokerConsumer {
		boolean consume(Object obj);
		void producerClosed();
		void producerOpened();
	}
	public interface IConsumerHandle {
		RCFuture<Void> disconnect();
	}

	public interface IBrokerProducer {
		Object produce();
	}
	public interface IProducerHandle {
		RCFuture<Void> disconnect();
	}



}
