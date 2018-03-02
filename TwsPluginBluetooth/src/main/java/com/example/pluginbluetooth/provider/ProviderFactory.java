package com.example.pluginbluetooth.provider;

import android.content.Context;
import android.support.annotation.NonNull;

import com.example.pluginbluetooth.SecondoApplication;
import com.example.pluginbluetooth.app.DeviceManager;
import com.example.pluginbluetooth.watch.WatchProvider;

/**
 * Created by Administrator on 2018/3/1.
 */

public class ProviderFactory {
    private static WatchProvider sWatchProvider;

    public static BluetoothOnboardingProvider createBluetoothOnboardingProvider() {
        return new BluetoothOnboardingProviderImpl(getWatch());
    }

    public static WatchProvider getWatch() {
        if (sWatchProvider == null) {
            sWatchProvider = createWatch();
        }
        return sWatchProvider;
    }

    public static Context getContext() {
        return SecondoApplication.getContext();
    }
    @NonNull
    private static WatchProvider createWatch() {
        final WatchProvider provider = new WatchProvider(getContext());

        // Set up DeviceManager as a listener from the very start
        final DeviceManager deviceManager = new DeviceManager(getContext());
        deviceManager.listenTo(provider);

        return provider;
    }
}
