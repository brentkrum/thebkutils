package com.thebk.app;

import com.thebk.app.utility.concurrent.TheBKEventExecutorGroup;
import com.thebk.utils.DefaultSystems;
import com.thebk.utils.concurrent.DefaultRCPromise;
import com.thebk.utils.config.Config;
import com.thebk.app.logging.LoggingImplRegistration;
import com.thebk.utils.metrics.CounterMetric;
import com.thebk.utils.metrics.MetricsEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Application {
	public static final String CONFIG_FW_PREFIX = "com.thebk.app.";
	private static final long SHUTDOWN_QUIET_PERIOD = Config.getInt(CONFIG_FW_PREFIX + "Application.shutdownQuietPeriod", 0);
	private static final long SHUTDOWN_MAX_WAIT = Config.getInt(CONFIG_FW_PREFIX + "Application.shutdownMaxWait", 15000);
	private static final CounterMetric m_applicationStart;
	private static final CounterMetric m_applicationStop;

	private static final PooledByteBufAllocator m_ioBufferPool = PooledByteBufAllocator.DEFAULT;
	private static final NioEventLoopGroup m_ioPool;
	private static final EventExecutorGroup m_taskPool;
	private static boolean m_haltOnFatalExit = true;
	private static IShutdownHandler m_userShutdownHandler;

	static {
		ApplicationRun.registerFatalExitHandler(() -> Application.fatalExit());
		m_ioPool = new NioEventLoopGroup(Config.getInt(CONFIG_FW_PREFIX + "Application.ioPoolCount", 2));
		int taskPoolSize = Config.getInt(CONFIG_FW_PREFIX + "Application.taskPoolCount", -1);
		if (taskPoolSize <= 0) {
			m_taskPool = new TheBKEventExecutorGroup();
		} else {
			m_taskPool = new TheBKEventExecutorGroup(taskPoolSize);
		}
		DefaultSystems.setDefaultTaskExecutor(m_taskPool);
		DefaultSystems.setDefaultByteBufAllocator(m_ioBufferPool);
		m_applicationStart = MetricsEngine.newCounterMetric("application-start");
		m_applicationStop = MetricsEngine.newCounterMetric("application-stop");
	}

	public static void fatalExit() {
		if (m_haltOnFatalExit) {
			if (LoggingImplRegistration.registeredLoggingImpl() != null) {
				LoggingImplRegistration.registeredLoggingImpl().shutdown();
			}
			Runtime.getRuntime().halt(1);
		}
	}

	public static void setShutdownHandler(IShutdownHandler shutdownHandler) {
		m_userShutdownHandler = shutdownHandler;
	}

	public static void shutdown() {
		terminate();
	}

	static void disableFatalExitHalt() {
		m_haltOnFatalExit = false;
	}

	public static ByteBufAllocator allocator() {
		return m_ioBufferPool;
	}

	public static ByteBuf allocateEmptyBuffer() {
		return m_ioBufferPool.heapBuffer(0,0);
	}

	public static CompositeByteBuf allocateCompositeBuffer() {
		return m_ioBufferPool.compositeBuffer();
	}

	public static CompositeByteBuf allocateCompositeBuffer(int maxNumComponents) {
		return m_ioBufferPool.compositeBuffer(maxNumComponents);
	}

	public static NioEventLoopGroup getIOPool() {
		return m_ioPool;
	}

	public static EventExecutorGroup getTaskPool() {
		return m_taskPool;
	}

	public static <T> Promise<T> newPromise() {
		return m_taskPool.next().<T>newPromise();
	}

	public static <T> Future<T> newSucceededFuture(T result) {
		return m_taskPool.next().newSucceededFuture(result);
	}
	public static Future<Void> newSucceededFuture() {
		return m_taskPool.next().newSucceededFuture(null);
	}

	public static <T> Future<T> newFailedFuture(Throwable cause) {
		return m_taskPool.next().newFailedFuture(cause);
	}

	public static void run() {
		final Logger LOG = LoggerFactory.getLogger(Application.class);
		final String ver = Application.class.getPackage().getImplementationVersion();
		if (ver != null) {
			LOG.info("Starting ({})", ver);
		} else {
			LOG.info("Starting");
		}
		m_applicationStart.increment();
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
	}

	public static boolean isTerminating() {
		return ApplicationRun.isTerminating();
	}

	public static void terminate() {
		Thread t = new Thread(() -> {
			try {
				terminate0();
			} catch (Throwable ex) {
				ex.printStackTrace(System.err);
			}
		}, "terminate()");
		t.setDaemon(false);
		t.start();
	}

	public static void terminateAndWait() {
		try {
			terminate0();
		} catch (Throwable ex) {
			ex.printStackTrace(System.err);
		}
	}

	private static void terminate0() {
		if (!ApplicationRun.indicateTermination()) {
			return;
		}

		m_applicationStop.increment();
		// User needs to snapshot in the shutdown handler if they want this counter
		final Logger LOG = LoggerFactory.getLogger(Application.class);
		if(m_userShutdownHandler != null) {
			LOG.info("Waiting on shutdown handler");
			DefaultRCPromise<Void> shutdownDone = DefaultRCPromise.create();
			try {
				m_userShutdownHandler.shutdown(shutdownDone);
				shutdownDone.await(Long.MAX_VALUE);
			} catch (Exception ex) {
				LOG.error("Exception in user shutdown handler", ex);
			}
			LOG.debug("Shutdown handler done");
		}
		finalTerminate();
	}

	private static void finalTerminate() {
		final Logger LOG = LoggerFactory.getLogger(Application.class);

		// Shut down thread pools
		LOG.debug("Waiting for io pool shutdown");
		m_ioPool.shutdownGracefully(SHUTDOWN_QUIET_PERIOD, SHUTDOWN_MAX_WAIT, TimeUnit.MILLISECONDS).awaitUninterruptibly();
		LOG.debug("Waiting for task pool shutdown");
		m_taskPool.shutdownGracefully(SHUTDOWN_QUIET_PERIOD, SHUTDOWN_MAX_WAIT, TimeUnit.MILLISECONDS).awaitUninterruptibly();
		LOG.info("End of shutdown hook");
		if (LoggingImplRegistration.registeredLoggingImpl() != null) {
			LoggingImplRegistration.registeredLoggingImpl().shutdown();
		}
	}

	private static class ShutdownHook extends Thread {
		ShutdownHook() {
			setName("ShutdownHook");
			setDaemon(false);
		}
		@Override
		public void run() {
			terminate0();
		}

	}

	public interface IShutdownHandler {
		void shutdown(DefaultRCPromise<Void> shutdownDone);
	}

}
