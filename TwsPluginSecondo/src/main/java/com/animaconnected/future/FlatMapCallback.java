package com.animaconnected.future;

public interface FlatMapCallback<T, D> {

    Future<D> onResult(T result) throws Exception;
}
