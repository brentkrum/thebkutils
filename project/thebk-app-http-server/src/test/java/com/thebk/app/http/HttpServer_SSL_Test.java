package com.thebk.app.http;

import com.thebk.app.Application;
import com.thebk.utils.http.MinimalHTTPSRequest;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

// keytool -genkey -noprompt -trustcacerts -keyalg RSA -alias selfsigned -keystore unit-test-server-keystore.jks -storepass changeit -keypass changeit -validity 3600 -keysize 2048 -dname "cn=fake-host.does.not.exist.com, ou=Development, o=TheBKUtils, c=US" -deststoretype pkcs12
// keytool -export -noprompt -alias selfsigned -keystore unit-test-server-keystore.jks -storepass changeit -file unit-test.cer
// keytool -import -noprompt -alias selfsigned -keystore unit-test-client-keystore.jks -storepass changeit -file unit-test.cer -deststoretype pkcs12
public class HttpServer_SSL_Test extends TestBase {

	@Test
	public void testUserHandler() {
		System.setProperty("javax.net.ssl.trustStore","unit-test-client-keystore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		CountDownLatch requestGoodLatch = new CountDownLatch(1);
		HttpServer httpServer = HttpServer.builder()
			.listenPort(10000)
			.useSSL(true)
			.sslKeyStoreFileName("unit-test-server-keystore.jks")
			.sslKeyStorePassword("changeit")
			.sslKeyStoreFileFormat("pkcs12")
			.onRequest((HttpServer.IHttpRequest request, HttpServer.IHttpResponse response)->{
				if (!request.requestMethod().equals("GET")) {
					response.respondOk(ByteBufUtil.writeAscii(Application.allocator(), "Request method is " + request.requestMethod() + " instead of GET"));
					return;
				}
				if (!request.requestURI().equals("/nothing_to_get.html")) {
					response.respondOk(ByteBufUtil.writeAscii(Application.allocator(), "Request URI is '" + request.requestURI() + "' instead of '/nothing_to_get.html'"));
					return;
				}
				response.respondOk(ByteBufUtil.writeAscii(Application.allocator(), "GOOD"));
			})
			.build();
		Assertions.assertTrue(httpServer.start().await(1000));

		String responseData = MinimalHTTPSRequest.get("localhost", 10000, "/nothing_to_get.html");
		Assertions.assertEquals("GOOD", responseData);

		Assertions.assertTrue(httpServer.stop().await(1000));
	}
}
