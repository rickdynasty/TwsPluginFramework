package com.example.pluginbluetooth.bluetooth.device;

import android.content.Context;
import android.util.Log;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.bluetooth.device.readwrite.Command;
import com.example.pluginbluetooth.bluetooth.device.readwrite.DeviceWriter;
import com.example.pluginbluetooth.bluetooth.gatt.DeviceListener;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.bluetooth.gatt.ReadCallback;
import com.example.pluginbluetooth.future.AlwaysCallback;
import com.example.pluginbluetooth.future.FlatMapCallback;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.FutureUtils;
import com.example.pluginbluetooth.future.MapCallback;
import com.example.pluginbluetooth.future.Promise;
import com.example.pluginbluetooth.utils.ByteUtils;
import com.example.pluginbluetooth.utils.DeviceUtils;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2018/3/1.
 */

public class WatchDevice implements WatchDeviceInterface {
    private static final String TAG = "rick_Print:WatchDevice";

    private static final long OTA_UNBOND_TIMEOUT = 10000;

    private boolean mIsConnected = false; // Do we have a BT connection up and running?
    private boolean mIsReady = false; // Are we ready to run arbitrary commands?
    private boolean mInOtaMode = false;

    private final CommandCenter mCommandCenter = new CommandCenter();
    private final Map<String, Future<String>> mOngoingReads = new HashMap<String, Future<String>>();
    private final DeviceWriter mDeviceWriter;

    private final Context mContext;
    private final GattDevice mDevice;
    private final CacheCenter mCache;

    private boolean mIsDebugEnabled = BuildConfig.DEBUG;

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
        final String cacheKey = characteristicUUID.toString();
        final Promise<String> promise = new Promise<String>();

        final String cachedResult;
        if (useCache) {
            cachedResult = mCache.getString(cacheKey);
        } else {
            cachedResult = null;
        }

        if (isInOtaMode()) {
            QRomLog.d(TAG, "readDeviceInformation: in DFU mode without cache");
            promise.resolve("");
        } else if (cachedResult != null) {
            QRomLog.d(TAG, "readDeviceInformation: with cache");
            promise.resolve(cachedResult);
        } else {
            QRomLog.d(TAG, "readDeviceInformation: no cache, reading from device");
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
                    QRomLog.d(TAG, "Reusing already ongoing request");
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

    public boolean isConnecting() {
        // We're connecting if we have a BT connection and aren't ready (i.e.
        // "connected") or in DFU mode yet
        return mIsConnected && !mIsReady && !mInOtaMode;
    }

    public boolean isConnected() {
        return mIsReady;
    }

    public boolean isInOtaMode() {
        return mInOtaMode;
    }

    private final Set<DeviceConnectionListener> mConnectionListeners = new CopyOnWriteArraySet<DeviceConnectionListener>();

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
//            String firstPairTime = SharedPreferencesUtil.getInstance()
//                    .getHealthSharedPreferences(HostProxy.getApplication())
//                    .getString(HealthDeviceInfoHandler.SP_FIRST_PAIR_WATCH_TIME, null);
//            if (TextUtils.isEmpty(firstPairTime)) {
//                final Calendar calendar = Calendar.getInstance();
//                // 注意系统Calendar的月份是索引方式存储的，取出来显示用的话须要进行+1操作
//                firstPairTime = calendar.get(Calendar.YEAR) + HealthDataProcessor.SEPARATOR_VALUE
//                        + calendar.get(Calendar.MONTH) + HealthDataProcessor.SEPARATOR_VALUE
//                        + calendar.get(Calendar.DATE);
//                SharedPreferencesUtil.getInstance().getHealthSharedPreferences(HostProxy.getApplication())
//                        .putString(HealthDeviceInfoHandler.SP_FIRST_PAIR_WATCH_TIME, firstPairTime);
//            }

            mIsConnected = true;
            //getCommandMap();
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
            } else if (mInOtaMode) {
                mInOtaMode = false;
                for (DeviceConnectionListener listener : mConnectionListeners) {
                    listener.onLeaveOtaMode();
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
            QRomLog.d(TAG, "onCharacteristicChanged(" + service + ", " + characteristic + ", " + data + ")");
        }

        @Override
        public void onBonded() {
            QRomLog.d(TAG, "Bonded");
        }

        @Override
        public void onConnectionStateChange(int newState, int status) {
            QRomLog.d(TAG, "onConnectionStateChange(" + newState + ", " + status + ")");
        }
    };

