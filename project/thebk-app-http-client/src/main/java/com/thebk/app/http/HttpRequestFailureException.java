package com.thebk.app.http;

public class HttpRequestFailureException extends Exception {
	public HttpRequestFailureException(String message) {
		super(message);
	}

	public HttpRequestFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
