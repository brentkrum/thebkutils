package com.thebk.utils.queue;

import com.thebk.utils.concurrent.DefaultRCPromise;
import com.thebk.utils.rc.RCBoolean;
import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MPSCFixedQueue_Test extends TestBase {

	@Test
	public void simple() {
		MPSCFixedQueue q = new MPSCFixedQueue(2);
		RCBoolean committed = RCBoolean.create(false);

		Assertions.assertTrue(q.enqueue(new Integer(1), committed));
		Assertions.assertTrue(committed.value());
		Assertions.assertEquals(1, ((Integer)q.peek()).intValue());

		Assertions.assertTrue(q.enqueue(new Integer(2), committed));
		Assertions.assertTrue(committed.value());
		Assertions.assertEquals(1, ((Integer)q.peek()).intValue());

		Assertions.assertFalse(q.enqueue(new Integer(3), committed));
		Assertions.assertFalse(committed.value());

		Assertions.assertEquals(1, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(2, ((Integer)q.peek()).intValue());
		Assertions.assertEquals(2, ((Integer)q.dequeue()).intValue());
		Assertions.assertNull(q.peek());

		Assertions.assertTrue(q.enqueue(new Integer(3), committed));
		Assertions.assertTrue(committed.value());
		Assertions.assertEquals(3, ((Integer)q.peek()).intValue());

		Assertions.assertTrue(q.enqueue(new Integer(4), committed));
		Assertions.assertTrue(committed.value());
		Assertions.assertEquals(3, ((Integer)q.peek()).intValue());

		Assertions.assertFalse(q.enqueue(new Integer(5), committed));
		Assertions.assertFalse(committed.value());

		Assertions.assertEquals(3, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(4, ((Integer)q.dequeue()).intValue());
		Assertions.assertNull(q.peek());

		committed.release();
	}

	@Test
	public void multiProducerSingleConsumer() throws InterruptedException {
		MPSCFixedQueue q = new MPSCFixedQueue(5);
		Producer threads[] = new Producer[5];

		Consumer consumer = new Consumer(q);
		for(int i=0; i<4; i++) {
			threads[i] = new Producer(q);
			threads[i].start();
		}
		consumer.start();
		for(int i=0; i<4; i++) {
			Assertions.assertTrue(threads[i].m_done.await(5000));
		}
		consumer.shutdown();
		Assertions.assertTrue(consumer.m_done.await(5000));
		Assertions.assertEquals(4*20000, consumer.m_numDequeued);
	}

	private static final class Producer extends Thread {
		private final DefaultRCPromise<Void> m_done = DefaultRCPromise.create();
		private final MPSCFixedQueue m_q;

		Producer(MPSCFixedQueue q) {
			m_q = q;
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
			for(int i=0; i<20000; i++) {
				DefaultRCPromise<Void> p = DefaultRCPromise.create();
				if (!m_q.enqueue(p.retain(), comitted)) {
					throw new RuntimeException("Should not happen");
				}
				if (!p.await(5000)) {
					throw new RuntimeException("Timeout");
				}
				p.release();
			}
			comitted.release();
		}
	}

	private static final class Consumer extends Thread {
		private final DefaultRCPromise<Void> m_done = DefaultRCPromise.create();
		private final MPSCFixedQueue m_q;
		private int m_numDequeued;
		private volatile boolean m_shutdown;

		Consumer(MPSCFixedQueue q) {
			m_q = q;
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
				DefaultRCPromise<Void> p = (DefaultRCPromise<Void>)m_q.dequeue();
				if (p != null) {
					m_numDequeued++;
					p.setSuccess(null);
					p.release();
				} else {
					Thread.yield();
				}
			}
		}
	}
}
