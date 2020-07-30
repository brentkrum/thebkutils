package com.thebk.utils.queue;

import com.thebk.utils.concurrent.LinkedTSNonBlockingRefCounter;
import com.thebk.utils.rc.RCBoolean;

public class MPSCUnboundedQueue implements TheBKQueue {
	private volatile LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> m_currentWrite = null;

	public MPSCUnboundedQueue() {
		m_currentWrite = new LinkedTSNonBlockingRefCounter<>(MPSCUnboundedQueueSlice.create());
	}

	@Override
	public boolean enqueue(Object o, RCBoolean committed) {
		while(true) {
			// m_currentWrite is never null
			LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
			LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> lastWrite = null;
			while (currentWrite != null) {
				MPSCUnboundedQueueSlice queue = currentWrite.checkout();
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
			final LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> newWrite = new LinkedTSNonBlockingRefCounter<>(MPSCUnboundedQueueSlice.create());
			// In the above loop, lastWrite was set to the last link in the chain at the time
			LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> cw = lastWrite;
			// Find the last link in the chain and add our new queue
			while(true) {
				final LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> next = cw.next();
				if (next == null && cw.setNext(newWrite)) {
					break;
				}
				cw = cw.next();
			}
		}
	}

	@Override
	public boolean enqueue(Object o) {
		RCBoolean committed = RCBoolean.create(false);
		if (enqueue(o, committed)) {
			committed.release();
			return true;
		}
		committed.release();
		return false;
	}

	@Override
	public Object dequeue() {
		while (true) {
			LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
			MPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> nextWrite = currentWrite.next();
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
			LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
			MPSCUnboundedQueueSlice queue = currentWrite.checkout();
			if (queue.isReadUsedUp()) {
				currentWrite.checkin();

				LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> nextWrite = currentWrite.next();
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
		LinkedTSNonBlockingRefCounter<MPSCUnboundedQueueSlice> currentWrite = m_currentWrite;
		int size = 0;
		while (currentWrite != null) {
			MPSCUnboundedQueueSlice queue = currentWrite.checkout();
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