package com.tws.plugin.core.compat;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.android.HackContentProviderClient;
import com.tws.plugin.core.android.HackIContentProvider;

import qrom.component.log.QRomLog;

/**
 * @author yongchen
 */
public class CompatForContentProvider {
    private static final String TAG = "CompatForContentProvider";

    public static Bundle call(Uri uri, String method, String arg, Bundle extras) {

        ContentResolver resolver = PluginLoader.getApplication().getContentResolver();

        if (Build.VERSION.SDK_INT >= 11) {
            try {
                return resolver.call(uri, method, arg, extras);
            } catch (Exception e) {
                QRomLog.e(TAG, "call uri:" + uri + "  fail, method is " + method + " arg:" + arg + " extras=" + extras);
            }
            return null;
        } else {
            ContentProviderClient client = resolver.acquireContentProviderClient(uri);
            if (client == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            try {
                HackContentProviderClient hackContentProviderClient = new HackContentProviderClient(client);
                Object mContentProvider = hackContentProviderClient.getContentProvider();
                if (mContentProvider != null) {
                    //public Bundle call(String method, String request, Bundle args)
                    Object result = new HackIContentProvider(mContentProvider).call(method, arg, extras);
                    return (Bundle) result;
                }

            } finally {
                client.release();
            }
            return null;
        }
    }
}
