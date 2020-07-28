package com.thebk.utils.rc;

import io.netty.util.*;

class StaticRCInteger extends RCInteger {
	StaticRCInteger() {
		super(null);
	}

	@Override
	public int refCnt() {
		return 1;
	}

	@Override
	public ReferenceCounted retain(int increment) {
		return this;
	}

	@Override
	public ReferenceCounted touch() {
		return this;
	}

	@Override
	public boolean release() {
		return false;
	}

	@Override
	public boolean release(int decrement) {
		return false;
	}
}