    private void onEnterOtaMode() {
        if (mIsConnected) {
            QRomLog.d(TAG, "Entering DFU mode");
            mInOtaMode = true;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onEnterOtaMode();
            }
        }
    }

    public Future<Boolean> readOnboardingDone() {
        return read("onboarding_done").map(new MapCallback<Value, Boolean>() {
            @Override
            public Boolean onResult(final Value result) throws IOException {
                return result.asIntegerValue().asInt() == 1;
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

        if (mInOtaMode) {
            mInOtaMode = false;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onLeaveOtaMode();
            }
        }

        // Clean up and disconnect
        //stopVibrateForIncomingCall();
        mDevice.unregisterListener(mDeviceListener);
        //cancelDfu();
        mDevice.disconnect();
    }

    public void registerConnectionListener(DeviceConnectionListener listener) {
        mConnectionListeners.add(listener);
    }

    public void unregisterConnectionListener(DeviceConnectionListener listener) {
        mConnectionListeners.remove(listener);
    }

    public void invalidateCache() {
        mCache.invalidate();
    }


    /* Write commands wrapping interaction with DeviceWriter. */
    private Future<Void> write(final String command, final Value value) {
        try {
            final Command cmd = mDeviceWriter.createCommand(command, value, new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    private Future<Void> write(final String command) {
        try {
            final Command cmd = mDeviceWriter.createCommand(command, ValueFactory.newInteger(0), new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    private Future<Void> writeAsList(final String command, final int value) {
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
        try {
            final Command cmd = mDeviceWriter.createCommand(command, (Value) null, new Promise<Void>());

            return mDeviceWriter.write(cmd);
        } catch (IOException ex) {
            return FutureUtils.error(ex);
        }
    }

    /**
     * Read command without any parameters
     */
    private Future<Value> read(final String command) {
        QRomLog.d(TAG, "read: " + command);

        if (mInOtaMode) {
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

    private static final int MAX_READ_TRIES = 3;

    private Future<Value> read() {
        final Promise<Value> promise = new Promise<Value>();

        mDevice.read(UUIDStorage.ANIMA_SERVICE, UUIDStorage.ANIMA_CHAR, new ReadCallback() {

            private int mTriesLeft = MAX_READ_TRIES;

            @Override
            public boolean onSuccess(final byte[] result) {
                final Value value;
                try {
                    value = unpackReceivedValue(result);
                    QRomLog.d(TAG, "Read " + value);

                    if (!value.isMapValue() && !value.isNilValue()) {
                        throw new IOException("Unexpected value");
                    }
                } catch (IOException e) {
                    if (mIsDebugEnabled) {
                        QRomLog.d(TAG, "Failed to parse: " + ByteUtils.bytesToHex(result), e);
                    }
                    mTriesLeft--;
                    if (mTriesLeft > 0) {
                        if (mIsDebugEnabled) {
                            QRomLog.d(TAG, "Retrying read");
                        }
                        return true; // retry the read
                    } else {
                        if (mIsDebugEnabled) {
                            QRomLog.d(TAG, "Not retrying read");
                        }
                        promise.reject(e);
                    }
                    return false;
                } catch (MessagePackException e) {
                    if (mIsDebugEnabled) {
                        QRomLog.d(TAG, "Failed to parse: " + ByteUtils.bytesToHex(result), e);
                    }
                    mTriesLeft--;
                    if (mTriesLeft > 0) {
                        if (mIsDebugEnabled) {
                            QRomLog.d(TAG, "Retrying read");
                        }
                        return true; // retry the read
                    } else {
                        if (mIsDebugEnabled) {
                            QRomLog.d(TAG, "Not retrying read");
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

    private int deviceToAppMonth(final Integer deviceMonth) {
        return deviceMonth - 1;
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

    public String getAddress() {
        return mDevice.getAddress();
    }

    public Future<Void> writeForgetDevice() {
        return write("forget_device");
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
            QRomLog.d(TAG, "Removing bonding for " + getAddress());
            mDevice.removeBond();
        } catch (Exception e) {
            QRomLog.i(TAG, "Removing bond failed", e);
            return FutureUtils.error(e);
        }
        return mDevice.waitForUnbonded(OTA_UNBOND_TIMEOUT);
    }

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
}
