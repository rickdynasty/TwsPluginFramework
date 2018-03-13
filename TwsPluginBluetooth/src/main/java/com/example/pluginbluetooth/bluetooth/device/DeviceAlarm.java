package com.example.pluginbluetooth.bluetooth.device;

import qrom.component.log.QRomLog;

public class DeviceAlarm {
    private static final String TAG = "rick_Print:DeviceAlarm";

    private final int mHours;
    private final int mMinutes;
    private final int mDaysBitSet;
    private final int mVibrationPattern;

    public DeviceAlarm(final int hours, final int minutes, final int daysBitSet, final int vibrationPattern) {
        mHours = hours;
        mMinutes = minutes;
//        mDaysBitSet = ByteUtils.encodeDaysOfWeek(daysBitSet, daysBitSet == 0);
        mDaysBitSet = daysBitSet;
        mVibrationPattern = vibrationPattern;
        QRomLog.i(TAG, "DeviceAlarm " + hours + " " + minutes + " " + daysBitSet);
        QRomLog.i(TAG, "DeviceAlarm2 " + mHours + " " + mMinutes + " " + mDaysBitSet);

    }

    public int getHours() {
        return mHours;
    }

    public int getMinutes() {
        return mMinutes;
    }

    public int getDaysBitSet() {
        return mDaysBitSet;
    }

    public int getVibrationPattern() {
        return mVibrationPattern;
    }

    @Override
    public String toString() {
        return "DeviceAlarm{" +
                "mHours=" + mHours +
                ", mMinutes=" + mMinutes +
                ", mDaysBitSet=" + mDaysBitSet +
                ", mVibrationPattern=" + mVibrationPattern +
                '}';
    }
}
