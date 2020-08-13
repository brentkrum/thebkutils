package com.thebk.utils.concurrent;

import io.netty.util.concurrent.FastThreadLocalThread;

public abstract class PerpetualSingleThread extends FastThreadLocalThread {
	private final RCPromise<Void> m_completion = StaticRCPromise.create();
	private boolean m_stopRunning;
	private boolean m_runAgain;

	public PerpetualSingleThread(String threadName) {
		setName(threadName);
		setDaemon(false);
	}

	public RCFuture<Void> completion() {
		return m_completion;
	}

	public synchronized void stopRunning() {
		m_stopRunning = true;
		notifyAll();
	}

	public synchronized void requestWork() {
		m_runAgain = true;
		notifyAll();
	}

	private synchronized boolean pauseForWork() {
		while(true) {
			if (m_stopRunning) {
				return false;
			}
			if (m_runAgain) {
				m_runAgain = false;
				return true;
			}
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
	}

	protected boolean stopRunningRequested() {
		return m_stopRunning == true;
	}

	@Override
	public void run() {
		onStart();
		try {
			while(pauseForWork()) {
				performWork();
			}

		} catch(Exception ex) {
			onUnhandled(ex);
			m_completion.setFailure(null).release();
			return;
		}
		onStop();
		m_completion.setSuccess(null).release();
	}

	protected abstract void performWork();
	protected abstract void onStart();
	protected abstract void onStop();
	protected abstract void onUnhandled(Exception ex);
}
