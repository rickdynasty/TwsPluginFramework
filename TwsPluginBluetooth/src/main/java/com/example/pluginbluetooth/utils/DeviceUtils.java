package com.example.pluginbluetooth.utils;

public class DeviceUtils {

    public static final String DEVICE_INFO_ADDRESS = "address";
    public static final String DEVICE_INFO_ITEM_NUMBER = "itemNumber";
    public static final String DEVICE_INFO_SERIAL_NUMBER = "serialNumber";
    public static final String DEVICE_INFO_MANUFACTURE_NAME = "manufactureName";
    public static final String DEVICE_INFO_HARDWARE_REVISION = "hardwareRevision";
    public static final String DEVICE_INFO_FIRMWARE_REVISION = "firmwareRevision";

    public static final int BUTTON_ACTION_NO_PRESS = 0;
    public static final int BUTTON_ACTION_PRESS = 1;
    public static final int BUTTON_ACTION_LONG_PRESS = 2;
    public static final int BUTTON_ACTION_DOUBLE_PRESS = 3;
    public static final int BUTTON_ACTION_TRIPLE_PRESS = 4;
    public static final int BUTTON_ACTION_QUADRUPLE_PRESS = 5;
    public static final int BUTTON_ACTION_SUPER_LONG_PRESS = 11;

    public static final int COMPLICATION_DATE = 0;
    public static final int COMPLICATION_TIMEZONE = 1;
    public static final int COMPLICATION_ALARM = 2;
    public static final int COMPLICATION_REMOTE = 3;
    public static final int COMPLICATION_STEPS = 4;
    public static final int COMPLICATION_TIME = 5;
    public static final int COMPLICATION_NONE = 6;
    public static final int COMPLICATION_TIMER = 14;

    public static String getButtonActionName(final int action) {
        switch (action) {
            case BUTTON_ACTION_NO_PRESS:
                return "no press";
            case BUTTON_ACTION_PRESS:
                return "single press";
            case BUTTON_ACTION_LONG_PRESS:
                return "long press";
            case BUTTON_ACTION_DOUBLE_PRESS:
                return "double press";
            case BUTTON_ACTION_TRIPLE_PRESS:
                return "triple press";
            case BUTTON_ACTION_QUADRUPLE_PRESS:
                return "quadruple press";
            case BUTTON_ACTION_SUPER_LONG_PRESS:
                return "super long press";
            default:
                return "unknown";
        }
    }
}
