package com.thebk.utils.concurrent;

import com.thebk.utils.parambag.ParamBag;

public interface RCFutureListenerWithParams<T> {
	void operationComplete(RCFuture<T> f, ParamBag<?> params);
}
