package com.thebk.utils.queue;

import com.thebk.utils.rc.RCBoolean;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueWrapper<T> implements BlockingQueue<T> {
	private final Object m_enqueueNotify = new Object();
	private final Object m_dequeueNotify = new Object();
	private final TheBKQueue m_queue;

	public BlockingQueueWrapper(TheBKQueue queue) {
		m_queue = queue;
	}

	private void notifyOfEnqueue() {
		synchronized (m_enqueueNotify) {
			m_enqueueNotify.notify();
		}
	}

	private void notifyOfDequeue() {
		synchronized (m_dequeueNotify) {
			m_dequeueNotify.notify();
		}
	}

	private void waitForNotification0(Object monitor) throws InterruptedException {
		synchronized (monitor) {
			monitor.wait();
		}
	}

	private boolean waitForNotification0(Object monitor, long timeout, TimeUnit unit, BooleanSupplier isDone) throws InterruptedException {
		long timeoutNanos = unit.toNanos(timeout);
		if (timeoutNanos <= 0) {
			return isDone.get();
		}
		if (isDone.get()) {
			return true;
		}
		long startTime = System.nanoTime();
		long waitTime = timeoutNanos;
		while (true) {
			synchronized (monitor) {
				if (isDone.get()) {
					return true;
				}
				monitor.wait(waitTime / 1000000, (int) (waitTime % 1000000));
			}
			if (isDone.get()) {
				return true;
			}
			waitTime = timeoutNanos - (System.nanoTime() - startTime);
			if (waitTime <= 0) {
				return isDone.get();
			}
		}
	}

	private T waitForNotification0(Object monitor, long timeout, TimeUnit unit, TSupplier<T> isDone) throws InterruptedException {
		long timeoutNanos = unit.toNanos(timeout);
		if (timeoutNanos <= 0) {
			return isDone.get();
		}
		T item;
		if ((item = isDone.get()) != null) {
			return item;
		}
		long startTime = System.nanoTime();
		long waitTime = timeoutNanos;
		while (true) {
			synchronized (monitor) {
				if ((item = isDone.get()) != null) {
					return item;
				}
				monitor.wait(waitTime / 1000000, (int) (waitTime % 1000000));
			}
			if ((item = isDone.get()) != null) {
				return item;
			}
			waitTime = timeoutNanos - (System.nanoTime() - startTime);
			if (waitTime <= 0) {
				return isDone.get();
			}
		}
	}

	@Override
	public boolean add(T t) {
		RCBoolean committed = RCBoolean.create(false);
		if (!m_queue.enqueue(t, committed)) {
			committed.release();
			throw new IllegalStateException("Queue full");
		}
		if (committed.value() == true) {
			notifyOfEnqueue();
		}
		committed.release();
		return true;
	}

	@Override
	public boolean offer(T t) {
		RCBoolean committed = RCBoolean.create(false);
		if (!m_queue.enqueue(t, committed)) {
			committed.release();
			return false;
		}
		if (committed.value() == true) {
			notifyOfEnqueue();
		}
		committed.release();
		return true;
	}

	@Override
	public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
		final RCBoolean committed = RCBoolean.create(false);
		if (waitForNotification0(m_dequeueNotify, timeout, unit, () -> m_queue.enqueue(t, committed))) {
			if (committed.value() == true) {
				notifyOfEnqueue();
			}
			committed.release();
			return true;
		}
		committed.release();
		return false;
	}

	@Override
	public void put(T t) throws InterruptedException {
		RCBoolean committed = RCBoolean.create(false);
		while(true) {
			if (m_queue.enqueue(t, committed)) {
				committed.release();
				notifyOfEnqueue();
				return;
			}
			waitForNotification0(m_dequeueNotify);
		}
	}

	@Override
	public T take() throws InterruptedException {
		while(true) {
			T item = (T)m_queue.dequeue();
			if (item != null) {
				notifyOfDequeue();
				return item;
			}
			waitForNotification0(m_enqueueNotify);
		}
	}

	@Override
	public T poll() {
		return (T)m_queue.dequeue();
	}

	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
		return waitForNotification0(m_enqueueNotify, timeout, unit, () -> (T)m_queue.dequeue());
	}

	@Override
	public int remainingCapacity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove() {
		T item = (T)m_queue.dequeue();
		if (item == null) {
			throw new NoSuchElementException();
		}
		return item;
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super T> c, int maxElements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T element() {
		T item = (T)m_queue.peek();
		if (item == null) {
			throw new NoSuchElementException();
		}
		return item;
	}

	@Override
	public T peek() {
		return (T)m_queue.peek();
	}

	@Override
	public int size() {
		return m_queue.size();
	}

	@Override
	public boolean isEmpty() {
		return m_queue.size() == 0;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		throw new UnsupportedOperationException();
	}

	private interface BooleanSupplier {
		boolean get();
	}
	private interface TSupplier<T> {
		T get();
	}

}
