package com.animaconnected.bluetooth.utils;

public interface Callback<T> {

    void onSuccess(T result);

    void onError(Throwable error);
}
