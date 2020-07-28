package com.thebk.utils.concurrent;

import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class RCPromise_Depth2_Test extends TestBase {

	@BeforeAll
	public static void init() {
		// Force a spawn for notify listener
		System.setProperty("RCPromise.max-notify-depth", "1");
	}


	@Test
	public void testDepthAsyncLaunch() throws InterruptedException, TimeoutException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		RCPromise<Void> outerDone = RCPromise.create(executorService);
		RCPromise<Void> innerDone = RCPromise.create(executorService);
		Thread launchingThread = Thread.currentThread();

		RCPromise<Integer> pInner = RCPromise.create(executorService);
		pInner.addListener((f) -> {
			try {
				// Inner should have been spawned into separate thread
				Assertions.assertNotEquals(launchingThread, Thread.currentThread());
				Assertions.assertTrue(f.isDone());
				Assertions.assertTrue(f.isSuccess());
				Assertions.assertEquals(2, f.get());
				innerDone.setSuccess(null);
			} catch(Exception ex) {
				innerDone.setFailure(ex);
			}
		});
		RCPromise<Integer> pOuter = RCPromise.create(executorService);
		pOuter.addListener((f) -> {
			try {
				// Outer should run on main thread
				Assertions.assertEquals(launchingThread, Thread.currentThread());
				pInner.setSuccess(2);
				outerDone.setSuccess(null);
			} catch(Exception ex) {
				outerDone.setFailure(ex);
			}
		});

		pOuter.setSuccess(1);
		Assertions.assertNull(outerDone.get(1000));
		Assertions.assertNull(innerDone.get(1000));

		// We can't control when the other threads release their instances, so can't test these are last
		pInner.release();
		pOuter.release();
		innerDone.release();
	}
}
