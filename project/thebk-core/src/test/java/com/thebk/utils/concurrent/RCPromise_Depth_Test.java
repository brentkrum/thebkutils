package com.thebk.utils.concurrent;

import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RCPromise_Depth_Test extends TestBase {

	@BeforeAll
	public static void init() {
		// Force a spawn for notify listener
		System.setProperty("RCPromise.max-notify-depth", "0");
	}

	@Test
	public void testDepthAsyncLaunch() throws TimeoutException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		RCPromise<Void> done1 = RCPromise.create(executorService);
		RCPromise<Void> done2 = RCPromise.create(executorService);
		Thread launchingThread = Thread.currentThread();

		RCPromise<Integer> p = RCPromise.create(executorService);
		p.addListener((f) -> {
			try {
				Assertions.assertNotEquals(launchingThread, Thread.currentThread());
				Assertions.assertTrue(p.isDone());
				Assertions.assertTrue(p.isSuccess());
				Assertions.assertEquals(1, f.get());
				done1.setSuccess(null);
			} catch(Exception ex) {
				done1.setFailure(ex);
			}
		});
		p.addListener((f) -> {
			try {
				Assertions.assertNotEquals(launchingThread, Thread.currentThread());
				done2.setSuccess(null);
			} catch(Exception ex) {
				done2.setFailure(ex);
			}
		});

		p.setSuccess(1);
		Assertions.assertNull(done1.get(1000));
		Assertions.assertNull(done2.get(1000));

		// This may or may not be the last release
		p.release();
		done1.release();
		done2.release();
	}
}
