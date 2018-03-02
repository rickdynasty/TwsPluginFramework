package com.example.pluginbluetooth.future.runner;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.Promise;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Queues and runs callables one-by-one on background threads
 * <p>
 * Results are returned on the main thread. Uses a shared thread pool by default.
 * <p>
 * Only call this and the returned futures from the main thread.
 */
@SuppressLint("NewApi")
public class SequentialBackgroundRunner implements BackgroundRunner {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Queue<Runnable> mQueue = new LinkedList<Runnable>();
    private final Object mLock = new Object();
    private final Object mRunLock = new Object();
    private final Executor mExecutor;
    private boolean mRunningTask = false;

    public SequentialBackgroundRunner() {
        mExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public SequentialBackgroundRunner(final Executor executor) {
        mExecutor = executor;
    }

    @Override
    public <T> Future<T> submit(final Callable<T> callable) {
        final Promise<T> promise = new Promise<T>();

        addToQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mRunLock) {
                        final T result = callable.call();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                promise.resolve(result);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            promise.reject(e);
                        }
                    });
                } finally {
                    synchronized (mLock) {
                        mRunningTask = false;
                        executeNextIfReadyLocked();
                    }
                }
            }
        });

        synchronized (mLock) {
            executeNextIfReadyLocked();
        }

        return promise.getFuture();
    }

    private void addToQueue(final Runnable runnable) {
        synchronized (mLock) {
            mQueue.add(runnable);
        }
    }

    private void executeNextIfReadyLocked() {
        if (!mRunningTask && !mQueue.isEmpty()) {
            mRunningTask = true;
            final Runnable runnable = mQueue.remove();
            mExecutor.execute(runnable);
        }
    }
}
