package com.tencent.tws.assistant.services;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;

import com.tencent.tws.assistant.content.TwsIntent;
import com.tencent.tws.assistant.provider.TwsSettings;

import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

public class TwsPowerManagerService {
	
	Context mtwsContext;
	private TwsMaskHandler mMaskHandler;
	private Handler mFaceDetectHandler;
	
	static final String TWS_TAG = "TwsPowerManagerService";
	static final String TWS_ACTION_START_FACEDETECT = "android.tws.action.start.facedetect";
	static final String TWS_ACTION_STOP_FACEDETECT = "android.tws.action.stop.facedetect";
	boolean twsmFaceDetectEnabled = true;
	final static boolean mb_isDebug = false; 	
	private boolean mBootComplete = false;
	private int mBrightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
	
	
	void tws_log(String tag, String msg){
	    if (mb_isDebug){
	    	Log.d(tag, msg);
	    }
	}
	
	public TwsPowerManagerService(Context context) {
		// TODO Auto-generated constructor stub
		mtwsContext = context;
	}
	
	public void twsInit(boolean bootComplete){
		HandlerThread hthread = new HandlerThread("TwsPowerManagerServiceThread");
		hthread.start();
		mMaskHandler = new TwsMaskHandler(hthread.getLooper());
		
		isNeedTwsMask();
		
		mFaceDetectHandler = new Handler();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mtwsContext.registerReceiver(mScreenStateChangeReceiver, filter);
		
		Uri faceDetecUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_FACE_DETECT_ENABLE);
		mtwsContext.getContentResolver().registerContentObserver(faceDetecUri, false, twsmFacedetectObserver);
		twsmFaceDetectEnabled = TwsSettings.System.getInt(mtwsContext.getContentResolver(), TwsSettings.System.TWS_FACE_DETECT_ENABLE, 0)>0;
		
		Uri brightnessModeUri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
		mtwsContext.getContentResolver().registerContentObserver(brightnessModeUri, false, mBrightnessModeObserver);
		mBrightnessMode = Settings.System.getInt(mtwsContext.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
		
		mCurrentScreenBrightness = Settings.System.getInt(mtwsContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 100);
		
		mBootComplete = bootComplete;
		
		if (mBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL){
			twsSetBacklightBrightness(mCurrentScreenBrightness);
		}
	}
	
