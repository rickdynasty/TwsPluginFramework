package com.example.pluginbluetooth.provider;

import com.example.pluginbluetooth.bluetooth.device.DeviceConnectionListener;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.watch.WatchProvider;

import qrom.component.log.QRomLog;

class BluetoothOnboardingProviderImpl implements BluetoothOnboardingProvider {

    private static final String TAG = "rick_Print:BluetoothOnboardingProviderImpl";

    private final WatchProvider mWatch;

    public BluetoothOnboardingProviderImpl(final WatchProvider watch) {
        mWatch = watch;
    }

    @Override
    public boolean isOnboardingFinished() {
        return mWatch.isOnboardingFinished();
    }

    @Override
    public boolean getWroteOnboardingDeviceSettings() {
        return mWatch.getWroteOnboardingDeviceSettings();
    }

    @Override
    public boolean hasDevice() {
        return mWatch.hasDevice();
    }

    @Override
    public boolean isConnecting() {
        return mWatch.isConnecting();
    }

    @Override
    public boolean isConnected() {
        return mWatch.isConnected();
    }

    @Override
    public boolean isInDfuMode() {
        return mWatch.isInDfuMode();
    }

    @Override
    public void setGattDevice(final GattDevice device) {
        QRomLog.i(TAG, "setGattDevice");
        mWatch.setGattDevice(device);
    }

    @Override
    public void registerDeviceConnectionListener(final DeviceConnectionListener listener) {
        mWatch.registerDeviceConnectionListener(listener);
    }

    @Override
    public void unregisterDeviceConnectionListener(final DeviceConnectionListener listener) {
        mWatch.unregisterDeviceConnectionListener(listener);
    }
}
