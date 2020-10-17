package com.thebk.utils.concurrent;

import io.netty.util.concurrent.FastThreadLocalThread;

public abstract class PerpetualSingleThread extends FastThreadLocalThread {
	private final RCPromise<Void> m_completion = StaticRCPromise.create();
	private boolean m_stopRunning;
	private boolean m_runAgain;
	private boolean m_alwaysPerformWorkOnWakeup;
	private long m_pauseTimeMS = Long.MAX_VALUE;

	public PerpetualSingleThread(String threadName) {
		setName(threadName);
		setDaemon(false);
	}

	protected void setSleepTime(long pauseTimeMS) {
		m_pauseTimeMS = pauseTimeMS;
	}

	protected void alwaysPerformWorkOnWakeup() {
		m_alwaysPerformWorkOnWakeup = true;
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
				wait(m_pauseTimeMS);
			} catch (InterruptedException e) {
			}
			if (m_alwaysPerformWorkOnWakeup) {
				m_runAgain = false;
				return (m_stopRunning) ? false : true;
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
