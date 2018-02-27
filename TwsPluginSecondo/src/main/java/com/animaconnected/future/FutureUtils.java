package com.animaconnected.future;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;

@SuppressLint("NewApi")
public class FutureUtils {

    public static <T> Future<List<T>> merge(List<Future<T>> futures) {
        return new MergePromise<T>(futures).getFuture();
    }

    public static <K, V> Future<Map<K, V>> unwrap(Map<K, Future<V>> futures) {
        final List<Future<Map.Entry<K, V>>> entries = new ArrayList<Future<Entry<K, V>>>();
        for (Map.Entry<K, Future<V>> entry : futures.entrySet()) {
            entries.add(unwrap(entry));
        }
        return merge(entries)
                .map(new MapCallback<List<Map.Entry<K, V>>, Map<K, V>>() {
                    @Override
                    public Map<K, V> onResult(final List<Map.Entry<K, V>> result) {
                        final Map<K, V> newMap = new HashMap<K, V>();
                        for (Map.Entry<K, V> entry : result) {
                            newMap.put(entry.getKey(), entry.getValue());
                        }
                        return newMap;
                    }
                });
    }

    public static <K, V> Future<Map.Entry<K, V>> unwrap(final Map.Entry<K, Future<V>> entry) {
        return entry.getValue().map(new MapCallback<V, Map.Entry<K, V>>() {
            @Override
            public Map.Entry<K, V> onResult(final V result) {
                return new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), result);
            }
        });
    }

    public static <T> Future<T> just(final T result) {
        Promise<T> promise = new Promise<T>();
        promise.resolve(result);
        return promise.getFuture();
    }

    public static <T> Future<T> error(final Throwable error) {
        Promise<T> promise = new Promise<T>();
        promise.reject(error);
        return promise.getFuture();
    }

}
