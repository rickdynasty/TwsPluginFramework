package com.example.pluginbluetooth.watch;

import android.content.Context;

import com.example.pluginbluetooth.behaviour.Behaviour;
import com.example.pluginbluetooth.bluetooth.device.DeviceConnectionListener;
import com.example.pluginbluetooth.bluetooth.device.WatchDevice;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.future.AlwaysCallback;
import com.example.pluginbluetooth.future.FailCallback;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.FutureUtils;
import com.example.pluginbluetooth.future.SuccessCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2018/3/1.
 */

public class WatchProvider implements DeviceConnectionListener {
    public static final String TAG = "rick_Print:WatchProvider";
    private final Context mContext;
    private final DeviceDataStorage mDeviceDataStorage;
    private boolean mIsWritingDeviceSettings = false;

    private final WatchStorage mStorage;

    private WatchDevice mWatchDevice;

    private final Set<WatchProviderListener> mListeners = new CopyOnWriteArraySet<WatchProviderListener>();
    private final Set<DeviceAvailableListener> mDeviceAvailableListeners = new CopyOnWriteArraySet<DeviceAvailableListener>();
    private final Set<DeviceConnectionListener> mDeviceConnectionListeners = new CopyOnWriteArraySet<DeviceConnectionListener>();

    public WatchProvider(final Context context) {
        mContext = context;

        mDeviceDataStorage = new DeviceDataStorage(mContext);
        mStorage = new WatchStorage(mContext);
    }

    public void setOnboardingStarted() {
        for (WatchProviderListener listener : mListeners) {
            listener.onOnboardingStarted();
        }
    }

    public void setOnboardingFinished(final boolean isFinished) {
        mDeviceDataStorage.setOnboardingFinished(isFinished);
        writeDeviceSettings();

        for (WatchProviderListener listener : mListeners) {
            listener.onOnboardingFinished(isFinished);
        }
    }

    public boolean isOnboardingFinished() {
        return mDeviceDataStorage.getIsOnboardingFinished();
    }

    public void setWroteOnboardingDeviceSettings(final boolean wroteDeviceSettings) {
        mDeviceDataStorage.setWroteOnboardingDeviceSettings(wroteDeviceSettings);
    }

    public boolean getWroteOnboardingDeviceSettings() {
        return mDeviceDataStorage.getWroteOnboardingDeviceSettings();
    }

    public void registerListener(WatchProviderListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(WatchProviderListener listener) {
        mListeners.remove(listener);
    }

    public void registerDeviceAvailableListener(final DeviceAvailableListener listener) {
        mDeviceAvailableListeners.add(listener);
    }

    public void unregisterDeviceAvailableListener(final DeviceAvailableListener listener) {
        mDeviceAvailableListeners.remove(listener);
    }

    public void registerDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListeners.add(listener);
    }

    public void unregisterDeviceConnectionListener(DeviceConnectionListener listener) {
        mDeviceConnectionListeners.remove(listener);
    }

    public boolean hasDevice() {
        return mWatchDevice != null;
    }

    public boolean isConnecting() {
        return mWatchDevice != null && mWatchDevice.isConnecting();
    }

    public boolean isConnected() {
        return mWatchDevice != null && mWatchDevice.isConnected();
    }

    public boolean isInDfuMode() {
        return mWatchDevice != null && mWatchDevice.isInDfuMode();
    }

    public void setGattDevice(final GattDevice device) {
        QRomLog.i(TAG, "Setting device: " + (device != null ? device.getAddress() : null));

        if (mWatchDevice != null) {
            for (DeviceAvailableListener listener : mDeviceAvailableListeners) {
                listener.onDeviceRemoved();
            }
        }

        setDevice(createWatchDeviceWithCache(device));
        // writeBluetoothConfig();
        mStorage.saveDevice(device);

        if (device != null) {
            for (DeviceAvailableListener listener : mDeviceAvailableListeners) {
                listener.onDeviceAdded();
            }
        }
    }

    private WatchDevice createWatchDeviceWithCache(final GattDevice gattDevice) {
        if (gattDevice == null) {
            return null;
        }

        return new WatchDevice(gattDevice, new SharedPreferencesCache(mContext));
    }

    private void setDevice(final WatchDevice watchDevice) {
        if (mWatchDevice != null) {
            mWatchDevice.close();
            mWatchDevice.unregisterConnectionListener(this);
            onDeviceRemovedOrReset();
        }

        mWatchDevice = watchDevice;

        if (mWatchDevice != null) {
            mWatchDevice.registerConnectionListener(this);
        }
    }

    /**
     * Forget the watch on the phone (without saying anything to the watch)
     * <p/>
     * It's useful to first do a resetDevice() if possible.
     */
    public void forgetDevice() {
        if (mWatchDevice != null) {
            mWatchDevice.removeBond();
        }
        setGattDevice(null);
    }

    private void onDeviceRemovedOrReset() {
        QRomLog.i(TAG, "Device was removed or reset. Clearing caches and marking device data as dirty.");
        mDeviceDataStorage.setDirty();
        mDeviceDataStorage.setAllDirty();
        mWatchDevice.invalidateCache();
    }

