package com.thebk.utils.concurrent;

import io.netty.util.*;

public class RCFailedFuture<T> extends AbstractReferenceCounted implements RCFuture<T> {
	private static final ResourceLeakDetector<RCFailedFuture> PROMISE_LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(RCFailedFuture.class, 1);
	private static final Recycler<RCFailedFuture> PROMISE_RECYCLER = new Recycler<RCFailedFuture>() {
		@Override
		protected RCFailedFuture newObject(Handle<RCFailedFuture> handle) {
			return new RCFailedFuture(handle);
		}
	};

	private final Recycler.Handle<RCFailedFuture> m_recylerHandle;

	private ResourceLeakTracker<RCFailedFuture> m_leakTracker;
	private Throwable m_cause;

	private RCFailedFuture(Recycler.Handle<RCFailedFuture> recylerHandle) {
		m_recylerHandle = recylerHandle;
	}

	@SuppressWarnings("unchecked")
	public static <T> RCFailedFuture<T> create(Throwable cause) {
		RCFailedFuture<T> promise = (RCFailedFuture<T>)PROMISE_RECYCLER.get();
		promise.init(cause);
		return promise;
	}

	private void init(Throwable cause) {
		m_leakTracker = PROMISE_LEAK_DETECT.track(this);
		m_cause = cause;
	}

	@Override
	public RCFailedFuture<T> addListener(RCFutureListener<T> listener, ParamBag params) {
		// Just call it directly
		listener.operationComplete(this, params);
		return this;
	}

	@Override
	public RCFailedFuture<T> addListener(RCFutureListener<T> listener) {
		// Just call it directly
		listener.operationComplete(this, null);
		return this;
	}

	/**
	 * Caller must have a ref count on this object when calling
	 * this method.
	 *
	 */
	@Override
	public boolean await(long timeoutInMS) {
		return true;
	}

	@Override
	public T get() {
		throw new RuntimeException(m_cause);
	}

	@Override
	public Throwable cause() {
		return m_cause;
	}

	@Override
	public boolean isSuccess() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		if (m_leakTracker != null) {
			m_leakTracker.record(hint);
		}
		return this;
	}

	@Override
	public RCFailedFuture<T> retain() {
		super.retain();
		return this;
	}

	@Override
	protected void deallocate() {
		if (m_leakTracker != null) {
			m_leakTracker.close(this);
		}
		m_cause = null;

		setRefCnt(1);
		m_recylerHandle.recycle(this);
	}

}
