package com.thebk.utils.concurrent;

import io.netty.util.ReferenceCounted;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class TSNonBlockingRefCounter<T extends ReferenceCounted> {
	private static final long CLEAR_VISITOR = Long.MIN_VALUE;
	private static final AtomicReferenceFieldUpdater<TSNonBlockingRefCounter, ReferenceCounted> m_objUpdater = AtomicReferenceFieldUpdater.newUpdater(TSNonBlockingRefCounter.class, ReferenceCounted.class, "m_obj");
	private static final AtomicLongFieldUpdater<TSNonBlockingRefCounter> m_refCountUpdater = AtomicLongFieldUpdater.newUpdater(TSNonBlockingRefCounter.class, "m_refCount");
	private static final AtomicLongFieldUpdater<TSNonBlockingRefCounter> m_currentVisitorUpdater = AtomicLongFieldUpdater.newUpdater(TSNonBlockingRefCounter.class, "m_currentVisitor");

	private volatile T m_obj;
	private volatile long m_refCount = 1;
	private volatile long m_currentVisitor;

	/**
	 *
	 * @param obj - reference count is passed in, this object will not call retain() but will call release()
	 *
	 */
	public TSNonBlockingRefCounter(T obj) {
		m_obj = obj;
	}

	public T checkout() {
		while(true) {
			final long currentVisitor = m_currentVisitor;
			if (currentVisitor == CLEAR_VISITOR) {
				return null;
			}
			// One or more threads context switched here.  clear() called by other thread, resulting in onFinalCheckin
			// which then calls init() again with a new object.
			addref();
			if (m_currentVisitorUpdater.compareAndSet(this, currentVisitor, currentVisitor+1)) {
				break;
			}
			release();
		}

		// m_obj is safe to use because we have a ref count preventing it from being cleared
		return m_obj;
	}

	public void checkin() {
		release();
	}

	public void clear() {
		while(true) {
			final long currentVisitor = m_currentVisitor;
			if (currentVisitor == CLEAR_VISITOR) {
				// Already cleared
				throw new IllegalStateException("Already cleared");
			}
			if (m_currentVisitorUpdater.compareAndSet(this, currentVisitor, CLEAR_VISITOR)) {
				// Release the addRef() that was done in constructor
				release();
				return;
			}
		}
	}

	private final void addref() {
		m_refCountUpdater.incrementAndGet(this);
	}

	private final void release() {
		// The only way for the ref count to hit 0 is if m_currentVisitor to be equal to CLEAR_VISITOR
		// and the addRef() in the constructor has been released
		if (m_refCountUpdater.decrementAndGet(this) == 0) {
			final T obj = m_obj;
			if (obj != null && m_objUpdater.compareAndSet(this, obj, null)) {
				obj.release();
			}
		}
	}
}
