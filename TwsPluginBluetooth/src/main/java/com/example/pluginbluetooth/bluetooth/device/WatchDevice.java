package com.example.pluginbluetooth.bluetooth.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.bluetooth.device.profile.InputDeviceConnector;
import com.example.pluginbluetooth.bluetooth.device.readwrite.Command;
import com.example.pluginbluetooth.bluetooth.device.readwrite.DeviceWriter;
import com.example.pluginbluetooth.bluetooth.gatt.DeviceListener;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.bluetooth.gatt.ReadCallback;
import com.example.pluginbluetooth.future.AlwaysCallback;
import com.example.pluginbluetooth.future.FailCallback;
import com.example.pluginbluetooth.future.FlatMapCallback;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.FutureUtils;
import com.example.pluginbluetooth.future.MapCallback;
import com.example.pluginbluetooth.future.Promise;
import com.example.pluginbluetooth.future.SuccessCallback;
import com.example.pluginbluetooth.utils.ByteUtils;
import com.example.pluginbluetooth.utils.DeviceUtils;
import com.example.pluginbluetooth.utils.MathsUtils;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ImmutableIntegerValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.msgpack.value.ValueType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2018/3/1.
 */

public class WatchDevice implements WatchDeviceInterface {

    public static final int DFU_ERROR_LOW_BATTERY = 8899;
    public static final int DFU_ERROR_TOO_COLD = 8900;

    // this constant is defined in a header file of the device sw
    private static final int CONFIG_VIBRATOR_FIRST_VIBRATION = 8;

    private static final String TAG = "rick_Print:WatchDevice";

    private boolean mIsDebugEnabled = BuildConfig.DEBUG; // Set to false to get
    // rid of most debug
    // logging from this
    // class

    private static final long DFU_DELAY_START = 1000;
    private static final long DFU_DELAY_END = 1000;
    private static final long DFU_UNBOND_TIMEOUT = 10000;
    private static final int CRASH_TYPE_HANDLED = 255;
    private static final int MAX_READ_TRIES = 3;
    private static final long TEST_RSSI_DELAY = 2000;
    private static final long TEST_FCTE_DELAY = 2000;
    private static final int MILLIS_PER_MINUTE = 60000;

    private static final String MIN_FW_VERSION = "20160720";
    private static final String FW_DIRTY = "-dirty";

    private final Context mContext;
    private final GattDevice mDevice;
    private final Handler mHandler = new Handler();
    private final Handler mIncomingCallVibrateHandler = new Handler();
    private final CommandCenter mCommandCenter = new CommandCenter();
    private final CacheCenter mCache;
    private final Map<String, Future<String>> mOngoingReads = new HashMap<String, Future<String>>();
    private final DeviceWriter mDeviceWriter;

    private final Set<DeviceConnectionListener> mConnectionListeners = new CopyOnWriteArraySet<DeviceConnectionListener>();
    // private final Set<DeviceEventListener> mEventListeners = new CopyOnWriteArraySet<DeviceEventListener>();
    // private final Set<DeviceDfuListener> mDfuListeners = new CopyOnWriteArraySet<DeviceDfuListener>();
    private FirmwareInfoListener mFirmwareInfoListener;

    private boolean mIsConnected = false; // Do we have a BT connection up and
    // running?
    private boolean mIsReady = false; // Are we ready to run arbitrary commands?
    private boolean mInDfuMode = false;
    private boolean mIsRunningDFU = false;
    private CapabilityCenter mCapabilityCenter;
    private Promise<Void> mDfuPromise;
    private VibrateRunnable mVibrateRunnable = new VibrateRunnable();
    private boolean mIsFetchingPostMortem;
    private int mCurrentConnInt = Integer.MAX_VALUE;

    private final DeviceListener mDeviceListener = new DeviceListener() {
        @Override
        public void onConnected() {
            // We now have a BT connection, but still need to finish our
            // handshaking.
            // Tell listeners that we're "connecting" for now.
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onConnecting();
            }

            // rick_Note:保存第一次配对成功的时间"月/日"
//            String firstPairTime = SharedPreferencesUtils.getString(HostProxy.getApplication(), SharedPreferencesUtils.SP_HEALTH_HAND_NAME, HealthDeviceInfoHandler.SP_FIRST_PAIR_WATCH_TIME, null);
//            if (TextUtils.isEmpty(firstPairTime)) {
//                final Calendar calendar = Calendar.getInstance();
//                // 注意系统Calendar的月份是索引方式存储的，取出来显示用的话须要进行+1操作
//                firstPairTime = calendar.get(Calendar.YEAR) + HealthDataProcessor.SEPARATOR_VALUE
//                        + calendar.get(Calendar.MONTH) + HealthDataProcessor.SEPARATOR_VALUE
//                        + calendar.get(Calendar.DATE);
//                SharedPreferencesUtils.putStringWithApply(HostProxy.getApplication(), SharedPreferencesUtils.SP_HEALTH_HAND_NAME, HealthDeviceInfoHandler.SP_FIRST_PAIR_WATCH_TIME, firstPairTime);
//            }

            mIsConnected = true;
            getCommandMap();
        }

