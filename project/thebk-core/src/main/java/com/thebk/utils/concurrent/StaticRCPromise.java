package com.thebk.utils.concurrent;

import com.thebk.utils.DefaultSystems;
import io.netty.util.*;

import java.util.concurrent.Executor;

public class StaticRCPromise<T> extends TheBKPromise<T> implements ReferenceCounted {

    private StaticRCPromise() {
    }

    @SuppressWarnings("unchecked")
    public static <T> StaticRCPromise<T> create(Executor executor) {
        StaticRCPromise<T> p = new StaticRCPromise<T>();
        p.init(executor);
        return p;
    }

    @SuppressWarnings("unchecked")
    public static <T> StaticRCPromise<T> createSucceeded(Executor executor, T result) {
        StaticRCPromise<T> p = new StaticRCPromise<T>();
        p.init(executor);
        p.setSuccess(result);
        return p;
    }

    @SuppressWarnings("unchecked")
    public static <T> StaticRCPromise<T> create() {
        StaticRCPromise<T> p = new StaticRCPromise<T>();
        p.init(DefaultSystems.taskExecutor());
        return p;
    }

    @SuppressWarnings("unchecked")
    public static <T> StaticRCPromise<T> createSucceeded(T result) {
        StaticRCPromise<T> p = new StaticRCPromise<T>();
        p.init(DefaultSystems.taskExecutor());
        p.setSuccess(result);
        return p;
    }

    @Override
    public int refCnt() {
        return 1;
    }

    @Override
    public RCPromise<T> retain() {
        return this;
    }

    @Override
    public RCPromise<T> retain(int increment) {
        return this;
    }

    @Override
    public RCPromise<T> touch() {
        return this;
    }

    @Override
    public RCPromise<T> touch(Object hint) {
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
