package com.example.pluginbluetooth.watch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.example.pluginbluetooth.bluetooth.device.CacheCenter;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;

import java.io.IOException;

import qrom.component.log.QRomLog;

public class SharedPreferencesCache implements CacheCenter {

    private static final String TAG = SharedPreferencesCache.class.getSimpleName();
    private static final String DEVICE_INFORMATION = "device_cache";

    private final SharedPreferences mSharedPreferences;

    public SharedPreferencesCache(final Context context) {
        mSharedPreferences = context.getSharedPreferences(DEVICE_INFORMATION, Context.MODE_PRIVATE);
    }

    @Override
    public String getString(final String key) {
        final String value = mSharedPreferences.getString(key, null);
        if (value != null) {
            QRomLog.i(TAG, "Found cached value for " + key);
        } else {
            QRomLog.i(TAG, "No cached value for " + key);
        }
        return value;
    }

    @Override
    public Value getValue(final String key) {
        final String result = mSharedPreferences.getString(key, null);
        if (result == null) {
            QRomLog.i(TAG, "No cached value for " + key);
            return null;
        }
        try {
            final byte[] bytes = Base64.decode(result, Base64.DEFAULT);
            final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);
            final Value value = unpacker.unpackValue();
            QRomLog.i(TAG, "Found cached value for " + key);
            return value;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read value from cache", e);
        }
        return null;
    }

    @Override
    public void put(final String key, final String value) {
        QRomLog.i(TAG, "Caching value for " + key);
        mSharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public void put(final String key, final Value value) {
        QRomLog.i(TAG, "Caching value for " + key);
        try {
            final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packValue(value);
            final String encodedValue = Base64.encodeToString(packer.toByteArray(), Base64.DEFAULT);
            mSharedPreferences.edit().putString(key, encodedValue).apply();
        } catch (IOException e) {
            Log.e(TAG, "Failed to cache value", e);
        }
    }

    @Override
    public void invalidate() {
        QRomLog.i(TAG, "Invalidating device read cache");
        mSharedPreferences.edit().clear().apply();
    }

    @Override
    public void invalidate(String key) {
        mSharedPreferences.edit().putString(key, null).apply();
    }
}
