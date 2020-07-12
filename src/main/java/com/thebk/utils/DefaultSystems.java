package com.thebk.utils;

import io.netty.buffer.ByteBufAllocator;

import java.util.concurrent.Executor;

public class DefaultSystems {
	private static Executor m_defaultTaskExec;
	private static ByteBufAllocator m_allocator;

	public static final Executor taskExecutor() {
		return m_defaultTaskExec;
	}

	public static ByteBufAllocator allocator() {
		return m_allocator;
	}

	public static final void setDefaultTaskExecutor(Executor executor) {
		m_defaultTaskExec = executor;
	}

	public static final void setDefaultByteBufAllocator(ByteBufAllocator allocator) {
		m_allocator = allocator;
	}
}
