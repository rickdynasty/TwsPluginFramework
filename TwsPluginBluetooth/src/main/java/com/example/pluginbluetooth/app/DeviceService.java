package com.example.pluginbluetooth.app;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.example.pluginbluetooth.provider.ProviderFactory;
import com.example.pluginbluetooth.watch.BaseWatchProviderListener;

import qrom.component.log.QRomLog;

public class DeviceService extends Service {

    private static final String TAG = DeviceService.class.getSimpleName();

    private Handler mBatteryNotificationHandler;
    private Runnable mBatteryNotificationRunnable;

    private final BaseWatchProviderListener mListener =
            new BaseWatchProviderListener() {
                @Override
                public void onAlarmEvent(final int alarmState) {
                }

                public void onConnectionChanged(boolean isConnected) {
                    final Context context = DeviceService.this.getApplicationContext();

                    if (!isConnected) {
                        // NotificationHandler.dismissMoveNotification(context);
                    } else {
                        ProviderFactory.getWatch().resetDevice();
                    }
                }

                @Override
                public void onStepsNow(int stepsToday, final int dayOfMonth) {
//                    ProviderFactory.getStepProvider().setStepsNow(stepsToday, dayOfMonth);
                }

                @Override
                public void onStillnessEvent(final int stillnessEvent) {
//                    if (stillnessEvent == WatchConstants.STILLNESS_STILL) {
//                        NotificationHandler.showMoveNotification(DeviceService.this.getApplicationContext());
//                    } else if (stillnessEvent == WatchConstants.STILLNESS_WINDOW_MOVED) {
//                        NotificationHandler.showMovedNotification(DeviceService.this.getApplicationContext());
//                    } else if (stillnessEvent == WatchConstants.STILLNESS_WINDOW_NOT_MOVED) {
//                        NotificationHandler.dismissMoveNotification(
//                                DeviceService.this.getApplicationContext());
//                    }
                }

                @Override
                public void onWroteDeviceSettings() {
//                    ProviderFactory.getStepProvider().setAcceptSteps(true);
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        QRomLog.i(TAG, "Starting service");

        // Note that we need to make sure we create a watch object and bootstrap everything here.
        ProviderFactory.getWatch().registerListener(mListener);
//        WatchDfuProvider.getInstance().registerListener(mDfuListener);
//
//        KronabyApplication.initialize();
        initializeService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ProviderFactory.getWatch().unregisterListener(mListener);
        // WatchDfuProvider.getInstance().unregisterListener(mDfuListener);

        mBatteryNotificationHandler.removeCallbacks(mBatteryNotificationRunnable);
    }

    private void initializeService() {
        //Diagnostics.sendAndDeleteCrashes(this);

        // Send phone diagnostics
        final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        QRomLog.i(TAG, "Memory class: " + am.getMemoryClass() + " MB");
        QRomLog.i(TAG, "Large memory class: " + am.getLargeMemoryClass() + " MB");

        mBatteryNotificationHandler = new Handler();
        mBatteryNotificationRunnable = new Runnable() {
            @Override
            public void run() {
                Context appContext = DeviceService.this.getApplicationContext();
//                WatchProvider.BatteryState batteryState = ProviderFactory.getWatch().getBatteryState();
//                if (batteryState == WatchProvider.BatteryState.NORMAL) {
//                    DeviceBatteryNotificationHandler.dismissNotifications(appContext);
//                } else {
//                    if (batteryState == WatchProvider.BatteryState.LOW) {
//                        DeviceBatteryNotificationHandler.showBatteryLowNotification(appContext);
//                    } else {
//                        DeviceBatteryNotificationHandler.showBatteryCriticalNotification(appContext);
//                    }
//                    mBatteryNotificationHandler.postDelayed(this, 60 * 60 * 1000); // repeat once every hour
//                }
            }
        };

        startOrRestartBatteryNotificationHandlerChecks();

    }

    private void startOrRestartBatteryNotificationHandlerChecks() {
        mBatteryNotificationHandler.removeCallbacks(mBatteryNotificationRunnable);
        mBatteryNotificationHandler.post(mBatteryNotificationRunnable);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static void start(final Context context) {
        context.startService(new Intent(context, DeviceService.class));
    }

    public static void stop(final Context context) {
        context.stopService(new Intent(context, DeviceService.class));
    }
}
