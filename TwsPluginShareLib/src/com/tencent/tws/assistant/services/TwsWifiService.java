package com.tencent.tws.assistant.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.INetworkStatsService;
import android.net.NetworkInfo;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;

import com.tencent.tws.assistant.provider.TwsSettings;

import android.util.Slog;
import static android.net.NetworkTemplate.buildTemplateWifi;
import android.net.INetworkStatsSession;

public class TwsWifiService {
	
	INetworkStatsService twsmNetworkStats = null;
	long twsmWifiStatStartTime;
	NetworkTemplate twsmTemplate;
	long twsmTotalBytesAfterScreenOff;	
	long twsmMaxSpeedInIdle  = 1*800;/* Bytes per second*/
	long twsmLastDetectWiFiTime;
	Intent twsmwifideviceidleIntent = null;
	Context mtwsContext = null;
	private AlarmManager mAlarmManager;
	private WifiManager mwifimgr = null;
	
	private static final int IDLE_REQUEST = 0;							   
    private static final String TAG = "TwsWifiService"; 
    private Handler mHandler = new Handler();
    private INetworkStatsSession mSession;
	
    private int mWifiSleepPolicy = TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER;
	
	public TwsWifiService(Context context) {
		// TODO Auto-generated constructor stub
		Slog.d(TAG, "TwsWifiService run");
		mtwsContext = context;
		twsmTemplate = buildTemplateWifi();
		
		mAlarmManager = (AlarmManager)mtwsContext.getSystemService(Context.ALARM_SERVICE);
		mwifimgr = (WifiManager)mtwsContext.getSystemService(Context.WIFI_SERVICE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mtwsContext.registerReceiver(mWiFiStateChangeReceiver, filter); 
		
		Intent idleIntent = new Intent(TWS_ACTION_DEVICE_IDLE, null);
        mIdleIntent = PendingIntent.getBroadcast(mtwsContext, IDLE_REQUEST, idleIntent, 0);
        
        Uri wifiSleepPolicyUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_WIFI_SLEEP_POLICY);
        mWifiSleepPolicy = TwsSettings.System.getInt(mtwsContext.getContentResolver(), 
        		TwsSettings.System.TWS_WIFI_SLEEP_POLICY, 
        		TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER);
        mtwsContext.getContentResolver().registerContentObserver(wifiSleepPolicyUri, false, mWifiSleepPolicyResolver);
        
        registerForBroadcasts();
	}
	
	private ContentObserver mWifiSleepPolicyResolver = new ContentObserver(new Handler()) {
    	public void onChange(boolean selfChange) {
    		mWifiSleepPolicy = TwsSettings.System.getInt(mtwsContext.getContentResolver(), 
    				 TwsSettings.System.TWS_WIFI_SLEEP_POLICY, 
    				 TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER);
    		Slog.d(TAG, "mWifiSleepPolicy = "+mWifiSleepPolicy);
    		
    	};
	};
	
