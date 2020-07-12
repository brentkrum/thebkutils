package com.thebk.utils;

public class MPSCUnboundedQueue {
	private volatile LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> m_currentWrite = null;

	public MPSCUnboundedQueue() {
		m_currentWrite = new LinkedTSNonBlockingRefCounter<>(InternalMPSCFixedOneShotQueue.create());
	}

	public boolean enqueue(Object o, RCBoolOutParam committed) {
		while(true) {
			// m_currentWrite is never null
			LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> currentWrite = m_currentWrite;
			LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> lastWrite = null;
			while (currentWrite != null) {
				InternalMPSCFixedOneShotQueue queue = currentWrite.checkout();
				if (queue != null) {
					if (!queue.isWriteFull() && queue.enqueue(o, committed)) {
						currentWrite.checkin();
						return true;
					}
					currentWrite.checkin();
				}
				lastWrite = currentWrite;
				currentWrite = lastWrite.next();
			}
			// If we are here, then lastWrite.next() returned null
			final LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> newWrite = new LinkedTSNonBlockingRefCounter<>(InternalMPSCFixedOneShotQueue.create());
			// In the above loop, lastWrite was set to the last link in the chain at the time
			LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> cw = lastWrite;
			// Find the last link in the chain and add our new queue
			while(true) {
				final LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> next = cw.next();
				if (next == null && cw.setNext(newWrite)) {
					break;
				}
				cw = cw.next();
			}
		}
	}

	public Object dequeue() {
		while (true) {
			LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> currentWrite = m_currentWrite;
			InternalMPSCFixedOneShotQueue queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> nextWrite = currentWrite.next();
				if (nextWrite == null) {
					// The currentWrite pos is the last link in the chain
					return null;
				}
				currentWrite.clear();

				// Update start of chain to the next link
				m_currentWrite = nextWrite;
				continue;
			}
			// This could return null if things have not been committed yet, but that is ok
			final Object o = queue.dequeue();
			currentWrite.checkin();
			return o;
		}
	}

	public Object peek() {
		while (true) {
			LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> currentWrite = m_currentWrite;
			InternalMPSCFixedOneShotQueue queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<InternalMPSCFixedOneShotQueue> nextWrite = currentWrite.next();
				if (nextWrite == null) {
					// The currentWrite pos is the last link in the chain
					return null;
				}
				currentWrite.clear();

				System.out.println("Update chain");
				// Update start of chain to the next link
				m_currentWrite = nextWrite;
				continue;
			}
			// This could return null if things have not been committed yet, but that is ok
			final Object o = queue.peek();
			currentWrite.checkin();
			return o;
		}
	}

	/**
	 * This is not thread-safe
	 */
	public void close() {
		m_currentWrite.clear();
		m_currentWrite = null;
	}
}