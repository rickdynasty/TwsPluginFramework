package com.example.pluginbluetooth.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import qrom.component.log.QRomLog;

class GattReadCommand extends GattCommand {

    private static final String TAG = GattReadCommand.class.getSimpleName();

    private final BluetoothGattCharacteristic mCharacteristic;
    private final ReadCallback mCallback;

    public GattReadCommand(final BluetoothGattCharacteristic characteristic, final ReadCallback callback) {
        mCharacteristic = characteristic;
        mCallback = callback;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        final boolean success = gatt.readCharacteristic(mCharacteristic);
        if (!success) {
            QRomLog.d(TAG, "Read failed!");
        }
    }

    @Override
    public boolean onRead(final byte[] value) {
        return mCallback.onSuccess(value);
    }

    @Override
    public void onError(final Throwable error) {
        mCallback.onError(error);
    }
}
