package com.thebk.utils.concurrent;

public interface RCFutureListener<T> {
	void operationComplete(RCFuture<T> f);
}