        @Override
        public void onHardToConnect() {
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onHardToConnect();
            }
        }

        @Override
        public void onDisconnected() {
            mIsConnected = false;
            if (mIsReady) {
                mIsReady = false;
                for (DeviceConnectionListener listener : mConnectionListeners) {
                    listener.onDisconnected();
                }
            } else if (mInDfuMode) {
                mInDfuMode = false;
                for (DeviceConnectionListener listener : mConnectionListeners) {
                    listener.onLeaveDfuMode();
                }
            } else {
                // We were connecting and failed, so send onDisconnected() to
                // listeners so they know
                for (DeviceConnectionListener listener : mConnectionListeners) {
                    listener.onDisconnected();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(final UUID service, final UUID characteristic, final byte[] data) {
            try {
                final Value value = unpackReceivedValue(data);

                final Map<String, Value> resultMap = mCommandCenter.translate(value);
                QRomLog.i(TAG, "notification: " + resultMap);

                for (Map.Entry<String, Value> entry : resultMap.entrySet()) {
                    if (entry.getKey().equals("alarm")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onAlarm(entry.getValue().asIntegerValue().asInt());
//                        }
                    } else if (entry.getKey().equals("button")) {
                        final ArrayValue values = entry.getValue().asArrayValue();
                        final int index = values.get(0).asIntegerValue().asInt();
                        final int action = values.get(1).asIntegerValue().asInt();
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onDeviceButtonClicked(index, action);
//                        }
                    } else if (entry.getKey().equals("steps_now")) {
                        final ArrayValue array = entry.getValue().asArrayValue();
                        if (array.size() == 2) {
                            final int steps = array.get(0).asIntegerValue().asInt();
                            final int day = array.get(1).asIntegerValue().asInt();
                            final Calendar calendar = Calendar.getInstance();
                            final int dayOnPhone = calendar.get(Calendar.DAY_OF_MONTH);
                            if (day == dayOnPhone) {
//                                for (DeviceEventListener listener : mEventListeners) {
//                                    listener.onStepsNow(steps, day);
//                                }
                            } else {
                                QRomLog.i(TAG, "Steps notification with the wrong day: " + day + " instead of "
                                        + dayOnPhone);
                            }
                        }
                    } else if (entry.getKey().equals("crash")) {
                        handleDeviceCrash(entry.getValue());
                    } else if (entry.getKey().equals("error")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onDeviceError(entry.getValue().asIntegerValue().asInt());
//                        }
                    } else if (entry.getKey().equals("call")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onPressDuringCall(entry.getValue().asIntegerValue().asInt());
//                        }
                    } else if (entry.getKey().equals("postmortem")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onDevicePostMortem();
//                        }
                    } else if (entry.getKey().equals("stillness")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onStillnessEvent(entry.getValue().asIntegerValue().asInt());
//                        }
                    } else if (entry.getKey().equals("debug_rssi")) {
//                        for (DeviceEventListener listener : mEventListeners) {
//                            listener.onRssiEvent(entry.getValue().asIntegerValue().asInt());
//                        }
                    } else if (entry.getKey().equals("conn_int_change")) {
                        final ArrayValue array = entry.getValue().asArrayValue();
                        if (array.size() == 3) {
                            final int currentConnInt = array.get(0).asIntegerValue().asInt();
                            mCurrentConnInt = currentConnInt;
                            final int slaveLatency = array.get(1).asIntegerValue().asInt();
                            final int timeout = array.get(2).asIntegerValue().asInt();
//                            for (DeviceEventListener listener : mEventListeners) {
//                                listener.onConnIntChange(currentConnInt, slaveLatency, timeout);
//                            }
                        }
                    } else if (entry.getKey().equals("diag_event")) {
                        handlDeviceDiagEvent(entry.getValue());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to parse notification data: " + e);
            }
        }

        @Override
        public void onBonded() {
            QRomLog.i(TAG, "Bonded");
//            for (DeviceEventListener listener : mEventListeners) {
//                listener.onBonded();
//            }
        }

        @Override
        public void onConnectionStateChange(int newState, int status) {
//            for (DeviceEventListener listener : mEventListeners) {
//                listener.onConnectionStateChange(newState, status);
//            }
        }
    };

    private void enterDfuMode() {
        if (mIsConnected) {
            QRomLog.i(TAG, "Entering DFU mode");
            mInDfuMode = true;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onEnterDfuMode();
            }
        }
    }

    private void handleDeviceCrash(final Value crash) {
        if (mIsFetchingPostMortem) {
            return;
        }
        mIsFetchingPostMortem = true;
        final int hw_reason = crash.asArrayValue().get(1).asIntegerValue().asInt();

        fetchDefinition("map_error").flatMap(new FlatMapCallback<Void, Value>() {

            @Override
            public Future<Value> onResult(final Void result) throws Exception {
                return read("status_crash");
            }
        }).flatMap(new FlatMapCallback<Value, Void>() {
            @Override
            public Future<Void> onResult(final Value result) throws Exception {
                final Map<String, Value> commandMap = mCommandCenter.translate(result);
                return readPostMortemData().flatMap(new FlatMapCallback<byte[], Void>() {
                    @Override
                    public Future<Void> onResult(final byte[] result) throws Exception {
                        Map<String, Value> values = null;
                        Set<String> keys = commandMap.keySet();
                        String type = null;
                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                type = key;
                                values = mCommandCenter.translate(commandMap.get(key));
                                break;
                            }
                        }
                        if (values == null) {
                            values = Collections.emptyMap();
                        }

                        notifyDeviceCrash(type, hw_reason, values, result);
                        return FutureUtils.just(null);
                    }
                }).success(new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        write("crash", ValueFactory.newInteger(CRASH_TYPE_HANDLED));
                    }
                });
            }
        }).always(new AlwaysCallback() {
            @Override
            public void onFinished() {
                mIsFetchingPostMortem = false;
            }
        });
    }

    private void handlDeviceDiagEvent(final Value diag) {
        fetchDefinition("map_diag_event").flatMap(new FlatMapCallback<Void, Void>() {
            @Override
            public Future<Void> onResult(Void result) throws Exception {
                final Map<String, Value> commandMap = mCommandCenter.translate(diag);
                final Map<String, String> map = mapValue2String(commandMap);

//                for (DeviceEventListener listener : mEventListeners) {
//                    listener.onDiagEvent(map);
//                }

                return FutureUtils.just(null);
            }
        });
    }

    private void notifyDeviceCrash(final String type, final int hwReason, final Map<String, Value> statusCrash,
                                   final byte[] crashStack) {
//        CrashStatus crashStatus = CrashStatusParser.parseCrash(type, hwReason, statusCrash);
//
//        for (DeviceEventListener listener : mEventListeners) {
//            listener.onDeviceCrash(crashStatus, crashStack);
//        }
    }

    private static Date createPastDate(final int day, final int hour, final int minute) {
        final Calendar result = Calendar.getInstance();

        result.set(Calendar.DAY_OF_MONTH, day);
        result.set(Calendar.HOUR_OF_DAY, hour);
        result.set(Calendar.MINUTE, minute);

        // We know that the date should be in the past, so if it's not then the
        // month is wrong
        if (result.after(new Date())) {
            result.add(Calendar.MONTH, -1);
        }

        return result.getTime();
    }

    public WatchDevice(final GattDevice device, final CacheCenter cache) {
        mContext = device.getContext();
        mCache = cache;

        mDevice = device;
        mDevice.registerListener(mDeviceListener);
        mDevice.connect();

        mDeviceWriter = new DeviceWriter(mDevice, mCommandCenter);
    }

    private Future<String> readDeviceInformation(final UUID characteristicUUID) {
        return readDeviceInformation(characteristicUUID, true);
    }

    private Future<String> readDeviceInformation(final UUID characteristicUUID, final boolean useCache) {
        QRomLog.i(TAG, "call readDeviceInformation(" + characteristicUUID + ", " + useCache + ")");
        final String cacheKey = characteristicUUID.toString();
        final Promise<String> promise = new Promise<String>();

        final String cachedResult;
        if (useCache) {
            cachedResult = mCache.getString(cacheKey);
        } else {
            cachedResult = null;
        }

        if (isInDfuMode()) {
            QRomLog.i(TAG, "readDeviceInformation: in DFU mode without cache");
            promise.resolve("");
        } else if (cachedResult != null) {
            QRomLog.i(TAG, "readDeviceInformation: with cache");
            promise.resolve(cachedResult);
        } else {
            QRomLog.i(TAG, "readDeviceInformation: no cache, reading from device");
            if (ongoingRequests(useCache, cacheKey, promise)) {
                return mOngoingReads.get(cacheKey);
            }

            mDevice.read(UUIDStorage.DEVICE_INFO_SERVICE, characteristicUUID, new ReadCallback() {
                @Override
                public boolean onSuccess(final byte[] result) {
                    try {
                        final String deviceInformation = new String(result, "UTF-8");
                        mCache.put(cacheKey, deviceInformation);
                        promise.resolve(deviceInformation);
                    } catch (IOException e) {
                        promise.reject(e);
                    }
                    return false; // don't retry the read
                }

                @Override
                public void onError(final Throwable error) {
                    QRomLog.i(TAG, "readDeviceInformation: no cache, mDevice.read::onError");
                    promise.reject(error);
                }
            });
        }

        return promise.getFuture();
    }

    private boolean ongoingRequests(final boolean useCache, final String cacheKey, final Promise<String> promise) {
        if (useCache) {
            if (mOngoingReads.containsKey(cacheKey)) {
                if (mIsDebugEnabled) {
                    QRomLog.i(TAG, "Reusing already ongoing request");
                }
                return true;
            } else {
                mOngoingReads.put(cacheKey, promise.getFuture());
                promise.getFuture().always(new AlwaysCallback() {
                    @Override
                    public void onFinished() {
                        mOngoingReads.remove(cacheKey); // No longer ongoing!
                        // The value will still
                        // be cached.
                    }
                });
            }
        }
        return false;
    }

    private String getCachedDeviceInformation(final UUID characteristicUUID) {
        final String cacheKey = characteristicUUID.toString();
        return mCache.getString(cacheKey);
    }

    private void getCommandMap() {
        readFirmwareVersionUncached().flatMap(new FlatMapCallback<String, Void>() {
            @Override
            public Future<Void> onResult(final String firmwareVersion) throws Exception {
                if (!isCompatibleFwVersion(firmwareVersion)) {
                    throw new RuntimeException("Too old FW version: " + firmwareVersion);
                } else if (mIsDebugEnabled) {
                    QRomLog.i(TAG, "Compatible FW version: " + firmwareVersion);
                }
                if (isFwDirty(firmwareVersion)) {
                    invalidateCache();
                    if (mFirmwareInfoListener != null) {
                        mFirmwareInfoListener.onFirmwareDirty();
                    }
                }
                return fetchDefinition("map_cmd");
            }
        }).success(new SuccessCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                onCommandMapReady();
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(final Throwable error) {
                QRomLog.i(TAG, "Failed to get command map:" + error);
                if (mDevice.isConnected() && !mDevice.isDisconnecting()) {
                    if (mDevice.hasGattService(UUIDStorage.ANIMA_SERVICE)) {
                        QRomLog.i(TAG, "has UUIDStorage.ANIMA_SERVICE, getCommandMap again");
                        getCommandMap(); // Try again
                    } else if (guessIfDeviceIsInDfu(mDevice)) {
                        QRomLog.i(TAG, "seems like deviceIsInDFU mode, enterDFUMode");
                        enterDfuMode();
                    } else {
                        QRomLog.i(TAG, "Couldn't get command map and don't know the state of the watch.");
                        mDevice.refreshConnection();
                    }
                } else {
                    QRomLog.i(TAG, "Couldn't get command map since we got disconnected...");
                    // In this case, we will wait for an automatic re-connect
                    // and try again.
                }
            }
        });
    }

    /*
     * Use with care and as a backup when not knowing the state of the device.
     * Guessing if device is in DFU mode in this way should only be the case
     * when a DFU has been aborted (BT off, out-of-range, turning of phone etc).
     * All other DFUs (normal DFU) is initiated by application and in a
     * controlled manner.
     */
    private boolean guessIfDeviceIsInDfu(final GattDevice device) {
        final List<UUID> services = device.getGattServices();

		/*
         * These 3 entries are the only ones that shall be visible for dfu mode.
		 * All 3 are also available in "normal" mode with additional service
		 * like ANIMA_SERVICE.
		 */
        if (services.size() == 3) {
            if (services.contains(UUIDStorage.GENERIC_ACCESS_SERVICE)
                    && services.contains(UUIDStorage.GENERIC_ATTRIBUTE_SERVICE)
                    && services.contains(UUIDStorage.DEVICE_FIRMWARE_UPDATE)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCompatibleFwVersion(final String firmwareVersion) {
        return MIN_FW_VERSION.compareTo(firmwareVersion) <= 0;
    }

    private boolean isFwDirty(final String firmwareVersion) {
        return firmwareVersion != null && firmwareVersion.endsWith(FW_DIRTY);
    }

    private Future<String> readFirmwareVersionUncached() {
        final UUID charFwVersion = UUIDStorage.DEVICE_INFO_FWR_REVISION;
        if (mIsDebugEnabled) {
            QRomLog.i(TAG, "Reading the firmware version...");
        }
        return readDeviceInformation(charFwVersion, false);
    }

    private void onCommandMapReady() {
        QRomLog.i(TAG, "Command map: " + mCommandCenter.getCommandMap().toString());
        mDevice.setNotification(UUIDStorage.ANIMA_SERVICE, UUIDStorage.NOTIFICATION_CHAR);
        mIsReady = true;
        initCapabilities();

        connectInputDeviceIfNeeded();

        // Notify listeners that we are connected
        for (DeviceConnectionListener listener : mConnectionListeners) {
            listener.onConnected();
        }
    }

    private Future<Value> readCapability() {
        return read("cap").map(new MapCallback<Value, Value>() {
            @Override
            public Value onResult(final Value result) {
                mCache.put("cap", result);
                return result;
            }
        });
    }

    public CapabilityCenter getCapabilityCenterSync() {
        getCapabilityCenter(); // reads from cache if possible
        return mCapabilityCenter;
    }

    public Future<CapabilityCenter> getCapabilityCenter() {
        final Promise<CapabilityCenter> promise = new Promise<CapabilityCenter>();

        if (mCommandCenter.hasCommand("cap")) {
            if (mCapabilityCenter != null) {
                promise.resolve(mCapabilityCenter);
            } else {
                Value cachedCapValue = mCache.getValue("cap");
                if (cachedCapValue != null) {
                    mCapabilityCenter = new CapabilityCenter(cachedCapValue);
                    promise.resolve(mCapabilityCenter);
                } else {
                    readCapability().success(new SuccessCallback<Value>() {
                        @Override
                        public void onSuccess(final Value value) {
                            mCapabilityCenter = new CapabilityCenter(value);
                            promise.resolve(mCapabilityCenter);
                        }
                    }).fail(new FailCallback() {
                        @Override
                        public void onFail(Throwable error) {
                            promise.resolve(null);
                        }
                    });
                }
            }
        } else {
            promise.resolve(null);
        }

        return promise.getFuture();
    }

    private void initCapabilities() {
        if (mCapabilityCenter == null && mCommandCenter.hasCommand("cap")) {
            Value cachedCapValue = mCache.getValue("cap");
            if (cachedCapValue != null) {
                mCapabilityCenter = new CapabilityCenter(cachedCapValue);
            }
        }
    }

    private Future<Void> fetchDefinition(final String definition) {
        // First check if it's already known
        if (mCommandCenter.isMapKnown(definition)) {
            return FutureUtils.just(null);
        }
        // Else fetch all its pages and add it to CommandCenter
        return readPages(definition).map(new MapCallback<Value, Void>() {
            @Override
            public Void onResult(final Value result) {
                mCommandCenter.parseMap(definition, result);
                return null;
            }
        });
    }

    /**
     * Reads all pages for a certain command with caching
     * <p/>
     * Caches the values of all the pages together and returns values from the
     * cache if available. Creates a msgpack array that contains each individual
     * page and returns the msgpack array as a Value object.
     * <p/>
     * This only works for certain commands that support paging (using a single
     * integer parameter for the page index).
     */
    private Future<Value> readPages(final String command) {
        // Available in the cache?
        final Value cachedValue = mCache.getValue(command);
        if (cachedValue != null) {
            return FutureUtils.just(cachedValue);
        }
        // Else, fetch each page until we get an empty result
        final List<Value> pages = new ArrayList<Value>();
        return readAsArray(command, 0).flatMap(new FlatMapCallback<Value, Value>() {
            @Override
            public Future<Value> onResult(final Value result) throws Exception {
                if (result.isNilValue()) {
                    return FutureUtils.just((Value) ValueFactory.newArray(pages));
                }
                pages.add(result);
                return readAsArray(command, pages.size()).flatMap(this); // Recurse
            }
        }).success(new SuccessCallback<Value>() {
            @Override
            public void onSuccess(final Value result) {
                mCache.put(command, result); // Put it in the cache before
                // returning it
            }
        });
    }

    /**
     * Read command without any parameters
     */
    private Future<Value> read(final String command) {
        QRomLog.i(TAG, "read: " + command);

        if (mInDfuMode) {
            return FutureUtils.error(new IllegalStateException("Can't read '" + command + "' in DFU mode"));
        }

        final int commandNumber = mCommandCenter.getCommandNumber(command);
        if (commandNumber < 0) {
            return FutureUtils.error(new IOException("No such command found: " + command));
        }

        final Future<Void> writeFuture = writeEmpty(command);
        final Future<Value> readFuture = read();

        final Future<Value> future = writeFuture.flatMap(new FlatMapCallback<Void, Value>() {
            @Override
            public Future<Value> onResult(final Void result) {
                return readFuture;
            }
        });

        return decodeReturnMap(command, future);
    }

    private Future<Value> read(final String command, final Value param) {
        final Future<Void> writeFuture = write(command, param);
        final Future<Value> readFuture = read(command);

        return writeFuture.flatMap(new FlatMapCallback<Void, Value>() {
            @Override
            public Future<Value> onResult(final Void result) {
                return readFuture;
            }
        });
    }

    /**
     * Read command with a parameter
     */
    private Future<Value> readAsArray(final String command, final int param) {
        if (mInDfuMode) {
            return FutureUtils.error(new IllegalStateException("Can't read '" + command + "' in DFU mode"));
        }

        final int commandNumber = mCommandCenter.getCommandNumber(command);
        if (commandNumber < 0) {
            return FutureUtils.error(new IOException("No such command found: " + command));
        }

        if (mIsDebugEnabled) {
            QRomLog.i(TAG, "read: " + command + " param: " + param);
        }

        final Future<Void> writeFuture = writeAsList(command, param);
        final Future<Value> readFuture = read();

        final Future<Value> future = writeFuture.flatMap(new FlatMapCallback<Void, Value>() {
            @Override
            public Future<Value> onResult(final Void result) {
                return readFuture;
            }
        });

        return decodeReturnMap(command, future);
    }

    private Future<Value> read() {
        final Promise<Value> promise = new Promise<Value>();

        mDevice.read(UUIDStorage.ANIMA_SERVICE, UUIDStorage.ANIMA_CHAR, new ReadCallback() {

            private int mTriesLeft = MAX_READ_TRIES;

            @Override
            public boolean onSuccess(final byte[] result) {
                final Value value;
                try {
                    value = unpackReceivedValue(result);
                    if (mIsDebugEnabled) {
                        QRomLog.i(TAG, "Read " + value);
                    }
                    if (!value.isMapValue() && !value.isNilValue()) {
                        throw new IOException("Unexpected value");
                    }
                } catch (IOException e) {
                    if (mIsDebugEnabled) {
                        QRomLog.i(TAG, "Failed to parse: " + ByteUtils.bytesToHex(result), e);
                    }
                    mTriesLeft--;
                    if (mTriesLeft > 0) {
                        if (mIsDebugEnabled) {
                            QRomLog.i(TAG, "Retrying read");
                        }
                        return true; // retry the read
                    } else {
                        if (mIsDebugEnabled) {
                            QRomLog.i(TAG, "Not retrying read");
                        }
                        promise.reject(e);
                    }
                    return false;
                } catch (MessagePackException e) {
                    if (mIsDebugEnabled) {
                        QRomLog.i(TAG, "Failed to parse: " + ByteUtils.bytesToHex(result), e);
                    }
                    mTriesLeft--;
                    if (mTriesLeft > 0) {
                        if (mIsDebugEnabled) {
                            QRomLog.i(TAG, "Retrying read");
                        }
                        return true; // retry the read
                    } else {
                        if (mIsDebugEnabled) {
                            QRomLog.i(TAG, "Not retrying read");
                        }
                        promise.reject(e);
                    }
                    return false;
                }
                promise.resolve(value);
                return false; // don't retry the read
            }

            @Override
            public void onError(final Throwable error) {
                promise.reject(error);
            }
        });

        return promise.getFuture();
    }

    private Value unpackReceivedValue(final byte[] result) throws IOException {
        if (result != null && result.length == 0) {
            return ValueFactory.newNil(); // Special case (the device sends
            // empty results as no bytes)
        }
        final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(result);
        return unpacker.unpackValue();
    }

    private Future<Value> decodeReturnMap(final String command, final Future<Value> future) {
        return future.map(new MapCallback<Value, Value>() {
            @Override
            public Value onResult(final Value result) throws IOException {
                // Return nil values as is
                if (result.isNilValue()) {
                    return result;
                }
                // Otherwise, it's wrapped in a map and we remove the outer map
                final Map<String, Value> resultMap = mCommandCenter.translate(result);
                final String resultCommand = getOnlyKey(resultMap);
                if (!command.equals(resultCommand)) {
                    Log.e(TAG, "Got unexpected reply " + resultCommand + " when reading " + command);
                    throw new RuntimeException("Got unexpected reply " + resultCommand + " when reading " + command);
                }
                return resultMap.get(resultCommand);
            }
        });
    }

    private static <T> T getOnlyKey(final Map<T, Value> resultMap) {
        if (resultMap.size() != 1) {
            throw new InvalidParameterException("Not exactly one key!");
        }
        return resultMap.keySet().iterator().next();
    }

    /**
     * Read a map with automatic translation of integer keys to strings
     *
     * @param command    the data command, e.g. "status_diag"
     * @param definition the definition of the mapping for the keys, e.g. "map_diag"
     */
    private Future<Map<String, Value>> readStringMap(final String command, final String definition) {
        QRomLog.i(TAG, "readStringMap: " + command + "(" + definition + ")");
        return fetchDefinition(definition).flatMap(new FlatMapCallback<Void, Value>() {
            @Override
            public Future<Value> onResult(final Void result) {
                return read(command);
            }
        }).map(new MapCallback<Value, Map<String, Value>>() {
            @Override
            public Map<String, Value> onResult(final Value result) {
                final Map<String, Value> map = mCommandCenter.translate(result);
                QRomLog.i(TAG, "readStringMap: " + command + " => " + map);
                return map;
            }
        });
    }

    public void invalidateCache() {
        mCapabilityCenter = null;
        mCache.invalidate();
    }

    private Future<Integer> readDfuReady() {
        return read("dfu_ready").map(new MapCallback<Value, Integer>() {
            @Override
            public Integer onResult(final Value result) {
                return result.asIntegerValue().asInt();
            }
        });
    }

    public void close() {
        // Tell listeners that we're disconnected (if we told them we were
        // connected)
        if (mIsReady) {
            mIsReady = false;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onDisconnected();
            }
        }

        if (mInDfuMode) {
            mInDfuMode = false;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onLeaveDfuMode();
            }
        }

        // Clean up and disconnect
        stopVibrateForIncomingCall();
        mDevice.unregisterListener(mDeviceListener);
        cancelDfu();
        mDevice.disconnect();
    }

    public void registerConnectionListener(DeviceConnectionListener listener) {
        mConnectionListeners.add(listener);
    }

    public void unregisterConnectionListener(DeviceConnectionListener listener) {
        mConnectionListeners.remove(listener);
    }

//    public void registerEventListener(DeviceEventListener listener) {
//        mEventListeners.add(listener);
//    }
//
//    public void unregisterEventListener(DeviceEventListener listener) {
//        mEventListeners.remove(listener);
//    }
//
//    public void registerDfuListener(DeviceDfuListener listener) {
//        mDfuListeners.add(listener);
//    }
//
//    public void unregisterDfuListener(DeviceDfuListener listener) {
//        mDfuListeners.remove(listener);
//    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public int getType() {
        return mDevice.getType();
    }

    public int getItemId() {
        return mDevice.getItemId();
    }

    public boolean isConnected() {
        return mIsReady;
    }

    public boolean isHardToConnect() {
        return mDevice.isHardToConnect();
    }

    public boolean isHighProbabilityForDFUSuccess() {
        return mDevice.isHighProbabilityForDFUSuccess();
    }

    public boolean isInDfuMode() {
        return mInDfuMode;
    }

    public boolean isRunningDfu() {
        return mIsRunningDFU;
    }

    public boolean isConnecting() {
        // We're connecting if we have a BT connection and aren't ready (i.e.
        // "connected") or in DFU mode yet
        return mIsConnected && !mIsReady && !mInDfuMode;
    }

    public Future<Void> startDfu(final Uri firmware) {
        if (firmware == null) {
            return FutureUtils.error(new InvalidParameterException("No update file available. Internet?"));
        }

        return checkIsDfuReady().flatMap(new FlatMapCallback<Void, Void>() {
            @Override
            public Future<Void> onResult(final Void result) throws Exception {
                if (mIsRunningDFU) {
                    throw new IllegalStateException("Already running a DFU");
                }

                mDfuPromise = new Promise<Void>();

                // Prepare for DFU
                mIsRunningDFU = true;
                mDevice.disconnect();

                // Register listener
//				DfuServiceListenerHelper.registerProgressListener(mContext, mDfuProgressListener);

                // Wait until the device is disconnected and then start the DFU
                startDfuWhenDisconnected(firmware, mDevice);
                return mDfuPromise.getFuture();
            }
        });
    }

    public Future<Void> startDfu(final String uriString) {
        File dir = Environment.getExternalStorageDirectory();
        return startDfu(Uri.fromFile(new File(dir, uriString)));
    }

    private void startDfuWhenDisconnected(final Uri firmware, final GattDevice device) {
        if (!device.isConnected()) {
            QRomLog.i(TAG, "No need to wait for a disconnect");
            callDfuLibraryDelayed(firmware, device);
        } else {
            QRomLog.i(TAG, "Waiting for device to disconnect before starting DFU...");
            device.registerListener(new DeviceListener() {
                @Override
                public void onConnected() {
                }

                @Override
                public void onDisconnected() {
                    QRomLog.i(TAG, "The device has been disconnected");
                    device.unregisterListener(this);
                    callDfuLibraryDelayed(firmware, device);
                }

                @Override
                public void onHardToConnect() {

                }

                @Override
                public void onCharacteristicChanged(final UUID service, final UUID characteristic, final byte[] data) {
                }

                @Override
                public void onBonded() {
                }

                @Override
                public void onConnectionStateChange(int newState, int status) {
                }
            });
        }
    }

    /**
     * Call the DFU library after removing bonding and waiting a little
     * <p/>
     * The reason for this is to increase reliability. Some Android phones don't
     * work reliably without some time to clean up after previous activities and
     * bonding triggers various bugs on some phones too.
     */
    private void callDfuLibraryDelayed(final Uri firmware, final GattDevice device) {
        removeBond().delay(DFU_DELAY_START).success(new SuccessCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                doCallDfuLibrary(device, firmware);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(final Throwable error) {
                // Treat unbonding errors as fatal (instead of taking our
                // chances and failing later)
                onDfuError(new RuntimeException("Failed to remove Bluetooth bond", error));
            }
        });
    }

    private void doCallDfuLibrary(final GattDevice device, final Uri firmware) {
        QRomLog.i(TAG, "Starting DFU");
//        new DfuServiceInitiator(device.getAddress()).setZip(firmware)
//                .setDeviceName(mContext.getString(R.string.dfu_device_name)).start(mContext, DfuService.class);
    }

    /**
     * Try to remove bond and return a Future that is resolved when the bond is
     * gone
     * <p/>
     * This works by requesting that Android removes the bond and then waiting
     * for it to disappear. It can fail either because the call to Android fails
     * or since we time out while waiting for the bond to disappear.
     */
    public Future<Void> removeBond() {
        try {
            QRomLog.i(TAG, "Removing bonding for " + getAddress());
            mDevice.removeBond();
        } catch (Exception e) {
            QRomLog.i(TAG, "Removing bond failed", e);
            return FutureUtils.error(e);
        }
        return mDevice.waitForUnbonded(DFU_UNBOND_TIMEOUT);
    }

