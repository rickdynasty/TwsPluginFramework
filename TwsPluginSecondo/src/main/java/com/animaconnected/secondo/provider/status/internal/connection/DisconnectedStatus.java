package com.animaconnected.secondo.provider.status.internal.connection;

import com.animaconnected.secondo.provider.status.internal.BaseStatusModel;

public class DisconnectedStatus extends BaseStatusModel {

    @Override
    public int getPriority() {
        return BaseStatusModel.DISCONNECTED_STATUS_PRIORITY;
    }
}
