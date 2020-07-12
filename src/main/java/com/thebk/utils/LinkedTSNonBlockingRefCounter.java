package com.thebk.utils;

import io.netty.util.ReferenceCounted;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public final class LinkedTSNonBlockingRefCounter<T extends ReferenceCounted> extends TSNonBlockingRefCounter<T> {
	private static final AtomicReferenceFieldUpdater<LinkedTSNonBlockingRefCounter, LinkedTSNonBlockingRefCounter> m_nextUpdater = AtomicReferenceFieldUpdater.newUpdater(LinkedTSNonBlockingRefCounter.class, LinkedTSNonBlockingRefCounter.class, "m_next");
	private volatile LinkedTSNonBlockingRefCounter<T> m_next;

	public LinkedTSNonBlockingRefCounter(T obj) {
		super(obj);
	}

	public LinkedTSNonBlockingRefCounter next() {
		return m_next;
	}

	public boolean setNext(LinkedTSNonBlockingRefCounter next) {
		return m_nextUpdater.compareAndSet(this, null, next);
	}

}
