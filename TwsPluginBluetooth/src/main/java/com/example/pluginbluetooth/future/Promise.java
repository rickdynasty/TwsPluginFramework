package com.example.pluginbluetooth.future;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * A promise of a future result
 * <p/>
 * The promise can either be resolved or rejected later on. Use getFuture() to get a future
 * object that can be returned to callers and then composed in various ways.
 * <p/>
 * Note that a promise can be used as a ResultCallback or Callback for compatibility with those.
 *
 * @param <T> the data type of the result
 */
public class Promise<T> {

    private enum State {PENDING, RESOLVED, REJECTED}

    private State mState = State.PENDING;
    private List<SuccessCallback<T>> mSuccessCallbacks = new ArrayList<SuccessCallback<T>>();
    private List<FailCallback> mFailCallbacks = new ArrayList<FailCallback>();
    private List<AlwaysCallback> mAlwaysCallbacks = new ArrayList<AlwaysCallback>();
    private T mResult;
    private Throwable mError;

    /**
     * Resolve a pending promise and report a result value
     * <p/>
     * All callbacks are called from the thread that calls resolve.
     *
     * @param result the final result
     */
    public void resolve(T result) {
        if (mState != State.PENDING) throw new IllegalStateException("Promise isn't pending!");
        mState = State.RESOLVED;
        mResult = result;

        notifySuccess();
        notifyAlwaysCallback();
    }

    /**
     * Break a pending promise and report the failure
     * <p/>
     * All callbacks are called from the thread that calls reject.
     *
     * @param error description of the failure
     */
    public void reject(Throwable error) {
        if (mState != State.PENDING) throw new IllegalStateException("Promise isn't pending!");
        mState = State.REJECTED;
        mError = error;

        notifyFail();
        notifyAlwaysCallback();
    }

    /**
     * Returns true if the future is still pending
     */
    private boolean isPending() {
        return mState == State.PENDING;
    }

    private void notifySuccess() {
        for (SuccessCallback<T> callback : mSuccessCallbacks) {
            callback.onSuccess(mResult);
        }
    }

    private void notifyFail() {
        for (FailCallback callback : mFailCallbacks) {
            callback.onFail(mError);
        }
    }

    private void notifyAlwaysCallback() {
        for (AlwaysCallback callback : mAlwaysCallbacks) {
            callback.onFinished();
        }
    }

    public Future<T> getFuture() {
        return mFuture;
    }

    private final Future<T> mFuture = new Future<T>() {
        @Override
        public Future<T> success(final SuccessCallback<T> callback) {
            mSuccessCallbacks.add(callback);
            if (mState == State.RESOLVED) callback.onSuccess(mResult);
            return this;
        }

        @Override
        public Future<T> fail(final FailCallback callback) {
            mFailCallbacks.add(callback);
            if (mState == State.REJECTED) callback.onFail(mError);
            return this;
        }

        @Override
        public Future<T> always(final AlwaysCallback callback) {
            mAlwaysCallbacks.add(callback);
            if (mState != State.PENDING) callback.onFinished();
            return this;
        }

        @Override
        public T get() {
            if (mState != State.RESOLVED) {
                throw new IllegalStateException("The Future's value is not available!");
            }
            return mResult;
        }

        @Override
        public <D> Future<D> flatMap(final FlatMapCallback<T, D> callback) {
            final Promise<D> pipePromise = new Promise<D>();

            success(new SuccessCallback<T>() {
                @Override
                public void onSuccess(final T result) {
                    final Future<D> future;
                    try {
                        future = callback.onResult(result);
                        if (future == null) {
                            throw new RuntimeException("Callback returned no future.");
                        }
                    } catch (Exception error) {
                        pipePromise.reject(error);
                        return;
                    }
                    future.success(new SuccessCallback<D>() {
                        @Override
                        public void onSuccess(final D result) {
                            pipePromise.resolve(result);
                        }
                    });
                    future.fail(new FailCallback() {
                        @Override
                        public void onFail(final Throwable error) {
                            pipePromise.reject(error);
                        }
                    });
                }
            });

            fail(new FailCallback() {
                @Override
                public void onFail(final Throwable error) {
                    pipePromise.reject(error);
                }
            });

            return pipePromise.getFuture();
        }

        @Override
        public Future<T> catchError(final MapCallback<Throwable, T> callback) {
            return catchError(Throwable.class, callback);
        }

        @Override
        public <E> Future<T> catchError(final Class<E> exceptionType, final MapCallback<E, T> callback) {
            final Promise<T> promise = new Promise<T>();

            success(new SuccessCallback<T>() {
                @Override
                public void onSuccess(final T result) {
                    promise.resolve(result);
                }
            });

            fail(new FailCallback() {
                @Override
                public void onFail(final Throwable error) {
                    if (!exceptionType.isInstance(error)) {
                        promise.reject(error); // Propagate exceptions of the wrong type as is
                        return;
                    }
                    final T mappedValue;
                    try {
                        mappedValue = callback.onResult(exceptionType.cast(error));
                    } catch (Exception newError) {
                        promise.reject(newError);
                        return;
                    }
                    promise.resolve(mappedValue);
                }
            });

            return promise.getFuture();
        }

        @Override
        public <D> Future<D> map(final MapCallback<T, D> callback) {
            final Promise<D> filterPromise = new Promise<D>();

            success(new SuccessCallback<T>() {
                @Override
                public void onSuccess(final T result) {
                    final D mappedValue;
                    try {
                        mappedValue = callback.onResult(result);
                    } catch (Exception error) {
                        filterPromise.reject(error);
                        return;
                    }
                    filterPromise.resolve(mappedValue);
                }
            });

            fail(new FailCallback() {
                @Override
                public void onFail(final Throwable error) {
                    filterPromise.reject(error);
                }
            });

            return filterPromise.getFuture();
        }

        @Override
        public Future<T> delay(final long milliseconds) {
            return flatMap(new FlatMapCallback<T, T>() {
                @Override
                public Future<T> onResult(final T result) {
                    final Promise<T> promise = new Promise<T>();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            promise.resolve(result);
                        }
                    }, milliseconds);

                    return promise.getFuture();
                }
            });
        }

        @Override
        public Future<T> timeout(final long milliseconds) {
            final Promise<T> promise = new Promise<T>();
            final Handler handler = new Handler();

            final Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (promise.isPending()) {
                        promise.reject(new TimeoutException("Timed out"));
                    }
                }
            };
            handler.postDelayed(timeoutRunnable, milliseconds);

            success(new SuccessCallback<T>() {
                @Override
                public void onSuccess(final T result) {
                    if (promise.isPending()) {
                        handler.removeCallbacks(timeoutRunnable);
                        promise.resolve(result);
                    }
                }
            });

            fail(new FailCallback() {
                @Override
                public void onFail(final Throwable error) {
                    if (promise.isPending()) {
                        handler.removeCallbacks(timeoutRunnable);
                        promise.reject(error);
                    }
                }
            });

            return promise.getFuture();
        }
    };
}
