package com.thebk.app.logging.impl;

import com.thebk.app.ApplicationRun;
import com.thebk.app.logging.ILoggingImplementation;

import com.thebk.utils.config.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;

import java.util.concurrent.TimeUnit;

public class Log4j2LogAdapter implements ILoggingImplementation {

	@Override
	public void bootstrap(String[] args) {
		String userFile = System.getProperty("log4j.appConfigurationFile");

		System.setProperty("log4j2.disableJmx", "true");
		if (userFile != null) {
			System.setProperty("log4j.configurationFile", "core-log4j2.xml," + userFile);
		} else {
			System.setProperty("log4j.configurationFile", "core-log4j2.xml");
		}
		PluginManager.addPackage("com.thebk.app.log4j2");
		initLogging();
	}

	@Override
	public void shutdown() {
		Configurator.shutdown(LoggerContext.getContext(), 60, TimeUnit.SECONDS); // TODO pull this from config
	}

	private static void initLogging() {
		LoggerContext.getContext(false).start();

		Config.setConfigLogger(new Log4jConfigLogger());
		for(String configKey : Config.keys()) {
			if (configKey.startsWith("logger.")) {
				final String loggerName = configKey.substring(7);
				final String levelString = Config.getString(configKey, null);
				final Level level;
				try {
					level = Level.getLevel(levelString);
				} catch(Exception ex) {
					LogManager.getLogger(Log4j2LogAdapter.class).error("Could not parse logger level '{}' for '{}'", levelString, configKey, ex);
					continue;
				}
				if (level == null) {
					LogManager.getLogger(Log4j2LogAdapter.class).error("Could not parse logger level '{}' for '{}'", levelString, configKey);

				} else if (loggerName.equals("root")) {
					Configurator.setRootLevel(level);

				} else {
					Configurator.setLevel(loggerName, level);
				}
			}
		}
		LogManager.getLogger(Log4j2LogAdapter.class).info("Logging initialized");
		if (ApplicationRun.isTerminating()) {
			LogManager.getLogger(Log4j2LogAdapter.class).error("Attempt to re-initialize a terminated application");
			ApplicationRun.fatalExit();
		}
	}
}
