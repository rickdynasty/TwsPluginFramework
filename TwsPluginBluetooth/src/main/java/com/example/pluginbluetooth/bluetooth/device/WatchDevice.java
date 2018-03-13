package com.example.pluginbluetooth.bluetooth.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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

    private CapabilityCenter mCapabilityCenter;

    private final CommandCenter mCommandCenter = new CommandCenter();
    private final Map<String, Future<String>> mOngoingReads = new HashMap<String, Future<String>>();
    private final DeviceWriter mDeviceWriter;

    private final Context mContext;
    private final GattDevice mDevice;
    private final CacheCenter mCache;

    private boolean mIsDebugEnabled = BuildConfig.DEBUG;

    public WatchDevice(final GattDevice device, final CacheCenter cache) {
        QRomLog.i(TAG, "WatchDevice device: " + (device != null ? device.getAddress() : null) + " cache:" + cache);

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

        if (isInOtaMode()) {
            QRomLog.i(TAG, "readDeviceInformation: in DFU(OTA) mode without cache");
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
                    QRomLog.i(TAG, "readDeviceInformation: no cache, mDevice.read::onSuccess");
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
    private FirmwareInfoListener mFirmwareInfoListener;

    private final DeviceListener mDeviceListener = new DeviceListener() {
        @Override
        public void onConnected() {
            QRomLog.i(TAG, "-=-=-=-=- DeviceListener::onConnected -=-=-=-=-");
            // We now have a BT connection, but still need to finish our
            // handshaking.
            // Tell listeners that we're "connecting" for now.
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onConnecting();
            }

            mIsConnected = true;
            getCommandMap();
        }

        @Override
        public void onHardToConnect() {
            QRomLog.i(TAG, "DeviceListener::onHardToConnect");
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onHardToConnect();
            }
        }

        @Override
        public void onDisconnected() {
            QRomLog.i(TAG, "DeviceListener::onDisconnected");
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
            QRomLog.i(TAG, "DeviceListener::onCharacteristicChanged(" + service + ", " + characteristic + ", " + data + ")");
        }

        @Override
        public void onBonded() {
            QRomLog.i(TAG, "DeviceListener::Bonded");
        }

        @Override
        public void onConnectionStateChange(int newState, int status) {
            QRomLog.i(TAG, "DeviceListener::onConnectionStateChange(" + newState + ", " + status + ")");
        }
    };

    private void getCommandMap() {
        QRomLog.i(TAG, "call getCommandMap");
        readFirmwareVersionUncached().flatMap(new FlatMapCallback<String, Void>() {
            @Override
            public Future<Void> onResult(final String firmwareVersion) throws Exception {
                QRomLog.i(TAG, "getCommandMap() flatMap onResult");
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
                QRomLog.i(TAG, "getCommandMap() success");
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

    private Future<String> readFirmwareVersionUncached() {
        QRomLog.i(TAG, "call readFirmwareVersionUncached");
        final UUID charFwVersion = UUIDStorage.DEVICE_INFO_FWR_REVISION;
        if (mIsDebugEnabled) {
            QRomLog.i(TAG, "Reading the firmware version...");
        }
        return readDeviceInformation(charFwVersion, false);
    }

    private static final String MIN_FW_VERSION = "20160720";
    private static final String FW_DIRTY = "-dirty";

    private boolean isCompatibleFwVersion(final String firmwareVersion) {
        return MIN_FW_VERSION.compareTo(firmwareVersion) <= 0;
    }

    private boolean isFwDirty(final String firmwareVersion) {
        return firmwareVersion != null && firmwareVersion.endsWith(FW_DIRTY);
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
     * Read command with a parameter
     */
    private Future<Value> readAsArray(final String command, final int param) {
        if (mInOtaMode) {
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

    private void onCommandMapReady() {
        QRomLog.i(TAG, "CALL onCommandMapReady() Command map: " + mCommandCenter.getCommandMap().toString());
        mDevice.setNotification(UUIDStorage.ANIMA_SERVICE, UUIDStorage.NOTIFICATION_CHAR);
        mIsReady = true;
        initCapabilities();

        connectInputDeviceIfNeeded();

        QRomLog.i(TAG, "--==-- onCommandMapReady() 01");
        // Notify listeners that we are connected
        for (DeviceConnectionListener listener : mConnectionListeners) {
            QRomLog.i(TAG, "--==-- onCommandMapReady() 回调：" + listener);
            listener.onConnected();
        }
    }

    private void connectInputDeviceIfNeeded() {
        if (mDevice != null) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mDevice.getAddress());
            InputDeviceConnector.getInstance().doConnectIfNeeded(device, null, mContext);
        }
    }

    private void initCapabilities() {
        if (mCapabilityCenter == null && mCommandCenter.hasCommand("cap")) {
            Value cachedCapValue = mCache.getValue("cap");
            if (cachedCapValue != null) {
                mCapabilityCenter = new CapabilityCenter(cachedCapValue);
            }
        }
    }

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

    private void enterDfuMode() {
        if (mIsConnected) {
            QRomLog.i(TAG, "Entering DFU mode");
            mInOtaMode = true;
            for (DeviceConnectionListener listener : mConnectionListeners) {
                listener.onEnterOtaMode();
            }
        }
    }

    private void onEnterOtaMode() {
        if (mIsConnected) {
            QRomLog.i(TAG, "Entering DFU mode");
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
        QRomLog.i(TAG, "read: " + command);

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
                    QRomLog.i(TAG, "Read " + value);

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
            QRomLog.i(TAG, "Removing bonding for " + getAddress());
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

    public interface FirmwareInfoListener {
        void onFirmwareDirty();
    }

}
