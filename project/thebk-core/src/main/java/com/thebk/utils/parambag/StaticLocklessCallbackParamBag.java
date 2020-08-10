package com.thebk.utils.parambag;

class StaticLocklessCallbackParamBag extends LocklessCallbackParamBag {
	StaticLocklessCallbackParamBag() {
		super(null);
	}


	public int refCnt() {
		return 1;
	}

	@Override
	public StaticLocklessCallbackParamBag retain(int increment) {
		return this;
	}

	@Override
	public StaticLocklessCallbackParamBag touch() {
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
	public StaticLocklessCallbackParamBag touch(Object hint) {
		return this;
	}

	@Override
	public StaticLocklessCallbackParamBag retain() {
		return this;
	}
}
