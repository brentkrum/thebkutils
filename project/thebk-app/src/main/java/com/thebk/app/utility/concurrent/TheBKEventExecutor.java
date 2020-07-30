package com.thebk.app.utility.concurrent;

import com.thebk.utils.queue.BlockingQueueWrapper;
import com.thebk.utils.queue.MPSCFixedQueue;
import com.thebk.utils.queue.MPSCUnboundedQueue;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.Queue;
import java.util.concurrent.Executor;

public class TheBKEventExecutor extends SingleThreadEventExecutor {

	public TheBKEventExecutor() {
		super(null, new DefaultThreadFactory(TheBKEventExecutor.class), true);
	}

	public TheBKEventExecutor(EventExecutorGroup parent, Executor executor) {
		super(parent, executor, true);
	}

	@Override
	protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
		if (maxPendingTasks == Integer.MAX_VALUE) {
			return new BlockingQueueWrapper(new MPSCUnboundedQueue());
		} else {
			return new BlockingQueueWrapper(new MPSCFixedQueue(maxPendingTasks));
		}
	}

	@Override
	protected void run() {
		for (;;) {
			Runnable task = takeTask();
			if (task != null) {
				task.run();
				updateLastExecutionTime();
			}

			if (confirmShutdown()) {
				break;
			}
		}
	}

}
