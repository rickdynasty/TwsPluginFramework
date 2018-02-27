package com.animaconnected.secondo.utils;

import android.os.Looper;
import android.util.Log;

public class ThreadUtils {

    private static final String TAG = ThreadUtils.class.getSimpleName();

    /**
     * Only used to catch implemantation mistakes
     */
    public static void assertIsOnMainThread() {
        if(BuildConfig.DEBUG && Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            Log.e(TAG, "Assertion failed: not on the main thread!");
            throw new RuntimeException("Assertion failed: not on the main thread!");
        }
    }

    /**
     * Only used to catch implemantation mistakes
     */
    public static void assertIsNotOnMainThread() {
        if(BuildConfig.DEBUG && Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId()) {
            Log.e(TAG, "Assertion failed: not allowed to be on the main thread!");
            throw new RuntimeException("Assertion failed: not allowed to be on the main thread!");
        }
    }
}