	private static boolean mbWiFiEnabled = false;
	private static NetworkInfo.State mWiFiNetworkState = NetworkInfo.State.UNKNOWN;					
	private BroadcastReceiver mWiFiStateChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)){
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				if (wifiState == WifiManager.WIFI_STATE_ENABLED){
					mbWiFiEnabled = true;
				}
				else if (wifiState == WifiManager.WIFI_STATE_DISABLED){
					mbWiFiEnabled = false;
				}
			}
			else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mWiFiNetworkState = info.getState();
                if (mScreenOff){
                	if (mWiFiNetworkState == NetworkInfo.State.CONNECTED && mForceSleep){
                    	mHandler.removeCallbacks(mStartThread);
                    }
                    else if (mWiFiNetworkState == NetworkInfo.State.DISCONNECTED
                    		&& mWifiSleepPolicy == TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER){
                    	mHandler.removeCallbacks(mStartThread);
                    	mHandler.postDelayed(mStartThread, NEVER_SLEEP_ACTION_DELAY);
        				mForceSleep = true;
                    }
                }
            }
		}
		
	};
	
	private boolean mForceSleep = false;
	StartWifiFlowDetectThread mStartThread = new StartWifiFlowDetectThread();
	class StartWifiFlowDetectThread implements Runnable  {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			twsmWifiStatStartTime = System.currentTimeMillis();
			twsmLastDetectWiFiTime = twsmWifiStatStartTime;
			twsmTotalBytesAfterScreenOff = twsGetWiFiFlow(twsmWifiStatStartTime);
			
			Slog.d(TAG, "mWifiSleepPolicy = "+mWifiSleepPolicy+", mForceSleep = "+mForceSleep);
			if (mWifiSleepPolicy == TwsSettings.System.TWS_WIFI_SLEEP_POLICY_DEFAULT
					|| (mWifiSleepPolicy == TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED && mPluggedType == 0)
					|| mForceSleep){
				twsStartWiFIFlowDetect(DEFAULT_IDLE_MS, twsmWifiStatStartTime);
			}
			else if (mWifiSleepPolicy == TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER && mWiFiNetworkState == NetworkInfo.State.DISCONNECTED){
				mHandler.removeCallbacks(mStartThread);
				mHandler.postDelayed(mStartThread, NEVER_SLEEP_ACTION_DELAY);
				mForceSleep = true;
			}
		}
	} 
	
	long twsGetWiFiFlow(long curTime){
		if (null == twsmNetworkStats){
    		twsmNetworkStats = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
		}
		
		long TotalBytes = 0;
		try{
			mSession = twsmNetworkStats.openSession();
			twsmNetworkStats.forceUpdate();
			TotalBytes = mSession.getSummaryForNetwork(twsmTemplate, twsmWifiStatStartTime, curTime).getTotalBytes();
		}
		catch (RemoteException e){
			TotalBytes = twsmTotalBytesAfterScreenOff;
			Slog.d(TAG, "mNetworkStats.getSummaryForNetwork exception|mWifiStatStartTime=" + twsmWifiStatStartTime + "|curr=" + curTime);
		}

		return TotalBytes;
	}
	
	private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(TWS_ACTION_DEVICE_IDLE);
        mtwsContext.registerReceiver(mReceiver, intentFilter);
    }
	
	private int mPluggedType;
	private boolean mScreenOff;
	private long SCREEN_OFF_ACTION_DELAY = 10*1000;
	private long NEVER_SLEEP_ACTION_DELAY = AlarmManager.INTERVAL_HALF_HOUR;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			
			if (action.equals(Intent.ACTION_SCREEN_ON)) {
				mScreenOff = false;
				twsRemoveTwsSetting();
			}
			else if (action.equals(Intent.ACTION_SCREEN_OFF)){
				mScreenOff = true;
				if (mbWiFiEnabled){
					mHandler.removeCallbacks(mStartThread);
					mHandler.postDelayed(mStartThread, SCREEN_OFF_ACTION_DELAY);
				}
			}
			else if (action.equals(TWS_ACTION_DEVICE_IDLE)){
				twsSetWiFiIdleState(DEFAULT_IDLE_MS);
			}
			else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int pluggedType = intent.getIntExtra("plugged", 0);
                Slog.d(TAG, "ACTION_BATTERY_CHANGED pluggedType: " + pluggedType);
                if (mScreenOff && shouldWifiStayAwake(mPluggedType) &&
                        !shouldWifiStayAwake(pluggedType)) {
                	twsmWifiStatStartTime = System.currentTimeMillis();
        			twsmLastDetectWiFiTime = twsmWifiStatStartTime;
        			twsmTotalBytesAfterScreenOff = twsGetWiFiFlow(twsmWifiStatStartTime);
                	twsStartWiFIFlowDetect(DEFAULT_IDLE_MS, twsmWifiStatStartTime);
                }
                mPluggedType = pluggedType;
            } 
		}
	};
	
	void twsSetWiFiIdleState(long idleMillis){
		long curr = System.currentTimeMillis();
		long TotalBytes = twsGetWiFiFlow(curr);		
		long DurationTime = (curr - twsmLastDetectWiFiTime)/1000;
		long FlowByte = TotalBytes - twsmTotalBytesAfterScreenOff;
		Slog.d(TAG, "TotalBytes = " + TotalBytes + "Byte, twsmTotalBytesAfterScreenOff = "+ twsmTotalBytesAfterScreenOff+"Byte" );
		Slog.d(TAG, "wifi flow is " + FlowByte + "Byte in last "+ DurationTime + "s");
		if (FlowByte > (twsmMaxSpeedInIdle * DurationTime)){
			Slog.d(TAG, "there are much traffic on wifi,will sleep later");
			twsStartWiFIFlowDetect(idleMillis, curr);
			twsmTotalBytesAfterScreenOff = TotalBytes;
			twsmLastDetectWiFiTime = curr;
		}
		else{   
			Slog.d(TAG, "there are no traffic on wifi,will sleep now");
			setWiFiEnable(false);
		}
    }
	
	private static final String TWS_POWER_SAVE_ACTION = "tws.action.POWER_SAVE_ACTION";
    private static final int TWS_WIFI_POWERSAVE_ACTION = 2;
    private static final String ACTION_DEVICE_IDLE ="com.android.server.WifiManager.action.DEVICE_IDLE";
    private static final String TWS_ACTION_DEVICE_IDLE = "com.tencent.tws.assistant.server.WifiManager.action.DEVICE_IDLE";
	private void setWiFiEnable(boolean bEnable){
		 if (mwifimgr != null){
			 if (!bEnable 
					 && mPluggedType == 0 
					 && mWiFiNetworkState == NetworkInfo.State.DISCONNECTED
					 && mScreenOff){
				 Intent intent = new Intent(TWS_POWER_SAVE_ACTION);
				 intent.putExtra(TWS_POWER_SAVE_ACTION, TWS_WIFI_POWERSAVE_ACTION);
				 mtwsContext.sendBroadcast(intent);
				 Intent idleIntent = new Intent(ACTION_DEVICE_IDLE);
				 mtwsContext.sendBroadcast(idleIntent);
			 }
			 else {
				 Slog.d(TAG, "It's time to sleep wifi, but don't meet the conditions, mPluggedType = "+mPluggedType+", mWiFiNetworkState = "+mWiFiNetworkState);
			 }
		 }
	}
	
	private boolean shouldWifiStayAwake(int plugType) {

        boolean bRet = true;
        
        if (mWifiSleepPolicy != TwsSettings.System.TWS_WIFI_SLEEP_POLICY_NEVER) {
        	if (mWifiSleepPolicy == TwsSettings.System.TWS_WIFI_SLEEP_POLICY_DEFAULT
        			|| plugType == 0){
        		bRet = false;
        	}
        } 

        return bRet;
    }
	
	private static final long DEFAULT_IDLE_MS =  15*60*1000; /* 15 minutes */
	private PendingIntent mIdleIntent;
	void twsStartWiFIFlowDetect(long idleMillis, long currTime){
		mAlarmManager.set(AlarmManager.RTC, currTime + idleMillis, mIdleIntent);
	}
    
	void twsRemoveTwsSetting(){
		mHandler.removeCallbacks(mStartThread);
		mForceSleep = false;
		Intent idleIntent = new Intent(TWS_ACTION_DEVICE_IDLE, null);
		PendingIntent sender = PendingIntent.getBroadcast(mtwsContext, 0, idleIntent, PendingIntent.FLAG_NO_CREATE);  
		if (sender != null){
			mAlarmManager.cancel(sender);
		}
	}

}
