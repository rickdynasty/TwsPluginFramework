package com.example.pluginbluetooth.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.example.pluginbluetooth.utils.ByteUtils;
import com.example.pluginbluetooth.utils.Callback;

import qrom.component.log.QRomLog;

class GattWriteCommand extends GattCommand {

    private static final String TAG = GattWriteCommand.class.getSimpleName();

    private final BluetoothGattCharacteristic mCharacteristic;
    private final byte[]                      mData;
    private final Callback<Void>              mCallback;
    private final boolean                     mIsDebugMode;

    public GattWriteCommand(final BluetoothGattCharacteristic characteristic, final byte[] data,
                            final Callback<Void> callback, final boolean enableDebugMode) {
        mCharacteristic = characteristic;
        mData = data;
        mCallback = callback;
        mIsDebugMode = enableDebugMode;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        if (mIsDebugMode) QRomLog.i(TAG, "Writing " + ByteUtils.bytesToHex(mData) + " to " + mCharacteristic.getUuid());
        mCharacteristic.setValue(mData);
        final boolean success = gatt.writeCharacteristic(mCharacteristic);
        if (!success) {
            QRomLog.i(TAG, "Failed to initiate write characteristic");
        }
    }

    @Override
    public void onWrite() {
        if (mCallback != null) mCallback.onSuccess(null);
    }

    @Override
    public void onError(final Throwable error) {
        if (mCallback != null) mCallback.onError(error);
    }

}
