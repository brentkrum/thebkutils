package com.thebk.utils.queue;

import com.thebk.utils.rc.RCBoolean;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

import java.util.Arrays;

public class PrivateFixedQueue implements TheBKQueue {
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

	@Override
	public boolean isFull() {
		return (m_writableCount == 0);
	}

	@Override
	public boolean isEmpty() {
		return (m_readableCount == 0);
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
		m_writableCount--;
		int index = (int)(m_writeableIndex % m_maxQueueSize);
		m_writeableIndex++;
		m_values[index] = o;
		m_readableCount++;
		return true;
	}

	@Override
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

	@Override
	public Object peek() {
		if (m_readableCount == 0) {
			return null;
		}
		int index = (int)(m_readableIndex % m_maxQueueSize);
		return m_values[index];
	}

	public void clear() {
		while(!isEmpty()) {
			Object o = dequeue();
			if (o instanceof ReferenceCounted) {
				ReferenceCountUtil.safeRelease(o);
			}
		}
	}
}