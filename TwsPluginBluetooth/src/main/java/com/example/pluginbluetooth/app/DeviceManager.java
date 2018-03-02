package com.example.pluginbluetooth.app;

import android.content.Context;

import com.example.pluginbluetooth.watch.DeviceAvailableListener;
import com.example.pluginbluetooth.watch.WatchProvider;

import qrom.component.log.QRomLog;

/**
 * Keeps track of whether we have a device or not and starts/stops the DeviceService
 */
public class DeviceManager implements DeviceAvailableListener {

    private static final String TAG = DeviceManager.class.getSimpleName();

    private final Context mContext;

    public DeviceManager(final Context context) {
        mContext = context;
    }

    /**
     * Called when a device is added. Always happens once if a device is loaded from disk.
     */
    @Override
    public void onDeviceAdded() {
        QRomLog.d(TAG, "Device added. Starting device service.");
        DeviceService.start(mContext);
    }

    /**
     * Called if we had a device and it is removed (not called when a new device replaces an old)
     */
    @Override
    public void onDeviceRemoved() {
        QRomLog.d(TAG, "Device removed. Stopping device service.");
        DeviceService.stop(mContext);
    }

    public void listenTo(final WatchProvider watchProvider) {
        watchProvider.registerDeviceAvailableListener(this); // Listen forever
        if (watchProvider.hasDevice()) {
            onDeviceAdded();
        }
    }
}
