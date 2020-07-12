package com.thebk.utils;

import io.netty.util.*;

public class RCBoolOutParam extends AbstractReferenceCounted {
	private static final ResourceLeakDetector<RCBoolOutParam> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(RCBoolOutParam.class, 1);
	private static final Recycler<RCBoolOutParam> RECYCLER = new Recycler<RCBoolOutParam>() {
		@Override
		protected RCBoolOutParam newObject(Recycler.Handle<RCBoolOutParam> handle) {
			return new RCBoolOutParam(handle);
		}
	};

	private final Recycler.Handle<RCBoolOutParam> m_handle;
	private ResourceLeakTracker<RCBoolOutParam> m_leakTracker;
	public boolean value;

	private RCBoolOutParam(Recycler.Handle<RCBoolOutParam> handle) {
		m_handle = handle;
	}

	private RCBoolOutParam init(boolean value) {
		m_leakTracker = LEAK_DETECT.track(this);
		value = value;
		return this;
	}

	public static RCBoolOutParam create(boolean value) {
		return RECYCLER.get().init(value);
	}

	@Override
	public RCBoolOutParam retain() {
		super.retain();
		return this;
	}

	@Override
	public RCBoolOutParam touch(Object hint) {
		if (m_leakTracker != null) {
			m_leakTracker.record(hint);
		}
		return this;
	}

	@Override
	protected void deallocate() {
		if (m_leakTracker != null) {
			m_leakTracker.close(this);
			m_leakTracker = null;
		}

		setRefCnt(1);
		m_handle.recycle(this);
	}
}
