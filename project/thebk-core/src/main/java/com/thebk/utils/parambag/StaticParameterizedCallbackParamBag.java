package com.thebk.utils.parambag;

class StaticParameterizedCallbackParamBag extends ParameterizedCallbackParamBag {
	StaticParameterizedCallbackParamBag() {
		super(null);
	}


	public int refCnt() {
		return 1;
	}

	@Override
	public StaticParameterizedCallbackParamBag retain(int increment) {
		return this;
	}

	@Override
	public StaticParameterizedCallbackParamBag touch() {
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
	public StaticParameterizedCallbackParamBag touch(Object hint) {
		return this;
	}

	@Override
	public StaticParameterizedCallbackParamBag retain() {
		return this;
	}
}
