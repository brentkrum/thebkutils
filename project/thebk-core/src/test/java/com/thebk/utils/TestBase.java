package com.thebk.utils;

import com.thebk.utils.config.Config;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestBase {
	@BeforeAll
	public static void baseInit() {
		System.setProperty("io.netty.leakDetectionLevel", "PARANOID");
		System.setProperty("io.netty.leakDetection.samplingInterval", "1");

		System.setProperty("log4j2.disableJmx", "true");
		System.setProperty("log4j.configurationFile", "test-log4j2.xml");

		DefaultSystems.setDefaultTaskExecutor(new DefaultEventExecutorGroup(2));
		DefaultSystems.setDefaultByteBufAllocator(PooledByteBufAllocator.DEFAULT);
		Config.init();
	}

	@AfterAll
	public static void baseDeinit() {
		((ExecutorService)DefaultSystems.taskExecutor()).shutdown();
	}
}

