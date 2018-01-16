package com.tws.plugin.bridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;

import com.tws.plugin.manager.PluginManagerProviderClient;

import qrom.component.log.QRomLog;

public class TwsPluginBridgeProvider extends ContentProvider {
    private static final String TAG = "TwsPluginBridgeProvider";

    public TwsPluginBridgeProvider() {
        QRomLog.i(TAG, "create provider proxy instance");
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        QRomLog.i(TAG, "query:" + uri);
        return PluginManagerProviderClient.query(uri, strings, s, strings1, s1);
    }

//    @Override
//    public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
//        QRomLog.i(TAG, "query:" + uri);
//        return PluginManagerProviderClient.query(uri, projection, queryArgs, cancellationSignal);
//    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        QRomLog.i(TAG, "query:" + uri);
        return PluginManagerProviderClient.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    @Override
    public String getType(Uri uri) {
        QRomLog.i(TAG, "getType:" + uri);
        return PluginManagerProviderClient.getType(uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        QRomLog.i(TAG, "insert:" + uri);
        return PluginManagerProviderClient.insert(uri, contentValues);
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        QRomLog.i(TAG, "delete:" + uri);
        return PluginManagerProviderClient.delete(uri, s, strings);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        QRomLog.i(TAG, "update:" + uri);
        return PluginManagerProviderClient.update(uri, contentValues, s, strings);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        //约定：原始url被吞掉了，所以调用这个函数的时候需要同时将原始url放入extras
        QRomLog.i(TAG, "call method:" + method + " arg:" + arg + " extras:" + extras);
        if (extras != null && extras.getParcelable("target_call") != null) {
            return PluginManagerProviderClient.call(method, arg, extras);
        }
        return null;
    }

}
