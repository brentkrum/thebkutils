package com.thebk.utils.parambag;

import com.thebk.utils.concurrent.LocklessProcessing;
import io.netty.util.*;

public class LocklessCallbackParamBag extends CallbackParamBag<LocklessCallbackParamBag> {
	private static final ResourceLeakDetector<LocklessCallbackParamBag> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(LocklessCallbackParamBag.class, 1);
	private static final Recycler<LocklessCallbackParamBag> RECYCLER = new Recycler<LocklessCallbackParamBag>() {
		@Override
		protected LocklessCallbackParamBag newObject(Handle<LocklessCallbackParamBag> handle) {
			return new LocklessCallbackParamBag(handle);
		}
	};
	private final Recycler.Handle<LocklessCallbackParamBag> m_recylerHandle;

	private ResourceLeakTracker<LocklessCallbackParamBag> m_leakTracker;
	private ParameterizedCallback m_callback;
	private LocklessProcessing m_callbackProcessing;

	LocklessCallbackParamBag(Recycler.Handle<LocklessCallbackParamBag> handle) {
		m_recylerHandle = handle;
	}

	public static LocklessCallbackParamBag create(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		LocklessCallbackParamBag bag = RECYCLER.get();
		bag.init(callback, callbackProcessing);
		return bag;
	}

	public static LocklessCallbackParamBag create(ParameterizedCallback callback) {
		LocklessCallbackParamBag bag = RECYCLER.get();
		bag.init(callback, null);
		return bag;
	}

	public static LocklessCallbackParamBag createStatic(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		LocklessCallbackParamBag bag = new StaticLocklessCallbackParamBag();
		bag.staticInit(callback, callbackProcessing);
		return bag;
	}

	private void init(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		m_leakTracker = LEAK_DETECT.track(this);
		m_callback = callback;
		m_callbackProcessing = callbackProcessing;
	}

	private void staticInit(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		m_callback = callback;
		m_callbackProcessing = callbackProcessing;
	}

	public LocklessProcessing processing() {
		return m_callbackProcessing;
	}

	public void locklessCallback() {
		m_callback.callback(this);
	}

	public void callback() {
		if (m_callbackProcessing != null) {
			m_callbackProcessing.runWork(this);
		} else {
			m_callback.callback(this);
		}
	}

	@Override
	protected void deallocate() {
		super.deallocate();
		m_callback = null;
		m_callbackProcessing = null;

		if (m_leakTracker != null) {
			m_leakTracker.close(this);
			m_leakTracker = null;
		}
		if (m_recylerHandle != null) {
			m_recylerHandle.recycle(this);
		}
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		if (m_leakTracker != null) {
			m_leakTracker.record(hint);
		}
		return this;
	}

	@Override
	public LocklessCallbackParamBag retain() {
		return (LocklessCallbackParamBag)super.retain();
	}
}
