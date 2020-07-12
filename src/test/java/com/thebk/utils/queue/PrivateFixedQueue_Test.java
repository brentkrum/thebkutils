package com.thebk.utils.queue;

import com.thebk.utils.TestBase;
import com.thebk.utils.queue.PrivateFixedQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrivateFixedQueue_Test extends TestBase {

	@Test
	public void simple() {
		PrivateFixedQueue q = new PrivateFixedQueue(2);

		Assertions.assertTrue(q.enqueue(new Integer(1)));
		Assertions.assertTrue(q.enqueue(new Integer(2)));
		Assertions.assertFalse(q.enqueue(new Integer(3)));

		Assertions.assertEquals(1, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(2, ((Integer)q.dequeue()).intValue());

		Assertions.assertTrue(q.enqueue(new Integer(3)));
		Assertions.assertTrue(q.enqueue(new Integer(4)));
		Assertions.assertFalse(q.enqueue(new Integer(5)));

		Assertions.assertEquals(3, ((Integer)q.dequeue()).intValue());
		Assertions.assertEquals(4, ((Integer)q.dequeue()).intValue());
	}

}
