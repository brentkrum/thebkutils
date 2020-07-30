package com.thebk.utils.queue;

import com.thebk.utils.TestBase;
import com.thebk.utils.concurrent.RCPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

public class SPSCUnboundedQueue_Test extends TestBase {
	@BeforeAll
	public static void init() {
		System.setProperty("com.thebk.utils.SPSCUnboundedQueueSlice", "2");
	}

	@Test
	public void simple() {
		SPSCUnboundedQueue q = new SPSCUnboundedQueue();

		Assertions.assertTrue(q.enqueue(new Integer(1)));
		Assertions.assertTrue(q.enqueue(new Integer(2)));
		Assertions.assertTrue(q.enqueue(new Integer(3)));

		Assertions.assertEquals(1, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(2, ((Integer)q.dequeue()).intValue());

		Assertions.assertTrue(q.enqueue(new Integer(4)));
		Assertions.assertTrue(q.enqueue(new Integer(5)));
		Assertions.assertTrue(q.enqueue(new Integer(6)));

		Assertions.assertEquals(3, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(4, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(5, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(6, ((Integer)q.dequeue()).intValue());
	}

	@Test
	public void multithreaded() throws TimeoutException {
		SPSCUnboundedQueue q = new SPSCUnboundedQueue();

		Consumer consumer = new Consumer(q);
		Producer producer = new Producer(q);
		producer.start();
		consumer.start();

		Assertions.assertNull(producer.m_done.get(5000));
		consumer.shutdown();
		Assertions.assertNull(consumer.m_done.get(5000));
	}

	private static final class Producer extends Thread {
		private final RCPromise<Void> m_done = RCPromise.create();
		private final SPSCUnboundedQueue m_q;

		Producer(SPSCUnboundedQueue q) {
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
			for(int i=0; i<20000; i++) {
				RCPromise<Void> p = RCPromise.create();
				Assertions.assertEquals(1, p.refCnt());
				if (!m_q.enqueue(p.retain())) {
					throw new RuntimeException("Should not happen");
				}
				if (!p.await(5000)) {
					throw new RuntimeException("Timeout");
				}
				p.release();
			}
		}
	}

	private static final class Consumer extends Thread {
		private final RCPromise<Void> m_done = RCPromise.create();
		private final SPSCUnboundedQueue m_q;
		private volatile boolean m_shutdown;

		Consumer(SPSCUnboundedQueue q) {
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
				RCPromise<Void> p = (RCPromise<Void>)m_q.dequeue();
				if (p != null) {
					p.setSuccess(null);
					p.release();
				} else {
					Thread.yield();
				}
			}
		}
	}
}
