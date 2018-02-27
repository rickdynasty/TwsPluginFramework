package com.animaconnected.future;

/**
 * Represents a value that will be made available asynchronously
 * <p/>
 * A future is pending until it either succeeds or fails. It's possible to register callbacks for those events and to
 * compose multiple futures together in various ways.
 * <p/>
 * The good thing about a future is that it might have a value available already or it might get a value in the
 * future, but it can be dealt with exactly the same way regardless. Therefore, the user don't need to add any special
 * cases for whether the future is still pending or not. Also, by chaining multiple futures together it's possible to
 * automatically abort on the first error and have any errors forwarded to the end of the chain for you (instead of
 * having to handle errors at each step in the chain). This is basically asynchronous exception handling.
 * <p/>
 * These futures are intentionally not thread safe, so only use them on a single thread at a time. If you do work on
 * a background thread and return a future, you need to communicate with your background thread without touching the
 * futures on the background thread. See the BackgroundRunner classes for ways of conveniently doing this.
 *
 * @param <T> The type of the future value
 */
public interface Future<T> {

    /**
     * Register a callback when the result becomes available
     * <p/>
     * The callback is called immediately if the value is already available.
     *
     * @param callback The callback to call as soon as the result is available.
     * @return a reference to this future so more calls can be chained after this.
     */
    Future<T> success(SuccessCallback<T> callback);

    /**
     * Register a callback when the future fails
     * <p/>
     * The callback is called immediately if the future has already failed.
     *
     * @param callback The callback to call as soon as the future has failed.
     * @return a reference to this future so more calls can be chained after this.
     */
    Future<T> fail(FailCallback callback);

    /**
     * Register a callback that is always called when the future either fails or succeeds
     * <p/>
     * The callback is called immediately if the future has already succeeded or failed.
     *
     * @param callback The callback to call as soon as the future isn't pending anymore.
     * @return a reference to this future so more calls can be chained after this.
     */
    Future<T> always(AlwaysCallback callback);

    /**
     * Get the result directly if the Future already has a successful value
     * <p/>
     * This throws an exception if the Future is still pending or has failed.
     */
    T get();

    /**
     * Map the result of this future to a new value of any type
     * <p/>
     * The mapping callback is only called if this future succeeds. Returns a future that gets the mapped value as
     * its result or any failure that occurs (either this future fails or the callback throws an exception).
     *
     * @param callback the callback that does the mapping of the future's result value.
     * @param <D>      The data type that we're mapping to.
     * @return a new future that returns the mapped value.
     */
    <D> Future<D> map(MapCallback<T, D> callback);

    /**
     * Map the result of this future to a new future of any type and flatten out the two futures
     * <p/>
     * Works similarly to map, but the returned future's value will be the value of the future from the callback
     * (and not that future itself). Doing a plain map and returning a future in the callback would create a future
     * of a future of a value. This method does that same thing and then "flattens" out one level of nested futures
     * so that the returned future will get the value of callback's future and will hide that future itself.
     * <p/>
     * The returned future fails if either future fails (this or the one from the callback) or the callback throws an
     * exception. No need to write any special error propagation source code. The chaining is automatic.
     *
     * @param callback a callback that returns a new future based on the result of this future
     * @param <D>      The type of the result that the callback's future has.
     * @return the new future, that chains this and the callback's return value after each other.
     */
    <D> Future<D> flatMap(FlatMapCallback<T, D> callback);

    /**
     * Map an error into a successful value (or fail again with a new exception)
     * <p/>
     * The new future behaves exactly the same when this future succeeds. When this future fails, the callback gets a
     * chance to handle the failure and return a successful value for the returned future anyway.
     *
     * @param callback a callback that gets the error and returns a value or throws a new exception.
     * @return a new future, that wraps the error handler on top of this future.
     */
    Future<T> catchError(MapCallback<Throwable, T> callback);

    /**
     * Map a specific type of error into a successful value (or fail again with a new exception)
     * <p/>
     * The new future behaves exactly the same when this future succeeds. When this future fails, the callback gets a
     * chance to handle the failure and return a successful value for the returned future anyway.
     * <p/>
     * This is very similar to a catch clause in synchronous Java source code. Only the specified exception type is
     * caught.
     *
     * @param exceptionType only catch exceptions that are instances of this type.
     * @param callback      a callback that gets the error and returns a value or throws a new exception.
     * @return a new future, that wraps the error handler on top of this future.
     */
    <E> Future<T> catchError(Class<E> exceptionType, MapCallback<E, T> callback);

    /**
     * Add a delay after this future's value is available before callbacks are called
     * <p/>
     * The delay doesn't happen if the future fails.
     *
     * @param milliseconds The number of milliseconds to wait
     * @return a new future that will have the same value as this, after the delay.
     */
    Future<T> delay(long milliseconds);

    /**
     * Add a timeout to the current future
     * <p/>
     * The new future will fail after the timeout, if a result isn't ready by then. Otherwise, it
     * will behave exactly like this future. If a timeout occurs, the actual value will be
     * ignored when it later becomes available (if it does).
     *
     * @param milliseconds The time from now that it will time out after.
     * @return a new future that will have the same value as this unless it times out.
     */
    Future<T> timeout(long milliseconds);
}
