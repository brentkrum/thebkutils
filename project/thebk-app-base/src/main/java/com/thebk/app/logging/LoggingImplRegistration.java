package com.thebk.app.logging;

import com.thebk.app.ApplicationRun;
import com.thebk.utils.config.Config;

public class LoggingImplRegistration {
	private static final boolean DEBUG_LOGGING = Config.getBoolean("com.thebk.app.debugLoggingInit", Boolean.FALSE);
	private static ILoggingImplementation m_impl;

	public static void register(ILoggingImplementation loggingImplementation) {
		if (DEBUG_LOGGING) {
			ApplicationRun.bootstrapLog("Using logging implementation " + loggingImplementation.getClass().getCanonicalName());
		}
		m_impl = loggingImplementation;
	}

	public static ILoggingImplementation registeredLoggingImpl() {
		return m_impl;
	}

	public static final boolean debugLoggingInit() {
		return DEBUG_LOGGING;
	}
}
