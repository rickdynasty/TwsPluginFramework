package com.animaconnected.secondo.provider.status.internal;

import com.animaconnected.secondo.provider.status.StatusModel;

public abstract class BaseStatusModel implements StatusModel {

    protected static final int DEVICE_BATTERY_STATUS_PRIORITY = 70;

    protected static final int DFU_FAILED_PRIORITY = 90;

    protected static final int DISCONNECTED_STATUS_PRIORITY = 100;

    protected static final int DFU_AVAILABLE_PRIORITY = 800;
    protected static final int DFU_REQUIRED_PRIORITY = 900;
    protected static final int DFU_RUNNING_PRIORITY = 1000;
    protected static final int DFU_SUCCESS_PRIORITY = 1000;

    protected static final int ACTIVE_WALK_PRIORITY = 2100;
    protected static final int ACTIVE_EMERGENCY_PRIORITY = 2100;
    protected static final int ACTIVE_NO_INVITE = 2100;

    protected static final int BLUETOOTH_DISABLED_PRIORITY = 2000;

    @Override
    public boolean isSameType(final StatusModel status) {
        return getClass().isInstance(status);
    }

}