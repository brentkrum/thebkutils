package com.thebk.utils.concurrent;

import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class DefaultRCPromise_Simple_Test extends TestBase {

	@Test
	public void testSimpleSuccessAndFail() {
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		// Since we are using recycled classes, let's make sure we test a bunch
		for(int i=0; i<100; i++) {
			DefaultRCPromise<Integer> p = DefaultRCPromise.create(executorService);
			Assertions.assertFalse(p.isDone());
			Assertions.assertFalse(p.isSuccess());
			p.setSuccess(1);
			Assertions.assertTrue(p.isDone());
			Assertions.assertTrue(p.isSuccess());
			Assertions.assertEquals(1, p.get());
			Assertions.assertTrue(p.release());

			DefaultRCPromise<Integer> p2 = DefaultRCPromise.create(executorService);
			Assertions.assertFalse(p2.isDone());
			Assertions.assertFalse(p2.isSuccess());
			p2.setFailure(new RuntimeException());
			Assertions.assertTrue(p2.isDone());
			Assertions.assertFalse(p2.isSuccess());
			Assertions.assertTrue(p2.cause() instanceof RuntimeException);
			Assertions.assertTrue(p2.release());
		}
	}

	@Test
	public void testSimpleListeners() throws InterruptedException, TimeoutException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		Thread launchingThread = Thread.currentThread();

		DefaultRCPromise<Integer> done1 = DefaultRCPromise.create(executorService);
		DefaultRCPromise<Integer> done2 = DefaultRCPromise.create(executorService);

		DefaultRCPromise<Integer> p = DefaultRCPromise.create(executorService);
		p.addListener((f) -> {
			try {
				Assertions.assertEquals(launchingThread, Thread.currentThread());
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
				Assertions.assertEquals(launchingThread, Thread.currentThread());
				done2.setSuccess(null);
			} catch(Exception ex) {
				done2.setFailure(ex);
			}
		});
		Assertions.assertEquals(3, p.refCnt());

		p.setSuccess(1);
		Assertions.assertNull(done1.get(0));
		Assertions.assertNull(done2.get(0));
		done1.release();
		done2.release();

		Assertions.assertEquals(1, p.refCnt());

		// Test post-completion listener
		DefaultRCPromise<Integer> done3 = DefaultRCPromise.create(executorService);
		p.addListener((f) -> {
			try {
				Assertions.assertEquals(launchingThread, Thread.currentThread());
				Assertions.assertTrue(p.isDone());
				Assertions.assertTrue(p.isSuccess());
				Assertions.assertEquals(1, f.get());
				done3.setSuccess(null);
			} catch (Exception ex) {
				done3.setFailure(ex);
			}
		});
		Assertions.assertNull(done3.get(0));
		done3.release();

		// Test post-completion listener with a thrown exception
		p.addListener((f) -> {
			RuntimeException ex = new RuntimeException("This is part of the test and is a good result");
			ex.setStackTrace(new StackTraceElement[0]);
			throw ex;
		});
		Assertions.assertTrue(p.release());
	}
}
