/*
* Copyright (C) 2007 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.tencent.tws.assistant.services;

import android.app.Service;
import android.util.Log;
import android.util.Slog;
import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.os.Process;
import android.os.Binder;
import android.os.UserHandle;
import android.os.IBinder;
import android.app.INotificationManager;
import android.content.Context;
import android.os.ServiceManager;
import android.os.RemoteException;

import com.tencent.tws.assistant.services.ITwsNotificationManager;
import com.tencent.tws.assistant.provider.TwsSettings;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import com.android.internal.os.AtomicFile;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import com.android.internal.util.FastXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import libcore.io.IoUtils;
import android.util.Xml;
import java.io.File;

import java.util.HashSet;
import java.util.Iterator;


/** {@hide} */
public class TwsNotificationManagerService extends ITwsNotificationManager.Stub {

	private static final String TAG = "TwsNotificationManagerService";
	private static final boolean DBG = false;

	private static final boolean ENABLE_BLOCKED_NOTIFICATIONS = true;

	private static final int DB_VERSION = 1;
	private static final String TAG_BODY = "notification-policy";
	private static final String ATTR_VERSION = "version";
	private static final String TAG_BLOCKED_PKGS = "blocked-packages";
	private static final String TAG_PACKAGE = "package";
	private static final String ATTR_NAME ="name";
	private static final String ATTR_NTF ="notificaiton";
	private static final String ATTR_STATUS ="status";
	private static final String ATTR_ICON ="icon";

	public static final String EXTRA_CMD_TPYE = "cmd_type";
	public static final String EXTRA_PKG_NAME = "pkg_name";
	public static final String EXTRA_PKG_INSTALL = "bInstall";
	public static final int CMD_PKG_INSTALL_NOTIFY = 1;

	private HashSet<String>  mBlockedPackages = new HashSet<String>();
	private AtomicFile mPolicyFile;
	private INotificationManager mNotificationMgr = null;
	private Context mContext = null;
	private ContentResolver resolver = null;
	class NotificationItemInfo {
		private String mPkg;
		private boolean bShowNotification;
		private boolean bShowStatus;
		private boolean bShowIcon;
	}

	class AppItemInfo {
		private String mPkg;
	}

	List<NotificationItemInfo>  mThirdBlockedItems = new ArrayList<NotificationItemInfo>();
	List<AppItemInfo> mAppList = new ArrayList<AppItemInfo>();
	private List<String> mWhiteList = new ArrayList<String>();
	private List<String> mBlackList = new ArrayList<String>();

	private final static int UPDATE_FLAG_NTY = 0;
	private final static int UPDATE_FLAG_STATUS = 1;
	private final static int UPDATE_FLAG_ICON = 2;

	TwsNotificationManagerService(Context context) {

		mContext = context;

		mNotificationMgr = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));

		loadBlockDb();

		resolver = mContext.getContentResolver();
		
