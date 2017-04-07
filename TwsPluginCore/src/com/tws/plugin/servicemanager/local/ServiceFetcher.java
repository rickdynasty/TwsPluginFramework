package com.tws.plugin.servicemanager.local;

/**
 * @author yongchen
 */
public abstract class ServiceFetcher {
    int mServiceId;
    String mGroupId;
    private Object mCachedInstance;

    public final Object getService() {
        synchronized (ServiceFetcher.this) {
            Object service = mCachedInstance;
            if (service != null) {
                return service;
            }
            return mCachedInstance = createService(mServiceId);
        }
    }

    public abstract Object createService(int serviceId);

}
