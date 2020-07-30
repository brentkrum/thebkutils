package com.thebk.utils.queue;

import com.thebk.utils.rc.RCBoolean;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public class MPSCFixedQueue implements TheBKQueue {
	private static final AtomicLongFieldUpdater<MPSCFixedQueue> m_writeableIndexUpdater = AtomicLongFieldUpdater.newUpdater(MPSCFixedQueue.class, "m_writeableIndex");
	private static final AtomicLongFieldUpdater<MPSCFixedQueue> m_readableCountUpdater = AtomicLongFieldUpdater.newUpdater(MPSCFixedQueue.class, "m_readableCount");
	private static final AtomicLongFieldUpdater<MPSCFixedQueue> m_writableCountUpdater = AtomicLongFieldUpdater.newUpdater(MPSCFixedQueue.class, "m_writableCount");

	private final AtomicLong m_status[]; // 0 - unset, 1-done, 2-comitted
	private final Object m_values[];
	private final int m_maxQueueSize;
	private long m_readableIndex;
	private volatile long m_writeableIndex;
	private volatile long m_writableCount;
	private volatile long m_readableCount = 0;

	public MPSCFixedQueue(int maxQueueSize) {
		if (maxQueueSize < 1) {
			throw new IllegalArgumentException("maxQueueSize cannot be less than 1");
		}
		m_maxQueueSize = maxQueueSize;
		m_status = new AtomicLong[maxQueueSize];
		for(int i=0; i<maxQueueSize; i++) {
			m_status[i] = new AtomicLong();
		}
		m_status[0].set(0xF0);
		m_values = new Object[maxQueueSize];
		m_writableCount = maxQueueSize;
	}


	@Override
	public boolean enqueue(Object o, RCBoolean committed) {
		long w = m_writableCountUpdater.decrementAndGet(this);
		if (w < 0) {
			m_writableCountUpdater.incrementAndGet(this);
			committed.set(false);
			return false;
		}
		long indexLong = m_writeableIndexUpdater.getAndIncrement(this); // Get current value THEN increment
		int index = (int)(indexLong % m_maxQueueSize);
		long committerFlag = (m_status[index].get() & 0xF0);
		boolean committer = (committerFlag == 0xF0);

		m_values[index] = o;

		// If we are not the comitter, set to 1.  If it fails, we have become the comitter
		if (!committer && m_status[index].compareAndSet(0, 1) == true) {
			return true;
		}
		// This thread is the committer
		// We skip updating ourselves to 1, we will go directly to 2
		committed.set(true);

		int numUpdated = 1;
		long nextIndexLong = indexLong;
		int nextIndex = index;
		while(true) {
			// Allow the value at nextIndex to be read
			m_status[nextIndex].set(2);

			// Now check the next index
			nextIndexLong++;
			nextIndex = (int)(nextIndexLong % m_maxQueueSize);

			// Try to assign the next index as the next comitter
			outer_loop:
			while(true) {
				// NOTE: We would be in a race with the reader who will be flipping 2's to 0's which is
				// why we do not update m_readableCountUpdater until the end so we don't end up stuck
				// in the queue for longer than worst case the maximum number of entries
				switch((int)m_status[nextIndex].get()) {
					case 1:
						break outer_loop;
					case 0:
						if (m_status[nextIndex].compareAndSet(0, 0xF0) == true) {
							// We do not release the updates until everything has been processed
							// to prevent this thread from becoming forever trapped in the queue
							m_readableCountUpdater.getAndAdd(this, numUpdated);
							return true;
						}
					case 2:
						// This will only happen if we wrap around and the queue is full
						if (m_status[nextIndex].compareAndSet(2, 0xF2) == true) {
							m_readableCountUpdater.getAndAdd(this, numUpdated);
							return true;
						}
						break;
				}
			}
			// The next index is 1, so we will commit it
			numUpdated++;
		}
		// This should never happen, we did not hand off the commit token
	}

	@Override
	public Object dequeue() {
		if (m_readableCount == 0) {
			return null;
		}
		m_readableCountUpdater.decrementAndGet(this);
		int index = (int)(m_readableIndex % m_maxQueueSize);
		m_readableIndex++;
		Object o = m_values[index];
		m_values[index] = null;
		// If we cannot set the 2 to a 0, then it is a F2, so we set to F0
		if (m_status[index].compareAndSet(2, 0) == false) {
			m_status[index].set(0xF0);
		}
		m_writableCountUpdater.incrementAndGet(this);
		return o;
	}

	@Override
	public Object peek() {
		if (m_readableCount == 0) {
			return null;
		}
		int index = (int)(m_readableIndex % m_maxQueueSize);
		return m_values[index];
	}

	@Override
	public boolean enqueue(Object o) {
		RCBoolean committed = RCBoolean.create(false);
		try {
			return enqueue(o, committed);
		} finally {
			committed.release();
		}
	}

	@Override
	public boolean isFull() {
		return (m_writableCount <= 0);
	}

	@Override
	public int size() {
		return (int)m_readableCount;
	}
}