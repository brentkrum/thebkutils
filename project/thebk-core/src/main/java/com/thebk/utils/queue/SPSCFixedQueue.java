package com.thebk.utils.queue;


import com.thebk.utils.rc.RCBoolean;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public class SPSCFixedQueue implements TheBKQueue {
	private static final AtomicLongFieldUpdater<SPSCFixedQueue> m_readableCountUpdater = AtomicLongFieldUpdater.newUpdater(SPSCFixedQueue.class, "m_readableCount");
	private static final AtomicLongFieldUpdater<SPSCFixedQueue> m_writableCountUpdater = AtomicLongFieldUpdater.newUpdater(SPSCFixedQueue.class, "m_writableCount");

	private final Object m_values[];
	private final int m_maxQueueSize;
	private volatile long m_writableCount;
	private volatile long m_readableCount = 0;
	private long m_readableIndex;
	private long m_writeableIndex;

	public SPSCFixedQueue(int maxQueueSize) {
		if (maxQueueSize < 1) {
			throw new IllegalArgumentException("maxQueueSize cannot be less than 1");
		}
		m_maxQueueSize = maxQueueSize;
		m_values = new Object[maxQueueSize];
		m_writableCount = maxQueueSize;
	}

	@Override
	public boolean enqueue(Object o, RCBoolean committed) {
		committed.set(true);
		return enqueue(o);
	}

	@Override
	public boolean enqueue(Object o) {
		if (m_writableCount == 0) {
			return false;
		}
		m_writableCountUpdater.decrementAndGet(this);
		int index = (int)(m_writeableIndex % m_maxQueueSize);
		m_writeableIndex++;
		m_values[index] = o;
		m_readableCountUpdater.incrementAndGet(this);
		return true;
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
	public boolean isFull() {
		return (m_writableCount == 0);
	}

	@Override
	public boolean isEmpty() {
		return (m_readableCount == 0);
	}

}