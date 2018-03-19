package com.example.pluginbluetooth.bluetooth.gatt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.utils.Callback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import qrom.component.log.QRomLog;

@SuppressLint("NewApi")
class GattConnection {

    private static final String TAG = "rick_Print:GattConnection";

    private static final long COMMAND_TIMEOUT = 60000;
    private static final long BONDING_TIMEOUT = 30000;
    private static final long PASSIVE_CONNECT_MIN_VALID_ATTEMPT_TIME = 1000;
    private static final long DELAY_BEFORE_CONNECTING_MS = 2000; // Re-connecting at once doesn't work
    private static final long LONG_GATT_CONNECT_TIMEOUT = 30 * 60 * 1000;
    private static final long SHORT_GATT_CONNECT_TIMEOUT = 60 * 1000;

    private final Handler mHandler = new Handler();
    private final Context mContext;

    private final Queue<GattCommand> mCommandList = new ArrayDeque<GattCommand>();
    private final BluetoothDevice mDevice;
    private final GattListener mListener;
    private GattConnectionAttemptListener mGattConnectionAttemptListener;

    private boolean mShouldBeDisconnected = false;
    private boolean mIsDisconnected = false;
    private boolean mHasBeenConnected = false;
    private boolean mIsBonding = false;
    private boolean mSkipBonding = false;
    private boolean mIsDebugEnabled = BuildConfig.DEBUG;
    private boolean mUseRefreshServices = false;
    private long mPassiveConnectTimestamp;
    private GattCommand mExecutingCommand = null;
    private BluetoothGatt mGatt;
    private GattConnectionAttempt mCurrentConnectionPhase;
    private final GattConnectionInstructor mGattConnectionInstructor;

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            onTimeout();
        }
    };

    private final Runnable mBondingRunnable = new Runnable() {
        @Override
        public void run() {
            onBondingTimeout();
        }
    };

    /*
     * Sometimes the call mGatt.connect() doesn't actually do anything, this timeout is for preventing getting stuck
     * in a state where the app thinks there is a passive connecting going on and there isn't
     */
    private final Runnable mConnectTooLongRunnable = new Runnable() {
        @Override
        public void run() {
            onConnectingTimeout();
        }
    };

    private final BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onBluetoothBondStateChanged(intent);
                }
            });
        }
    };

    private final BroadcastReceiver mAdapterStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            final int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
            if (state != prevState && state == BluetoothAdapter.STATE_ON) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBluetoothTurnedOn();
                    }
                });
            }
        }
    };

    GattConnection(final Context context,
                   final BluetoothDevice bluetoothDevice,
                   final GattListener listener,
                   final GattConnectionInstructor gattConnectionInstructor,
                   final boolean useRefreshServices) {
        mContext = context;
        mDevice = bluetoothDevice;
        mListener = listener;
        mPassiveConnectTimestamp = -PASSIVE_CONNECT_MIN_VALID_ATTEMPT_TIME;
        changeConnectionPhase(GattConnectionAttempt.IDLE);
        mGattConnectionInstructor = gattConnectionInstructor != null ? gattConnectionInstructor :
                new GattConnectionInstructor() {
                    @Override
                    public boolean shallContinueTrying(GattConnectionAttempt currentConnectionAttempt) {
                        return true;
                    }
                };

        setUseRefreshService(useRefreshServices);
    }

    public void setConnectionAttemptListener(GattConnectionAttemptListener listener) {
        mGattConnectionAttemptListener = listener;
    }

    public boolean isPassivelyTryingToConnect() {
        return mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_PASSIVE;
    }

    public boolean didConnectUsingActiveConnect() {
        return mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_ACTIVE_SUCCESS;
    }

    public void connect() {
        QRomLog.i(TAG, "connect()");
        if (mShouldBeDisconnected) {
            throw new IllegalStateException("GattConnection can't be re-used");
        }

        changeConnectionPhase(GattConnectionAttempt.WAITING_TO_START_CONNECTING);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doConnect();
            }
        }, DELAY_BEFORE_CONNECTING_MS);
    }

    private void doConnect() {
        QRomLog.i(TAG, "doConnect()");
        if (mShouldBeDisconnected) {
            QRomLog.i(TAG, "Not connecting to " + mDevice.getAddress() + " due to cancellation");
        } else {
            mContext.registerReceiver(mBondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            mContext.registerReceiver(mAdapterStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            // don't start a connection attempt if bluetooth is not enabled.
            // starting connection attempts when bluetooth is not enabled can set the
            // phones bluetooth stack in some serious problems on some devices
            if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                startActiveConnect();
            } else {
                QRomLog.i(TAG, "Bluetooth is not enabled, just wait for it to be enabled!");
            }
        }
    }

    private void startActiveConnect() {
        QRomLog.i(TAG, "Connecting to " + mDevice.getAddress());
        startConnectGattTimeout(SHORT_GATT_CONNECT_TIMEOUT);
        changeConnectionPhase(GattConnectionAttempt.CONNECTING_ACTIVE);

        /* Not declaring protocol will lead to Android trying to automatically detect
        protocol: BREDR or LE. Based on investigation this can cause problems trying to use the
        wrong protocol. This is also confirmed reading various forums and opensource. */
        // rick_Note:需要判断Android版本是否运行在6.0及以上
        if (Build.VERSION.SDK_INT >= 23) {// Build.VERSION_CODES.M) {
            mGatt = mDevice.connectGatt(mContext, false, mCallback);// , BluetoothDevice.TRANSPORT_LE);
        } else {
            mGatt = mDevice.connectGatt(mContext, false, mCallback);
        }

    }

    /**
     * Tries to disconnect as soon as possible
     * <p/>
     * If a command is currently executing, then we'll disconnect directly after that. Otherwise,
     * we disconnect immediately.
     */
    public void disconnect() {
        if (!mShouldBeDisconnected) {
            if (mIsDebugEnabled) QRomLog.i(TAG, "Disconnect");
            mListener.onDisconnecting(); // Tell the listener that we're starting to disconnect (it may take a while)
            mShouldBeDisconnected = true;
            if (mExecutingCommand == null) {
                doDisconnect();
            } else {
                if (mIsDebugEnabled) QRomLog.i(TAG, "Waiting for current command to finish");
                // We'll wait for the current command to finish or time out before calling
                // doDisconnect(). This happens later since mShouldBeDisconnected is true.
            }
        }
    }

    private void doDisconnect() {
        if (mGatt != null) {
            QRomLog.i(TAG, "Disconnecting...");
            startTimeout(COMMAND_TIMEOUT);
            mGatt.disconnect();
            if (!mHasBeenConnected) {
                close(); // We won't get a callback if we haven't been connected...
            }
        }
    }

    /**
     * Close the gatt object after we've disconnected
     * <p/>
     * It's best to not call this while we're running any GATT operations.
     */
    private void close() {
        QRomLog.i(TAG, "Closing...");
        if (!mIsDisconnected) {
            mIsDisconnected = true;

            if (!mShouldBeDisconnected) {
                mListener.onDisconnecting(); // Send out a disconnecting event before cleaning up ongoing commands
                mShouldBeDisconnected = true;
            }

            // Clean up the connection if there is one
            if (mGatt != null) {
                stopTimeout();
                for (GattCommand command : mCommandList) {
                    command.onError(new RuntimeException("Got disconnected"));
                }
                mCommandList.clear();

                if (mExecutingCommand != null) {
                    mExecutingCommand.onError(new RuntimeException("Got disconnected"));
                    mExecutingCommand = null;
                }

                try {
                    mGatt.close();
                } catch (Throwable error) {
                    QRomLog.i(TAG, "BluetoothGatt.close() threw: " + error);
                    // Ignore these... We don't want to handle them.
                }
                mGatt = null;
            }

            mContext.unregisterReceiver(mBondStateReceiver);
            mContext.unregisterReceiver(mAdapterStateReceiver);

            stopConnectGattTimeout();

            // Notify the listener even if we never actually connected
            mListener.onDisconnected();
        } else {
            QRomLog.i(TAG, "Already closed");
        }
    }

    private void onBluetoothBondStateChanged(final Intent intent) {
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device.getAddress().equals(mDevice.getAddress()) && device.getBondState() == BluetoothDevice.BOND_BONDED) {
            onDeviceWasBonded();
        }
    }

    private void onDeviceWasBonded() {
        stopBondingTimeout();
        mListener.onBonded();
        if (mIsBonding) {
            QRomLog.i(TAG, "Bonding finished. Discovering services.");
            mIsBonding = false;
            discoverServices();
        } else if (mExecutingCommand != null && mGatt != null) {
            QRomLog.i(TAG, "Bonded! Re-running current command.");
            mExecutingCommand.execute(mGatt);
        } else {
            QRomLog.i(TAG, "Bonded!");
        }
    }

    /**
     * Handle situations where bonding doesn't seem to work out
     * <p/>
     * This could indicate some completely unexpected issue, but it also happens when the other end is in bootloader
     * mode since it doesn't support bonding then. Therefore, we should try to continue and see what happens here.
     * <p/>
     * The most likely outcome is that we will find out that we're in "DFU mode" where a previous DFU has failed for
     * some reason and we need to retry that. It's not possible to know that at this point, however.
     */
    private void onBondingTimeout() {
        if (mIsBonding) {
            QRomLog.i(TAG, "Bonding timed out. Discovering services.");
            mIsBonding = false;
            discoverServices();
        }
    }

    private void onConnectingTimeout() {
        QRomLog.i(TAG, "Connection took too long, terminate and retry the whole connection!.");
        if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_ACTIVE) {
            changeConnectionPhase(GattConnectionAttempt.CONNECTING_ACTIVE_TIMED_OUT_FORCED);
        } else {
            changeConnectionPhase(GattConnectionAttempt.CONNECTING_PASSIVE_TIMED_OUT);
        }
        close();
    }

    private void onBluetoothTurnedOn() {
        QRomLog.i(TAG, "Bluetooth was turned on. Lets retry the whole connection!");
        close();
    }

    public boolean hasGattService(final UUID service) {
        return mGatt != null && mGatt.getService(service) != null;
    }

    public List<UUID> getGattServices() {
        List<UUID> services = new ArrayList<UUID>();

        if (mGatt != null) {
            for (final BluetoothGattService s : mGatt.getServices()) {
                services.add(s.getUuid());
            }
        }

        return services;
    }

    public boolean hasGattCharacteristic(final UUID service, final UUID characteristic) {
        return mGatt != null && mGatt.getService(service) != null &&
                mGatt.getService(service).getCharacteristic(characteristic) != null;
    }

    public boolean refreshServices() {
        return GattRefresh.refreshServices(mGatt);
    }

    public void read(final UUID service,
                     final UUID characteristic,
                     final ReadCallback callback) {
        if (mIsDebugEnabled) QRomLog.i(TAG, "read: " + service + " / " + characteristic);
        final BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            GattCommand command = new GattReadCommand(c, callback);
            runCommand(command);
        } else {
            QRomLog.i(TAG, "Read failed!");
            if (callback != null) {
                callback.onError(new RuntimeException("Couldn't find characteristic!"));
            }
        }
    }

    public void write(final UUID service,
                      final UUID characteristic,
                      final byte[] data,
                      final Callback<Void> callback) {
        if (mIsDebugEnabled) QRomLog.i(TAG, "write: " + service + " / " + characteristic);
        final BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            GattCommand command = new GattWriteCommand(c, data, callback, mIsDebugEnabled);
            runCommand(command);
        } else {
            QRomLog.i(TAG, "Write failed!");
            if (callback != null) {
                callback.onError(new RuntimeException("Write failed. Didn't find characteristic!"));
            }
        }
    }

    public void setDebugMode(final boolean enable) {
        mIsDebugEnabled = enable;
    }

    public void setUseRefreshService(final boolean enable) {
        mUseRefreshServices = enable;

        QRomLog.i(TAG, enable ? "Using refreshServices" : "Using removeBond");
    }

    void setNotification(final UUID service,
                         final UUID characteristic,
                         final Callback<Void> callback) {
        QRomLog.i(TAG, "Set notification: " + service + " / " + characteristic);
        final BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            GattCommand command = new GattSetNotificationCommand(c, true, callback);
            runCommand(command);
        } else {
            QRomLog.i(TAG, "Setting notification failed!");
            if (callback != null) {
                callback.onError(new RuntimeException("Didn't find characteristic!"));
            }
        }
    }

    private void runCommand(final GattCommand command) {
        if (mShouldBeDisconnected) {
            if (mIsDebugEnabled) QRomLog.i(TAG, "Rejecting new command since we're disconnecting");
            command.onError(new RuntimeException("Disconnecting"));
        } else if (mExecutingCommand == null) {
            if (mIsDebugEnabled) QRomLog.i(TAG, "Starting command directly: " +
                    command.getClass().getSimpleName());
            mExecutingCommand = command;
            startTimeout(COMMAND_TIMEOUT);
            command.execute(mGatt);
        } else {
            if (mIsDebugEnabled) QRomLog.i(TAG, "Queuing command");
            mCommandList.add(command);
        }
    }

    private BluetoothGattCharacteristic getCharacteristic(final UUID service,
                                                          final UUID characteristic) {
        final BluetoothGattService gattService = mGatt != null ? mGatt.getService(service) : null;

        if (gattService != null) {
            final BluetoothGattCharacteristic c = gattService.getCharacteristic(characteristic);
            if (c != null) {
                return c;
            } else {
                QRomLog.i(TAG, "Characteristic not found: " + characteristic);
            }
        } else {
            QRomLog.i(TAG, "Service not found: " + service);
        }

        return null;
    }

    private void onCharacteristicRead(final byte[] value, final int status) {
        if (mExecutingCommand == null) return;
        if (mIsDebugEnabled) QRomLog.i(TAG, "Characteristic read (status = " + status + ")");

        if (status == BluetoothGatt.GATT_SUCCESS) {
            final boolean retry = mExecutingCommand.onRead(value);
            if (retry) {
                retryCommand();
            } else {
                onSuccessfulCommand();
            }
        }
    }

    private void onCharacteristicWrite(final int status) {
        if (mExecutingCommand == null) return;
        if (mIsDebugEnabled) QRomLog.i(TAG, "Characteristic written (status = " + status + ")");

        if (status == BluetoothGatt.GATT_SUCCESS) {
            mExecutingCommand.onWrite();
            onSuccessfulCommand();
        }
    }

    private void onDescriptorWrite(final int status) {
        if (mExecutingCommand == null) return;
        if (mIsDebugEnabled) QRomLog.i(TAG, "Descriptor written (status = " + status + ")");

        if (status == BluetoothGatt.GATT_SUCCESS) {
            mExecutingCommand.onDescriptorWrite();
            onSuccessfulCommand();
        }
    }

    private void retryCommand() {
        if (mShouldBeDisconnected) {
            mExecutingCommand.onError(new RuntimeException("Not retrying due to disconnect request"));
            mExecutingCommand = null;
            doDisconnect();
        } else {
            QRomLog.i(TAG, "Retrying command...");
            mExecutingCommand.execute(mGatt);
        }
    }

    private void onSuccessfulCommand() {
        if (mIsDebugEnabled) QRomLog.i(TAG, "Command succeeded");
        stopTimeout();

        if (mShouldBeDisconnected) {
            mExecutingCommand = null;
            doDisconnect();
            return;
        }

        mExecutingCommand = mCommandList.poll();

        if (mExecutingCommand != null) { // queue not empty
            if (mIsDebugEnabled) QRomLog.i(TAG, "Executing queued command: " +
                    mExecutingCommand.getClass().getSimpleName());
            startTimeout(COMMAND_TIMEOUT);
            mExecutingCommand.execute(mGatt);
        } else {
            QRomLog.i(TAG, "No command in queue");
        }
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

    private void onCharacteristicChanged(final UUID service,
                                         final UUID characteristic,
                                         final byte[] value) {
        if (mGatt == null) return;
        if (mIsDebugEnabled) QRomLog.i(TAG, "Characteristic changed: " + characteristic);
        mListener.onCharacteristicChanged(service, characteristic, value);
    }

    private void onConnectionStateChange(final int newState, final int status) {
        QRomLog.i(TAG, "onConnectionStateChange (newState:" + newState + ", status:" + status + ")");
        stopConnectGattTimeout();
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            QRomLog.i(TAG, "onConnectionStateChange  STATE_CONNECTED");
            if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_ACTIVE) {
                QRomLog.i(TAG, "onConnectionStateChange  01");
                changeConnectionPhase(GattConnectionAttempt.CONNECTING_ACTIVE_SUCCESS);
            } else if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_PASSIVE) {
                QRomLog.i(TAG, "onConnectionStateChange  02");
                changeConnectionPhase(GattConnectionAttempt.CONNECTING_PASSIVE_SUCCESS);
            } else {
                QRomLog.i(TAG, "onConnectionStateChange  03");
                changeConnectionPhase(GattConnectionAttempt.IDLE); // best to do
                QRomLog.i(TAG, "Weird connectionPhaseState, should not happen: " +
                        mCurrentConnectionPhase);
            }
            QRomLog.i(TAG, "Connected (status = " + status + ")");
            mHasBeenConnected = true;
            createBondIfNeeded();
            if (!mIsBonding) {
                discoverServices();
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            QRomLog.i(TAG, "onConnectionStateChange  STATE_DISCONNECTED");
            QRomLog.i(TAG, "Disconnected (status = " + status + ")");
            // Stop all time-out timers (no reason when we're disconnected!)
            stopTimeout();
            stopBondingTimeout();
            // What to do next?
            if (mIsBonding && !mShouldBeDisconnected && mGatt != null && !mSkipBonding) {
                QRomLog.i(TAG, "Bonding seemed to trigger a disconnect. Retry without bond request.");
                mHasBeenConnected = false;
                mIsBonding = false;
                mSkipBonding = true;
                startPassiveConnect();
            } else if (mHasBeenConnected || mShouldBeDisconnected) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    QRomLog.i(TAG, "Disconnected with an error code. (Don't) Remove bond here.");
                }
                close();
            } else if ((status != BluetoothGatt.GATT_SUCCESS
                    && mDevice.getBondState() != BluetoothDevice.BOND_NONE)
                    || isInvalidPassiveConnectAttempt()) {
                if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_ACTIVE) {
                    changeConnectionPhase(GattConnectionAttempt.CONNECTING_ACTIVE_FAILED);
                } else if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_PASSIVE) {
                    changeConnectionPhase(GattConnectionAttempt.CONNECTING_PASSIVE_FAILED);
                }
                QRomLog.i(TAG, "Got directly disconnected with an error code. Removing bond and try again.");

                if (mUseRefreshServices) {
                    refreshServices();
                } else {
                    removeBond();
                }
                close();
            } else if (mGatt != null) {
                if (mCurrentConnectionPhase == GattConnectionAttempt.CONNECTING_ACTIVE) {
                    changeConnectionPhase(GattConnectionAttempt.CONNECTING_ACTIVE_TIMED_OUT);
                }

                if (mGattConnectionInstructor.shallContinueTrying(mCurrentConnectionPhase)) {
                    QRomLog.i(TAG, "Starting a passive connection to " + mDevice.getAddress());
                    startPassiveConnect();
                } else {
                    close();
                }
            }
        } else {
            QRomLog.e(TAG, "Unknown connection state!");
        }

        mListener.onConnectionStateChange(newState, status);
    }

    private void startPassiveConnect() {
        changeConnectionPhase(GattConnectionAttempt.CONNECTING_PASSIVE);
        mPassiveConnectTimestamp = SystemClock.uptimeMillis();
        startConnectGattTimeout(LONG_GATT_CONNECT_TIMEOUT);
        mGatt.connect();
    }

    private void startConnectGattTimeout(final long timeoutTime) {
        QRomLog.i(TAG, "Starting connection timeout with timeoutTime: " + timeoutTime + " ...");
        mHandler.removeCallbacks(mConnectTooLongRunnable);
        mHandler.postDelayed(mConnectTooLongRunnable, timeoutTime);
    }

    private void stopConnectGattTimeout() {
        QRomLog.i(TAG, "Stopping connection timeout...");
        mHandler.removeCallbacks(mConnectTooLongRunnable);
    }

    private void createBondIfNeeded() {
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            QRomLog.i(TAG, "Already bonded");
        } else if (mSkipBonding) {
            QRomLog.i(TAG, "Skipping bonding...");
        } else if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            QRomLog.i(TAG, "Device is bonding");
            mIsBonding = true;
        } else {
            QRomLog.i(TAG, "Requesting bonding");
            final boolean result = mDevice.createBond();
            if (result) {
                mIsBonding = true;
            } else {
                QRomLog.e(TAG, "createBond() failed");
            }
        }
        if (mIsBonding) {
            startBondingTimeout(); // If bonding with the device fails, we can only detect it using a timeout.
        }
    }

    private void startBondingTimeout() {
        QRomLog.i(TAG, "Starting bonding timeout...");
        mHandler.removeCallbacks(mBondingRunnable);
        mHandler.postDelayed(mBondingRunnable, BONDING_TIMEOUT);
    }

    private void stopBondingTimeout() {
        QRomLog.i(TAG, "Stopping bonding timeout...");
        mHandler.removeCallbacks(mBondingRunnable);
    }

    private void removeBond() {
        if (mDevice.getBondState() != BluetoothDevice.BOND_NONE) {
            try {
                final Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
                boolean result = (Boolean) removeBondMethod.invoke(mDevice);
                if (!result) {
                    QRomLog.i(TAG, "Failed to remove bond");
                }
            } catch (IllegalAccessException e) {
                QRomLog.i(TAG, "Failed to remove bond", e);
            } catch (NoSuchMethodException e) {
                QRomLog.i(TAG, "Failed to remove bond", e);
            } catch (InvocationTargetException e) {
                QRomLog.i(TAG, "Failed to remove bond", e);
            }
        }
    }

    // TODO discover services failes sometimes and gets stopped by timeout, why? can we fix?
    private void discoverServices() {
        GattCommand command = new GattDiscoverServicesCommand(new Callback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                mListener.onConnected();
            }

            @Override
            public void onError(final Throwable error) {
                // TODO cancel timeout if we know we failed
            }
        });
        runCommand(command);
    }

    // TODO: handle multiple onServicesDiscovered() calls robustly
    private void onServicesDiscovered(final int status) {
        QRomLog.i(TAG, "Services discovered");
        if (mExecutingCommand == null) return;

        if (status == BluetoothGatt.GATT_SUCCESS) {
            mExecutingCommand.onServicesDiscovered();
            onSuccessfulCommand();
        }
    }

    private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         final int status) {
            QRomLog.i(TAG, "BluetoothGattCallback::onCharacteristicRead");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onCharacteristicRead(
                            cloneCharacteristicValue(characteristic), // clone, since it's not immutable
                            status);
                }
            });

        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic,
                                          final int status) {
            QRomLog.i(TAG, "BluetoothGattCallback::onCharacteristicWrite");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onCharacteristicWrite(status);
                }
            });

        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            QRomLog.i(TAG, "BluetoothGattCallback::onCharacteristicChanged");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onCharacteristicChanged(
                            characteristic.getService().getUuid(),
                            characteristic.getUuid(),
                            cloneCharacteristicValue(characteristic)); // clone, since it's not immutable
                }
            });
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt,
                                      final BluetoothGattDescriptor descriptor,
                                      final int status) {
            QRomLog.i(TAG, "BluetoothGattCallback::onDescriptorWrite");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onDescriptorWrite(status);
                }
            });
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            final int status,
                                            final int newState) {
            QRomLog.i(TAG, "BluetoothGattCallback::onConnectionStateChange status：" + status + " newState=" + newState);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onConnectionStateChange(newState, status);
                }
            });
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            QRomLog.i(TAG, "BluetoothGattCallback::onServicesDiscovered");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    GattConnection.this.onServicesDiscovered(status);
                }
            });
        }
    };

    private byte[] cloneCharacteristicValue(final BluetoothGattCharacteristic characteristic) {
        if (characteristic == null || characteristic.getValue() == null) {
            return new byte[0];
        }
        return characteristic.getValue().clone();
    }

    private void onTimeout() {
        QRomLog.i(TAG, "Timed out!");
        if (mExecutingCommand != null) {
            mExecutingCommand.onError(new RuntimeException("Timeout"));
            mExecutingCommand = null;
            disconnect();
        } else {
            close(); // Timeout while disconnecting...
        }
    }

    private void startTimeout(final long timeoutMs) {
        if (mIsDebugEnabled) QRomLog.i(TAG, "Starting timeout");
        mHandler.postDelayed(mTimeoutRunnable, timeoutMs);
    }

    private void stopTimeout() {
        if (mIsDebugEnabled) QRomLog.i(TAG, "Canceling timeout");
        mHandler.removeCallbacks(mTimeoutRunnable);
    }

    /**
     * Checks whether a connection attempt is passive and invalid
     * <p>
     * A passive connection attempt shall not fail directly, but during some
     * circumstances it does, then it is better to know that and just close the
     * GattConnection.
     *
     * @return true if it is a passive connect attempt and it is invalid
     */
    private boolean isInvalidPassiveConnectAttempt() {
        return SystemClock.uptimeMillis() - mPassiveConnectTimestamp <
                PASSIVE_CONNECT_MIN_VALID_ATTEMPT_TIME;
    }

    private void changeConnectionPhase(GattConnectionAttempt newConnectionPhase) {
        mCurrentConnectionPhase = newConnectionPhase;
        if (mGattConnectionAttemptListener != null) {
            mGattConnectionAttemptListener.onConnectionPhaseChanged(newConnectionPhase);
        }
    }

    interface GattConnectionInstructor {

        boolean shallContinueTrying(GattConnectionAttempt currentConnectionAttempt);

    }
}