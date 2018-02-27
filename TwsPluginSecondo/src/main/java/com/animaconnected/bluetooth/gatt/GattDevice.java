package com.animaconnected.bluetooth.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.animaconnected.future.Future;
import com.animaconnected.future.Promise;
import com.animaconnected.bluetooth.device.scanner.GattDeviceScanner;
import com.animaconnected.bluetooth.utils.Callback;
import com.animaconnected.secondo.utils.BuildConfig;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class GattDevice implements GattConnection.GattConnectionInstructor, GattConnectionAttemptListener {

    public static final String TAG = GattDevice.class.getSimpleName();

    private static final int HARD_TO_CONNECT_TIME_MS = 60000;
    public static final int MAX_SCAN_TIME_MS = 30000;

    private static final int MAX_NBR_OF_HARD_CONNECTION_ATTEMPTS = 2;

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_PRIMO = 1;
    public static final int TYPE_SECONDO = 2;
    public static final int TYPE_GARBO = 3;

    private static final String[] sTypeNames = {"Unknown", "Primo", "Secondo", "Garbo"};

    private final Context mContext;
    private BluetoothDevice mBluetoothDevice;
    private final int mType;
    private final int mItemId;
    private final int mRssi;

    private final Set<DeviceListener> mListeners = new CopyOnWriteArraySet<DeviceListener>();

    private GattConnection mGattConnection;
    private boolean mShouldBeConnected = false;
    private boolean mIsConnected = false;
    private boolean mIsDisconnecting = false;
    private boolean mIsDebugEnabled = BuildConfig.DEBUG;
    private boolean mUseRefreshServices = false;
    private GattDeviceScanner mGattDeviceScanner;
    private boolean mTryHardToConnect;
    private int mCurrentNbrOfHardConnectionAttempts;
    private long mConnectionAttemptTimestamp;

    private Handler mHandler;

    private Runnable mCheckForConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHardToConnect()) {
                for (DeviceListener listener : mListeners) {
                    listener.onHardToConnect();
                }
            }
        }
    };

    private final GattListener mGattListener = new GattListener() {
        @Override
        public void onConnected() {
            mIsDisconnecting = false;
            mIsConnected = true;
            mConnectionAttemptTimestamp = Long.MAX_VALUE;
            for (DeviceListener listener : mListeners) {
                listener.onConnected();
            }
        }

        @Override
        public void onDisconnecting() {
            mIsDisconnecting = true;
        }

        @Override
        public void onDisconnected() {
            if (mIsConnected && mShouldBeConnected) {
                restartHardToConnectCheck();
            }
            mIsDisconnecting = false;
            mIsConnected = false;
            for (DeviceListener listener : mListeners) {
                listener.onDisconnected();
            }
            if (mShouldBeConnected) {
                if (mTryHardToConnect &&
                        mCurrentNbrOfHardConnectionAttempts < MAX_NBR_OF_HARD_CONNECTION_ATTEMPTS) {
                    scanToRefreshBluetoothDevice();
                } else {
                    gattConnect();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(final UUID service, final UUID characteristic, final byte[] data) {
            for (DeviceListener listener : mListeners) {
                listener.onCharacteristicChanged(service, characteristic, data);
            }
        }

        @Override
        public void onBonded() {
            for (DeviceListener listener : mListeners) {
                listener.onBonded();
            }
        }

        @Override
        public void onConnectionStateChange(int newState, int status) {
            for (DeviceListener listener : mListeners) {
                listener.onConnectionStateChange(newState, status);
            }
        }
    };

    public GattDevice(final Context context, final BluetoothDevice device, final int deviceType, final int itemId,
                      final int rssi) {
        mContext = context.getApplicationContext();
        mBluetoothDevice = device;
        mType = deviceType;
        mItemId = itemId;
        mRssi = rssi;
    }

    /**
     * Construct a GattDevice from saved device info
     * <p/>
     * This is completely thread safe (but the rest of the class isn't necessarily).
     */
    public GattDevice(final Context context, final String deviceAddress, final int deviceType,
                      final int itemId) {
        mContext = context;
        mBluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress); // thread-safe call
        mType = deviceType;
        mItemId = itemId;
        mRssi = 0;
        mTryHardToConnect = false;
    }

    /**
     * Try to remove the bond for this device
     * <p/>
     * It might fail by throwing an exception. It's not necessarily safe to assume that the bond is gone immediately
     * if it returns successfully.
     */
    public void removeBond() throws Exception {
        if (mBluetoothDevice.getBondState() != BluetoothDevice.BOND_NONE) {
            final Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
            boolean result = (Boolean) removeBondMethod.invoke(mBluetoothDevice);
            if (!result) {
                throw new RuntimeException("removeBond failed!");
            }
        }
    }

    /**
     * Return a Future that's resolved as soon as the device has no bonding anymore
     * <p/>
     * It uses simple polling, since that doesn't require any permissions. We have a timeout to make sure we don't
     * keep polling forever. The Future fails if we time out.
     * <p/>
     * The only way to find out if a removeBond() succeeded is to use this function and choose an appropriate timeout.
     *
     * @param timeoutMs The number of milliseconds to give up after.
     */
    public Future<Void> waitForUnbonded(final long timeoutMs) {
        final Promise<Void> promise = new Promise<Void>();
        final long startTime = System.currentTimeMillis();

        // Just poll the bond state until we're debonded or timoutMs elapsed. No need to optimize this.
        if (mBluetoothDevice.getBondState() != BluetoothDevice.BOND_NONE) {
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            promise.reject(new RuntimeException("Timed out waiting for bond to be removed"));
                        } else {
                            handler.postDelayed(this, 100);
                        }
                    } else {
                        promise.resolve(null);
                    }
                }
            });
        } else {
            promise.resolve(null);
        }

        return promise.getFuture();
    }

    public String getAddress() {
        return mBluetoothDevice.getAddress();
    }

    public int getType() {
        return mType;
    }

    public int getItemId() {
        return mItemId;
    }

    public String getTypeName() {
        return sTypeNames[mType];
    }

    public int getRssi() {
        return mRssi;
    }

    public boolean isBonded() {
        return mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    public void connect() {
        if (!mShouldBeConnected) {
            restartHardToConnectCheck();
            mShouldBeConnected = true;
            gattConnect();
        }
    }

    public void disconnect() {
        if (mShouldBeConnected) {
            mShouldBeConnected = false;
            mGattConnection.disconnect();
        }
    }

    public void refreshConnection() {
        if (mShouldBeConnected) {
            // calling disconnect while mShouldBeConnected will result in a disconnect,
            // ondisconnected, and connect!
            mGattConnection.disconnect();
        }
    }

    private void gattConnect() {
        mGattConnection = new GattConnection(mContext, mBluetoothDevice, mGattListener, this,
                mUseRefreshServices);
        mGattConnection.setConnectionAttemptListener(this);
        mGattConnection.setDebugMode(mIsDebugEnabled);
        mGattConnection.connect();
    }

    public void registerListener(DeviceListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(DeviceListener listener) {
        mListeners.remove(listener);
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean isDisconnecting() {
        return mIsDisconnecting;
    }

    public boolean hasGattService(final UUID service) {
        if (mIsConnected) {
            return mGattConnection.hasGattService(service);
        } else {
            return false;
        }
    }

    public @NonNull List<UUID> getGattServices() {
        if (mIsConnected) {
            return mGattConnection.getGattServices();
        } else {
            return Collections.emptyList();
        }
    }

    public boolean hasGattCharacteristic(final UUID service, final UUID characteristic) {
        if (mIsConnected) {
            return mGattConnection.hasGattCharacteristic(service, characteristic);
        } else {
            return false;
        }
    }

    public boolean refreshServices() {
        if (mIsConnected) {
            return mGattConnection.refreshServices();
        } else {
            return false;
        }
    }

    public void read(UUID service, UUID characteristic, ReadCallback callback) {
        if (mIsConnected) {
            mGattConnection.read(service, characteristic, callback);
        } else {
            callback.onError(new RuntimeException("Not connected"));
        }
    }

    public void write(UUID service, UUID characteristic, byte[] data) {
        write(service, characteristic, data, null);
    }

    public void write(UUID service, UUID characteristic, byte[] data, Callback<Void> callback) {
        if (mIsConnected) {
            mGattConnection.write(service, characteristic, data, callback);
        } else if (callback != null) {
            callback.onError(new RuntimeException("Not connected"));
        }
    }

    public void setNotification(UUID service, UUID characteristic) {
        setNotification(service, characteristic, null);
    }

    public void setNotification(UUID service, UUID characteristic, Callback<Void> callback) {
        if (mIsConnected) {
            mGattConnection.setNotification(service, characteristic, callback);
        } else {
            callback.onError(new RuntimeException("Not connected"));
        }
    }

    public void setDebugMode(final boolean enable) {
        mIsDebugEnabled = enable;

        if (mGattConnection != null) {
            mGattConnection.setDebugMode(enable);
        }
    }

    public void setUseRefreshService(final boolean enable) {
        if (mGattConnection != null) {
            mUseRefreshServices = enable;

            mGattConnection.setUseRefreshService(enable);
        }
    }

    @Override
    public String toString() {
        if (mBluetoothDevice.getName() != null) {
            return mBluetoothDevice.getAddress() + " (" + mBluetoothDevice.getName() + ")";
        }
        return mBluetoothDevice.getAddress();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GattDevice that = (GattDevice) o;
        return mBluetoothDevice.getAddress().equals(that.getAddress());
    }

    @Override
    public int hashCode() {
        return mBluetoothDevice.getAddress().hashCode();
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public boolean shallContinueTrying(GattConnectionAttempt currentConnectionAttempt) {
        return !mTryHardToConnect;
    }

    public void tryHardToConnect() {
        Log.d(TAG, "tryHardToConnect");
        if (!mIsConnected) {
            mTryHardToConnect = true;
            if (mGattConnection != null && mGattConnection.isPassivelyTryingToConnect()) {
                Log.d(TAG, "gattConnection is passively trying to connect... refreshConnection");
                refreshConnection();
            }
        }
    }

    public boolean isHardToConnect() {
        return mGattConnection != null &&
                (SystemClock.uptimeMillis() - mConnectionAttemptTimestamp > HARD_TO_CONNECT_TIME_MS);
    }

    public boolean isHighProbabilityForDFUSuccess() {
        return mGattConnection != null && mGattConnection.didConnectUsingActiveConnect();
    }

    private void scanToRefreshBluetoothDevice() {
        mCurrentNbrOfHardConnectionAttempts++;
        if (mGattDeviceScanner == null) {
            mGattDeviceScanner = new GattDeviceScanner(mContext);
        }
        mGattDeviceScanner.startScan(mBluetoothDevice.getAddress(), MAX_SCAN_TIME_MS,
                new GattDeviceScanner.GattDeviceScannerListener() {
            @Override
            public void onScanFinished(GattDevice device) {
                Log.d(TAG, "onScanFinished device: " + device);
                if (device != null) {
                    mBluetoothDevice = device.mBluetoothDevice;
                }
                mTryHardToConnect = false; // only do this once
                if (mShouldBeConnected) {
                    gattConnect();
                }
            }
        });
    }

    private void restartHardToConnectCheck() {
        if (mHandler == null) {
            mHandler = new Handler();
        } else {
            mHandler.removeCallbacks(mCheckForConnectionRunnable);
        }
        mConnectionAttemptTimestamp = SystemClock.uptimeMillis();
        mHandler.postDelayed(mCheckForConnectionRunnable, HARD_TO_CONNECT_TIME_MS + 1);
    }

    @Override
    public void onConnectionPhaseChanged(GattConnectionAttempt gattConnectionAttempt) {
        Log.d(TAG, "onConnectionPhaseChanged: " + gattConnectionAttempt);

        switch (gattConnectionAttempt) {
            case CONNECTING_ACTIVE_SUCCESS:
            case CONNECTING_PASSIVE_SUCCESS:
                mCurrentNbrOfHardConnectionAttempts = 0;
                break;
            default:
                break;
        }
    }
}
