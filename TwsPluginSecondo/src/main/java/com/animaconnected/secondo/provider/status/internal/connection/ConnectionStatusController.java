package com.animaconnected.secondo.provider.status.internal.connection;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.animaconnected.secondo.provider.status.StatusChangeListener;
import com.animaconnected.secondo.provider.status.StatusController;
import com.animaconnected.secondo.provider.status.StatusModel;
import com.animaconnected.bluetooth.device.DeviceConnectionListener;
import com.animaconnected.watch.WatchProvider;

public class ConnectionStatusController implements StatusController, DeviceConnectionListener {

    private final Handler mHandler = new Handler();
    private final StatusChangeListener mListener;
    private final WatchProvider mWatch;
    private StatusModel mStatus = null;

    public ConnectionStatusController(final StatusChangeListener listener,
                                      final WatchProvider watch,
                                      final Context context) {
        mListener = listener;
        mWatch = watch;
        mWatch.registerDeviceConnectionListener(this); // Listen forever

        context.registerReceiver(
                new BroadcastReceiver() {
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
                },
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        update();
    }

    private void update() {
        mStatus = createStatus();
        mListener.onStatusChanged();
    }

    private StatusModel createStatus() {
        final boolean isBluetoothEnabled = BluetoothAdapter.getDefaultAdapter().isEnabled();

        if (!isBluetoothEnabled) {
            return new BluetoothDisabledStatus();
        } else if (!isConnected()) {
            return new DisconnectedStatus();
        } else {
            return null;
        }
    }

    private boolean isConnected() {
        return mWatch.isConnected();
    }

    @Override
    public StatusModel getCurrent() {
        return mStatus;
    }

    @Override
    public void onConnecting() {
    }

    @Override
    public void onConnected() {
        update();
    }

    @Override
    public void onDisconnected() {
        update();
    }

    @Override
    public void onHardToConnect() {
        update();
    }

    @Override
    public void onEnterDfuMode() {
        update();
    }

    @Override
    public void onLeaveDfuMode() {
        update();
    }

    private void onBluetoothToggled() {
        update();
    }
}
