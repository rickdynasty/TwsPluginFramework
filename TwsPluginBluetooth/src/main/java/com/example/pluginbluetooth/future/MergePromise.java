package com.example.pluginbluetooth.future;

import java.util.ArrayList;
import java.util.List;

class MergePromise<T> {

    private final Promise<List<T>> mPromise = new Promise<List<T>>();
    private boolean mFailed = false;
    private int mNumSucceeded = 0;

    MergePromise(final List<Future<T>> futures) {
        final int numFutures = futures.size();
        final List<T> results = new ArrayList<T>(numFutures);
        if (numFutures > 0) {
            for (int i = 0; i < numFutures; i++) {
                final int index = i;
                final Future<T> future = futures.get(index);
                results.add(null);

                future.fail(new FailCallback() {
                    @Override
                    public void onFail(final Throwable error) {
                        if (!mFailed) {
                            mFailed = true;
                            mPromise.reject(error); // We return the first error
                        }
                    }
                });

                future.success(new SuccessCallback<T>() {
                    @Override
                    public void onSuccess(final T result) {
                        results.set(index, result);
                        mNumSucceeded++;
                        if (mNumSucceeded == numFutures) {
                            mPromise.resolve(results);
                        }
                    }
                });
            }
        } else {
            mPromise.resolve(results);
        }
    }

    public Future<List<T>> getFuture() {
        return mPromise.getFuture();
    }
}
