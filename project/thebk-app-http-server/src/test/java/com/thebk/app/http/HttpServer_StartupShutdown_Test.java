package com.thebk.app.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpServer_StartupShutdown_Test extends TestBase {

	@Test
	public void test() {
		HttpServer httpServer = HttpServer.builder().listenPort(10000).build();
		Assertions.assertTrue(httpServer.start().await(1000));
		Assertions.assertTrue(httpServer.stop().await(1000));
	}
}
