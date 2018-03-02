package com.example.pluginbluetooth.bluetooth.gatt;

public interface ReadCallback {

    /**
     * Called when a read was successful
     *
     * Return false if the value is valid and true to retry the read.
     */
    boolean onSuccess(byte[] value);

    void onError(Throwable error);
}
