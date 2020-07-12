package com.thebk.utils.queue;

import com.denaliai.fw.utility.concurrent.RCPromise;
import com.thebk.utils.rc.RCBoolean;
import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MPSCUnboundedQueue_Test extends TestBase {
	private static final int NUM_TO_PRODUCE = 500_000;

	@Test
	public void simple() {
		MPSCUnboundedQueue q = new MPSCUnboundedQueue();
		RCBoolean committed = RCBoolean.create(false);

		int value = 1;
		for(int i = 0; i< InternalMPSCFixedOneShotQueue.QUEUE_SLICE_SIZE; i++) {
			Assertions.assertTrue(q.enqueue(new Integer(value++), committed));
			Assertions.assertTrue(committed.value);
		}
		Assertions.assertEquals(1, ((Integer)q.peek()).intValue());
		for(int i=0; i<InternalMPSCFixedOneShotQueue.QUEUE_SLICE_SIZE/2; i++) {
			Assertions.assertTrue(q.enqueue(new Integer(value++), committed));
			Assertions.assertTrue(committed.value);
		}
		Assertions.assertEquals(1, ((Integer)q.peek()).intValue());
		for(int i=1; i<value; i++) {
			Assertions.assertEquals(i, ((Integer)q.dequeue()).intValue());
		}
		Assertions.assertNull(q.dequeue());
		Assertions.assertNull(q.peek());
		q.close();
	}

	@Test
	public void multiProducerSingleConsumer() throws InterruptedException {
		MPSCUnboundedQueue q = new MPSCUnboundedQueue();
		Producer threads[] = new Producer[5];

		Consumer consumer = new Consumer(q, threads.length);
		for(int i=0; i<threads.length; i++) {
			threads[i] = new Producer(q, i);
			threads[i].start();
		}
		consumer.start();
		for(int i=0; i<threads.length; i++) {
			Assertions.assertTrue(threads[i].m_done.await(10000));
			threads[i].m_done.get();
			Assertions.assertTrue(threads[i].m_done.isSuccess());
		}
		Assertions.assertTrue(consumer.m_done.await(10000));
		consumer.m_done.get();
		Assertions.assertTrue(consumer.m_done.isSuccess());
		q.close();
	}

	private static final class Producer extends Thread {
		private final RCPromise<Void> m_done = RCPromise.create();
		private final MPSCUnboundedQueue m_q;
		private final int m_index;

		Producer(MPSCUnboundedQueue q, int index) {
			m_q = q;
			m_index = index;
		}

		@Override
		public void run() {
			try {
				_run();
				m_done.setSuccess(null);
			}  catch(Exception ex) {
				m_done.setFailure(ex);
			}
		}

		private void _run() {
			RCBoolean comitted = RCBoolean.create(false);
			for(int i=0; i<NUM_TO_PRODUCE; i++) {
				if (!m_q.enqueue(new Message(m_index, i), comitted)) {
					throw new RuntimeException("Should not happen");
				}
			}
			comitted.release();
		}
	}

	private static final class Consumer extends Thread {
		private final RCPromise<Void> m_done = RCPromise.create();
		private final MPSCUnboundedQueue m_q;
		private int[] m_numDequeued;
		private int m_numDone;
		private volatile boolean m_shutdown;

		Consumer(MPSCUnboundedQueue q, int numProducers) {
			m_q = q;
			m_numDequeued = new int[numProducers];
		}

		public void shutdown() {
			m_shutdown = true;
		}

		@Override
		public void run() {
			try {
				_run();
				m_done.setSuccess(null);
			}  catch(Exception ex) {
				m_done.setFailure(ex);
			}
		}

		private void _run() {
			while(!m_shutdown) {
				Message msg = (Message)m_q.dequeue();
				if (msg == null) {
					Thread.yield();
					continue;
				}
				if (msg.Value != m_numDequeued[msg.Index]) {
					throw new RuntimeException("m_numDequeued[" + msg.Index + "] " + m_numDequeued[msg.Index] + " != " + msg.Value);
				}
				m_numDequeued[msg.Index]++;
				if (m_numDequeued[msg.Index] == NUM_TO_PRODUCE) {
					m_numDone++;
					if (m_numDone == m_numDequeued.length) {
						return;
					}
				}
			}
		}
	}

	private static class Message {
		public final int Index;
		public final int Value;

		Message(int index, int value) {
			Index = index;
			Value = value;
		}
	}
}
