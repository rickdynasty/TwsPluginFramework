package com.animaconnected.secondo.provider.status.internal.connection;

import android.bluetooth.BluetoothAdapter;

import com.animaconnected.secondo.provider.status.internal.BaseStatusModel;

public class BluetoothDisabledStatus extends BaseStatusModel {

    @Override
    public int getPriority() {
        return BLUETOOTH_DISABLED_PRIORITY;
    }

    public void enableBluetooth() {
        BluetoothAdapter.getDefaultAdapter().enable();
    }
}
