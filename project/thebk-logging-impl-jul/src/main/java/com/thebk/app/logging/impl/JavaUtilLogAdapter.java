package com.thebk.app.logging.impl;


import com.thebk.app.logging.ILoggingImplementation;
import com.thebk.utils.config.Config;

public class JavaUtilLogAdapter implements ILoggingImplementation {

	@Override
	public void bootstrap(String[] args) {
		System.setProperty("java.util.logging.config.class", LoggingConfigClass.class.getCanonicalName());
		System.setProperty("java.util.logging.manager", DenaliLogManager.class.getName());
		initLogging();
	}

	@Override
	public void shutdown() {
		LoggingConfigClass.shutdown();
	}

	private static void initLogging() {
		// Initialize Java Util Logging
		java.util.logging.LogManager.getLogManager();

		Config.setConfigLogger(new JavaUtilConfigLogger());
	}

}
