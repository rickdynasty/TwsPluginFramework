package com.example.pluginbluetooth.screens.onboarding;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.pluginbluetooth.SecondoApplication;
import com.example.pluginbluetooth.bluetooth.device.DeviceConnectionListener;
import com.example.pluginbluetooth.bluetooth.device.WatchDevice;
import com.example.pluginbluetooth.bluetooth.device.scanner.WatchScanner;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.provider.BluetoothOnboardingProvider;
import com.example.pluginbluetooth.provider.ProviderFactory;
import com.example.pluginbluetooth.utils.Bonding;
import com.example.pluginbluetooth.watch.BaseWatchProviderListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2018/3/1.
 */

public class Onboarding implements WatchScanner.WatchScannerListener, DeviceConnectionListener {
    private static final String TAG = "rick_Print:Onboarding";

    private static final String SHARED_PREFS_NAME = "onboarding_shared_prefs";
    private static final String PREFS_KEY_PREVIOUS_ONBOARDED_DEVICE = "previous_onboarded_device";
    private static final String SP_PAIR = "sp_pair";
    private static final String KEY_HAS_PAIR_SUCCESS = "key_has_pair_success";
    private static final int WELCOME_DURATION_MS = 2500;
    private static final int SCAN_MINIMUM_DURATION_MS = 11000;
    private static final int CONNECTING_TIMEOUT_MS = 90000;

    private static Onboarding sInstance;
    private final Context mContext;

    private WatchScanner mWatchScanner;

    private State mState = State.PAUSED;
    private State mPreviousState = State.PAUSED;

    private final BaseWatchProviderListener mWatchProviderListener = new BaseWatchProviderListener() {
        @Override
        public void onWroteDeviceSettings() {
            QRomLog.i(TAG, "Device settings written. Updating state.");
            updateState();
        }
    };

    private final BluetoothOnboardingProvider mProvider;
    private final Set<GattDevice> mDevices = new HashSet<GattDevice>();

    private OnboardingChangeListener mListener;

    private Set<State> mVisitedStateSet;
    private boolean mLastKnownInternetAvailable;

    public enum State {
        BLUETOOTH_ENABLING,     // 蓝牙不可用(系统开关没开)
        LOCATION_ENABLING,      // 定位不可用(系统开关没开)
        LOCATION_PERMISSION,    // 没定位权限
        SCANNING,               //扫描ing
        CONNECTING,             //链接ing
        CONNECTED,              //已链接
        FINISHING, // calibration check for instance
        FINISHED,
        FAIL,
        PAUSED,
        CANCEL;

        private final boolean mRequiresInternet;

        State() {
            this(false);
        }

        State(boolean requiresInternet) {
            mRequiresInternet = requiresInternet;
        }

        boolean isRequiringInternet() {
            return mRequiresInternet;
        }
    }

    private String getStateDes(State state) {
        switch (state) {
            case BLUETOOTH_ENABLING:     // 蓝牙不可用(系统开关没开)
                return "BLUETOOTH_ENABLING";
            case LOCATION_ENABLING:      // 定位不可用(系统开关没开)
                return "LOCATION_ENABLING";
            case LOCATION_PERMISSION:    // 没定位权限
                return "LOCATION_PERMISSION";
            case SCANNING:               //扫描ing
                return "SCANNING";
            case CONNECTING:             //链接ing
                return "CONNECTING";
            case CONNECTED:              //已链接
                return "CONNECTED";
            case FINISHING: // calibration check for instance
                return "FINISHING";
            case FINISHED:
                return "FINISHED";
            case FAIL:
                return "FAIL";
            case PAUSED:
                return "PAUSED";
            case CANCEL:
                return "CANCEL";
            default:
                return "unKown";
        }
    }

