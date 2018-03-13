package com.example.pluginbluetooth.watch;

import android.content.Context;
import android.os.Handler;

import com.example.pluginbluetooth.behaviour.Behaviour;
import com.example.pluginbluetooth.bluetooth.device.DeviceConnectionListener;
import com.example.pluginbluetooth.bluetooth.device.WatchDevice;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.future.AlwaysCallback;
import com.example.pluginbluetooth.future.FailCallback;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.FutureUtils;
import com.example.pluginbluetooth.future.SuccessCallback;
import com.example.pluginbluetooth.utils.DeviceUtils;

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

    private static final int UPPER_BUTTON = 0;
    private static final int CROWN_BUTTON = 1;
    private static final int LOWER_BUTTON = 2;

    private static final boolean ENABLE_UART = false;
    private static final boolean ENABLE_TEMPERATURE = false;
    private static final boolean ENABLE_MINUTE_TICK = true;
    private static final int ON_ERROR_REBOOT_TIMEOUT = 60;

    private static final long PERIODIC_TASKS_INTERVAL_MS = 60L * 60L * 1000L; // 1 hour
    private static final long DAILY_INTERVAL_MS = 24L * 60L * 60L * 1000L; // 1 day
    private static final long SYNC_SETTINGS_INTERVAL_MS = 24L * 60L * 60L * 1000L; // 7 days

    private static final int ERROR_BATTERY_CRITICAL = 5;
    private static final int ERROR_BATTERY_WARNING = 6;
    private static final int ERROR_CALIBRATION_TIMEOUT = 17;

    private final Handler mHandler = new Handler();
    private final Context mContext;


    private final WatchStorage mStorage;

    private WatchDevice mWatchDevice;

    private final Set<WatchProviderListener> mListeners = new CopyOnWriteArraySet<WatchProviderListener>();
    private final Set<DeviceAvailableListener> mDeviceAvailableListeners = new CopyOnWriteArraySet<DeviceAvailableListener>();
    private final Set<DeviceConnectionListener> mDeviceConnectionListeners = new CopyOnWriteArraySet<DeviceConnectionListener>();

    private final DeviceDataStorage mDeviceDataStorage;
    private boolean mIsWritingDeviceSettings = false;

    public WatchProvider(final Context context) {
        mContext = context;

        mStorage = new WatchStorage(mContext);
        mDeviceDataStorage = new DeviceDataStorage(mContext);
    }

    private void loadDeviceInBackground() {
        mStorage.loadDevice()
                .success(new SuccessCallback<GattDevice>() {
                    @Override
                    public void onSuccess(final GattDevice result) {
                        setGattDevice(result);
                    }
                });
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

    public void setDeviceTime() {
        mDeviceDataStorage.setDirty();
        mDeviceDataStorage.setForceTimeWrite();
        writeDeviceSettings();
    }

    public void enableBluetoothDebug(final boolean enabled) {
        //mWatchDevice.setDebugMode(enabled);
    }

    public boolean isBluetoothInDebug() {
        if (mWatchDevice != null) {
            //return mWatchDevice.getDebugMode();
        }

        return false;
    }

    public Future<Void> setConfigSettings(final Map<String, Integer> settings) {
        if (mWatchDevice != null) {
            if (settings == null || settings.size() == 0) {
                return FutureUtils.just(null);
            }

            try {
                return mWatchDevice.writeConfigSettings(settings);
            } catch (NumberFormatException ex) {
                return FutureUtils.error(new RuntimeException("Error converting map string value to int"));
            }
        }

        return FutureUtils.error(new RuntimeException("No watch device"));
    }

    public void setCalibrationMode(final boolean enabled) {
        if (mWatchDevice != null) {
            mWatchDevice.writeRecalibrate(enabled)
                    .success(new SuccessCallback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                            if (!enabled) {
                                setDeviceTime();
                            }
                        }
                    })
                    .fail(new FailCallback() {
                        @Override
                        public void onFail(final Throwable error) {
                            QRomLog.i(TAG, "Failed to change calibration. Error = [" + error + "]");
                        }
                    });
        }
    }

    public Future<Void> makeNewCalibration(final int motor, final int angleDiff) {
        return mWatchDevice.writeRecalibrateMove(motor, angleDiff);
    }

    public Future<Void> alert(final int alert) {
        if (!isConnected()) {
            return FutureUtils.error(new IllegalStateException("No connected device"));
        }
        return mWatchDevice.writeAlert(alert);
    }

    public boolean isConnected() {
        return mWatchDevice != null && mWatchDevice.isConnected();
    }

    public boolean isHardToConnect() {
        return mWatchDevice != null && mWatchDevice.isHardToConnect();
    }

    public boolean isHighProbabilityForDFUSuccess() {
        return mWatchDevice != null && mWatchDevice.isHighProbabilityForDFUSuccess();
    }

    public boolean isInDfuMode() {
        return mWatchDevice != null && mWatchDevice.isInDfuMode();
    }

    public boolean isConnecting() {
        return mWatchDevice != null && mWatchDevice.isConnecting();
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


    @Override
    public void onDisconnected() {
        QRomLog.i(TAG, "Device disconnected");

        for (WatchProviderListener listener : mListeners) {
            listener.onConnectionChanged(false);
        }
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onDisconnected();
        }
    }

    @Override
    public void onConnecting() {
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onConnecting();
        }
    }

    @Override
    public void onConnected() {
        QRomLog.i(TAG, "Device connected");
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
    public void onHardToConnect() {
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onHardToConnect();
        }
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


    @Override
    public void onEnterDfuMode() {
        QRomLog.i(TAG, "DFU resume available");
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onEnterDfuMode();
        }
    }

    @Override
    public void onLeaveDfuMode() {

        QRomLog.i(TAG, "DFU resume unavailable");
        for (DeviceConnectionListener listener : mDeviceConnectionListeners) {
            listener.onLeaveDfuMode();
        }

    }


    /**
     * Reads steps sum for day relative to today
     *
     * @param day 0 = today, 1 = yesterday, ...
     * @return list of [step count, day of month]
     */
    public Future<List<Integer>> readSteps(final int day) {
        if (mWatchDevice == null) {
            return FutureUtils.error(new RuntimeException("Device isn't connected"));
        }
        return mWatchDevice.readStepsDay(day);
    }

    public Future<Void> writeStepsDay(int steps, int dayOfMonth) {
        if (mWatchDevice == null) {
            return FutureUtils.error(new RuntimeException("Device isn't connected"));
        }
        return mWatchDevice.writeStepsDay(steps, dayOfMonth);
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

    private List<Future<Void>> writeCommonSettings() {
        final List<Future<Void>> retList = new ArrayList<Future<Void>>();

//        final Behaviour complicationBehaviour = getBehaviourCloneForSlot(WatchConstants.SLOT_COMPLICATION);
//        final Behaviour crownBehaviour = getBehaviourCloneForSlot(WatchConstants.SLOT_CROWN);
//        final Set<Integer> activeComplications =
//                new HashSet<Integer>(Arrays.asList(
//                        complicationBehaviour.getDeviceComplicationMode(),
//                        crownBehaviour.getDeviceComplicationMode()));
//
//        if (mDeviceDataStorage.getForceTimeWrite()) {
//            retList.add(mWatchDevice.writeWatchTime().success(new SuccessCallback<Void>() {
//                @Override
//                public void onSuccess(Void result) {
//                    if (BuildConfig.DUBUG_TOOL) {
//                        mWatchDevice.getDateTimeForDebug();
//                    }
//                }
//            }).fail(new FailCallback() {
//                @Override
//                public void onFail(final Throwable error) {
//                    QRomLog.i(TAG, "writeCommonSettings::writeWatchTime fail ERROR:", error);
//                    if (BuildConfig.DUBUG_TOOL) {
//                        Toast.makeText(HostProxy.getApplication(), "writeWatchTime fail!", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }));
//        }
//
//        // Only write complication changes if they have changed
//        if (mDeviceDataStorage.isComplicationDirty() || mDeviceDataStorage.getForceTimeWrite()) {
//            if (activeComplications.contains(DeviceUtils.COMPLICATION_TIMEZONE)) {
//                // Write time and timezone (timezone must be written after time)
//                retList.add(mWatchDevice.writeTimeZone(mDeviceDataStorage.getTimeZoneId()));
//            }
//        }
//
//        // Write complication modes, TODO could increase performance here by only writing when a complication changed
//        int mainMode = DeviceUtils.COMPLICATION_TIME;
//        int alternateMode = crownBehaviour.getDeviceComplicationMode();
//
//        if (WatchDeviceSupport.hasComplication(mWatchDevice.getType())) {
//            int secondaryMainMode = complicationBehaviour.getDeviceComplicationMode();
//            retList.add(mWatchDevice.writeComplicationModes(mainMode,
//                    alternateMode,
//                    secondaryMainMode,
//                    secondaryMainMode));
//
//        } else {
//            retList.add(mWatchDevice.writeComplicationModes(mainMode,
//                    alternateMode));
//        }
//
//        if (mDeviceDataStorage.isAlarmsDirty()) {
//            // 写入闹钟通知配置
//            List<DeviceAlarm> result = NotificationConfig.getClockList();
//            for (DeviceAlarm dm : result) {
//                QRomLog.i("kaelpu", "DeviceAlarm = " + dm.toString());
//            }
//            writeAlarms(result);
//        }
//
//        if (mDeviceDataStorage.isStillnessDirty()) { // Only write stillness if changed
//            // Write stillness
//            final boolean stillnessActive = mDeviceDataStorage.getStillnessActive();
//            retList.add(writeStillness(stillnessActive));
//        }
//
//        if (mDeviceDataStorage.isStepsGoalDirty()) { // Only write steps goal if changed
//            // Write steps goal
//            retList.add(mWatchDevice.writeStepsTarget(mDeviceDataStorage.getStepGoal()));
//        }
//
//        if (mDeviceDataStorage.isOnboardingDirty()) { // Only write onboarding finish if changed
//            if (mDeviceDataStorage.getIsOnboardingFinished()) {
//                retList.add(mWatchDevice.writeOnboardingDone(true));
//            }
//        }
//
//        if (mDeviceDataStorage.isStepsDirty()) {
//            retList.add(mWatchDevice.writeStepsDay(
//                    mDeviceDataStorage.getStepsToday(),
//                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
//        }
//
//        if (mDeviceDataStorage.isAlertConfigDirty()) {
//            retList.add(writeAlertConfig(mDeviceDataStorage.getAlertConfigBitmasks(false)));
//        }
//
//        final int topTrigger = getTrigger(WatchConstants.SLOT_TOP);
//        final int bottomTrigger = getTrigger(WatchConstants.SLOT_BOTTOM);
//        retList.add(mWatchDevice.writeTriggers(topTrigger, bottomTrigger));
//
//        if (mDeviceDataStorage.isRemoteConfigDirty()) {
//            retList.add(writeConfigSettings());
//            retList.add(writeVibratorConfig());
//        }

        return retList;
    }

    private int getTrigger(final int slot) {
        if (slot != WatchConstants.SLOT_BOTTOM && slot != WatchConstants.SLOT_TOP) {
            return WatchConstants.TRIGGER_NONE;
        }

        final String type = "music";//mStorage.getBehaviourTypeForSlot(slot);
        if (type.equals("music"/*Music.TYPE*/)) {
            return WatchConstants.TRIGGER_MEDIACTRL;
        } else {
            return WatchConstants.TRIGGER_NONE;
        }
    }

    private void onDeviceRemovedOrReset() {
        QRomLog.i(TAG, "Device was removed or reset. Clearing caches and marking device data as dirty.");
        mDeviceDataStorage.setDirty();
        mDeviceDataStorage.setAllDirty();
        mWatchDevice.invalidateCache();
    }


    private void writeBluetoothConfig() {
//        if (mWatchDevice != null) {
//            final AppBluetoothConfig config = mRemoteConfigController.getAppBluetoothConfig();
//
//            if (config != null) {
//                mWatchDevice.setUseRefreshServices(config.useRefreshServices);
//            }
//        }
    }

    public Future<Map<String, String>> getDeviceInformation() {
        if (mWatchDevice == null) {
            return FutureUtils.error(new IllegalStateException("No device"));
        } else {
            return mWatchDevice.readDeviceInformation();
        }
    }


    public boolean hasDevice() {
        return mWatchDevice != null;
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

    private static int getMagicKeyFromAction(int action) {
        switch (action) {
            case DeviceUtils.BUTTON_ACTION_SUPER_LONG_PRESS:
                return WatchConstants.MAGIC_KEY_ONE;
            default:
                return WatchConstants.MAGIC_KEY_NONE;
        }
    }

}
