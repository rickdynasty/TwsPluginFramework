package com.animaconnected.secondo.provider.status;

public interface StatusProvider extends StatusChangeListener {

    void registerListener(StatusChangeListener listener);

    void unregisterListener(StatusChangeListener listener);

    StatusModel getCurrent();

    void addController(StatusController statusController);

}
