package com.animaconnected.future.runner;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.animaconnected.future.Future;
import com.animaconnected.future.Promise;

/**
 * Runs callables on background threads without any additional queuing
 * <p>
 * Results are returned on the main thread. Uses a shared thread pool by default.
 * <p>
 * Only call this and the returned futures from the main thread.
 */
@SuppressLint("NewApi")
public class ParallelBackgroundRunner implements BackgroundRunner {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor;

    public ParallelBackgroundRunner() {
        mExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public ParallelBackgroundRunner(final Executor executor) {
        mExecutor = executor;
    }

    @Override
    public <T> Future<T> submit(final Callable<T> callable) {
        final Promise<T> promise = new Promise<T>();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = callable.call();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            promise.resolve(result);
                        }
                    });
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            promise.reject(e);
                        }
                    });
                }
            }
        });

        return promise.getFuture();
    }
}
