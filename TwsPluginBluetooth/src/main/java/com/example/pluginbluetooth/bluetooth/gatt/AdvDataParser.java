package com.example.pluginbluetooth.bluetooth.gatt;

import com.example.pluginbluetooth.utils.ByteUtils;

import java.util.Arrays;

class AdvDataParser {

    public static int parseDeviceType(final byte[] scanData) {
        if (scanData != null && scanData.length == 3) {
            int cafe = ByteUtils.decodeUInt16LE(scanData, 0);
            if (cafe == 0x02cf) {
                final int type = scanData[2];
                switch (type) {
                    case 0:
                        return GattDevice.TYPE_PRIMO;
                    case 1:
                        return GattDevice.TYPE_SECONDO;
                    case 2:
                        return GattDevice.TYPE_GARBO; // Garbo (Rova). Return same internal type for now.
                    case 3:
                        return GattDevice.TYPE_GARBO; // Garbo (Platta). Return same internal type for now.
                }
            }
        }
        return GattDevice.TYPE_UNKNOWN;
    }

    public static byte[] decodeManufacturerData(final byte[] scanData) {
        if (scanData == null) return new byte[0];
        int index = 0;
        while (index + 1 < scanData.length && (scanData[index + 1] & 0xff) != 0xff) {
            index += (scanData[index] & 0xff) + 1;
        }
        if (index + 2 < scanData.length) {
            final int length = (scanData[index] & 0xff);
            return Arrays.copyOfRange(scanData, index + 2, index + 1 + length);
        }
        return new byte[0];
    }

    public static int parseItemId(final byte[] scanData) {
        if (scanData != null && scanData.length == 3) {
            int cafe = ByteUtils.decodeUInt16LE(scanData, 0);
            if (cafe == 0x02cf) {
                return scanData[2];
            }
        }
        return -1;
    }
}
