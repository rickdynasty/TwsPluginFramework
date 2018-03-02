package com.example.pluginbluetooth.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;

import com.example.pluginbluetooth.utils.Callback;

import qrom.component.log.QRomLog;

class GattDiscoverServicesCommand extends GattCommand {

    private static final String TAG      = GattDiscoverServicesCommand.class.getSimpleName();
    private static final long   DELAY_MS = 1600;

    private Callback<Void> mCallback;
    private boolean mHasFailed = false;

    public GattDiscoverServicesCommand(final Callback<Void> callback) {
        mCallback = callback;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mHasFailed)
                    doDiscoverServices(gatt);
            }
        }, DELAY_MS);
    }

    private void doDiscoverServices(final BluetoothGatt gatt) {
        final boolean success = gatt.discoverServices();
        if (!success) {
            QRomLog.d(TAG, "Discover services did not successfully start!");
        }
    }

    @Override
    public void onServicesDiscovered() {
        mCallback.onSuccess(null);
    }

    @Override
    public void onError(final Throwable error) {
        mCallback.onError(error);
        mHasFailed = true;
    }
}
