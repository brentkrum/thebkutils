package com.thebk.utils;

import io.netty.util.*;

import java.util.concurrent.atomic.*;

class InternalMPSCFixedOneShotQueue extends AbstractReferenceCounted {
	static final int QUEUE_SLICE_SIZE = 4096;
	private static final ResourceLeakDetector<InternalMPSCFixedOneShotQueue> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(InternalMPSCFixedOneShotQueue.class, 1);
	private static final Recycler<InternalMPSCFixedOneShotQueue> RECYCLER = new Recycler<InternalMPSCFixedOneShotQueue>() {
		@Override
		protected InternalMPSCFixedOneShotQueue newObject(Recycler.Handle<InternalMPSCFixedOneShotQueue> handle) {
			return new InternalMPSCFixedOneShotQueue(handle);
		}
	};
	private static final AtomicLongFieldUpdater<InternalMPSCFixedOneShotQueue> m_writeableIndexUpdater = AtomicLongFieldUpdater.newUpdater(InternalMPSCFixedOneShotQueue.class, "m_writeableIndex");
	private static final AtomicLongFieldUpdater<InternalMPSCFixedOneShotQueue> m_readableCountUpdater = AtomicLongFieldUpdater.newUpdater(InternalMPSCFixedOneShotQueue.class, "m_readableCount");
	private static final AtomicReferenceFieldUpdater<InternalMPSCFixedOneShotQueue, InternalMPSCFixedOneShotQueue> m_nextUpdate = AtomicReferenceFieldUpdater.newUpdater(InternalMPSCFixedOneShotQueue.class, InternalMPSCFixedOneShotQueue.class, "m_next");

	private static final int m_maxQueueSize = QUEUE_SLICE_SIZE;

	private final Recycler.Handle<InternalMPSCFixedOneShotQueue> m_handle;
	private final AtomicLong m_status[]; // 0 - unset, 1-done, 2-comitted
	private final Object m_values[];

	private ResourceLeakTracker<InternalMPSCFixedOneShotQueue> m_leakTracker;
	private long m_readableIndex;
	private volatile long m_writeableIndex;
	private volatile long m_readableCount;
	private volatile InternalMPSCFixedOneShotQueue m_next;

	public InternalMPSCFixedOneShotQueue(Recycler.Handle<InternalMPSCFixedOneShotQueue> handle) {
		m_handle = handle;
		m_status = new AtomicLong[m_maxQueueSize];
		for(int i=0; i<m_maxQueueSize; i++) {
			m_status[i] = new AtomicLong();
		}
		m_status[0].set(0xF0);
		m_values = new Object[m_maxQueueSize];
	}

	private void init() {
		m_leakTracker = LEAK_DETECT.track(this);
	}

	public static InternalMPSCFixedOneShotQueue create() {
		InternalMPSCFixedOneShotQueue ref = RECYCLER.get();
		ref.init();
		return ref;
	}

	public boolean isWriteFull() {
		return (m_writeableIndex >= m_maxQueueSize);
	}

	public boolean enqueue(Object o, RCBoolOutParam committed) {
		long indexLong = m_writeableIndexUpdater.getAndIncrement(this); // Get current value THEN increment
		if (indexLong >= m_maxQueueSize) {
			committed.value = false;
			return false;
		}
		int index = (int)indexLong;
		long committerFlag = (m_status[index].get() & 0xF0);
		boolean committer = (committerFlag == 0xF0);

		m_values[index] = o;

		// If we are not the comitter, set to 1.  If it fails, we have become the comitter
		if (!committer && m_status[index].compareAndSet(0, 1) == true) {
			return true;
		}
		// This thread is the committer
		// We skip updating ourselves to 1, we will go directly to 2
		committed.value = true;

		int numUpdated = 1;
		long nextIndexLong = indexLong;
		int nextIndex = index;
		while(nextIndexLong < m_maxQueueSize) {
			// Allow the value at nextIndex to be read
			m_status[nextIndex].set(2);

			// Now check the next index
			nextIndexLong++;
			nextIndex = (int)nextIndexLong;
			if (nextIndexLong == m_maxQueueSize) {
				break;
			}
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
						break;
					case 2:
					default:
						throw new RuntimeException("This should never happen");
				}
			}
			// The next index is 1, so we will commit it
			numUpdated++;
		}
		// We did not hand off the commit token so the whole queue is now comitted
		m_readableCountUpdater.getAndAdd(this, numUpdated);
		return true;
	}

	public Object dequeue() {
		if (m_readableCount == 0 || m_readableIndex >= m_maxQueueSize) {
			return null;
		}
		m_readableCountUpdater.decrementAndGet(this);
		int index = (int)m_readableIndex++;

		Object o = m_values[index];
		m_values[index] = null;
		// If we cannot set the 2 to a 0, then it is a F2, so we set to F0
		if (m_status[index].compareAndSet(2, 0) == false) {
			m_status[index].set(0xF0);
		}
		return o;
	}

	public boolean isReadUsedUp() {
		return (m_readableIndex >= m_maxQueueSize);
	}

	public Object peek() {
		if (m_readableCount == 0 || m_readableIndex >= m_maxQueueSize) {
			return null;
		}
		int index = (int)m_readableIndex;
		return m_values[index];
	}

	@Override
	protected void deallocate() {
		m_values[0] = null;
		m_status[0].set(0xF0);
		for(int i=1; i<m_maxQueueSize; i++) {
			m_values[i] = null;
			m_status[i].set(0);
		}
		m_readableIndex = 0;
		m_writeableIndex = 0;
		m_readableCount = 0;
		m_next = null;

		m_leakTracker.close(this);
		setRefCnt(1);
		m_handle.recycle(this);
	}

	@Override
	public InternalMPSCFixedOneShotQueue touch(Object hint) {
		m_leakTracker.record(hint);
		return this;
	}

}