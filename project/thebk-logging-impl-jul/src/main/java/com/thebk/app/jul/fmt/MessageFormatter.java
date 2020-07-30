package com.thebk.app.jul.fmt;

import java.util.logging.LogRecord;

public class MessageFormatter extends BaseFormatter {
	public MessageFormatter() {
	}

	@Override
	public void write(StringBuilder out, LogRecord record) {
		out.append(record.getMessage());
	}
}
