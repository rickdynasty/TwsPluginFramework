package com.animaconnected.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;

abstract class GattCommand {

    public abstract void execute(final BluetoothGatt gatt);

    public boolean onRead(final byte[] value) {
        return false; // return true to retry the read
    }

    public void onWrite() {

    }

    public void onServicesDiscovered() {

    }

    public void onDescriptorWrite() {

    }

    public abstract void onError(final Throwable error);
}
