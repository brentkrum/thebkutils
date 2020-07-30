package com.thebk.app.jul.fmt;

import java.util.logging.LogRecord;

public class SourceFormatter extends BaseFormatter {
	public SourceFormatter() {
	}

	@Override
	public void write(StringBuilder out, LogRecord record) {
		if (record.getSourceClassName() != null) {
			out.append(record.getSourceClassName());
			if (record.getSourceMethodName() != null) {
				out.append(' ').append(record.getSourceMethodName());
			}
		} else {
			out.append(record.getLoggerName());
		}
	}
}
