package com.thebk.app.logging.impl;

import com.thebk.app.logging.LoggingImplRegistration;

public class Registration {
	static {
		LoggingImplRegistration.register(new Log4j2LogAdapter());
	}
}
