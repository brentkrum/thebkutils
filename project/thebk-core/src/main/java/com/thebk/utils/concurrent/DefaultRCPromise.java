package com.thebk.utils.concurrent;

import com.thebk.utils.DefaultSystems;
import io.netty.util.*;

import java.util.concurrent.Executor;

public class DefaultRCPromise<T> extends TheBKPromise<T> implements ReferenceCounted {
    private static final ResourceLeakDetector<DefaultRCPromise> PROMISE_LEAK_DETECT = ResourceLeakDetectorFactory.instance().newResourceLeakDetector(DefaultRCPromise.class, 1);
    private static final Recycler<DefaultRCPromise> PROMISE_RECYCLER = new Recycler<DefaultRCPromise>() {
        @Override
        protected DefaultRCPromise newObject(Handle<DefaultRCPromise> handle) {
            return new DefaultRCPromise(handle);
        }
    };
    private final Recycler.Handle<DefaultRCPromise> m_recylerHandle;
    private ResourceLeakTracker<DefaultRCPromise> m_leakTracker;
    private final ReferenceCountDelegate m_refCounted = new ReferenceCountDelegate(() -> myDeallocate());

    private DefaultRCPromise(Recycler.Handle<DefaultRCPromise> recylerHandle) {
        m_recylerHandle = recylerHandle;
    }

    @Override
    protected void init(Executor executor) {
        super.init(executor);
        m_leakTracker = PROMISE_LEAK_DETECT.track(this);
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultRCPromise<T> create(Executor executor) {
        DefaultRCPromise<T> promise = PROMISE_RECYCLER.get();
        promise.init(executor);
        return promise;
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultRCPromise<T> create() {
        DefaultRCPromise<T> promise = PROMISE_RECYCLER.get();
        promise.init(DefaultSystems.taskExecutor());
        return promise;
    }

    @Override
    public int refCnt() {
        return m_refCounted.refCnt();
    }

    @Override
    public RCPromise<T> retain() {
        m_refCounted.retain();
        return this;
    }

    @Override
    public RCPromise<T> retain(int increment) {
        m_refCounted.retain(increment);
        return this;
    }

    @Override
    public RCPromise<T> touch() {
        if (m_leakTracker != null) {
            m_leakTracker.record();
        }
        return this;
    }

    @Override
    public RCPromise<T> touch(Object hint) {
        if (m_leakTracker != null) {
            m_leakTracker.record(hint);
        }
        return this;
    }

    @Override
    public boolean release() {
        return m_refCounted.release();
    }

    @Override
    public boolean release(int decrement) {
        return m_refCounted.release(decrement);
    }

    private void myDeallocate() {
        clear();
        if (m_leakTracker != null) {
            m_leakTracker.close(this);
            m_leakTracker = null;
        }
        m_refCounted.externalSetRefCnt(1);
        m_recylerHandle.recycle(this);
    }

}
