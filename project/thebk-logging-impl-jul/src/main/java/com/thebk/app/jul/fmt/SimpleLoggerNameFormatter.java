package com.thebk.app.jul.fmt;

import java.util.logging.LogRecord;

public class SimpleLoggerNameFormatter extends BaseFormatter {
	public SimpleLoggerNameFormatter() {
	}

	@Override
	public void write(StringBuilder out, LogRecord record) {
		String loggerName = record.getLoggerName();
		int lastDot = loggerName.lastIndexOf('.');
		if (lastDot == -1) {
			out.append(loggerName);
		} else {
			out.append(loggerName, lastDot+1, loggerName.length());
		}
	}
}
