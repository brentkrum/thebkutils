package com.thebk.app.jul.fmt;

import java.util.logging.LogRecord;

public class LoggerNameFormatter extends BaseFormatter {
	public LoggerNameFormatter() {
	}

	@Override
	public void write(StringBuilder out, LogRecord record) {
		out.append(record.getLoggerName());
	}
}
