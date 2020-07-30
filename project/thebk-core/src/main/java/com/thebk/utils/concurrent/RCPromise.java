package com.thebk.utils.concurrent;

import io.netty.util.ReferenceCounted;

public interface RCPromise<T> extends ReferenceCounted, RCFuture<T> {
    RCPromise<T> setSuccess(T result);
    boolean trySuccess(T result);
    boolean tryFailure(Throwable cause);
    RCPromise<T> setFailure(Throwable cause);

    RCPromise<T> retain();
    RCPromise<T> retain(int increment);
    RCPromise<T> touch();
    RCPromise<T> touch(Object hint);
}
