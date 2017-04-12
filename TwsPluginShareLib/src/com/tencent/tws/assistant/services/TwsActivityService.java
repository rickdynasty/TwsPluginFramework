package com.tencent.tws.assistant.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;
import com.tencent.tws.assistant.content.TwsIntent;
import com.tencent.tws.sharelib.R;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

public class TwsActivityService {
	
	static final String TAG = "TwsActivityService";
	boolean mbScreenOn = false;
	boolean mbTwsTrimAppEnable = false;
	Handler mTwsHandler = null;
	static final int TWS_TRIMAPP_DELAY = 5*60*1000;
	Context mContext;
	ActivityManager mActivityManager;	
	
	
	public TwsActivityService(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		twsReadBlackListAppPkgNameFromXml(mContext);
		twsReadUserSetBlackListApp();
		
		Looper lp = mContext.getMainLooper();
		mTwsHandler = new Handler(lp);
		
		mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		
		IntentFilter filter = new IntentFilter();
        filter.addAction(TwsIntent.TWS_ADD_BLACKLIST_APP_ACTION);
		filter.addAction(TwsIntent.TWS_REMOVE_BLACKLIST_APP_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mAddBlackListAppReceiver, filter);
	}
	
	BroadcastReceiver mAddBlackListAppReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String szAction = intent.getAction();
			String szPackageName = intent.getStringExtra(TwsIntent.TWS_BLACK_APP_PACKAGE_NAME);
			if (szAction.equals(TwsIntent.TWS_ADD_BLACKLIST_APP_ACTION)){
				twsmBacklistApp.add(szPackageName);
			}
			else if (szAction.equals(TwsIntent.TWS_REMOVE_BLACKLIST_APP_ACTION)){
				twsmBacklistApp.remove(szPackageName);
			}
			else if (szAction.equals(Intent.ACTION_SCREEN_ON)){
				mbScreenOn = true;
				mbTwsTrimAppEnable = false;
				mTwsHandler.removeCallbacks(mTimeoutTask);
			}
			else if (szAction.equals(Intent.ACTION_SCREEN_OFF)){
				mbScreenOn = false;
				mTwsHandler.postDelayed(mTimeoutTask, TWS_TRIMAPP_DELAY);
			}
		}
	};
	
	private final TimeoutTask mTimeoutTask = new TimeoutTask();
	private class TimeoutTask implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mbTwsTrimAppEnable = true;
			TrimedApps.clear();
			twsKillBackgroundApp();
			twsSendTrimedAppList();
		}
    }
	
	private static final String TrimedAppKey = "tws.trimed.blacklist_app_key";
	private static final String TWS_ACTION_SEND_TRIMED_APP_LIST = "tws.action.SEND_TRIMED_APP_LIST";
	private ArrayList<String> TrimedApps = new ArrayList<String>();

	private void twsKillBackgroundApp(){
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager.getRunningAppProcesses();
		List<RunningTaskInfo>  tasksInfo = mActivityManager.getRunningTasks(1);
		String topPkgName = "";
		if (tasksInfo != null && tasksInfo.size() >0){
			topPkgName = tasksInfo.get(0).topActivity.getPackageName(); 
		}
		
		for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList){
			int pid = appProcess.pid;
			String processName = appProcess.processName;
			
			String[] pkgNameList = appProcess.pkgList;
			int pkgListLength = pkgNameList.length;
			
			for(int i=0; i<pkgListLength; i++){
				String pkgName = pkgNameList[i];
				if (!pkgName.equals(topPkgName) && twsIsBlackListApp(pkgName)){
					Slog.d(TAG, "kill background application "+pkgName+", pid = "+pid);
					mActivityManager.forceStopPackage(pkgName);
    				TrimedApps.add(pkgName);
				}
			}
		}
	}
	
	
	void twsSendTrimedAppList(){
		if (!TrimedApps.isEmpty()){
			Bundle b = new Bundle();
			b.putStringArrayList(TrimedAppKey, TrimedApps);
			Intent intent = new Intent(TWS_ACTION_SEND_TRIMED_APP_LIST);
			intent.putExtras(b);
			mContext.sendBroadcast(intent);
		}
	}
	
	boolean twsIsBlackListApp(String szPkgname){
    	boolean bRet = false;
    	
    	int blacklistSize = twsmBacklistApp.size();
		if (blacklistSize == 0){
			twsReadBlackListAppPkgNameFromXml(mContext);
			twsReadUserSetBlackListApp();
			blacklistSize = twsmBacklistApp.size();
		}

		for(int i=0; i<blacklistSize; i++){
			if (szPkgname.contains(twsmBacklistApp.get(i))){
				bRet = true;
				break;
			}
		}
    
    	return bRet;
    }
	
	static final String TWS_TAG_DEVICE = "sleepmode";
    static final String TWS_TAG_ITEM = "item";
    static final String TWS_ATTR_NAME = "name";
	ArrayList<String> twsmBacklistApp = new ArrayList<String>();
    void twsReadBlackListAppPkgNameFromXml(Context ctx){
    	XmlResourceParser parser = ctx.getResources().getXml(R.xml.sleepmode_blacklist);
    	if (parser == null) {
    		return;
    	}
    	try {
			XmlUtils.beginDocument(parser, TWS_TAG_DEVICE);
			
			while (true){
				XmlUtils.nextElement(parser);
				String element = parser.getName();
				if (element == null) {
					break;
				}			
				if (element.equals(TWS_TAG_ITEM)){
					String name = null;
					name = parser.getAttributeValue(null, TWS_ATTR_NAME);
					twsmBacklistApp.add(name);	
				}		
			}		
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			parser.close();
		}
    }

    static final String USER_SET_BLACKLISTAPP_PATH = "data/system/userset_blacklist.xml";
    void twsReadUserSetBlackListApp_old(){
    	File path = Environment.getRootDirectory();
    	final File deviceInfoFile = new File(path, USER_SET_BLACKLISTAPP_PATH);
    	
    	if (deviceInfoFile.exists()){
        	FileReader deviceInfoReader;
        	
        	try {
				deviceInfoReader = new FileReader(deviceInfoFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Log.d(TAG, "Can't open "+path+"/"+USER_SET_BLACKLISTAPP_PATH);
				return;
			}
        	
        	XmlPullParser parser = Xml.newPullParser();
        	try {
				parser.setInput(deviceInfoReader);
				
				XmlUtils.beginDocument(parser, TWS_TAG_DEVICE);
				
				while (true) {
	                XmlUtils.nextElement(parser);

	                String element = parser.getName();
	                if (element == null) break;
	                
	                if (element.equals(TWS_TAG_ITEM)) {
	                    String name = null;
	                    name = parser.getAttributeValue(null, TWS_ATTR_NAME);
	                    twsmBacklistApp.add(name);
	                }
	            }
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(TAG, "Exception in deivceinfo parse:"+e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(TAG, "Exception in deivceinfo parse:"+e);
			}
    	}
    }

	public static final String TWS_FILE_FORBID_APP = "/data/system/user_blacklist.xml";
    void twsReadUserSetBlackListApp(){
		try {
	    	InputStream xml = new FileInputStream(TWS_FILE_FORBID_APP);
	        XmlPullParser pullParser = Xml.newPullParser();
	        pullParser.setInput(xml, "UTF-8");        
	        int event = pullParser.getEventType();
	        String pkgName = null;
	        while (event != XmlPullParser.END_DOCUMENT){
	            
	            switch (event) {
	            
	            case XmlPullParser.START_DOCUMENT:
	            	//mSucAPs = new ArrayList<SucAPInfo>();  
	                break;    
	            case XmlPullParser.START_TAG:    
	                if ("item".equals(pullParser.getName())){
	                    //int id = Integer.valueOf(pullParser.getAttributeValue(0));
	                    pkgName = pullParser.getAttributeValue(null, "name");
	                    Log.d(TAG, "twsReadUserSetBlackListApp pkgName = " + pkgName);
	                    twsmBacklistApp.add(pkgName);
	                }
	                break;
	                
	            case XmlPullParser.END_TAG:
	                if ("item".equals(pullParser.getName())){
	                	pkgName = null;
	                }
	                break;
	                
	            }
	            
	            event = pullParser.next();
	        }
		}catch(Exception e) {
			Log.d(TAG, "twsReadUserSetBlackListApp eexception e=" + e); 
		}

    }

}
