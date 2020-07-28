package com.thebk.utils.concurrent;

import com.thebk.utils.DefaultSystems;
import com.thebk.utils.parambag.CallbackParamBag;
import com.thebk.utils.queue.MPSCUnboundedQueue;
import com.thebk.utils.queue.TheBKQueue;
import com.thebk.utils.rc.RCBoolean;
import com.thebk.utils.rc.RCInteger;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class LocklessProcessing {
//    private static final int MAX_CALLBACK_DEPTH = Config.getFWInt("core.LocklessProcessing.maxCallbackDepth", 128);
//    private static final int DEFAULT_MAX_NUM_WORK_PER_RUN = Config.getFWInt("core.LocklessProcessing.maxNumWorkPerRun", 8);
    private static final int MAX_CALLBACK_DEPTH = 128;
    private static final int DEFAULT_MAX_NUM_WORK_PER_RUN = 8;
    private static final FastThreadLocal<RCInteger> m_callbackDepth = new FastThreadLocal<RCInteger>() {
        @Override
        protected RCInteger initialValue() throws Exception {
            return RCInteger.createStatic(0);
        }
    };

    private final Runner m_runner = new Runner();
    private final AtomicLong m_currentWorkTick = new AtomicLong();
    private final TheBKQueue m_workQueue = new MPSCUnboundedQueue();
    private final int m_maxNumWorkPerRun;
    private final Runnable m_runCallback;
    private final Executor m_executor = DefaultSystems.taskExecutor();
    private volatile Thread m_currentWorker;

    public LocklessProcessing() {
        this(DEFAULT_MAX_NUM_WORK_PER_RUN, null);
    }

    public LocklessProcessing(Runnable runCallback) {
        this(DEFAULT_MAX_NUM_WORK_PER_RUN, runCallback);
    }

    public LocklessProcessing(int maxNumWorkPerRun) {
        this(maxNumWorkPerRun, null);
    }

    public LocklessProcessing(int maxNumWorkPerRun, Runnable runCallback) {
        m_maxNumWorkPerRun = maxNumWorkPerRun;
        m_runCallback = runCallback;
    }

    /**
     * Supplied ParamBag will be released after it is run
     *
     */
    public final void runWork(CallbackParamBag bag) {
        if (currentThreadIsWorker() && callbackDepthOk()) {
            RCInteger rci = m_callbackDepth.get();
            rci.increment();
            try {
                bag.locklessCallback();
            } finally {
                rci.decrement();
            }
            return;
        }
        final long currentRequestTick = m_currentWorkTick.get();

        RCBoolean committed = RCBoolean.create(false);
        m_workQueue.enqueue(bag, committed);

        // We do not want to signal the runner since the item is not accessible from the queue until
        // it is committed
        if (committed.value() == true) {
            // The trick here is that m_currentWorkTick only needs to change, it does not
            // indicate a number of items to do or a count of things to process.  This is just
            // a housekeeping number which must change to keep the worker running
            if (m_currentWorkTick.compareAndSet(currentRequestTick, currentRequestTick + 1)) {
                if (currentRequestTick == 0) {
                    // no worker is currently running work
                    m_runner.run();
                } else {
                    // Updated successfully, currently running work will pick this up
                }
            } else if (m_currentWorkTick.compareAndSet(0, currentRequestTick + 1)) {
                // no worker is currently running work
                m_runner.run();
            } else {
                // This conditions happens when there are multiple threads calling requestMoreWork
                // Only one can win to be considered the "owner" of submitting this work item
                // The one that loses, can silently just return it has nothing to do
            }
        }
        committed.release();
    }

    public final boolean currentThreadIsWorker() {
        return m_currentWorker == Thread.currentThread();
    }

    public final static boolean callbackDepthOk() {
        return m_callbackDepth.get().value() < MAX_CALLBACK_DEPTH;
    }

    /**
     * Supplied ParamBag will be released after it is run
     *
     */
    public final void submitWork(CallbackParamBag bag) {
        final long currentRequestTick = m_currentWorkTick.get();

        RCBoolean committed = RCBoolean.create(false);
        m_workQueue.enqueue(bag, committed);

        // We do not want to signal the runner since the item is not accessible from the queue until
        // it is committed
        if (committed.value() == true) {
            // The trick here is that m_currentWorkTick only needs to change, it does not
            // indicate a number of items to do or a count of things to process.  This is just
            // a housekeeping number which must change to keep the worker running
            if (m_currentWorkTick.compareAndSet(currentRequestTick, currentRequestTick + 1)) {
                if (currentRequestTick == 0) {
                    // no worker is currently running work, schedule another thread to work queue
                    m_executor.execute(m_runner);
                } else {
                    // Updated successfully, currently running work will pick this up
                }
            } else if (m_currentWorkTick.compareAndSet(0, currentRequestTick + 1)) {
                // no worker is currently running work, schedule another thread to work queue
                m_executor.execute(m_runner);
            } else {
                // This conditions happens when there are multiple threads calling requestMoreWork
                // Only one can win to be considered the "owner" of submitting this work item
                // The one that loses, can silently just return it has nothing to do
            }
        }
    }

    public void requestProcessingLoop() {
        final long currentRequestTick = m_currentWorkTick.get();

        // The trick here is that m_currentWorkTick only needs to change, it does not
        // indicate a number of items to do or a count of things to process.  This is just
        // a housekeeping number which must change to keep the worker running
        if (m_currentWorkTick.compareAndSet(currentRequestTick, currentRequestTick + 1)) {
            if (currentRequestTick == 0) {
                // no worker is currently running work, schedule another thread to work queue
                m_executor.execute(m_runner);
            } else {
                // Updated successfully, currently running work will pick this up
            }
        } else if (m_currentWorkTick.compareAndSet(0, currentRequestTick + 1)) {
            // no worker is currently running work, schedule another thread to work queue
            m_executor.execute(m_runner);
        } else {
            // This conditions happens when there are multiple threads calling requestProcessingLoop
            // Only one can win to be considered the "owner" of submitting this work item
            // The one that loses, can silently just return it has nothing to do
        }
    }

    public void runProcessingLoop() {
        final long currentRequestTick = m_currentWorkTick.get();

        // The trick here is that m_currentWorkTick only needs to change, it does not
        // indicate a number of items to do or a count of things to process.  This is just
        // a housekeeping number which must change to keep the worker running
        if (m_currentWorkTick.compareAndSet(currentRequestTick, currentRequestTick + 1)) {
            if (currentRequestTick == 0) {
                // no worker is currently running work
                m_runner.run();
            } else {
                // Updated successfully, currently running work will pick this up
            }
        } else if (m_currentWorkTick.compareAndSet(0, currentRequestTick + 1)) {
            // no worker is currently running work
            m_runner.run();
        } else {
            // This conditions happens when there are multiple threads calling runProcessingLoop
            // Only one can win to be considered the "owner" of submitting this work item
            // The one that loses, can silently just return it has nothing to do
        }
    }

    protected void unhandledException(Throwable t) {
        LoggerFactory.getLogger(LocklessProcessing.class).error("Unhandled exception in callback of ParamBag", t);
    }

    private final void queueLoop() {
        m_currentWorker = Thread.currentThread();
        try {
            _queueLoop();
        } finally {
            m_currentWorker = null;
        }
    }

    private final void _queueLoop() {
        RCInteger rci = m_callbackDepth.get();
        rci.increment();
        try {
            int numProcessed = 0;
            while (true) {
                CallbackParamBag bag = (CallbackParamBag)m_workQueue.dequeue();
                if (bag == null) {
                    break;
                }
                try {
                    bag.callback();
                } catch (Throwable t) {
                    unhandledException(t);
                }
                bag.release();
                numProcessed++;
                if (numProcessed >= m_maxNumWorkPerRun) {
                    // We have exceeded the number of bags allowed to be processed so stop processing
                    break;
                }
            }
        } finally {
            rci.decrement();
        }
        if (m_runCallback != null) {
            m_runCallback.run();
        }
    }

    private final class Runner implements Runnable {
        @Override
        public final void run() {
            final long lastRequestTick = m_currentWorkTick.get();
            try {
                queueLoop();
            } finally {
                // Set tick to 0 only if it matches the starting tick
                if (m_currentWorkTick.compareAndSet(lastRequestTick, 0) == false) {
                    // Starting tick didn't match, so we got another request for work while running
                    m_executor.execute(this);
                } else {
                    // No work requested, stopping work
                }
            }
        }
    }
}