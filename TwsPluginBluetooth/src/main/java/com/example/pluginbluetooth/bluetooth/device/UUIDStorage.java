package com.example.pluginbluetooth.bluetooth.device;

import java.util.UUID;

public class UUIDStorage {

    public static final UUID ANIMA_SERVICE = UUID.fromString("6e406d41-b5a3-f393-e0a9-e6414d494e41");
    public static final UUID ANIMA_CHAR = UUID.fromString("6e401980-b5a3-f393-e0a9-e6414d494e41");
    public static final UUID NOTIFICATION_CHAR = UUID.fromString("6e401981-b5a3-f393-e0a9-e6414d494e41");

    public static final UUID GENERIC_ACCESS_SERVICE =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID GENERIC_ATTRIBUTE_SERVICE =
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_SERVICE =
            UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_MANUFACTER_NAME =
            UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_MODEL_NUMBER =
            UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_SERIAL_NUMBER =
            UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_HWR_REVISION =
            UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFO_FWR_REVISION =
            UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_FIRMWARE_UPDATE =
            UUID.fromString("00001530-1212-efde-1523-785feabcd123");
}
