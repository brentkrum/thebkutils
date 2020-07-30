package com.thebk.app.test;

import com.thebk.app.Application;
import com.thebk.app.ApplicationBootstrap;
import com.thebk.app.log4j2.TestCaptureAppender;
import com.thebk.app.log4j2.TestLevelCheckAppender;

public abstract class AbstractTestBase {

	/**
	 * Helpful properties for debug tracing:
	 *    System.setProperty("com.thebk.app.logger-level-root", "TRACE");
	 *    System.setProperty("com.thebk.app.logger-level-fw", "TRACE");
	 *
	 */
	protected static void bootstrap() {
		System.setProperty("io.netty.leakDetection.level", "PARANOID");
		System.setProperty("io.netty.leakDetection.targetRecords", Integer.toString(Integer.MAX_VALUE));
		System.setProperty("log4j.appConfigurationFile", "test-log4j2.xml");
		System.setProperty("com.thebk.app.logger-level-root", System.getProperty("com.thebk.app.logger-level-root", "DEBUG"));
		ApplicationBootstrap.bootstrap(new String[]{});
		TestCaptureAppender.clear();
		TestLevelCheckAppender.reset();
		Application.run();
	}

	protected static void deinit() {
		TestCaptureAppender.clear();
		TestLevelCheckAppender.reset();
		Application.terminateAndWait();
	}
}
