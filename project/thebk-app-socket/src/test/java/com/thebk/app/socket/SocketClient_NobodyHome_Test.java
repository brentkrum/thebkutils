package com.thebk.app.socket;

import com.thebk.app.Application;
import com.thebk.app.test.AbstractTestBase;
import com.thebk.utils.concurrent.RCFuture;
import com.thebk.utils.pipe.Pipe;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SocketClient_NobodyHome_Test extends AbstractTestBase {
	@BeforeAll
	public static void init() {
		System.setProperty("com.thebk.app.logger-level-root", "TRACE");
		AbstractTestBase.bootstrap();
	}

	@AfterAll
	public static void deinit() {
		AbstractTestBase.deinit();
	}

	@Test
	public void test() {
		Promise<Pipe.IProducer> connectedPromise = Application.newPromise();;

		SocketClient client = SocketClient.builder("localhost", 10000)
			.onConnect((conn, outPipe) -> {
				connectedPromise.tryFailure(new IllegalStateException("onConnect should not be called"));
			})
			.onDisconnect((conn, inPipe, outPipe) -> {
				inPipe.close();
				outPipe.close();
				connectedPromise.tryFailure(new IllegalStateException("onDisconnect should not be called"));
			})
			.onFailure((conn, cause) -> {
				connectedPromise.tryFailure(new IllegalStateException("onFailure should not be called", cause));
			})
			.build();
		RCFuture<Void> startFuture = client.start();
		RCFuture<Void> stopFuture = client.stop();
		Assertions.assertTrue(startFuture.await(1000));
		Assertions.assertTrue(startFuture.isDone());
		Assertions.assertFalse(startFuture.isSuccess());

		Assertions.assertTrue(stopFuture.await(1000));
		Assertions.assertTrue(stopFuture.isDone());
		Assertions.assertTrue(stopFuture.isSuccess());

		// connectedPromise should not fire
		Assertions.assertFalse(connectedPromise.awaitUninterruptibly(0));
	}
}