//    public boolean refreshServices() {
//        return mDevice.refreshServices();
//    }

    public void cancelDfu() {
        if (mDfuPromise != null) {
            mDfuPromise.reject(new RuntimeException("Operation canceled"));
        }
        finishDfu();
    }

    public void setFirmwareInfoListener(FirmwareInfoListener firmwareInfoListener) {
        mFirmwareInfoListener = firmwareInfoListener;
    }

    private void finishDfu() {
        if (mIsRunningDFU) {
            mDfuPromise = null;
            mIsRunningDFU = false;
//			DfuServiceListenerHelper.unregisterProgressListener(mContext, mDfuProgressListener);
            mDevice.connect();
        }
    }

    private void onDfuSuccess() {
        QRomLog.i(TAG, "DFU successful. Clearing all caches.");
        mCommandCenter.clear();
        String itemNumber = mCache.getString(UUIDStorage.DEVICE_INFO_MODEL_NUMBER.toString());
        mCapabilityCenter = null;
        mCache.invalidate();
        if (itemNumber != null) {
            mCache.put(UUIDStorage.DEVICE_INFO_MODEL_NUMBER.toString(), itemNumber);
        }
        if (mDfuPromise != null) {
            mDfuPromise.resolve(null);
        }
        finishDfu();
    }

    private void onDfuError(final Throwable error) {
        if (mDfuPromise != null) {
            mDfuPromise.reject(error);
        }
        finishDfu();
    }

    private void onDfuProgress(final int percent, final float speed) {
//        for (DeviceDfuListener listener : mDfuListeners) {
//            listener.onDfuProgress(percent, speed);
//        }
    }

    public static Map<String, String> mapValue2String(final Map<String, Value> valueMap) {
        final Map<String, String> result = new LinkedHashMap<String, String>();

        for (Map.Entry<String, Value> entry : valueMap.entrySet()) {
            final Value value = entry.getValue();
            String valueString;

            if (value.getValueType() == ValueType.BINARY) {
                valueString = ByteUtils.bytesToHex(value.asBinaryValue().asByteArray());
            } else {
                valueString = value.toString();
            }

            result.put(entry.getKey(), valueString);
        }

        return result;
    }

    private void startVibrateForIncomingCall(final int alert) {
        if (mCapabilityCenter == null || !mCapabilityCenter.hasCallRepeatsAlert()) {
            mVibrateRunnable.reset(alert);
            mIncomingCallVibrateHandler.removeCallbacks(mVibrateRunnable);
            mIncomingCallVibrateHandler.post(mVibrateRunnable);
        }
    }

    private void stopVibrateForIncomingCall() {
        mIncomingCallVibrateHandler.removeCallbacks(mVibrateRunnable);
    }

    /**
     * Convert Calendar.DAY_OF_WEEK to the device day of week 0 => Mon, .., 6 =>
     * Sun
     */
    private int getDeviceDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            case Calendar.SATURDAY:
                return 5;
            case Calendar.SUNDAY:
                return 6;
            default:
                throw new InvalidParameterException("Not a valid day of week");
        }
    }

    private int deviceToAppMonth(final Integer deviceMonth) {
        return deviceMonth - 1;
    }

    private class VibrateRunnable implements Runnable {

        private static final int TIME_BETWEEN_ALERTS_MS = 3000;
        private static final int MAX_TOTAL_ALERT_TIME_MS = 90000;
        private static final int MAX_NBR_OF_ALERTS = MAX_TOTAL_ALERT_TIME_MS / TIME_BETWEEN_ALERTS_MS;

        private int mAlert;
        private int mNbrOfAlerts;

        public VibrateRunnable() {
            mAlert = 1;
        }

        public void reset(int alert) {
            mAlert = alert;
            mNbrOfAlerts = 0;
        }

        @Override
        public void run() {
            writeAlert(mAlert);
            mNbrOfAlerts++;
            if (mNbrOfAlerts < MAX_NBR_OF_ALERTS) {
                mIncomingCallVibrateHandler.postDelayed(this, TIME_BETWEEN_ALERTS_MS);
            }
        }
    }

    /* Write commands wrapping interaction with DeviceWriter. */
    private Future<Void> write(final String command, final Value value) {
        QRomLog.i(TAG, "write(" + command + "," + value + ")");
        try {
            final Command cmd = mDeviceWriter.createCommand(command, value, new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    private Future<Void> write(final String command) {
        QRomLog.i(TAG, "write(" + command + ")");
        try {
            final Command cmd = mDeviceWriter.createCommand(command, ValueFactory.newInteger(0), new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    private Future<Void> writeAsList(final String command, final int value) {
        QRomLog.i(TAG, "writeAsList(" + command + "," + value + ")");
        try {
            final Value intValue = ValueFactory.newInteger(value);
            final List<Value> listValue = Collections.singletonList(intValue);
            final Command cmd = mDeviceWriter.createCommand(command, listValue, new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    private Future<Void> writeEmpty(final String command) {
        QRomLog.i(TAG, "writeEmpty(" + command + ")");
        try {
            final Command cmd = mDeviceWriter.createCommand(command, (Value) null, new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    /*******************************
     * WatchDeviceInterface
     *******************************/
    @Override
    public Future<Void> checkIsDfuReady() {
        if (mInDfuMode) {
            return FutureUtils.just(null); // It's not possible to check for DFU
            // readiness in DFU mode.
        } else {
            return readDfuReady().catchError(Throwable.class, new MapCallback<Throwable, Integer>() {
                @Override
                public Integer onResult(final Throwable error) throws IOException {
                    QRomLog.i(TAG, "Couldn't run dfu_ready command. Assuming that device is ready.", error);
                    return 0;
                }
            }).map(new MapCallback<Integer, Void>() {
                @Override
                public Void onResult(final Integer result) throws IOException {
                    switch (result) {
                        case 0:
                            return null;
                        case 1:
                            throw new RuntimeException("Battery too low for DFU. Code: " + DFU_ERROR_LOW_BATTERY);
                        case 2:
                            throw new RuntimeException("Temperature too cold for DFU. Code: " + DFU_ERROR_TOO_COLD);
                        default:
                            throw new RuntimeException("The device isn't ready for a DFU");
                    }
                }
            });
        }
    }

    @Override
    public void setDebugMode(final boolean enable) {
        mIsDebugEnabled = enable;

        mDeviceWriter.setDebugMode(enable);
//        mDevice.setDebugMode(enable);
    }

    @Override
    public void setUseRefreshServices(boolean enable) {
        if (mDevice != null) {
//            mDevice.setUseRefreshService(enable);
        }
    }

    @Override
    public boolean getDebugMode() {
        return mIsDebugEnabled;
    }

    @Override
    public boolean isBonded() {
        return mDevice.isBonded();
    }

    @Override
    public Future<Void> writeAlarms(final List<DeviceAlarm> alarms) {
        final List<Value> encodedAlarms = new ArrayList<Value>(alarms.size());
        for (DeviceAlarm alarm : alarms) {
            final Value data = ValueFactory.newArray(ValueFactory.newInteger(alarm.getHours()),
                    ValueFactory.newInteger(alarm.getMinutes()), ValueFactory.newInteger(alarm.getDaysBitSet()));
            encodedAlarms.add(data);
        }

        return write("alarm", ValueFactory.newArray(encodedAlarms));
    }

    @Override
    public Future<Void> writeAlert(final int alert) {
        return write("alert", ValueFactory.newInteger(alert));
    }

    @Override
    public Future<Void> writeAlertConfig(final int[] alertConfigBitmasks) {
        if (alertConfigBitmasks == null || alertConfigBitmasks.length != 3) {
            return FutureUtils.error(new IllegalArgumentException("alertConfigBitmasks must have length == 3"));
        }

        final Promise<Void> promise = new Promise<Void>();
        final String COMMAND = "alert_assign";
        write(
                COMMAND,
                ValueFactory.newArray(ValueFactory.newInteger(alertConfigBitmasks[0]),
                        ValueFactory.newInteger(alertConfigBitmasks[1]),
                        ValueFactory.newInteger(alertConfigBitmasks[2]))).success(new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                promise.resolve(result);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Throwable error) {
                QRomLog.i(TAG, "write \"" + COMMAND + "\" failed, check that FW >= 20170124.01");
                promise.resolve(null);
            }
        });

        return promise.getFuture();
    }

    @Override
    public Future<Void> writeBaseConfig(final int timeResolutionMinutes, final int enableStepcounter) {
        return write(
                "config_base",
                ValueFactory.newArray(ValueFactory.newInteger(timeResolutionMinutes),
                        ValueFactory.newInteger(enableStepcounter)));
    }

    @Override
    public Future<Void> writeBaseConfig(final int timeResolutionMinutes) {
        return write("config_base", ValueFactory.newInteger(timeResolutionMinutes));
    }

    @Override
    public Future<Void> writeComplicationMode(final int primaryFaceMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(primaryFaceMode));
        return write("set_complication_mode", data);
    }

    @Override
    public Future<Void> writeComplicationMode(final int primaryFaceMode, final int secondaryFaceMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(primaryFaceMode),
                ValueFactory.newInteger(secondaryFaceMode));
        return write("set_complication_mode", data);
    }

    @Override
    public Future<Void> writeComplicationModes(final int mainMode, final int alternateMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(mainMode),
                ValueFactory.newInteger(alternateMode));
        return write("complications", data);
    }

    @Override
    public Future<Void> writeComplicationModes(final int mainMode, final int alternateMode,
                                               final int secondaryFaceMainMode, final int secondaryFaceAlternateMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(mainMode),
                ValueFactory.newInteger(alternateMode), ValueFactory.newInteger(secondaryFaceMainMode),
                ValueFactory.newInteger(secondaryFaceAlternateMode));
        return write("complications", data);
    }

    @Override
    public Future<Void> writeComplicationModes(final int mainMode, final int alternateMode, final int otherMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(mainMode),
                ValueFactory.newInteger(alternateMode), ValueFactory.newInteger(otherMode));
        return write("complications", data);
    }

    @Override
    public Future<Void> writeComplicationModes(final int mainMode, final int alternateMode, final int otherMode,
                                               final int primaryFaceMainMode, final int primaryFaceAlternateMode, final int primaryFaceOtherMode) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(mainMode),
                ValueFactory.newInteger(alternateMode), ValueFactory.newInteger(otherMode),
                ValueFactory.newInteger(primaryFaceMainMode), ValueFactory.newInteger(primaryFaceAlternateMode),
                ValueFactory.newInteger(primaryFaceOtherMode));
        return write("complications", data);
    }

    @Override
    public Future<Void> writeConfigSettings(final Map<String, Integer> settings) {
        final Promise<?> promise = new Promise();

        fetchDefinition("map_settings").flatMap(new FlatMapCallback<Void, Void>() {
            @Override
            public Future<Void> onResult(Void result) throws Exception {
                final ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();

				/* Add all values in map. */
                for (String key : settings.keySet()) {
                    final int nbr = mCommandCenter.getCommandNumber(key);

					/* Only add valid commands. */
                    if (nbr >= 0) {
                        map.put(ValueFactory.newInteger(nbr), ValueFactory.newInteger(settings.get(key)));
                    }
                }

                final MapValue mapValue = map.build();

                if (mapValue.size() > 0) {
                    return write("settings", mapValue);
                } else {
                    return FutureUtils.just(null);
                }
            }
        }).success(new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                QRomLog.i(TAG, "Commands written to settings.");

                promise.resolve(null);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Throwable error) {
                QRomLog.i(TAG, "No commands found in settings map.");

                promise.resolve(null);
            }
        });

        return (Future<Void>) promise.getFuture();
    }

    @Override
    public Future<Void> writeConfigVibrator(int[]... patterns) {
        final List<Future<Void>> futures = new ArrayList<Future<Void>>();

        int vibratorIndex = WatchDevice.CONFIG_VIBRATOR_FIRST_VIBRATION;
        for (int[] pattern : patterns) {
            ArrayList<Value> values = new ArrayList<Value>();
            values.add(ValueFactory.newInteger(vibratorIndex++));
            for (int i = 0; i < pattern.length; i++) {
                values.add(ValueFactory.newInteger(pattern[i]));
            }
            futures.add(write("vibrator_config", ValueFactory.newArray(values)));
        }

        return FutureUtils.merge(futures).flatMap(new FlatMapCallback<List<Void>, Void>() {
            @Override
            public Future<Void> onResult(List<Void> result) throws Exception {
                return FutureUtils.just(null);
            }
        });
    }

    @Override
    public Future<Void> writeCrash() {
        return write("crash", ValueFactory.newInteger(255));
    }

    @Override
    public Future<Void> writeDateTime(final int year, final int month, final int day, final int hour, final int min,
                                      final int sec, final int weekday) {
        QRomLog.i(TAG, "writeDateTime(" + year + ", " + month + "," + day +
                "," + hour + ", " + min + "," + sec + "," + weekday + ")");
        final ImmutableIntegerValue hourEx = ValueFactory.newInteger(hour);
        final ImmutableIntegerValue minuteEx = ValueFactory.newInteger(min);
        final ImmutableIntegerValue secondEx = ValueFactory.newInteger(sec);
        Exception here = new Exception();
        here.fillInStackTrace();
        QRomLog.i(TAG, "call writeDateTime 02 （hour:" + hour + ", min:" + min +
                ", sec:" + sec + ") hourEx=" + hourEx + " minuteEx=" + minuteEx + " secondEx=" + secondEx, here);

        return write("datetime", ValueFactory.newArray(ValueFactory.newInteger(year), ValueFactory.newInteger(month),
                ValueFactory.newInteger(day), hourEx, minuteEx, secondEx, ValueFactory.newInteger(weekday)));
    }

    @Override
    public Future<Void> writeDebug(final Value command) {
        return write(command.asStringValue().toString());
    }

    @Override
    public Future<Void> writeDebug(final Value command, final Value data) {
        return write(command.asStringValue().toString(), data);
    }

    @Override
    public Future<Void> writeDebugAppError(final int errorCode) {
        return write("debug_apperror", ValueFactory.newInteger(errorCode));
    }

    @Override
    public Future<Void> writeDebugConfig(final List<Integer> config) {
        if (config.isEmpty()) {
            return FutureUtils.just(null);
        }

        final List<Value> configList = new ArrayList<Value>();

        for (int i = 0; i < config.size(); i++) {
            configList.add(ValueFactory.newInteger(config.get(i)));
        }

        return write("config_debug", ValueFactory.newArray(configList));
    }

    @Override
    public Future<Void> writeDebugConfig(final boolean timeCompress, final boolean enableUart,
                                         final boolean enableTemperature, final boolean enableLedAndVibrationOnDisconnect, final boolean deprecate,
                                         final int onErrorRebootTimeout, final int millisPerMinuteTick, final boolean rssiNotification) {
        List<Integer> config = new ArrayList<Integer>();

        config.add(timeCompress ? 1 : 0);
        config.add(enableUart ? 1 : 0);
        config.add(enableTemperature ? 1 : 0);
        config.add(enableLedAndVibrationOnDisconnect ? 1 : 0);
        config.add(0); // DEPRECATED
        config.add(onErrorRebootTimeout);
        config.add(millisPerMinuteTick);
        config.add(rssiNotification ? 1 : 0);

        return writeDebugConfig(config);
    }

    @Override
    public Future<Void> writeDebugConfig(final boolean demoMode, final boolean enableUart,
                                         final boolean enableTemperature, final boolean enableLedOnDisconnect, final boolean enableMinuteTick,
                                         final int onErrorRebootTimeout, final boolean rssiNotification) {
        return writeDebugConfig(demoMode, enableUart, enableTemperature, enableLedOnDisconnect, enableMinuteTick,
                onErrorRebootTimeout, MILLIS_PER_MINUTE, rssiNotification);
    }

    @Override
    public Future<Void> writeDebugHardFault() {
        return write("debug_hardfault");
    }

    @Override
    public Future<Void> writeDebugReset(final int resetType) {
        return write("debug_reset", ValueFactory.newInteger(resetType));
    }

    @Override
    public Future<Void> writeEinkImg(final byte[] data) {
        final List<Value> values = new ArrayList<Value>();

        for (Byte img_val : data) {
            values.add(ValueFactory.newInteger(img_val));
        }

        return write("disp_img", ValueFactory.newBinary(data));
    }

    @Override
    public Future<Void> writeEinkImgCmd(final int cmd) {
        /*
         * Cmd:0 = Start image transfer Cmd:1 = Finish image transfer
		 */
        if (cmd != 0 && cmd != 1) {
            QRomLog.i(TAG, "Unexpected EinkImgCmd");
            throw new RuntimeException("EinkImgCmd Aborted");
        }

        return write("disp_img", ValueFactory.newInteger(cmd));
    }

    @Override
    public Future<Void> writeForgetDevice() {
        QRomLog.i(TAG, "writeForgetDevice()");
        writeStepsDay(0, Calendar.getInstance().get(Calendar.DATE));
        return write("forget_device");
    }

    @Override
    public Future<Void> writeIncomingCall(final int number, final boolean isRinging, final Integer alert) {
        QRomLog.i(TAG, "writeIncomingCall(" + number + ", " + isRinging + "," + alert + ")");
        Future<Void> future = write("call",
                ValueFactory.newArray(ValueFactory.newInteger(number), ValueFactory.newInteger(isRinging ? 1 : 0)));

        if (isRinging && alert != null) {
            //startVibrateForIncomingCall(alert);
        } else {
            //stopVibrateForIncomingCall();
        }

        return future;
    }

    @Override
    public Future<Void> writeMotor(final int motor, final int value) {
        QRomLog.i(TAG, "writeMotor(" + motor + ", " + value + ")");
        return write("stepper_goto",
                ValueFactory.newArray(ValueFactory.newInteger(motor), ValueFactory.newInteger(value)));
    }

    @Override
    public Future<Void> writeMotorDelay(final int value) {
        QRomLog.i(TAG, "writeMotorDelay(" + value + ")");
        return write("stepper_delay", ValueFactory.newInteger(value));
    }

    @Override
    public Future<Void> writeOnboardingDone(final boolean finished) {
        QRomLog.i(TAG, "writeOnboardingDone(" + finished + ")");
        return write("onboarding_done", ValueFactory.newInteger(finished ? 1 : 0));
    }

    @Override
    public Future<Void> writePostMortem() {
        QRomLog.i(TAG, "writePostMortem()");
        return write("postmortem", ValueFactory.newInteger(0));
    }

    @Override
    public Future<Void> writeRecalibrate(final boolean enable) {
        QRomLog.i(TAG, "writeRecalibrateMove(" + enable + ")");
        return write("recalibrate", ValueFactory.newBoolean(enable));
    }

    @Override
    public Future<Void> writeRecalibrateMove(final int motor, final int steps) {
        QRomLog.i(TAG, "writeRecalibrateMove(" + motor + ", " + steps + ")");
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(motor), ValueFactory.newInteger(steps));
        return write("recalibrate_move", data);
    }

    @Override
    public Future<Void> writeStartVibrator() {
        QRomLog.i(TAG, "writeRecalibrateMove()");
        return write("vibrator_start");
    }

    @Override
    public Future<Void> writeStartVibratorWithPattern(int[] pattern) {
        ArrayList<Value> values = new ArrayList<Value>();
        for (int i = 0; i < pattern.length; i++) {
            values.add(ValueFactory.newInteger(pattern[i]));
        }
        QRomLog.i(TAG, "writeStartVibratorWithPattern called");
        return write("vibrator_start", ValueFactory.newArray(values));
    }

    @Override
    public Future<Void> writeStepperExecPredef(final int handNo1, final int handNo2, final int patternIndex2,
                                               final int patternIndex3) {
        return write(
                "stepper_exec_predef",
                ValueFactory.newArray(ValueFactory.newInteger(handNo1), ValueFactory.newInteger(handNo2),
                        ValueFactory.newInteger(patternIndex2), ValueFactory.newInteger(patternIndex3)));
    }

    @Override
    public Future<Void> writeSteps(final int total, final int[] weekdays) {
        QRomLog.i(TAG, "writeSteps(" + total + "," + weekdays + ")");
        final List<Value> values = new ArrayList<Value>();
        values.add(ValueFactory.newInteger(total));
        for (Integer weekdayValue : weekdays) {
            values.add(ValueFactory.newInteger(weekdayValue));
        }
        return write("steps", ValueFactory.newArray(values));
    }

    @Override
    public Future<Void> writeStepsDay(int steps, int dayOfMonth) {
        QRomLog.i(TAG, "writeStepsDay(" + steps + "," + dayOfMonth + ")");
        return write("steps_day",
                ValueFactory.newArray(ValueFactory.newInteger(steps), ValueFactory.newInteger(dayOfMonth)));
    }

    @Override
    public Future<Void> writeStepsTarget(final int stepsTarget) {
        QRomLog.i(TAG, "writeStepsTarget(" + stepsTarget + ")");
        return write("steps_target", ValueFactory.newInteger(stepsTarget));
    }

    @Override
    public Future<Void> writeStillness(final int timeout, final int window, final int start, final int end) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(timeout), ValueFactory.newInteger(window),
                ValueFactory.newInteger(start), ValueFactory.newInteger(end));
        return write("stillness", data);
    }

    @Override
    public Future<Void> writeStopVibrator() {
        return write("vibrator_end");
    }

    @Override
    public Future<Void> writeTest(final int testCase, final int val) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(testCase), ValueFactory.newInteger(val));
        return write("test", data);
    }

    @Override
    public Future<Void> writeTimeZone(final String timeZoneId) {
        final Calendar watch = Calendar.getInstance();
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));

        final int hourDiff = calendar.get(Calendar.HOUR_OF_DAY) - watch.get(Calendar.HOUR_OF_DAY);
        final int minuteDiff = calendar.get(Calendar.MINUTE) - watch.get(Calendar.MINUTE);

        return write(
                "timezone",
                ValueFactory.newArray(ValueFactory.newInteger(MathsUtils.floorMod(hourDiff, 24)),
                        ValueFactory.newInteger(MathsUtils.floorMod(minuteDiff, 60))));
    }

    @Override
    public Future<Void> writeTriggers(final int upperTrigger, final int lowerTrigger) {
        final Value data = ValueFactory.newArray(ValueFactory.newInteger(upperTrigger),
                ValueFactory.newInteger(lowerTrigger));
        return write("triggers", data);
    }

    @Override
    public Future<Void> writeVbat() {
        return write("vbat");
    }

    @Override
    public Future<Void> writeVbatSim(final int mv) {
        return write("vbat_sim", ValueFactory.newInteger(mv));
    }

    @Override
    public Future<Void> writeWatchTime() {
        final Calendar calendar = Calendar.getInstance();

        return write(
                "datetime",
                ValueFactory.newArray(ValueFactory.newInteger(calendar.get(Calendar.YEAR)),
                        ValueFactory.newInteger(calendar.get(Calendar.MONTH) + 1),
                        ValueFactory.newInteger(calendar.get(Calendar.DAY_OF_MONTH)),
                        ValueFactory.newInteger(calendar.get(Calendar.HOUR_OF_DAY)),
                        ValueFactory.newInteger(calendar.get(Calendar.MINUTE)),
                        ValueFactory.newInteger(calendar.get(Calendar.SECOND)),
                        ValueFactory.newInteger(getDeviceDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))));
    }

    @Override
    public Future<Map<String, String>> readBuildInfo() {
        return readStringMap("status_buildinfo", "map_buildinfo").map(
                new MapCallback<Map<String, Value>, Map<String, String>>() {
                    @Override
                    public Map<String, String> onResult(Map<String, Value> result) {
                        Map<String, String> newMap = new HashMap<String, String>();
                        for (Map.Entry<String, Value> entry : result.entrySet()) {
                            newMap.put(entry.getKey(), entry.getValue().asStringValue().toString());
                        }
                        return newMap;
                    }
                });
    }

    @Override
    public Future<Map<String, String>> readBuildInfoBl() {
        return readStringMap("status_buildinfo_bl", "map_buildinfo").map(
                new MapCallback<Map<String, Value>, Map<String, String>>() {
                    @Override
                    public Map<String, String> onResult(Map<String, Value> result) {
                        Map<String, String> newMap = new HashMap<String, String>();
                        for (Map.Entry<String, Value> entry : result.entrySet()) {
                            newMap.put(entry.getKey(), entry.getValue().asStringValue().toString());
                        }
                        return newMap;
                    }
                });
    }

    @Override
    public Future<List<Integer>> readCoil() {
        return read("test_coil").map(new MapCallback<Value, List<Integer>>() {
            @Override
            public List<Integer> onResult(final Value result) {
                List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }
                return values;
            }
        });
    }

    @Override
    public Future<Integer> readCurrentConnInt() {
        return FutureUtils.just(mCurrentConnInt);
    }

    public void getDateTimeForDebug() {
        read("datetime").map(new MapCallback<Value, Boolean>() {
            @Override
            public Boolean onResult(final Value result) throws IOException {
                List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }

                return true;
            }
        });
    }

    @Override
    public Future<Boolean> readDateTime() {
        return read("datetime").map(new MapCallback<Value, Boolean>() {
            @Override
            public Boolean onResult(final Value result) throws IOException {
                List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }

                final Calendar appTime = Calendar.getInstance();

                final Calendar deviceBeforeTime = Calendar.getInstance();
                deviceBeforeTime.set(values.get(0), deviceToAppMonth(values.get(1)), values.get(2), values.get(3),
                        values.get(4), values.get(5) - 5);

                final Calendar deviceAfterTime = Calendar.getInstance();
                deviceAfterTime.set(values.get(0), deviceToAppMonth(values.get(1)), values.get(2), values.get(3),
                        values.get(4), values.get(5) + 25);

                return appTime.after(deviceBeforeTime) && appTime.before(deviceAfterTime)
                        && values.get(6) == getDeviceDayOfWeek(appTime.get(Calendar.DAY_OF_WEEK));
            }
        });
    }

    @Override
    public Future<List<Integer>> readDebugDisconnect() {
        return read("debug_disconnect").map(new MapCallback<Value, List<Integer>>() {
            @Override
            public List<Integer> onResult(final Value result) throws IOException {
                final List<Integer> list = new ArrayList<Integer>();

                for (Value value : result.asArrayValue().list()) {
                    list.add(value.asIntegerValue().asInt());
                }

                return list;
            }
        });
    }

    @Override
    public Future<Map<String, String>> readDeviceInformation() {
        final Map<String, Future<String>> wrappedMap = new HashMap<String, Future<String>>();

        wrappedMap.put(DeviceUtils.DEVICE_INFO_FIRMWARE_REVISION,
                readDeviceInformation(UUIDStorage.DEVICE_INFO_FWR_REVISION));
        wrappedMap.put(DeviceUtils.DEVICE_INFO_HARDWARE_REVISION,
                readDeviceInformation(UUIDStorage.DEVICE_INFO_HWR_REVISION));
        wrappedMap.put(DeviceUtils.DEVICE_INFO_MANUFACTURE_NAME,
                readDeviceInformation(UUIDStorage.DEVICE_INFO_MANUFACTER_NAME));
        wrappedMap
                .put(DeviceUtils.DEVICE_INFO_ITEM_NUMBER, readDeviceInformation(UUIDStorage.DEVICE_INFO_MODEL_NUMBER));
        wrappedMap.put(DeviceUtils.DEVICE_INFO_SERIAL_NUMBER,
                readDeviceInformation(UUIDStorage.DEVICE_INFO_SERIAL_NUMBER));
        wrappedMap.put(DeviceUtils.DEVICE_INFO_ADDRESS, FutureUtils.just(mDevice.getAddress()));

        return FutureUtils.unwrap(wrappedMap);
    }

    @Override
    public Future<String> readDeviceItemNumber() {
        return readDeviceInformation(UUIDStorage.DEVICE_INFO_MODEL_NUMBER);
    }

    @Override
    public Future<Map<String, Value>> readDiagnostics() {
        return readStringMap("status_diag", "map_diag");
    }

    @Override
    public Future<List<Integer>> readFcte() {
        return write("test_fcte", ValueFactory.newInteger(1)).delay(TEST_FCTE_DELAY)
                .flatMap(new FlatMapCallback<Void, Void>() {
                    @Override
                    public Future<Void> onResult(final Void result) throws Exception {
                        return write("test_fcte", ValueFactory.newInteger(0));
                    }
                }).flatMap(new FlatMapCallback<Void, Value>() {
                    @Override
                    public Future<Value> onResult(final Void result) throws Exception {
                        return read("test_fcte");
                    }
                }).map(new MapCallback<Value, List<Integer>>() {
                    @Override
                    public List<Integer> onResult(final Value result) throws IOException {
                        List<Integer> values = new ArrayList<Integer>();
                        for (Value value : result.asArrayValue().list()) {
                            values.add(value.asIntegerValue().asInt());
                        }
                        return values;
                    }
                });
    }

    @Override
    public String readFirmwareVersion() {
        return getCachedDeviceInformation(UUIDStorage.DEVICE_INFO_FWR_REVISION);
    }

    @Override
    public Future<Boolean> readOnboardingDone() {
        return read("onboarding_done").map(new MapCallback<Value, Boolean>() {
            @Override
            public Boolean onResult(final Value result) throws IOException {
                return result.asIntegerValue().asInt() == 1;
            }
        });
    }

    @Override
    public Future<Value> readPostMortem() {
        final List<Value> pages = new ArrayList<Value>();
        return readAsArray("postmortem", 0).flatMap(new FlatMapCallback<Value, Value>() {
            @Override
            public Future<Value> onResult(final Value result) throws Exception {
                pages.add(result);
                final byte[] data = result.asBinaryValue().asByteArray();
                if (data.length < 500) {
                    return FutureUtils.just((Value) ValueFactory.newArray(pages));
                }
                return readAsArray("postmortem", pages.size()).flatMap(this); // Recurse
            }
        });
    }

    @Override
    public Future<byte[]> readPostMortemData() {
        return readPostMortem().map(new MapCallback<Value, byte[]>() {
            @Override
            public byte[] onResult(final Value result) throws IOException {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (final Value page : result.asArrayValue()) {
                    byte[] data = page.asBinaryValue().asByteArray();
                    try {
                        outputStream.write(data);
                    } catch (IOException e) {
                        QRomLog.i(TAG, "Failed to read post mortem ", e);
                    }
                }
                return outputStream.toByteArray();
            }
        });
    }

    @Override
    public Future<Value> readUartDump(final ProgressCallback progress) {
        final List<Value> pages = new ArrayList<Value>();
        return read("dump_uart").flatMap(new FlatMapCallback<Value, Value>() {
            @Override
            public Future<Value> onResult(final Value result) throws Exception {
                final int page_count = result.asIntegerValue().asInt();

                if (progress != null) {
                    progress.onProgress(0, page_count);
                }

                return readAsArray("dump_uart", 0).flatMap(new FlatMapCallback<Value, Value>() {
                    @Override
                    public Future<Value> onResult(final Value result) throws Exception {
                        pages.add(result);

                        if (progress != null) {
                            progress.onProgress(pages.size(), page_count);
                        }

                        final byte[] data = result.asBinaryValue().asByteArray();
                        if (data.length < 500) {
                            return FutureUtils.just((Value) ValueFactory.newArray(pages));
                        }
                        return readAsArray("dump_uart", pages.size()).flatMap(this); // Recurse
                    }
                });
            }
        });
    }

    @Override
    public Future<byte[]> readUartDumpData(final ProgressCallback progress) {
        return readUartDump(progress).map(new MapCallback<Value, byte[]>() {
            @Override
            public byte[] onResult(final Value result) throws IOException {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (final Value page : result.asArrayValue()) {
                    byte[] data = page.asBinaryValue().asByteArray();
                    try {
                        outputStream.write(data);
                    } catch (IOException e) {
                        QRomLog.i(TAG, "Failed to read post mortem ", e);
                    }
                }
                return outputStream.toByteArray();
            }
        });
    }

    @Override
    public Future<Integer> readRssi() {
        return read("rssi").map(new MapCallback<Value, Integer>() {
            @Override
            public Integer onResult(final Value result) throws IOException {
                return result.asIntegerValue().asInt();
            }
        });
    }

    @Override
    public Future<String> readSerialNumber() {
        return readDeviceInformation(UUIDStorage.DEVICE_INFO_SERIAL_NUMBER);
    }

    @Override
    public Future<List<Integer>> readSteps() {
        return read("steps").map(new MapCallback<Value, List<Integer>>() {
            @Override
            public List<Integer> onResult(final Value result) {
                List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }
                return values;
            }
        });
    }

    @Override
    public Future<List<Integer>> readStepsDay(final int day) {
        return readAsArray("steps_day", day).map(new MapCallback<Value, List<Integer>>() {
            @Override
            public List<Integer> onResult(final Value result) {
                final List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }
                if (values.size() < 2) {
                    throw new IllegalArgumentException("Didn't get a two element list");
                }
                return values;
            }
        });
    }

    @Override
    public Future<Integer> readStepsTarget() {
        return read("steps_target").map(new MapCallback<Value, Integer>() {
            @Override
            public Integer onResult(final Value result) {
                return result.asIntegerValue().asInt();
            }
        });
    }

    @Override
    public Future<List<Integer>> readStillness() {
        return read("stillness").map(new MapCallback<Value, List<Integer>>() {
            @Override
            public List<Integer> onResult(final Value result) {
                List<Integer> values = new ArrayList<Integer>();
                for (Value value : result.asArrayValue().list()) {
                    values.add(value.asIntegerValue().asInt());
                }
                return values;
            }
        });
    }

    @Override
    public Future<Integer> readVbat() {
        return read("vbat").map(new MapCallback<Value, Integer>() {
            @Override
            public Integer onResult(final Value result) {
                return result.asIntegerValue().asInt();
            }
        });
    }

    public void tryHardToConnect() {
        if (mDevice != null) {
            mDevice.tryHardToConnect();
        }
    }

    private void connectInputDeviceIfNeeded() {
        if (mDevice != null) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mDevice.getAddress());
            InputDeviceConnector.getInstance().doConnectIfNeeded(device, null, mContext);
        }
    }

    public interface FirmwareInfoListener {
        void onFirmwareDirty();
    }

    public CommandCenter getCommandCenter() {
        return mCommandCenter;
    }
}
