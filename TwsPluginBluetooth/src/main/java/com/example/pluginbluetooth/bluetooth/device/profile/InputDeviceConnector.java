package com.example.pluginbluetooth.bluetooth.device.profile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import qrom.component.log.QRomLog;

@SuppressLint("NewApi")
public class InputDeviceConnector implements BluetoothProfile.ServiceListener {

    private static final String TAG = InputDeviceConnector.class.getSimpleName();
    private static InputDeviceConnector sInstance;

    private ProfileConnectionListener mProfileConnectionListener;
    private static final int PROFILE_INPUT_DEVICE = 4; // BluetoothProfile.INPUT_DEVICE
    private BluetoothProfile mProxy;
    private BluetoothDevice mDevice;

    private InputDeviceConnector() {
        //singleton
    }

    public void doConnectIfNeeded(BluetoothDevice device,
                                  ProfileConnectionListener profileConnectionListener,
                                  Context context) {
        QRomLog.i(TAG, "doConnectIfNeeded... ");
        mDevice = device;
        mProfileConnectionListener = profileConnectionListener;
        if (isConnected()) {
            reportConnectionResultToListenerIfNeeded(true);
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            // Some android devices stops reporting service listener callbacks for the proxy
            // after one connection, that is why the current one is closed and a new is setup here
            if (mProxy != null) {
                adapter.closeProfileProxy(PROFILE_INPUT_DEVICE, mProxy);
            }
            // getProfileProxy will result in a call to onServiceConnected where the proxy can
            // be fetched
            adapter.getProfileProxy(context, this, PROFILE_INPUT_DEVICE);
        }
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        QRomLog.i(TAG, "onServiceConnected profile: " + profile);
        if (profile == PROFILE_INPUT_DEVICE) {
            mProxy = proxy;
            boolean success = true;
            if (!isConnected()) {
                success = tryConnect();
            }
            reportConnectionResultToListenerIfNeeded(success);
        } else {
            QRomLog.i(TAG, "This should not happend, the profile should be: " + PROFILE_INPUT_DEVICE);
        }
    }

    @Override
    public void onServiceDisconnected(int profile) {
        QRomLog.i(TAG, "onServiceDisconnected profile: " + profile);
        if (profile == PROFILE_INPUT_DEVICE) {
            reportConnectionResultToListenerIfNeeded(false);
        }
    }

    private boolean isConnected() {
        if (mProxy != null) {
            List<BluetoothDevice> connectedDevices = mProxy.getConnectedDevices();
            for (BluetoothDevice device : connectedDevices) {
                if (mDevice.getAddress().equals(device.getAddress())) {
                    QRomLog.i(TAG, "Input device is connected");
                    return true;
                }
            }
        }
        QRomLog.i(TAG, "Input device is disconnected");
        return false;
    }

    private void reportConnectionResultToListenerIfNeeded(boolean success) {
        if (mProfileConnectionListener != null) {
            mProfileConnectionListener.onConnectionAttemptFinished(success);
            mProfileConnectionListener = null;
        }
    }

    private boolean tryConnect() {
        boolean success = false;
        try {
            QRomLog.i(TAG, "Trying to connect input device...");
            Method createConnectionMethod = mProxy.getClass().getMethod("connect",
                    BluetoothDevice.class);
            success = (Boolean) createConnectionMethod.invoke(mProxy, mDevice);
            QRomLog.i(TAG, "Input device invoked connect success = " + success);
            reportConnectionResultToListenerIfNeeded(success);
        } catch (NoSuchMethodException e) {
            QRomLog.i(TAG, "Failed to connect to HID profile", e);
        } catch (IllegalAccessException e) {
            QRomLog.i(TAG, "Failed to connect to HID profile", e);
        } catch (InvocationTargetException e) {
            QRomLog.i(TAG, "Failed to connect to HID profile", e);
        } catch (ClassCastException e) {
            QRomLog.i(TAG, "Failed to connect to HID profile", e);
        }
        return success;
    }

    public static InputDeviceConnector getInstance() {
        if (sInstance == null) {
            sInstance = new InputDeviceConnector();
        }
        return sInstance;
    }
}
