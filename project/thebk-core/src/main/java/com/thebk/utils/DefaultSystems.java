package com.thebk.utils;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.concurrent.EventExecutorGroup;


public class DefaultSystems {
	private static EventExecutorGroup m_defaultTaskExec;
	private static ByteBufAllocator m_allocator;

	public static final EventExecutorGroup taskExecutor() {
		return m_defaultTaskExec;
	}

	public static ByteBufAllocator allocator() {
		return m_allocator;
	}

	public static final void setDefaultTaskExecutor(EventExecutorGroup executor) {
		m_defaultTaskExec = executor;
	}

	public static final void setDefaultByteBufAllocator(ByteBufAllocator allocator) {
		m_allocator = allocator;
	}
}
