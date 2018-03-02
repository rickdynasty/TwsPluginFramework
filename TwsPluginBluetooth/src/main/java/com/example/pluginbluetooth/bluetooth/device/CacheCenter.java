package com.example.pluginbluetooth.bluetooth.device;

import org.msgpack.value.Value;

public interface CacheCenter {

    String getString(String key);

    Value getValue(String key);

    void put(String key, String value);

    void put(String key, Value value);

    void invalidate();

    void invalidate(String key);
}
