package com.tencent.tws.assistant.services;



import java.util.ArrayList;
import java.util.List;

import android.app.AppGlobals;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;


public class TwsDumpActivitiesForAutoStart {

	private ArrayList<String> mBlackList = new ArrayList<String>(); 
	private static final String AUTHORITY = "com.tencent.tws.assistant.permission";
	public static final String AUTO_TABLE_NAME = "autotable";
	public static final Uri AUTO_CONTENT_URI = Uri.withAppendedPath(Uri.parse("content://" + AUTHORITY), AUTO_TABLE_NAME);
	
	private static Context mContext;
	private static TwsDumpActivitiesForAutoStart instance;
	private static final String TAG = "TwsDumpActivitiesForAutoStart";
	
	private static final int READ_AUTO_START_FIRSTLY = 1;
	private static final int CHANGE_AUTO_START = 2;
	private static final int READ_AUTO_TIME = 10 * 1000;
	private static final int CHANGE_AUTO_TIME = 20 * 1000;
	
	private TwsAutoStartChangedHandler mHandler;
	private boolean bWaitUpdate = false;
	
	private TwsDumpActivitiesForAutoStart(Context ctx) {
		mContext = ctx;
		HandlerThread hthread = new HandlerThread("TwsAutoStartChangedHandlerThread");		
		hthread.start();		
		mHandler = new TwsAutoStartChangedHandler(hthread.getLooper());
	}

	public static TwsDumpActivitiesForAutoStart getInstance(Context ctx){
		if(instance == null){
			instance = new TwsDumpActivitiesForAutoStart(ctx);
		}
		return instance;
	}
	
	private class TwsAutoStartChangedHandler extends Handler {

		public TwsAutoStartChangedHandler(Looper looper) {
			super(looper);
			
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {
				case READ_AUTO_START_FIRSTLY:
					readAutoStartDb();
					mContext.getContentResolver().registerContentObserver(AUTO_CONTENT_URI, true, mBlackListObserver);
					break;
				case CHANGE_AUTO_START:
					readAutoStartDb();
					bWaitUpdate = false;
					break;
			}		
		}
		
			
	}
	
	public void readAutoStartDbFirstly() {
		mHandler.sendEmptyMessageDelayed(READ_AUTO_START_FIRSTLY, READ_AUTO_TIME);
		
	}
	
	private void readAutoStartDb() {
		ArrayList<String> mBlackListQuery = new ArrayList<String>();
		Cursor cursor = null;
		ContentProviderClient client = null;
		try {
			ContentResolver mResolver = mContext.getContentResolver();
//			cursor = mResolver.query(AUTO_CONTENT_URI, null, null, null, null);
			client = mResolver.acquireUnstableContentProviderClient(AUTO_CONTENT_URI);
			 if(client != null) {
				 cursor=  client.query(AUTO_CONTENT_URI, null, null, null, null);
			 }
			if(cursor == null)return ;
			if(cursor.getCount() > 0){
				while (cursor.moveToNext()) {
					boolean disable = false;
					String pkgname = cursor.getString(cursor.getColumnIndex("_name"));
					int state = cursor.getInt(cursor.getColumnIndex("_state"));
					disable = (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
					if(disable){
						mBlackListQuery.add(pkgname);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();	
		} finally {
			if (client != null) {
           	 	client.release();
			}
			if (cursor != null) {
				cursor.close();
			}
		}
		mBlackList = mBlackListQuery;
		Slog.d(TAG, " readAutoStartDb: end...., mBlackList contains " + mBlackList.toString());
	}
	
	private ContentObserver mBlackListObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (!bWaitUpdate) {
				Slog.d(TAG, " onChange: readAutoStartDb");
				bWaitUpdate = true;
				mHandler.sendEmptyMessageDelayed(CHANGE_AUTO_START, CHANGE_AUTO_TIME);
			}
		}
	};
	
	private ArrayList<String> getDisableList(){
		return mBlackList;
	}

	public boolean canAtuoStart(Intent service, boolean systemready, ArrayList<String> arList){
		try {
    		 if (!systemready) {
    			 return true;
    		 }
    		 if(arList == null || arList.size() == 0 || service == null) {
    			 return true;
    		 }
    		 int callingUid = Binder.getCallingUid();
    		 if(callingUid < 10000) {
    			 return true;
    		 }
    		 ArrayList<String> blackNameList = getDisableList();
    		 if(blackNameList == null || blackNameList.size() == 0) {
    			 return true;
    		 }	 
    		 if(AppGlobals.getPackageManager() == null) {
    			 return true;
    		 }
    		 List<ResolveInfo> allServiceResolves = AppGlobals.getPackageManager().queryIntentServices(service, null, 0, UserHandle.getCallingUserId());
    		 if(allServiceResolves == null) {
    			 return true;
    		 }
    		 for(ResolveInfo ri : allServiceResolves) {
    			 if(ri.serviceInfo == null || ri.serviceInfo.packageName == null) {
    				 continue;
    			 }
    			 for(String blackName : blackNameList) {
    				 if(blackName.equals(ri.serviceInfo.packageName)) {
    					 boolean isExistInStack = false;
    					 for(String pkgname : arList) {
    						 if (pkgname.equals(blackName)) {
    							 isExistInStack = true;
    						 }
    					 }
    					 if(!isExistInStack) {
    						 Slog.d(TAG, "canAtuoStart: do not startService " + blackName);
    						 return false;
    					 }
    				 }
    			 }
    		 }
		} catch (RemoteException e) {
			e.printStackTrace();
		} 
		
		return true;
		
	}

	
}