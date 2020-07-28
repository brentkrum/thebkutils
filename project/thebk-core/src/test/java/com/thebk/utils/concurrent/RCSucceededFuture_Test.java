package com.thebk.utils.concurrent;

import com.thebk.utils.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

public class RCSucceededFuture_Test extends TestBase {

	@Test
	public void test() throws TimeoutException {
		RCFuture<Integer> p = RCSucceededFuture.create(1);
		Assertions.assertTrue(p.isDone());
		Assertions.assertTrue(p.isSuccess());
		Assertions.assertEquals(1, p.get());
		Assertions.assertTrue(p.release());

		RCFuture<Integer> p2 = RCSucceededFuture.create(2);
		Assertions.assertTrue(p2.isDone());
		Assertions.assertTrue(p2.isSuccess());
		Assertions.assertEquals(2, p2.get());

		RCPromise<Void> done1 = RCPromise.create();
		p2.addListener((f) -> {
			try {
				Assertions.assertTrue(f.isDone());
				Assertions.assertTrue(f.isSuccess());
				Assertions.assertEquals(2, f.get());
				done1.setSuccess(null);
			} catch(Exception ex) {
				done1.setFailure(ex);
			}
		});
		Assertions.assertTrue(p2.release());
		Assertions.assertNull(done1.get(0));
		Assertions.assertTrue(done1.release());
	}
}
