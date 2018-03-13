package com.example.pluginbluetooth.provider;

import com.example.pluginbluetooth.bluetooth.device.DeviceConnectionListener;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;

/**
 * Created by Administrator on 2018/3/1.
 */

public interface BluetoothOnboardingProvider {
    boolean isOnboardingFinished();

    boolean hasDevice();

    boolean isConnecting();

    boolean isConnected();

    boolean isInDfuMode();

    void setGattDevice(GattDevice device);

    boolean getWroteOnboardingDeviceSettings();

    void registerDeviceConnectionListener(DeviceConnectionListener listener);

    void unregisterDeviceConnectionListener(DeviceConnectionListener listener);
}
