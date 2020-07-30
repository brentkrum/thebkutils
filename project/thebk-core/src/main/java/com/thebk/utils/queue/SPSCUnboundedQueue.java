package com.thebk.utils.queue;

import com.thebk.utils.concurrent.LinkedTSNonBlockingRefCounter;
import com.thebk.utils.rc.RCBoolean;

public class SPSCUnboundedQueue implements TheBKQueue {
	private volatile LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> m_currentWrite = null;

	public SPSCUnboundedQueue() {
		m_currentWrite = new LinkedTSNonBlockingRefCounter<>(SPSCUnboundedQueueSlice.create());
	}

	@Override
	public boolean enqueue(Object o, RCBoolean committed) {
		committed.set(true);
		return enqueue(o);
	}

	@Override
	public boolean enqueue(Object o) {
		// m_currentWrite is never null
		LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
		LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> lastWrite = currentWrite;
		while (currentWrite != null) {
			SPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue != null) {
				if (queue.enqueue(o)) {
					currentWrite.checkin();
					return true;
				}
				currentWrite.checkin();
			}
			lastWrite = currentWrite;
			currentWrite = currentWrite.next();
		}
		final LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> newWrite = new LinkedTSNonBlockingRefCounter<>(SPSCUnboundedQueueSlice.create());
		lastWrite.setNext(newWrite);
		SPSCUnboundedQueueSlice queue = newWrite.checkout();
		queue.enqueue(o);
		newWrite.checkin();
		return true;
	}

	@Override
	public Object dequeue() {
		while (true) {
			LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
			SPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> nextWrite = currentWrite.next();
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

	@Override
	public Object peek() {
		while (true) {
			LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
			SPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> nextWrite = currentWrite.next();
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
			final Object o = queue.peek();
			currentWrite.checkin();
			return o;
		}
	}

	@Override
	public boolean isFull() {
		return false;
	}

	@Override
	public int size() {
		LinkedTSNonBlockingRefCounter<SPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
		int size = 0;
		while (currentWrite != null) {
			SPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue != null) {
				size += queue.size();
				currentWrite.checkin();
			}
			currentWrite = currentWrite.next();
		}
		return size;
	}

	/**
	 * This is not thread-safe
	 */
	public void close() {
		m_currentWrite.clear();
		m_currentWrite = null;
	}
}