    private final Handler mHandler = new Handler();
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            onTimer();
        }
    };

    private final BroadcastReceiver mAdapterStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            final int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
            if (state != prevState) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBluetoothToggled();
                    }
                });
            }
        }
    };

    private final BroadcastReceiver mConnectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateInternetConnectivityEnabled(false);
                }
            });
        }
    };

    public Onboarding(@NonNull final BluetoothOnboardingProvider provider, @NonNull final Context context) {
        QRomLog.i(TAG, "Onboarding");
        mProvider = provider;

        mContext = context;
        mVisitedStateSet = new HashSet<State>();
        mWatchScanner = new WatchScanner(mContext);

        updateState();
    }

    public void setChangeListener(final OnboardingChangeListener listener) {
        mListener = listener;
    }

    public State getState() {
        return mState;
    }

    public State getPreviousState() {
        return mPreviousState;
    }

    public void resume() {
        QRomLog.i(TAG, "resume");
        updateState();
    }

    public void pause() {
        gotoState(State.PAUSED);
    }

    public boolean hasVisitedState(State state) {
        return mVisitedStateSet.contains(state);
    }

    public void onLocationEnabled() {
        Log.d(TAG, "Location has been enabled. Updating state.");
        updateState();
    }

    public void updateState() {
        final State nextState = getNextState(); // This could be the current state and then we leave and re-enter!

        gotoState(nextState);
    }

    public static Onboarding getInstance() {
        // Onboarding instance is not thread safe
        if (sInstance == null) {
            sInstance = new Onboarding(ProviderFactory.createBluetoothOnboardingProvider(),
                    SecondoApplication.getContext());
        }

        return sInstance;
    }

    interface OnboardingChangeListener {

        void onOnboardingStateChanged();

        void foundOneDeviceWhenScanning();

        void onInternetConnectivityChanged(boolean requiredInState, boolean enabled);
    }

    private boolean isDeviceBonded() {
        WatchDevice watchDevice = ProviderFactory.getWatch().getDevice();
        if (watchDevice != null) {
            String address = watchDevice.getAddress();
            if (address != null) {
                return Bonding.isDeviceBonded(address);
            }
        }
        return false;
    }

    private void forgetCurrentDeviceIfNeeded() {
        QRomLog.i(TAG, "forgetCurrentDeviceIfNeeded");
        if (!isConnected() && !isInOtaMode() && !isDeviceBonded()) {
            QRomLog.i(TAG, "Failed to connect. Forgetting device.");
            ProviderFactory.getWatch().forgetDevice();
        }
    }

    public void finishOnboarding() {
        QRomLog.i(TAG, "finishOnboarding");
        ProviderFactory.getWatch().setOnboardingFinished(true);
        updateState();
    }

    public void startFinishingOnboarding() {
        QRomLog.i(TAG, "startFinishingOnboarding");
        savePreviousTriedDevice(null);
        // Check if the watch is in DFU mode or got disconnected during the timeout
        if (!ProviderFactory.getWatch().isConnected()) {
            finishOnboarding();
        } else {
            gotoState(State.FINISHING);
        }
    }

    private State getNextState() {
        if (onboardingIsFinished()) {
            return State.FINISHED;
        } else if (!isBluetoothEnabled()) {
            return State.BLUETOOTH_ENABLING;
        } else if (!isLocationEnabled()) {
            return State.LOCATION_ENABLING;
        } else if (!hasLocationPermission()) {
            return State.LOCATION_PERMISSION;
        } else if (!hasDevice()) {
            return State.SCANNING;
        } else if (!isConnected() || !getWroteOnboardingDeviceSettings()) {
            return State.CONNECTING;
        } else {
            return State.CONNECTED;
        }
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().getDefaultAdapter().isEnabled();
    }

    public boolean isLocationEnabled() {
        try {
            final int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            QRomLog.i(TAG, "Failed to get location settings. Assuming that it's on.");
            return true;
        }
    }

    public void onLocationPermissionGranted() {
        Log.d(TAG, "Location permission granted. Updating state.");
        updateState();
    }

    private boolean onboardingIsFinished() {
        return mProvider.isOnboardingFinished();
    }

    private boolean hasDevice() {
        return mProvider.hasDevice();
    }

    public boolean foundOneDeviceWhenScanning() {
        return mWatchScanner.hasOneDeviceBeenFound();
    }

    public boolean isConnected() {
        return mProvider.isConnected();
    }

    public boolean isInOtaMode() {
        return mProvider.isInOtaMode();
    }

    private boolean getWroteOnboardingDeviceSettings() {
        return mProvider.getWroteOnboardingDeviceSettings();
    }

    public void gotoState(final State state) {
        QRomLog.i(TAG, "Changing state: " + getStateDes(mState) + " => " + getStateDes(state));
        final boolean stateChanged = mState != state;
        leaveState(mState);
        mState = state;
        enterState(mState);
        if (mListener != null && stateChanged) {
            mListener.onOnboardingStateChanged();
            mLastKnownInternetAvailable = isInternetAccessEnabled();
            mListener.onInternetConnectivityChanged(mState.isRequiringInternet(), mLastKnownInternetAvailable);
        }
    }

    private void leaveState(final State state) {
        QRomLog.i(TAG, "leaveState: " + getStateDes(state));
        mPreviousState = state;
        switch (state) {
            case BLUETOOTH_ENABLING:
                mContext.unregisterReceiver(mAdapterStateReceiver);
                break;
            case SCANNING:
                stopScan();
                break;
            case CONNECTING:
                stopConnecting();
                break;
            default:
                break; // Ignore. Not all states do something when leaving.
        }

        if (state.isRequiringInternet()) {
            mContext.unregisterReceiver(mConnectivityChangedReceiver);
        }
    }

    private void enterState(final State state) {
        QRomLog.i(TAG, "enterState: " + getStateDes(state));
        mVisitedStateSet.add(state);
        switch (state) {
            case BLUETOOTH_ENABLING:
                mContext.registerReceiver(mAdapterStateReceiver,
                        new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                break;
            case SCANNING:
                ProviderFactory.getWatch().setOnboardingStarted();
                cleanUpAnyPreviousTriedDevice();
                startScan();
                break;
            case CONNECTING:
                startConnecting();
                break;
            default:
                break; // Ignore. Not all states do something when entering.
        }

        if (state.isRequiringInternet()) {
            mContext.registerReceiver(mConnectivityChangedReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void onTimer() {
        switch (mState) {
            case CONNECTING:
                QRomLog.i(TAG, "Connecting timed out");
                forgetCurrentDeviceIfNeeded();
                gotoState(State.FAIL);
                break;
            default:
                break; // Ignore. Not all states use the timer.
        }
    }

    private void startTimer(final int milliseconds) {
        QRomLog.i(TAG, "startTimer");
        mHandler.removeCallbacks(mTimerRunnable);
        mHandler.postDelayed(mTimerRunnable, milliseconds);
    }

    private void stopTimer() {
        mHandler.removeCallbacks(mTimerRunnable);
    }

    public void startScan() {
        QRomLog.i(TAG, "call startScan 开始扫描...");
        mWatchScanner.registerListener(this);

        mWatchScanner.startScan();
    }

    public void stopScan() {
        QRomLog.i(TAG, "stopScan");
        mWatchScanner.unregisterListener(this);

        mWatchScanner.stopScan();
    }

    public boolean isInternetAccessEnabled() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private GattDevice selectGattDevice() {
        final List<GattDevice> devices = new ArrayList<GattDevice>(mDevices);

        Collections.sort(devices, new Comparator<GattDevice>() {
            @Override
            public int compare(final GattDevice lhs, final GattDevice rhs) {
                return Integer.compare(rhs.getRssi(), lhs.getRssi());
            }
        });

        for (GattDevice gattDevice : devices) {
            QRomLog.i(TAG, "Found device: " + gattDevice.getAddress() + " " + gattDevice.getRssi());
        }

        if (mDevices.isEmpty()) {
            throw new RuntimeException("No devices! This should never happen.");
        }
        return devices.get(0);
    }

    private void setDevice(final GattDevice device) {
        QRomLog.i(TAG, "setDevice .. ");
        if (device != null) {
            String address = device.getAddress();
            if (address != null) {
                savePreviousTriedDevice(address);
            }
        }
        mProvider.setGattDevice(device);
    }

    private void startConnecting() {
        QRomLog.i(TAG, "startConnecting");
        mProvider.registerDeviceConnectionListener(this);
        ProviderFactory.getWatch().registerListener(mWatchProviderListener);
        if (isConnected() && getWroteOnboardingDeviceSettings() && !isInOtaMode()) {
            QRomLog.i(TAG, "Already connected. Updating state.");
            updateState();
        }
        startTimer(CONNECTING_TIMEOUT_MS);
    }

    private void stopConnecting() {
        QRomLog.i(TAG, "stopConnecting");
        mProvider.unregisterDeviceConnectionListener(this);
        ProviderFactory.getWatch().unregisterListener(mWatchProviderListener);
        stopTimer();
        // Don't continue connecting in the background
        forgetCurrentDeviceIfNeeded();
    }

    private void onBluetoothToggled() {
        QRomLog.i(TAG, "onBluetoothToggled");
        if (mState == State.BLUETOOTH_ENABLING) {
            QRomLog.i(TAG, "Bluetooth toggled. Updating state.");
            updateState();
        } else {
            QRomLog.i(TAG, "Bluetooth was toggled in unexpected state " + mState);
        }
    }

    /**
     * Checks the current internet connectivity and let any listener knows the status
     *
     * @param forceCallback always report status to listener if this is true, otherswise only report if
     *                      internet connectivity or state has changed
     */
    public void updateInternetConnectivityEnabled(boolean forceCallback) {
        boolean isInternetAvailable = isInternetAccessEnabled();
        if (isInternetAvailable != mLastKnownInternetAvailable || forceCallback) {
            mLastKnownInternetAvailable = isInternetAvailable;
            if (mListener != null) {
                mListener.onInternetConnectivityChanged(mState.isRequiringInternet(), mLastKnownInternetAvailable);
            }
        }
    }

    private void cleanUpAnyPreviousTriedDevice() {
        String address = getPreviousTriedDevice();
        if (address != null) {
            Bonding.removeBondFromDevice(address);
            savePreviousTriedDevice(null);
        }
    }

    private String getPreviousTriedDevice() {
        Context context = SecondoApplication.getContext();
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).getString(
                PREFS_KEY_PREVIOUS_ONBOARDED_DEVICE, null);
    }

    private void savePreviousTriedDevice(String address) {
        Context context = SecondoApplication.getContext();
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().putString(
                PREFS_KEY_PREVIOUS_ONBOARDED_DEVICE, address).apply();
    }


    public static void setHasPairSuccess(boolean hasPairSuccess) {
        Context context = SecondoApplication.getContext();
        context.getSharedPreferences(SP_PAIR, Context.MODE_PRIVATE).edit().putBoolean(
                KEY_HAS_PAIR_SUCCESS, hasPairSuccess).apply();
    }

    @Override
    public void onScanFirstWatchFound() {
        QRomLog.i(TAG, "onScanFirstWatchFound");
        mListener.foundOneDeviceWhenScanning();
    }

    @Override
    public void onScanFinished(GattDevice device) {
        QRomLog.i(TAG, "onScanFinished");
        if (device != null) {
            // Remove bonding information before attempting to connect to device
            try {
                device.removeBond();
            } catch (Exception e) {
                QRomLog.i(TAG, "Remove bond failed", e);
            }

            // Set the selected device as our globally used device
            setDevice(device);

            // Continue to the next state
            QRomLog.i(TAG, "Continuing to next state after setting device");
        }

        if (device == null) {
            gotoState(State.FAIL);
        } else {
            updateState();
        }
    }

    @Override
    public void onConnecting() {
        QRomLog.i(TAG, "Connecting");
    }

    @Override
    public void onConnected() {
        QRomLog.i(TAG, "Connected. Updating state.");
        ProviderFactory.getWatch().getDeviceInformation(); // Start this read here so it's cached when we enter app
        updateState();
    }

    @Override
    public void onDisconnected() {
        QRomLog.i(TAG, "onDisconnected");
    }

    @Override
    public void onHardToConnect() {
        QRomLog.i(TAG, "onHardToConnect");
    }

    @Override
    public void onEnterOtaMode() {
        QRomLog.i(TAG, "onEnterOtaMode");
    }

    @Override
    public void onLeaveOtaMode() {
        QRomLog.i(TAG, "onLeaveOtaMode");
    }
}