//		Log.d(TAG, "tws notification manager service started");
	}

	public void setNotificationsEnabledForPackage(String pkg, boolean enabled, int flag , int id) {

		ensureCallerSystem();

		if (DBG) {
			Slog.v(TAG, (enabled?"en":"dis") + "abling notifications for " + pkg);
		}

		updateThirdBlockedItems(pkg,enabled,flag);

		if (enabled && flag == UPDATE_FLAG_NTY) {
			if("com.android.providers.downloads.ui".equals(pkg)||"com.android.providers.downloads".equals(pkg)){
			   mBlockedPackages.remove("com.android.providers.downloads");
			   mBlockedPackages.remove("com.android.providers.downloads.ui");
			}else{
			   mBlockedPackages.remove(pkg);
			}
		} else if(!enabled && (flag == UPDATE_FLAG_NTY || flag == UPDATE_FLAG_STATUS)){
			if("com.android.providers.downloads.ui".equals(pkg)||"com.android.providers.downloads".equals(pkg)){
			   mBlockedPackages.add("com.android.providers.downloads");
			   mBlockedPackages.add("com.android.providers.downloads.ui");
			   pkg = "com.android.providers.downloads";
			}else{
			   mBlockedPackages.add(pkg);
			}
			// Now, cancel any outstanding notifications that are part of a just-disabled app
			if (ENABLE_BLOCKED_NOTIFICATIONS) {
				int INVALID_VAL = -1;
				if(id != INVALID_VAL) {
					try {
						mNotificationMgr.cancelAllNotifications(pkg,UserHandle.USER_ALL); 
					} catch (RemoteException e)
					{
						e.printStackTrace();
					}
				}
			}
			// Don't bother canceling toasts, they'll go away soon enough.
		}

		if(DBG){
			for (NotificationItemInfo mItem : mThirdBlockedItems) {
//				Log.d(TAG,"mItem pkg="+mItem.mPkg+";bShowNotification = "+mItem.bShowNotification+";bShowStatus = "+mItem.bShowStatus+";bShowIcon = "+mItem.bShowIcon);
			}
		}
		
		writeBlockDb();

//		Log.d(TAG,"writeBlockDb mThirdBlockedItems size = "+mThirdBlockedItems.size());
	}

	public boolean shouldShowNotification(Notification notification, String pkg) {
		/*if(isNotificationOnGoing(notification)){
			if(!isStatusEnabledForPackage(pkg)){
				Log.d(TAG, "shouldShowNotification 0 :" + pkg);
				return false;
			}
		} else */if(!isNotificationsEnabledForPackage(pkg)){
//			 Log.d(TAG,"shouldShowNotification 1 :"+pkg);
			 return false;
		}

		return true;
	}

	public boolean isStatusEnabledForPackage(String pkg) {

		if(!ENABLE_BLOCKED_NOTIFICATIONS) return true;

		return isStatusEnabledForPackageInternal(pkg);
	}

	public boolean isNotificationsEnabledForPackage(String pkg) {

		if(!ENABLE_BLOCKED_NOTIFICATIONS) return true;

		if(DBG) Log.d(TAG,"mThirdBlockedItems size = "+mThirdBlockedItems.size());

		return isNotificationsEnabledForPackageInternal(pkg);
	}

	public boolean shouldShowIcon(String pkg) {
		boolean enabled = true;
//		Log.d(TAG,"shouldShowIcon :"+pkg);
		for(NotificationItemInfo mBlockedItem : mThirdBlockedItems){
			if(pkg.equalsIgnoreCase(mBlockedItem.mPkg) && mBlockedItem.bShowIcon){
				enabled = false;
//				Log.d(TAG,"shouldShowIcon :enabled = true");
				break;
			}
		}
		return enabled;
	}

	public void installDeletePackage(String pkg , boolean bInstall) {

		if(DBG) Log.d(TAG, "installDeletePackage(" + pkg + ", " + bInstall + ")");

		if(pkg == null) {
			return;
		}

		synchronized(mThirdBlockedItems) {
			final int currentIconValue = TwsSettings.System.getInt(resolver, "notification_display_icon_label",2);
			if(bInstall){
				NotificationItemInfo mTwsNotificationItemInfo = new NotificationItemInfo();
				mTwsNotificationItemInfo.mPkg = pkg;
				if(currentIconValue == 0){
					mTwsNotificationItemInfo.bShowIcon = true;
					mTwsNotificationItemInfo.bShowNotification = true;
				}
				else if(currentIconValue == 1){
					mTwsNotificationItemInfo.bShowIcon = false;
					mTwsNotificationItemInfo.bShowNotification = true;
				}
				else{
					if(isPhoneOrTws(pkg)){
						mTwsNotificationItemInfo.bShowIcon = true;
					}
					else{
						mTwsNotificationItemInfo.bShowIcon = false;
					}
					mTwsNotificationItemInfo.bShowNotification = true;
				}
				
				mTwsNotificationItemInfo.bShowStatus = false;
				mThirdBlockedItems.add(mTwsNotificationItemInfo);
			}else{
				NotificationItemInfo mItem = findItem(pkg);
				if(mItem == null)
					return;
				mThirdBlockedItems.remove(mItem);
			}
			writeBlockDb();
		}
	}

	void loadBlockDb() {
		synchronized(mThirdBlockedItems) {
			getAppList();
			Slog.v(TAG, "mAppList size =  " + mAppList.size());
			boolean bWrite = false;
			if (mPolicyFile == null) {
				
				File mFile = new File("/data/system/notification_policy.xml");
				
				try{
					if(!mFile.exists()){
						boolean b = mFile.createNewFile();
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				
				File dir = new File("/data/system");
				//notification_comfig.xml
				mPolicyFile = new AtomicFile(mFile);

				mBlockedPackages.clear();
				mThirdBlockedItems.clear();
//				Log.d(TAG,"twsThirdBlockedItems0 size = "+mThirdBlockedItems.size());
				FileInputStream infile = null;
				try {
					infile = mPolicyFile.openRead();
					final XmlPullParser parser = Xml.newPullParser();
					parser.setInput(infile, null);

					int type;
					String tag;
					int version = DB_VERSION;
					while ((type = parser.next()) != END_DOCUMENT) {
						tag = parser.getName();
						if (type == START_TAG) {
							if (TAG_BODY.equals(tag)) {
								version = Integer.parseInt(parser.getAttributeValue(null, ATTR_VERSION));
							} else if (TAG_BLOCKED_PKGS.equals(tag)) {
								while ((type = parser.next()) != END_DOCUMENT) {
									tag = parser.getName();
									if (TAG_PACKAGE.equals(tag) && type == END_TAG) {
										//mBlockedPackages.add(parser.getAttributeValue(null, ATTR_NAME));
										NotificationItemInfo mItemInfo = new NotificationItemInfo();
										mItemInfo.mPkg = parser.getAttributeValue(null, ATTR_NAME);
										mItemInfo.bShowNotification = stringToBool(parser.getAttributeValue(null, ATTR_NTF));
										mItemInfo.bShowStatus = stringToBool("0");
										mItemInfo.bShowIcon = stringToBool(parser.getAttributeValue(null, ATTR_ICON));
										mThirdBlockedItems.add(mItemInfo);
									} else if (TAG_BLOCKED_PKGS.equals(tag) && type == END_TAG) {
										break;
									}
								}
							}
						}
					}
//					Log.d(TAG,"twsThirdBlockedItems1 size = "+mThirdBlockedItems.size());
					if(DBG){
						for (NotificationItemInfo mItem : mThirdBlockedItems) {
//							Log.d(TAG,"mItem pkg="+mItem.mPkg+";bShowNotification = "+mItem.bShowNotification+";bShowStatus = "+mItem.bShowStatus+";bShowIcon = "+mItem.bShowIcon);
						}
					}
					
					List<NotificationItemInfo> tmpAppList = new ArrayList<NotificationItemInfo>();
					for(AppItemInfo mItem : mAppList){
						if(DBG)Log.d(TAG, "mItem.mPkg =  " + mItem.mPkg);
						if(mThirdBlockedItems.size() <= 0){
							NotificationItemInfo mNewItemInfo = new NotificationItemInfo();
							mNewItemInfo.mPkg = mItem.mPkg;
							mNewItemInfo.bShowNotification = true;
							mNewItemInfo.bShowStatus = false;//
							if(isPhoneOrTws(mItem.mPkg))
								mNewItemInfo.bShowIcon = true;
							else
								mNewItemInfo.bShowIcon = false;
							tmpAppList.add(mNewItemInfo);
							mBlockedPackages.add(mNewItemInfo.mPkg);
							bWrite = true;
							if(DBG)Log.d(TAG, "twsBlockedPackages0.add");
						}else{
							if(!containItem(mItem,mThirdBlockedItems)){
								NotificationItemInfo mNewItemInfo = new NotificationItemInfo();
								mNewItemInfo.mPkg = mItem.mPkg;
								mNewItemInfo.bShowNotification = true;
								mNewItemInfo.bShowStatus = false;//
								mNewItemInfo.bShowIcon = true;
								tmpAppList.add(mNewItemInfo);
								mBlockedPackages.add(mNewItemInfo.mPkg);
								bWrite = true;
								if(DBG)Log.d(TAG, "twsBlockedPackages1.add");
							}
						}
						
					}
//					Log.d(TAG, "tmpAppList size =  " + tmpAppList.size());
					mThirdBlockedItems.addAll(tmpAppList);
					if(bWrite){
						if(DBG){
							for (NotificationItemInfo mItem : mThirdBlockedItems) {
								Log.d(TAG,"mItem pkg="+mItem.mPkg+";bShowNotification = "+mItem.bShowNotification+";bShowStatus = "+mItem.bShowStatus+";bShowIcon = "+mItem.bShowIcon);
							}
						}
						writeBlockDb();
						bWrite  = false;
					}
				} catch (FileNotFoundException e) {
					// No data yet
				} catch (IOException e) {
					Log.wtf(TAG, "Unable to read blocked notifications database", e);
				} catch (NumberFormatException e) {
					Log.wtf(TAG, "Unable to parse blocked notifications database", e);
				} catch (XmlPullParserException e) {
					Log.wtf(TAG, "Unable to parse blocked notifications database", e);
				} finally {
					IoUtils.closeQuietly(infile);
				}
			}
		}
	}

	void writeBlockDb() {
		synchronized(mThirdBlockedItems) {
			FileOutputStream outfile = null;
			try {
				outfile = mPolicyFile.startWrite();

				XmlSerializer out = new FastXmlSerializer();
				out.setOutput(outfile, "utf-8");

				out.startDocument(null, true);

				out.startTag(null, TAG_BODY); {
					out.attribute(null, ATTR_VERSION, String.valueOf(DB_VERSION));
					out.startTag(null, TAG_BLOCKED_PKGS); {
						// write all known network policies
						for (NotificationItemInfo mItem : mThirdBlockedItems) {
								out.startTag(null, TAG_PACKAGE); {
								out.attribute(null, ATTR_NAME, mItem.mPkg);
								out.attribute(null, ATTR_NTF, boolToString(mItem.bShowNotification));
								out.attribute(null, ATTR_STATUS, boolToString(mItem.bShowStatus));
								out.attribute(null, ATTR_ICON, boolToString(mItem.bShowIcon));
								
							} out.endTag(null, TAG_PACKAGE);
						}
					} out.endTag(null, TAG_BLOCKED_PKGS);
				} out.endTag(null, TAG_BODY);

				out.endDocument();

				mPolicyFile.finishWrite(outfile);
			} catch (IOException e) {
				if (outfile != null) {
					mPolicyFile.failWrite(outfile);
				}
			}
		}
	}

	// Unchecked. Not exposed via Binder, but can be called in the course of enqueue*().
	boolean isNotificationsEnabledForPackageInternal(String pkg) {
		boolean enabled = true;
		for(NotificationItemInfo mBlockedItem : mThirdBlockedItems){
			if(pkg.equalsIgnoreCase(mBlockedItem.mPkg) && (!mBlockedItem.bShowNotification)){
				enabled = false;
				break;
			}
		}
		if (DBG) {
			Slog.v(TAG, "notifications are " + (enabled?"en":"dis") + "abled for " + pkg);
			Iterator iterator = mBlockedPackages.iterator();
			while(iterator.hasNext())
			Slog.w(TAG,"isNotificationsEnabledForPackageInternal pkg="+iterator.next());
		}
		return enabled;
	}

	// Unchecked. Not exposed via Binder, but can be called in the course of enqueue*().
	 boolean isStatusEnabledForPackageInternal(String pkg) {
	   boolean enabled = true;
		for(NotificationItemInfo mBlockedItem : mThirdBlockedItems){
			if(pkg.equalsIgnoreCase(mBlockedItem.mPkg) && (!mBlockedItem.bShowStatus)){
				enabled = false;
				break;
			}
		}
		return enabled;
	}

	private void updateThirdBlockedItems(String pkg, boolean mEnabled,int flags){
		synchronized(mThirdBlockedItems) {
//			Log.d(TAG,"mThirdBlockedItems size = "+mThirdBlockedItems.size());
			for (NotificationItemInfo mItem : mThirdBlockedItems) {
				if(mItem.mPkg.equalsIgnoreCase(pkg)){
					boolean bShowNty = mItem.bShowNotification;
					boolean bShowStatus = mItem.bShowStatus;
					boolean bShowIcon = mItem.bShowIcon;
					NotificationItemInfo mTwsNotificationItemInfo = new NotificationItemInfo();
					mTwsNotificationItemInfo.mPkg = pkg;
					if(flags == UPDATE_FLAG_NTY){
						mTwsNotificationItemInfo.bShowStatus = bShowStatus;
						mTwsNotificationItemInfo.bShowIcon = bShowIcon;
						mTwsNotificationItemInfo.bShowNotification = mEnabled;
					}else if(flags == UPDATE_FLAG_STATUS){
						mTwsNotificationItemInfo.bShowStatus = mEnabled;
						mTwsNotificationItemInfo.bShowIcon = bShowIcon;
						mTwsNotificationItemInfo.bShowNotification = bShowNty;
					}else if(flags == UPDATE_FLAG_ICON){
						mTwsNotificationItemInfo.bShowStatus = bShowStatus;
						mTwsNotificationItemInfo.bShowIcon = mEnabled;
						mTwsNotificationItemInfo.bShowNotification = bShowNty;
					}
//					Log.d(TAG,"updateThirdBlockedItems pkg = "+pkg);
					mThirdBlockedItems.remove(mItem);
					mThirdBlockedItems.add(mTwsNotificationItemInfo);
					break;
				}
			}
		}
	}

	void ensureCallerSystem() {
		int uid = Binder.getCallingUid();
		if (uid == Process.SYSTEM_UID || uid == 0) {
			return;
		}
		throw new SecurityException("Disallowed call for uid " + uid);
	}

	private void getAppList() {
		mAppList.clear();
		//List<ResolveInfo> appPackageInfos = getAllTheLaunch();
		PackageManager mPm = mContext.getPackageManager();
		List<PackageInfo> packs = mPm.getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++) {
			PackageInfo packInf = packs.get(i);
			ApplicationInfo pinfo = null;
			try {
				pinfo = mPm.getApplicationInfo(packInf.packageName,
						PackageManager.GET_UNINSTALLED_PACKAGES);				
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			if(pinfo == null){
				continue;
			}
			if (!isSystemPackage(pinfo.packageName)&& !isInBlackList(pinfo.packageName)) {
				AppItemInfo shareInfo = new AppItemInfo();
				shareInfo.mPkg = pinfo.packageName;
				mAppList.add(shareInfo);
			}
		}
	}

	private List<ResolveInfo> getAllTheLaunch() {
		Intent it = new Intent(Intent.ACTION_MAIN);
		// it.addCategory(Intent.CATEGORY_HOME);
		it.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> ra = mContext.getPackageManager().queryIntentActivities(it, 0);
		return ra;
	}
	private void initBlackList(){
		mBlackList.clear();
		mBlackList.add("com.android.OriginalSettings");
		mBlackList.add("com.android.systemui");
		mBlackList.add("com.tencent.nanji.updater");
	}
	private void initWhiteList(){
		mWhiteList.clear();
		//mWhiteList.add("com.android.contacts");
		//mWhiteList.add("com.tencent.launcher");
		//mWhiteList.add("com.android.deskclock");
		//mWhiteList.add("com.android.calendar");
		mWhiteList.add("com.immomo.momo");
		mWhiteList.add("com.sina.weibo");
		mWhiteList.add("com.tencent.mobileqq");
		mWhiteList.add("com.tencent.mm");
		mWhiteList.add("com.tencent.WBlog");
		mWhiteList.add("com.android.OriginalSettings");
		//mWhiteList.add("com.android.providers.downloads.ui");
		//mWhiteList.add("com.android.calculator2");
		//mWhiteList.add("com.tencent.qqmusic");
		//mWhiteList.add("com.android.providers.downloads");
	}

	private boolean isPhoneOrTws(String packageName) {
		initWhiteList();
		for(String item : mWhiteList){
			if(item.equalsIgnoreCase(packageName)){
				return true;
			}
		}
		return false;
	}
	
	private boolean isInBlackList(String packageName) {
			initBlackList();
			for(String item : mBlackList){
				if(item.equalsIgnoreCase(packageName)){
					return true;
				}
			}
			return false;
		}
	private boolean isSystemPackage(String packagename) {
		try {
			PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(packagename, 0);
			if (isSystemApp(pInfo) || isSystemUpdateApp(pInfo)) {
				return true;
			} else {
				return false;
			}
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}

	}

	private boolean isSystemApp(PackageInfo info) {
		return ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
	}

	private boolean isSystemUpdateApp(PackageInfo info) {
		return ((info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
	}

	private String boolToString(boolean b){
		int mStr = b ? 1 : 0;
		return String.valueOf(mStr);
	}

	private boolean stringToBool(String str){
		if(str == null)
			return true;
		if(str.equalsIgnoreCase("1"))
			return true;
		else
			return false;
	}

	private boolean containItem(AppItemInfo mBlockedItem,List<NotificationItemInfo> mThirdBlockedItems){
		for(NotificationItemInfo mItem : mThirdBlockedItems){
			if(mItem.mPkg.equalsIgnoreCase(mBlockedItem.mPkg))
				return true;
		}
		return false;
	}

	private NotificationItemInfo findItem(String pkg){
		for(NotificationItemInfo mItem : mThirdBlockedItems){
			if(pkg.equalsIgnoreCase(mItem.mPkg))
				return mItem;
		}
		return null;
	}

	private boolean isNotificationOnGoing(Notification notification) {
		boolean mFlags = (notification.flags & (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR)) == 0 ? false
				: true;
		return mFlags;
	}
}
