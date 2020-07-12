package com.thebk.utils.rc;

import io.netty.util.*;

public class RCBoolean extends AbstractReferenceCounted {
	private static final ResourceLeakDetector<RCBoolean> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(RCBoolean.class, 1);
	private static final Recycler<RCBoolean> RECYCLER = new Recycler<RCBoolean>() {
		@Override
		protected RCBoolean newObject(Recycler.Handle<RCBoolean> handle) {
			return new RCBoolean(handle);
		}
	};

	private final Recycler.Handle<RCBoolean> m_handle;
	private ResourceLeakTracker<RCBoolean> m_leakTracker;
	private boolean m_value;

	private RCBoolean(Recycler.Handle<RCBoolean> handle) {
		m_handle = handle;
	}

	private RCBoolean init(boolean value) {
		m_leakTracker = LEAK_DETECT.track(this);
		m_value = value;
		return this;
	}

	public static RCBoolean create(boolean value) {
		return RECYCLER.get().init(value);
	}

	public boolean value() {
		return m_value;
	}

	public void set(boolean value) {
		m_value = value;
	}

	@Override
	public RCBoolean retain() {
		super.retain();
		return this;
	}

	@Override
	public RCBoolean touch(Object hint) {
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
