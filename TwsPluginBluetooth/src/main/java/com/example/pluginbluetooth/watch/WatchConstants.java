package com.example.pluginbluetooth.watch;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.SecondoApplication;

public class WatchConstants {

    public static final int SLOT_UNKNOWN = 0;
    public static final int SLOT_COMPLICATION = 2;
    public static final int SLOT_CROWN = 3;
    public static final int SLOT_TOP = 4;
    public static final int SLOT_BOTTOM = 5;

    public static final int MAGIC_KEY_NONE = -1;
    public static final int MAGIC_KEY_ONE = 0;

    public static final int MOTOR_UNKNOWN = -1;
    public static final int MOTOR_PRIMARY_HOUR = 0;
    public static final int MOTOR_PRIMARY_MINUTES = 1;
    public static final int MOTOR_SECONDARY_HOUR = 2;
    public static final int MOTOR_SECONDARY_MINUTES = 3;

    public static final int TRIGGER_NONE = 0;
    public static final int TRIGGER_CAMERA = 1;
    public static final int TRIGGER_MEDIACTRL = 2;
    public static final int TRIGGER_MUTE = 3;

    public static final int STILLNESS_WINDOW_NOT_MOVED = 0;
    public static final int STILLNESS_WINDOW_MOVED = 1;
    public static final int STILLNESS_STILL = 2;

    public static final int WATCH_VIBRATION_1 = 1;
    public static final int WATCH_VIBRATION_2 = 2;
    public static final int WATCH_VIBRATION_3 = 3;

    public static final int ALERT_NONE = 0;
    public static final int ALERT_ALARM = 1;
    public static final int ALERT_STILLNESS = 2;
    public static final int ALERT_STEP_GOAL = 4;
    public static final int ALERT_ALL_FUNCTIONS = 7;

    public static final int ALARM_START = 0;
    public static final int ALARM_RESTART = 1;
    public static final int ALARM_RETRY = 2;
    public static final int ALARM_SNOOZE = 3;
    public static final int ALARM_DISMISS = 4;
    public static final int ALARM_TIMEOUT = 5;

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    public static String getDefaultCity() {
        return SecondoApplication.getApplication().getResources().getString(R.string.default_city);
    }

    public static String getDefaultCountry() {
        return SecondoApplication.getApplication().getResources().getString(R.string.default_country);
    }
}
