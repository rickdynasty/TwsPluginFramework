package com.example.pluginbluetooth.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;

import com.example.pluginbluetooth.utils.Callback;

import java.util.UUID;

import qrom.component.log.QRomLog;

class GattSetNotificationCommand extends GattCommand {

    private static final String TAG = GattSetNotificationCommand.class.getSimpleName();

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Handler mHandler = new Handler();
    private final BluetoothGattCharacteristic mCharacteristic;
    private final boolean mListen;
    private final Callback<Void> mCallback;

    public GattSetNotificationCommand(final BluetoothGattCharacteristic characteristic, final boolean listen,
                                      final Callback<Void> callback) {
        mCharacteristic = characteristic;
        mListen = listen;
        mCallback = callback;
    }

    @Override
    public void onDescriptorWrite() {
        if (mCallback != null) mCallback.onSuccess(null);
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        final boolean success = gatt.setCharacteristicNotification(mCharacteristic, mListen);

        if (!success) {
            onError(new RuntimeException("Failed to initiate set characteristic notification"));
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                if (descriptor == null) {
                    onError(new RuntimeException("Failed to set notification. Didn't find GATT descriptor!"));
                    return;
                }

                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                final boolean descriptorSuccess = gatt.writeDescriptor(descriptor);
                if (!descriptorSuccess) {
                    QRomLog.d(TAG, "Failed to write to descriptor");
                }
            }
        }, 1000);
    }

    @Override
    public void onError(final Throwable error) {
        QRomLog.d(TAG, error.getMessage());
        if (mCallback != null) mCallback.onError(error);
    }

}
