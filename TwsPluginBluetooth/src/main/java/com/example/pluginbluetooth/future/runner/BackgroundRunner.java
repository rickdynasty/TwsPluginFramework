package com.example.pluginbluetooth.future.runner;

import com.example.pluginbluetooth.future.Future;

import java.util.concurrent.Callable;

/**
 * Interface for various "runners" that run something on a background thread and returns a Future.
 */
public interface BackgroundRunner {

    /**
     * Runs callable on a background thread and return a Future result
     * <p>
     * Should only be called on the main thread and resolves/rejects the Future on the main thread.
     * <p>
     * Some runners will queue tasks before actually running them.
     *
     * @param callable The task to run on the background thread.
     * @param <T>      The value type of the Future result.
     * @return Future representing the future return value of the callable or any thrown exception.
     */
    <T> Future<T> submit(final Callable<T> callable);
}