	BroadcastReceiver mScreenStateChangeReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (mBootComplete){
				String szAction = intent.getAction();
				
				if (szAction.equals(Intent.ACTION_SCREEN_OFF)){
					if (mHaveMasked){
						if (mHaveManualChangedWhenAutoMode || mDimMasked){
							mMaskHandler.sendEmptyMessage(REMOVE_BRIGHTNESS_MASK);
						}
					}
					
					mHaveManualChangedWhenAutoMode = false;
					mDimMasked = false;
				}
			}
		}
	};
	
	private final int SET_BRIGHTNESS_MASK =2;
	private final int REMOVE_BRIGHTNESS_MASK =3;
	class TwsMaskHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if (msg.what == SET_BRIGHTNESS_MASK){
				if (!mHaveMasked){
    				if (mWindowManager == null){
    					createFloatView();
    				}
    				mWindowManager.addView(mFloatLayout, wmParams);
    				mHaveMasked = true;
    			}
				
				int value = msg.arg1;
				
    			setBrightnessMaskValue(value+30);
			}
			else if (msg.what == REMOVE_BRIGHTNESS_MASK){
				if (mHaveMasked){
    				setBrightnessMaskValue(100);
    				mWindowManager.removeView(mFloatLayout);
    				mHaveMasked = false;
    			}
			}
		}
		
		public TwsMaskHandler(Looper looper) {
			// TODO Auto-generated constructor stub
			super(looper);
		}
	}
	
	TwsBrightnessModeObserver mBrightnessModeObserver = new TwsBrightnessModeObserver(new Handler());
	class TwsBrightnessModeObserver extends ContentObserver{

		public TwsBrightnessModeObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);
			int brightnessMode = Settings.System.getInt(mtwsContext.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
			if (brightnessMode != mBrightnessMode){
				mBrightnessMode = brightnessMode;
				if (mBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC){
					if (mHaveMasked){
						mMaskHandler.sendEmptyMessage(REMOVE_BRIGHTNESS_MASK);
					}
				}
				else if (mBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL){
					int value = Settings.System.getInt(mtwsContext.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS, TwsIntent.TWS_BRIGHTNESS_DIM);
					twsSetBacklightBrightness(value);
				}
				
				mHaveManualChangedWhenAutoMode = false;
			}
		}
	}
	
	TwsFaceDetectObserver twsmFacedetectObserver = new TwsFaceDetectObserver(new Handler());
	class TwsFaceDetectObserver extends ContentObserver{

		public TwsFaceDetectObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);
			twsmFaceDetectEnabled = TwsSettings.System.getInt(mtwsContext.getContentResolver(), TwsSettings.System.TWS_FACE_DETECT_ENABLE, 0)>0;
		}
	}
	
	public void  twsSetFaceDetectTimeoutLocked(int nextState, int wakelockstate, int dimDelay){
		
		if (mBootComplete && mFaceDetectHandler != null){
			mFaceDetectHandler.removeCallbacks(twsmFaceDetectTimeoutTask);
	    	// when next state is dim and don't have app acquired screen on bit and user enable the face detect,start the face detect
	    	if (twsmFaceDetectEnabled){
	    		if (nextState == TwsIntent.SCREEN_DIM && (wakelockstate & TwsIntent.SCREEN_ON_BIT) == 0){
	        		long nFaceDetectDelay = 0;
	        		if (dimDelay > TWS_FACE_DETECT_TIME_GAP){
	        			nFaceDetectDelay = dimDelay - TWS_FACE_DETECT_TIME_GAP;
	                }
//	        		Log.d(TWS_TAG, "twsStartFaceDetectTimeoutTask:: nFaceDetectDelay = "+nFaceDetectDelay);      	
	        		mFaceDetectHandler.postDelayed(twsmFaceDetectTimeoutTask, nFaceDetectDelay);
	        	}
	    		else if (nextState == TwsIntent.SCREEN_BRIGHT){
	    			twsStopFaceDetect();
	    		}
	    	}
		}
    }
    
    static final int TWS_FACE_DETECT_TIME_GAP = 4000;
    TwsFaceDetectTimeoutTask twsmFaceDetectTimeoutTask = new TwsFaceDetectTimeoutTask();
    boolean twsmFaceDetectStarted = false;
    class TwsFaceDetectTimeoutTask implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			twsStartFaceDetect();
		}
    }
    
    void twsStopFaceDetect(){
		if (twsmFaceDetectStarted){
			Intent intent = new Intent(TWS_ACTION_STOP_FACEDETECT);
    		if (intent != null){
//    			Log.d(TWS_TAG, "twsStopFaceDetect:: stop face detect!");
    			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    			mtwsContext.sendBroadcast(intent);
    			twsmFaceDetectStarted = false;
    		}
		}
    }
    
    void twsStartFaceDetect(){
    	Intent intent = new Intent(TWS_ACTION_START_FACEDETECT);
    	if (intent != null){
//    		Log.d(TWS_TAG, "twsStartFaceDetect:: start face detect!");
    		mtwsContext.sendBroadcast(intent);
    		twsmFaceDetectStarted = true;
    	}
    }
    
    private int mPowerState = -1;
    private boolean mDimMasked = false;
    public void twsSendPowerStateChangeBroadcast(int newState){
    	if (mBootComplete && mPowerState!=newState 
    			&& (mCurrentScreenBrightness >= TwsIntent.TWS_BRIGHTNESS_DIM 
    			|| (mBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC && !mHaveManualChangedWhenAutoMode))){
    		
//    		Log.d(TWS_TAG, "mPowerState = "+mPowerState +", newState = "+newState);
    		
    		Message msg = Message.obtain(mMaskHandler);
    		
    		if (newState > TwsIntent.SCREEN_DIM && mPowerState == TwsIntent.SCREEN_DIM){
    			msg.what = REMOVE_BRIGHTNESS_MASK;
    			mDimMasked = false;
    		}
    		else if (newState == TwsIntent.SCREEN_DIM && mPowerState > TwsIntent.SCREEN_DIM){
    			msg.what = SET_BRIGHTNESS_MASK;
    			msg.arg1 = 20;
    			mDimMasked = true;
    		}
    		mMaskHandler.sendMessage(msg);
    	}
    	
    	mPowerState = newState;
    }
    
    private boolean mIsNeedTwsMask = true;										
    private int mCurrentScreenBrightness;										
    private boolean mHaveMasked = false;
    private boolean mHaveManualChangedWhenAutoMode = false;
    private void isNeedTwsMask(){
    	mIsNeedTwsMask = SystemProperties.getBoolean("ro.tws.brightness_mask", false);
    }
    
    public int twsSetAutoBrightnessValue(int value){
    	int bRet = value;
    	
    	if(mBootComplete && mHaveManualChangedWhenAutoMode){
    		bRet = mCurrentScreenBrightness;
    		if (mIsNeedTwsMask){
        		Message msg = Message.obtain(mMaskHandler);
        		if (bRet < TwsIntent.TWS_BRIGHTNESS_DIM){
        			msg.what = SET_BRIGHTNESS_MASK;
        			msg.arg1 = bRet;
        			bRet = TwsIntent.TWS_BRIGHTNESS_DIM;
        		}
        		else {
        			msg.what = REMOVE_BRIGHTNESS_MASK;
        		}
        		mMaskHandler.sendMessage(msg);
        	}
    	}
    	
    	return bRet;
    }

    public int twsSetBacklightBrightness(int value){
    	
    	if (!mBootComplete){
    		return value;
    	}
    	
    	if (mBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC){
    		mHaveManualChangedWhenAutoMode = true;
    	}
    	else {
    		mHaveManualChangedWhenAutoMode = false;
    	}
    	
    	mCurrentScreenBrightness = value;
    	
//    	Log.d(TWS_TAG, "twsSetBacklightBrightness:: value = "+value);
    	if (mIsNeedTwsMask){
    		Message msg = Message.obtain(mMaskHandler);
    		if (value < TwsIntent.TWS_BRIGHTNESS_DIM){
    			msg.what = SET_BRIGHTNESS_MASK;
    			msg.arg1 = value;
    			value = TwsIntent.TWS_BRIGHTNESS_DIM;
    		}
    		else {
    			msg.what = REMOVE_BRIGHTNESS_MASK;
    		}
    		mMaskHandler.sendMessage(msg);
    	}
    	
    	return value;
    }
    
    LinearLayout mFloatLayout;
    WindowManager mWindowManager;
	WindowManager.LayoutParams wmParams;
	void createFloatView(){
		mWindowManager = (WindowManager) mtwsContext.getSystemService(Context.WINDOW_SERVICE);
		wmParams = new WindowManager.LayoutParams();
		
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE					// 不需要捕获焦点
				| LayoutParams.FLAG_NOT_TOUCHABLE							// 不需要响应触摸
				| LayoutParams.FLAG_DIM_BEHIND;								// 允许调节dimAmount参数
		
		
		wmParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY;					// 盖在所有应用窗口的上面，包括通知栏和锁屏
		wmParams.format = PixelFormat.RGBA_8888;  
		     
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;  
		wmParams.dimAmount = 0.0f;
     
		wmParams.x = 0;  
        wmParams.y = 0;  
        
        wmParams.width = 0;
        wmParams.height = 0;

        mFloatLayout = new LinearLayout(mtwsContext);
        
        mFloatLayout.setBackgroundColor(0x00000000);
	}
	
	void setBrightnessMaskValue(int value){
		if (mWindowManager != null && mFloatLayout != null && wmParams != null && mHaveMasked){
			float dimValue = 1.0f-(float)value/100.0f;
			wmParams.dimAmount = dimValue<0.9f?dimValue:0.9f;
			mWindowManager.updateViewLayout(mFloatLayout, wmParams);
		}
	}
}
