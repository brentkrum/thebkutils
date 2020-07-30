package com.thebk.utils.concurrent;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

public class ReferenceCountDelegate extends AbstractReferenceCounted {
	private final Runnable m_callOnDeallocate;

	public ReferenceCountDelegate(Runnable callOnDeallocate) {
		m_callOnDeallocate = callOnDeallocate;
	}

	public void externalSetRefCnt(int refCnt) {
		setRefCnt(refCnt);
	}

	@Override
	protected void deallocate() {
		m_callOnDeallocate.run();
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		throw new UnsupportedOperationException("This should be handled by object containing this one");
	}
}
