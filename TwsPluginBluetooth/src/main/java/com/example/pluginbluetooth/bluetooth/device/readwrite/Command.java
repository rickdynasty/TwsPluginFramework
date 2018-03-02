package com.example.pluginbluetooth.bluetooth.device.readwrite;


import com.example.pluginbluetooth.future.Promise;

import java.util.Arrays;

public class Command {

    private final String mName;
    private final byte[] mData;
    private final Promise<Void> mPromise;
    private long mTimestamp;

    public Command(final String name, final byte[] data, final Promise<Void> promise) {
        mName = name;
        mData = data;
        mPromise = promise;
    }

    public Command(final String name, final Promise<Void> promise) {
        mName = name;
        mPromise = promise;
        mData = null;
    }

    public Command(final String name, final byte[] data) {
        mName = name;
        mData = data;
        mPromise = null;
    }

    public String getName() {
        return mName;
    }

    public byte[] getData() {
        return mData;
    }

    public void setTimestamp(final long timestamp) {
        mTimestamp = timestamp;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Promise<Void> getPromise() {
        return mPromise;
    }

    @Override
    public String toString() {
        return "cmd: " + mName + ", value: " + Arrays.toString(mData);
    }
}
