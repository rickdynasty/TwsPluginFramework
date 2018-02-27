package com.animaconnected.bluetooth.gatt;

import java.util.UUID;

interface GattListener {

    void onConnected();

    void onDisconnecting();

    void onDisconnected();

    void onCharacteristicChanged(final UUID service, final UUID characteristic, byte[] data);

    void onBonded();

    void onConnectionStateChange(final int newState, final int status);
}
