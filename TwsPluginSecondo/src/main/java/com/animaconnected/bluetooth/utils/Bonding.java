package com.animaconnected.bluetooth.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;

public class Bonding {

    private static final String TAG = Bonding.class.getSimpleName();

    private Bonding() {
    }

    public static void removeBondFromDevice(String address) {
        try {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            if (device != null && device.getBondState() != BluetoothDevice.BOND_NONE) {
                final Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
                boolean result = (Boolean) removeBondMethod.invoke(device);
                if (!result) {
                    Log.d(TAG, "Failed to remove bond");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to remove bond", e);
        }
    }

    public static boolean isDeviceBonded(String deviceAddress) {
        if (deviceAddress != null) {
            for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                if (device != null && deviceAddress.equals(device.getAddress())) {
                    return true;
                }
            }
        }

        return false;
    }
}
