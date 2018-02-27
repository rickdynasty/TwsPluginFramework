package com.animaconnected.bluetooth.device;

public interface DeviceConnectionListener {

    /**
     * The phone has established an initial BT connection but is not ready to communicate with the watch
     * <p/>
     * This is followed by onConnected(), onEnterDfuMode or onDisconnected().
     */
    void onConnecting();

    /**
     * The phone is ready to communicate with the watch
     */
    void onConnected();

    /**
     * The phone is no longer connected or connecting
     */
    void onDisconnected();

    /**
     * It is hard to get a connection
     */
    void onHardToConnect();

    /**
     * We're connected to the watch, but all we can do is a DFU
     * <p/>
     * Possible reasons: old FW or the watch lacks full FW (it's in DFU mode itself).
     */
    void onEnterDfuMode();

    /**
     * No longer in DFU mode
     */
    void onLeaveDfuMode();

}
