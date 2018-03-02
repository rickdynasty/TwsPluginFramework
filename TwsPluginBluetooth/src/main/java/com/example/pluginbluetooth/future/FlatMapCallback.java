package com.example.pluginbluetooth.future;

public interface FlatMapCallback<T, D> {

    Future<D> onResult(T result) throws Exception;
}
