package com.thebk.utils.parambag

import io.netty.util.*;

class StaticCallbackParamBag extends CallbackParamBag {
	StaticCallbackParamBag() {
		super(null);
	}


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

	@Override
	public ReferenceCounted touch(Object hint) {
		return this;
	}

	@Override
	public StaticCallbackParamBag retain() {
		return this;
	}
}
