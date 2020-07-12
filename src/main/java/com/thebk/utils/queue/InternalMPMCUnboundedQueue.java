package com.thebk.utils.queue;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

final class InternalMPMCUnboundedQueue {
//	private static final AtomicReferenceFieldUpdater<InternalMPMCUnboundedQueue, Node> m_tailUpdate = AtomicReferenceFieldUpdater.newUpdater(InternalMPMCUnboundedQueue.class, Node.class, "m_tail");
//
//	private volatile Node m_tail;
//	private volatile Node m_head;
//
//	/**
//	 * Enqueue - add to tail
//	 * Dequeue - remove from head
//	 *
//	 *
//	 * Head --------- Tail
//	 *
//	 * <--prev   next-->
//	 *
//	 */
//	public InternalMPMCUnboundedQueue() {
//	}
//
//
//	public void enqueue(Object o) {
//		Node newNode = new Node();
//		newNode.value = o;
//		while(true) {
//			final Node theCurrentTail = m_tail;
//			newNode.prev = theCurrentTail;
//			if (m_tailUpdate.compareAndSet(this, theCurrentTail, newNode)) {
//				return;
//			}
//		}
//	}
//
//	private void prepForRead() {
//		// Get m_head + STAMP
//		if (m_head == null) {
//			// Start at the current tail and walk towards the head setting next pointers
//			Node cur = m_tail;
//			if (cur != null) {
//				Node next = null;
//				Node first = null;
//				while (cur != null) {
//					cur.next = next;
//					next = cur;
//					first = cur;
//					cur = cur.prev;
//				}
//				// This need to be compareAndSet STAMP
//				m_head = first;
//			}
//		}
//	}
//
//	public Object dequeue() {
//		prepForRead();
//		if (m_head == null) {
//			return null;
//		}
//		while(true) {
//			// This needs to be get m_head + STAMP
//			Object o = m_head.value;
//			// This need to be compareAndSet STAMP
//			need to fix this
//			m_head = m_head.next;
//			return o;
//		}
//	}
//
//	public Object peek() {
//		prepForRead();
//		if (m_head == null) {
//			return null;
//		}
//		return m_head.value;
//	}
//
//	private static final class Node {
//		Node prev;
//		Node next;
//		Object value;
//	}
}