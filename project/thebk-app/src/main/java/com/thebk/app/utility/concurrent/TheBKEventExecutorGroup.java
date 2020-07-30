package com.thebk.app.utility.concurrent;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class TheBKEventExecutorGroup extends MultithreadEventExecutorGroup {
	private static final int DEFAULT_NUM_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors()*2 );

	public TheBKEventExecutorGroup() {
		super(DEFAULT_NUM_THREADS, (ThreadFactory)null);
	}

	/**
	 * Create a new instance
	 *
	 * @param nThreads the number of threads to use
	 */
	public TheBKEventExecutorGroup(int nThreads) {
		super(nThreads, (ThreadFactory) null);
	}

	@Override
	protected EventExecutor newChild(Executor executor, Object... args) throws Exception {
		return new TheBKEventExecutor(this, executor);
	}
}
