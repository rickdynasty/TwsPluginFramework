package com.example.pluginbluetooth.utils;

public interface Callback<T> {

    void onSuccess(T result);

    void onError(Throwable error);
}
