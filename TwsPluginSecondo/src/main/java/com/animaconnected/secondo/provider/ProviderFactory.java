package com.animaconnected.secondo.provider;

import android.content.Context;
import android.support.annotation.NonNull;

import com.animaconnected.SecondoApplication;
import com.animaconnected.secondo.provider.status.StatusProvider;
import com.animaconnected.secondo.provider.status.internal.StatusProviderImpl;
import com.animaconnected.secondo.utils.ThreadUtils;
import com.animaconnected.watch.WatchProvider;

/**
 * Created by Administrator on 2018/2/27.
 */

public class ProviderFactory {
    private static StatusProvider sStatusProvider;
    private static WatchProvider sWatchProvider;

    private ProviderFactory() {
    }

    public static Context getContext() {
        return SecondoApplication.getContext();
    }

    public static StatusProvider getStatusProvider() {
        ThreadUtils.assertIsOnMainThread();
        if (sStatusProvider == null) {
            sStatusProvider = createStatusProvider();
        }
        return sStatusProvider;
    }

    @NonNull
    private static StatusProviderImpl createStatusProvider() {
        final StatusProviderImpl provider = new StatusProviderImpl();

        // Set up all controllers
//        provider.addController(new ConnectionStatusController(provider, getWatch(), getContext()));
//        provider.addController(new DfuStatusController(provider, getBackgroundUpdateChecker(), getWatch(), getContext(),
//                ProviderFactory.getWatchUpdateProvider()));
//        provider.addController(new DistressStatusController(provider, getContext()));
//        provider.addController(new DeviceBatteryStatusController(provider, getWatch()));

        return provider;
    }

    public static WatchProvider getWatch() {
        ThreadUtils.assertIsOnMainThread();
        if (sWatchProvider == null) {
            sWatchProvider = createWatch();
        }
        return sWatchProvider;
    }

    @NonNull
    private static WatchProvider createWatch() {
        final WatchProvider provider = new WatchProvider(/*getContext(), getBehavourFactory(), getWatchAlarmProvider(),
                createWatchDiagnostics(), RemoteConfigController.getInstance(getContext())*/);

        // Set up DeviceManager as a listener from the very start
        // final DeviceManager deviceManager = new DeviceManager(getContext());
        // deviceManager.listenTo(provider);

        return provider;
    }
}
