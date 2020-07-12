package com.thebk.utils;

import com.denaliai.fw.utility.concurrent.RCPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SPSCFixedQueue_Test extends TestBase {

	@Test
	public void simple() {
		SPSCFixedQueue q = new SPSCFixedQueue(2);

		Assertions.assertTrue(q.enqueue(new Integer(1)));
		Assertions.assertTrue(q.enqueue(new Integer(2)));
		Assertions.assertFalse(q.enqueue(new Integer(3)));

		Assertions.assertEquals(1, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(2, ((Integer)q.dequeue()).intValue());

		Assertions.assertTrue(q.enqueue(new Integer(3)));
		Assertions.assertTrue(q.enqueue(new Integer(4)));
		Assertions.assertFalse(q.enqueue(new Integer(5)));

		Assertions.assertEquals(3, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(4, ((Integer)q.dequeue()).intValue());
	}

	@Test
	public void multithreaded() throws InterruptedException {
		SPSCFixedQueue q = new SPSCFixedQueue(5);
		Producer threads[] = new Producer[1];

		Consumer consumer = new Consumer(q);
		for(int i=0; i<threads.length; i++) {
			threads[i] = new Producer(q);
			threads[i].start();
		}
		consumer.start();
		for(int i=0; i<threads.length; i++) {
			Assertions.assertTrue(threads[i].m_done.await(5000));
		}
		consumer.shutdown();
		Assertions.assertTrue(consumer.m_done.await(5000));
	}

	private static final class Producer extends Thread {
		private final RCPromise<Void> m_done = RCPromise.create();
		private final SPSCFixedQueue m_q;

		Producer(SPSCFixedQueue q) {
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
				if (!m_q.enqueue(p)) {
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
		private final SPSCFixedQueue m_q;
		private volatile boolean m_shutdown;

		Consumer(SPSCFixedQueue q) {
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
				} else {
					Thread.yield();
				}
			}
		}
	}
}
