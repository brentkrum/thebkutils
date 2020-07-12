package com.thebk.utils.parambag

import io.netty.util.*;

public class CallbackParamBag extends ParamBag<CallbackParamBag> {
	private static final ResourceLeakDetector<CallbackParamBag> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(CallbackParamBag.class, 1);
	private static final Recycler<CallbackParamBag> RECYCLER = new Recycler<CallbackParamBag>() {
		@Override
		protected CallbackParamBag newObject(Handle<CallbackParamBag> handle) {
			return new CallbackParamBag(handle);
		}
	};
	private final Recycler.Handle<CallbackParamBag> m_recylerHandle;

	private ResourceLeakTracker<CallbackParamBag> m_leakTracker;
	private ParameterizedCallback m_callback;
	private LocklessProcessing m_callbackProcessing;

	CallbackParamBag(Recycler.Handle<CallbackParamBag> handle) {
		m_recylerHandle = handle;
	}

	public static CallbackParamBag create(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		CallbackParamBag bag = RECYCLER.get();
		bag.init(callback, callbackProcessing);
		return bag;
	}

	public static CallbackParamBag create(ParameterizedCallback callback) {
		CallbackParamBag bag = RECYCLER.get();
		bag.init(callback, null);
		return bag;
	}

	public static CallbackParamBag createStatic(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		CallbackParamBag bag = new StaticCallbackParamBag();
		bag.staticInit(callback, callbackProcessing);
		return bag;
	}

	private void init(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		m_leakTracker = LEAK_DETECT.track(this);
		m_callback = callback;
		m_callbackProcessing = callbackProcessing;
	}

	private void staticInit(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		m_leakTracker = LEAK_DETECT.track(this);
		m_callback = callback;
		m_callbackProcessing = callbackProcessing;
	}

	void locklessCallback() {
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
	public CallbackParamBag retain() {
		return (CallbackParamBag)super.retain();
	}
}
