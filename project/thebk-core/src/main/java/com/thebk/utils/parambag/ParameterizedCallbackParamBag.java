package com.thebk.utils.parambag;

import com.thebk.utils.concurrent.LocklessProcessing;
import io.netty.util.*;

public class ParameterizedCallbackParamBag extends CallbackParamBag<ParameterizedCallbackParamBag> {
	private static final ResourceLeakDetector<ParameterizedCallbackParamBag> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ParameterizedCallbackParamBag.class, 1);
	private static final Recycler<ParameterizedCallbackParamBag> RECYCLER = new Recycler<ParameterizedCallbackParamBag>() {
		@Override
		protected ParameterizedCallbackParamBag newObject(Handle<ParameterizedCallbackParamBag> handle) {
			return new ParameterizedCallbackParamBag(handle);
		}
	};
	private final Recycler.Handle<ParameterizedCallbackParamBag> m_recylerHandle;

	private ResourceLeakTracker<ParameterizedCallbackParamBag> m_leakTracker;
	private ParameterizedCallback m_callback;

	ParameterizedCallbackParamBag(Recycler.Handle<ParameterizedCallbackParamBag> handle) {
		m_recylerHandle = handle;
	}

	public static ParameterizedCallbackParamBag create(ParameterizedCallback callback) {
		ParameterizedCallbackParamBag bag = RECYCLER.get();
		bag.init(callback, null);
		return bag;
	}

	public static ParameterizedCallbackParamBag createStatic(ParameterizedCallback callback) {
		ParameterizedCallbackParamBag bag = new StaticParameterizedCallbackParamBag();
		bag.staticInit(callback);
		return bag;
	}

	private void init(ParameterizedCallback callback, LocklessProcessing callbackProcessing) {
		m_leakTracker = LEAK_DETECT.track(this);
		m_callback = callback;
	}

	private void staticInit(ParameterizedCallback callback) {
		m_callback = callback;
	}

	public void locklessCallback() {
		m_callback.callback(this);
	}

	public void callback() {
		m_callback.callback(this);
	}

	@Override
	protected void deallocate() {
		super.deallocate();
		m_callback = null;
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
