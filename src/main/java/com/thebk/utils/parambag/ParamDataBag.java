package com.thebk.utils.parambag

import io.netty.util.*;

public class ParamDataBag extends ParamBag<ParamDataBag> {
	private static final ResourceLeakDetector<ParamDataBag> LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(ParamDataBag.class, 1);
	private static final Recycler<ParamDataBag> RECYCLER = new Recycler<ParamDataBag>() {
		@Override
		protected ParamDataBag newObject(Handle<ParamDataBag> handle) {
			return new ParamDataBag(handle);
		}
	};
	private final Recycler.Handle<ParamDataBag> m_recylerHandle;

	private ResourceLeakTracker<ParamDataBag> m_leakTracker;

	protected ParamDataBag(Recycler.Handle<ParamDataBag> handle) {
		m_recylerHandle = handle;
	}

	public static ParamDataBag create() {
		ParamDataBag bag = RECYCLER.get();
		bag.init();
		return bag;
	}

	private void init() {
		m_leakTracker = LEAK_DETECT.track(this);
	}

	@Override
	protected void deallocate() {
		super.deallocate();
		if (m_leakTracker != null) {
			m_leakTracker.close(this);
			m_leakTracker = null;
		}
		m_recylerHandle.recycle(this);
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		if (m_leakTracker != null) {
			m_leakTracker.record(hint);
		}
		return this;
	}

	@Override
	public ParamDataBag retain() {
		return (ParamDataBag)super.retain();
	}
}
