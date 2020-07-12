package com.thebk.utils.concurrent;

public interface RCFutureListenerWithParams<T> {
	void operationComplete(RCFuture<T> f, ParamBag<?> params);
}
