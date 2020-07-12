package com.thebk.utils;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

final class InternalMPSCUnboundedQueue {
	private static final AtomicReferenceFieldUpdater<InternalMPSCUnboundedQueue, Node> m_tailUpdate = AtomicReferenceFieldUpdater.newUpdater(InternalMPSCUnboundedQueue.class, Node.class, "m_tail");

	private volatile Node m_tail;
	private Node m_head;

	/**
	 * Enqueue - add to tail
	 * Dequeue - remove from head
	 *
	 *
	 * Head --------- Tail
	 *
	 * <--prev   next-->
	 *
	 */
	public InternalMPSCUnboundedQueue() {
	}


	public void enqueue(Object o) {
		Node newNode = new Node();
		newNode.value = o;
		while(true) {
			newNode.prev = m_tail;
			if (m_tailUpdate.compareAndSet(this, newNode.prev, newNode)) {
				return;
			}
		}
	}

	private void prepForRead() {
		if (m_head == null) {
			// Start at the current tail and walk towards the head setting next pointers
			Node cur = m_tail;
			if (cur != null) {
				Node next = null;
				while (cur != null) {
					cur.next = next;
					next = cur;
					m_head = cur;
					cur = cur.prev;
				}
			}
		}
	}

	public Object dequeue() {
		prepForRead();
		if (m_head == null) {
			return null;
		}
		Object o = m_head.value;
		m_head = m_head.next;
		return o;
	}

	public Object peek() {
		prepForRead();
		if (m_head == null) {
			return null;
		}
		return m_head.value;
	}

	private static final class Node {
		Node prev;
		Node next;
		Object value;
	}
}