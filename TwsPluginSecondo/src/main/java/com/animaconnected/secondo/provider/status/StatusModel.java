package com.animaconnected.secondo.provider.status;

public interface StatusModel {

    int getPriority();

    boolean isSameType(StatusModel status);
}
