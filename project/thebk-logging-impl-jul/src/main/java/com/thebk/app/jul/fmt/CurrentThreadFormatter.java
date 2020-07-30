package com.thebk.app.jul.fmt;

import java.util.logging.LogRecord;

public class CurrentThreadFormatter extends BaseFormatter {
	public CurrentThreadFormatter() {
	}

	@Override
	public void write(StringBuilder out, LogRecord record) {
		Thread cur = Thread.currentThread();
		out.append(cur.getName());
	}
}
