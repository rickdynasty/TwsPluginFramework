package com.tencent.tws.assistant.services;

import android.app.Notification;

interface ITwsNotificationManager
{
    void setNotificationsEnabledForPackage(String pkg, boolean enabled, int flag , int id);
    boolean shouldShowNotification(in Notification notification, String pkg);
    boolean shouldShowIcon(String pkg);
    void installDeletePackage(String pkg , boolean bInstall);
}
