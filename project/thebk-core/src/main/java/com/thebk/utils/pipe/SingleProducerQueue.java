package com.thebk.utils.pipe;

import com.thebk.utils.concurrent.DefaultRCPromise;
import com.thebk.utils.queue.SPSCFixedQueue;
import com.thebk.utils.concurrent.RCFuture;
import com.thebk.utils.concurrent.RCSucceededFuture;

public class SingleProducerQueue implements ConnectablePipe.IBrokerProducer {
	private final SPSCFixedQueue m_inQueue;
	private final Runnable m_onQueueNotFullCallback;
	private boolean m_notifyVsRunProcessor = true;
	private ConnectablePipe m_pipe;
	private volatile boolean m_queueFull;
	private ConnectablePipe.IProducerHandle m_handle;

	public SingleProducerQueue(Runnable onQueueNotFullCallback) {
		this(128, onQueueNotFullCallback);
	}

	public SingleProducerQueue(int producerQueueSize, Runnable onQueueNotFullCallback) {
		m_inQueue = new SPSCFixedQueue(producerQueueSize);
		m_onQueueNotFullCallback = onQueueNotFullCallback;
	}

	public void setProcessorInteraction(boolean notifyVsRunProcessor) {
		m_notifyVsRunProcessor = notifyVsRunProcessor;
	}

	public RCFuture<Void> open(ConnectablePipe pipe) {
		m_queueFull = false;
		m_pipe = pipe;

		// One p ref returned
		DefaultRCPromise<Void> p = DefaultRCPromise.create();
		// one p ref used by cp listener
		p.retain();

		// We are returned one ref to cp
		RCFuture<ConnectablePipe.IProducerHandle> cp = pipe.connectProducer(this);
		// Listener holds a ref to cp
		cp.addListener((f) -> {
			try {
				if (f.isSuccess()) {
					m_handle = f.get();
					p.setSuccess(null);
				} else {
					p.setFailure(f.cause());
				}
			} finally {
				p.release();
			}
		});
		// Release returned cp ref
		cp.release();
		return p;
	}

	public RCFuture<Void> close() {
		m_pipe = null;
		if (m_handle != null) {
			final ConnectablePipe.IProducerHandle handle = m_handle;
			m_handle = null;
			return handle.disconnect();
		}
		return RCSucceededFuture.create(null);
	}

	/*
		This must be guaranteed by the caller to be only called by a single thread at a time
	*/
	public boolean enqueue(Object o) {
		if (m_queueFull || !m_inQueue.enqueue(o)) {
			m_queueFull = true;
			return false;
		}
		if (m_notifyVsRunProcessor) {
			m_pipe.notifyProcessor();
		} else {
			m_pipe.runProcessor();
		}
		return true;
	}


	@Override
	public Object produce() {
		final Object o = m_inQueue.dequeue();
		if (o != null && m_queueFull) {
			// This is called by the Pipe thread which may or may not be the producer thread
			m_queueFull = false;
			if (m_onQueueNotFullCallback != null) {
				m_onQueueNotFullCallback.run();
			}
		}
		return o;
	}
}