    @Override
    public void onConnecting() {
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onConnecting();
        }
    }

    @Override
    public void onConnected() {
        QRomLog.i(TAG, "onConnected");
        for (WatchProviderListener listener : mListeners) {
            QRomLog.i(TAG, "onConnected 回调listener：" + listener + " onConnectionChanged(true)");
            listener.onConnectionChanged(true);
        }

        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            QRomLog.i(TAG, "onConnected 回调listener：" + listener + " 的onConnected()");
            listener.onConnected();
        }

        deviceConnected()
                .always(new AlwaysCallback() {
                    @Override
                    public void onFinished() {
                        // writeDeviceSettings();
                        // readDeviceDebugDisconnect();
                    }
                });
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onHardToConnect() {

    }

    @Override
    public void onEnterDfuMode() {

    }

    @Override
    public void onLeaveDfuMode() {

    }

    private Future<Boolean> deviceConnected() {
        return mWatchDevice.readOnboardingDone()
                .success(new SuccessCallback<Boolean>() {
                    @Override
                    public void onSuccess(final Boolean onboardingDone) {
                        if (!onboardingDone) {
                            mDeviceDataStorage.setOnboardingFinished(mDeviceDataStorage.getIsOnboardingFinished());
                            mDeviceDataStorage.setAllDirty();
                            writeDeviceSettings();
                        }
                        checkTimeUpToDate();
                    }
                });
    }

    private void writeDeviceSettings() {
        QRomLog.i(TAG, "writeDeviceSettings");
        if (!mIsWritingDeviceSettings && isConnected() && mDeviceDataStorage.isDirty()) {
            QRomLog.i(TAG, "Starting device settings sync...");
            mDeviceDataStorage.setSyncPending(); // The settings sync starts here
            mIsWritingDeviceSettings = true;

            final List<Future<Void>> futures = new ArrayList<Future<Void>>();

            if (mDeviceDataStorage.isBaseConfigDirty()) {
                // Write base config
                //futures.add(mWatchDevice.writeBaseConfig(10, mDeviceDataStorage.getStepsActive() ? 1 : 0));
            }

            if (mDeviceDataStorage.isDebugConfigDirty()) {
                // Write debug config
//                futures.add(mWatchDevice.writeDebugConfig(
//                        mDeviceDataStorage.getDemoMode(),
//                        ENABLE_UART,
//                        ENABLE_TEMPERATURE,
//                        mRemoteConfigController.getDeviceDisconnectLedAndVibrateEnable(),
//                        ENABLE_MINUTE_TICK,
//                        ON_ERROR_REBOOT_TIMEOUT,
//                        mDeviceDataStorage.getRssiNotification()));
            }

            //futures.addAll(writeCommonSettings());

            // Wait for all the writes to finish
            FutureUtils.merge(futures)
                    .success(new SuccessCallback<List<Void>>() {
                        @Override
                        public void onSuccess(final List<Void> result) {
                            QRomLog.i(TAG, "Device settings sent successfully");
                            mDeviceDataStorage.setSyncDone(); // Mark the sync as successful!
                            mIsWritingDeviceSettings = false;
                            setWroteOnboardingDeviceSettings(true); // Only used in the onboarding
                            for (WatchProviderListener listener : mListeners) {
                                listener.onWroteDeviceSettings();
                            }
                            writeDeviceSettings(); // There could be dirty settings again...
                        }
                    })
                    .fail(new FailCallback() {
                        @Override
                        public void onFail(final Throwable error) {
                            QRomLog.i(TAG, "Failed to send device settings: " + error);
                            mIsWritingDeviceSettings = false;
                            // Don't try again if we failed. Wait for next connection. We don't
                            // want to get stuck in a loop here.
                        }
                    });
        } else {
            QRomLog.i(TAG, "writeDeviceSettings NOT writing settings." + " " + "mIsWritingDeviceSettings " +
                    mIsWritingDeviceSettings + " " + "isConnected() " + isConnected() + " " +
                    "mDeviceDataStorage.isDirty() " + mDeviceDataStorage.isDirty());
        }
    }

    private void checkTimeUpToDate() {
        mWatchDevice.readDateTime()
                .success(new SuccessCallback<Boolean>() {
                    @Override
                    public void onSuccess(final Boolean watchIsUpToDate) {
                        if (!watchIsUpToDate) {
                            mDeviceDataStorage.setForceTimeWrite();
                            mDeviceDataStorage.setDirty();
                            writeDeviceSettings();
                        }
                    }
                });
    }

    interface WatchProviderListener {

        void onAlarmEvent(int alarmState);

        void onButtonClicked(int slot, String behaviour, final int action);

        void onConnectionChanged(boolean isConnected);

        void onDeviceDebugDisconnect(Map<String, String> deviceDiag);

        void onStepsNow(int stepsToday, final int dayOfMonth);

        void onDaily();

        void onStillnessEvent(int stillnessEvent);

        void onRssiEvent(final int onRssiEvent);

        void onDiagEvent(final Map<String, String> diagEvent);

        void onWroteDeviceSettings();

        void onOnboardingStarted();

        void onOnboardingFinished(final boolean isFinished);

        void onTriggerSet(final int slot, final Behaviour behaviour);

        void onConnIntChange(int currentConnInt, int slaveLatency, int timeout);

        void onCalibrationTimeout();
    }

    // Avoid abusing this method. In many cases the correct solution is to add a new method to WatchProvider instead!
    public WatchDevice getDevice() {
        return mWatchDevice;
    }

    public Future<Void> resetDevice() {
        if (isConnected()) {
            return mWatchDevice.writeForgetDevice();
        } else {
            return FutureUtils.error(new RuntimeException("Not connected"));
        }
    }

    public Future<Map<String, String>> getDeviceInformation() {
        if (mWatchDevice == null) {
            return FutureUtils.error(new IllegalStateException("No device"));
        } else {
            return mWatchDevice.readDeviceInformation();
        }
    }
}
