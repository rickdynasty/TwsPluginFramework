package com.example.pluginbluetooth.bluetooth.device;

/**
 * Created by Administrator on 2018/3/1.
 */

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

    void onEnterDfuMode();

    void onLeaveDfuMode();

}
