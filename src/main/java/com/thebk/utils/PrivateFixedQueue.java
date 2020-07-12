package com.thebk.utils;

public class PrivateFixedQueue {
	private final Object m_values[];
	private final int m_maxQueueSize;
	private long m_writableCount;
	private long m_readableCount;
	private long m_readableIndex;
	private long m_writeableIndex;

	public PrivateFixedQueue(int maxQueueSize) {
		if (maxQueueSize < 1) {
			throw new IllegalArgumentException("maxQueueSize cannot be less than 1");
		}
		m_maxQueueSize = maxQueueSize;
		m_values = new Object[maxQueueSize];
		m_writableCount = maxQueueSize;
	}

	public boolean enqueue(Object o) {
		if (m_writableCount == 0) {
			return false;
		}
		m_writableCount--;
		int index = (int)(m_writeableIndex % m_maxQueueSize);
		m_writeableIndex++;
		m_values[index] = o;
		m_readableCount++;
		return true;
	}

	public Object dequeue() {
		if (m_readableCount == 0) {
			return null;
		}
		m_readableCount--;
		int index = (int)(m_readableIndex % m_maxQueueSize);
		m_readableIndex++;
		Object o = m_values[index];
		m_values[index] = null;
		m_writableCount++;
		return o;
	}

	public Object peek() {
		if (m_readableCount == 0) {
			return null;
		}
		int index = (int)(m_readableIndex % m_maxQueueSize);
		return m_values[index];
	}

}