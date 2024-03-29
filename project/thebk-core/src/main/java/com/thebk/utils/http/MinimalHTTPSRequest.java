package com.thebk.utils.http;

import java.net.URL;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinimalHTTPSRequest {

	private MinimalHTTPSRequest() {
	}

	public static String get(String urlString) {
		return MinimalHTTPRequest.get(urlString);
	}

	public static String get(URL url) {
		return get(url, null);
	}
	public static String get(URL url, final Map<String, String> headers) {
		if (url.getProtocol() == "http") {
			return MinimalHTTPRequest.get(url);
		} else {
			final int port = (url.getPort() == -1) ? 443 : url.getPort();
			return get(url.getHost(), port, url.getFile(), headers);
		}
	}

	public static String get(String host, String file) {
		return get(host, 443, file, null);
	}
	public static String get(String host, String file, final Map<String, String> headers) {
		return get(host, 443, file, headers);
	}

	public static String get(String host, int port, String file) {
		return get(host, port, file, null);
	}

	public static String get(String host, int port, String file, final Map<String, String> headers) {
		final Logger LOG = LoggerFactory.getLogger(MinimalHTTPSRequest.class.getName() + "." + host);
		final SocketFactory factory = SSLSocketFactory.getDefault();
		return MinimalHTTPRequest.get(factory, host, port, file, headers, LOG);
	}

	public static String post(String urlString, String contentType, String postedData) {
		return post(urlString, contentType, (Map<String, String>)null, postedData);
	}
	public static String post(String urlString, String contentType, final Map<String, String> headers, String postedData) {
		return MinimalHTTPRequest.post(urlString, contentType, postedData);
	}
	public static String post(URL url, String contentType, String postedData) {
		return post(url, contentType, null, postedData);
	}
	public static String post(URL url, String contentType, final Map<String, String> headers, String postedData) {
		if (url.getProtocol() == "http") {
			return MinimalHTTPRequest.post(url, contentType, postedData);
		} else {
			final int port = (url.getPort() == -1) ? 443 : url.getPort();
			return post(url.getHost(), port, url.getFile(), contentType, postedData);
		}
	}
	public static String post(String host, String file, String contentType, String postedData) {
		return post(host, file, contentType, null, postedData);
	}
	public static String post(String host, String file, String contentType, final Map<String, String> headers, String postedData) {
		return post(host, 443, file, contentType, headers, postedData);
	}
	public static String post(String host, int port, String file, String contentType, String postedData) {
		final Logger LOG = LoggerFactory.getLogger(MinimalHTTPSRequest.class.getName() + "." + host);
		final SocketFactory factory = SSLSocketFactory.getDefault();
		return MinimalHTTPRequest.post(factory, host, port, file, LOG, contentType, postedData);
	}
	public static String post(String host, int port, String file, String contentType, final Map<String, String> headers, String postedData) {
		final Logger LOG = LoggerFactory.getLogger(MinimalHTTPSRequest.class.getName() + "." + host);
		final SocketFactory factory = SSLSocketFactory.getDefault();
		return MinimalHTTPRequest.post(factory, host, port, file, LOG, contentType, headers, postedData);
	}
}
