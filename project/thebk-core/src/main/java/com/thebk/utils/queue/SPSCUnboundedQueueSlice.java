package com.thebk.utils.queue;


import com.thebk.utils.config.Config;
import com.thebk.utils.rc.RCBoolean;
import io.netty.util.*;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public class SPSCUnboundedQueueSlice extends AbstractReferenceCounted implements TheBKQueue {
	static final int QUEUE_SLICE_SIZE = Config.getInt("com.thebk.utils.queue.SPSCUnboundedQueueSlice", 128);
	private static final ResourceLeakDetector<SPSCUnboundedQueueSlice> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(SPSCUnboundedQueueSlice.class, 1);
	private static final Recycler<SPSCUnboundedQueueSlice> RECYCLER = new Recycler<SPSCUnboundedQueueSlice>() {
		@Override
		protected SPSCUnboundedQueueSlice newObject(Recycler.Handle<SPSCUnboundedQueueSlice> handle) {
			return new SPSCUnboundedQueueSlice(handle);
		}
	};
	private static final AtomicLongFieldUpdater<SPSCUnboundedQueueSlice> m_readableCountUpdater = AtomicLongFieldUpdater.newUpdater(SPSCUnboundedQueueSlice.class, "m_readableCount");
	private static final int m_maxQueueSize = QUEUE_SLICE_SIZE;

	private final Recycler.Handle<SPSCUnboundedQueueSlice> m_handle;
	private final Object m_values[] = new Object[m_maxQueueSize];

	private ResourceLeakTracker<SPSCUnboundedQueueSlice> m_leakTracker;
	private volatile long m_writableCount = m_maxQueueSize;
	private volatile long m_readableCount;
	private long m_readableIndex;
	private long m_writeableIndex;

	private SPSCUnboundedQueueSlice(Recycler.Handle<SPSCUnboundedQueueSlice> handle) {
		m_handle = handle;
	}

	private void init() {
		m_leakTracker = LEAK_DETECT.track(this);
	}

	public static SPSCUnboundedQueueSlice create() {
		SPSCUnboundedQueueSlice ref = RECYCLER.get();
		ref.init();
		return ref;
	}

	@Override
	public int size() {
		return (int)m_readableCount;
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
		return o;
	}

	public boolean isReadUsedUp() {
		return (m_readableIndex >= m_maxQueueSize);
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
	protected void deallocate() {
		m_readableIndex = 0;
		m_writeableIndex = 0;
		m_readableCount = 0;
		m_writableCount = m_maxQueueSize;

		m_leakTracker.close(this);
		m_leakTracker = null;
		setRefCnt(1);
		m_handle.recycle(this);
	}

	@Override
	public SPSCUnboundedQueueSlice touch(Object hint) {
		m_leakTracker.record(hint);
		return this;
	}

}