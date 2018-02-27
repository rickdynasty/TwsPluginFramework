package com.animaconnected.watch;

import com.animaconnected.bluetooth.device.DeviceConnectionListener;
import com.animaconnected.bluetooth.device.WatchDevice;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Administrator on 2018/2/27.
 */

public class WatchProvider {
    private WatchDevice mWatchDevice;

    private final Set<DeviceConnectionListener> mDeviceConnectionListeners = new CopyOnWriteArraySet<DeviceConnectionListener>();

    public void registerDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListeners.add(listener);
    }

    public void unregisterDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListeners.remove(listener);
    }

    public boolean isConnected() {
        return mWatchDevice != null && mWatchDevice.isConnected();
    }
}
