package com.animaconnected.bluetooth.gatt;

import java.util.UUID;

public interface DeviceListener {

    void onConnected();

    void onDisconnected();

    void onHardToConnect();

    void onCharacteristicChanged(final UUID service, final UUID characteristic, byte[] data);

    void onBonded();

    void onConnectionStateChange(final int newState, final int status);
}
