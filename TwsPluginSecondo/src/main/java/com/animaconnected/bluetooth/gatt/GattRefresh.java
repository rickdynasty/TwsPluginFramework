package com.animaconnected.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.lang.reflect.Method;

public class GattRefresh {

    private static final String TAG = GattRefresh.class.getSimpleName();

    private GattRefresh() {
    }

    public static boolean refreshServices(final BluetoothGatt gatt) {
        boolean success = false;

        /*
         * If the device is bonded this is up to the Service Changed characteristic to notify Android
         * that the services has changed. There is no need for this trick in that case.
         * If not bonded, the Android should not keep the services cached when the Service Changed
         * characteristic is present in the target device database.
         * However, due to the Android bug (still exists in Android 5.0.1), it is keeping them anyway
         * and the only way to clear services is by using this hidden refresh method.
         */
        if (gatt != null && gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
            try {
                final Method refresh = gatt.getClass().getMethod("refresh");

                if (refresh != null) {
                    success = (Boolean) refresh.invoke(gatt);
                }
            } catch (Exception e) {
                Log.d(TAG, "An exception occurred while refreshing device", e);
            }
        }

        Log.d(TAG, "Refreshing result: " + success);

        return success;
    }
}
