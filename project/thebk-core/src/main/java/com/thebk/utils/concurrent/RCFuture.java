package com.thebk.utils.concurrent;

import com.thebk.utils.parambag.CallbackParamBag;
import com.thebk.utils.parambag.ParamBag;
import io.netty.util.ReferenceCounted;

import java.util.concurrent.TimeoutException;

public interface RCFuture<T> extends ReferenceCounted {
	/**
	 * The parameterized callback on the ParamBag will be called with the RCFuture passed in as the
	 * first object.  Since the ref is given to the bag, the supplied future MUST be relesaed()
	 */
	RCFuture<T> addListener(CallbackParamBag params);
	RCFuture<T> addListener(RCFutureListenerWithParams<T> listener, ParamBag params);
	RCFuture<T> addListener(RCFutureListener<T> listener);
	RCFuture<T> addChain(RCPromise<T> chain);
	RCFuture<T> addTriggerSuccess(RCPromise<Void> trigger);

	T get();
	T get(long timeoutInMS) throws TimeoutException;
	Throwable cause();
	boolean isSuccess();
	boolean isDone();
	boolean await(long timeoutInMS);

	RCFuture<T> retain();
	RCFuture<T> retain(int increment);
	RCFuture<T> touch();
	RCFuture<T> touch(Object hint);
}
