package com.tencent.tws.assistant.content;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemProperties;

public class TwsIntent {
	
	/*添加App到黑名单时的广播*/
	public static final String TWS_ADD_BLACKLIST_APP_ACTION = "tws.intent.action.ADD_BLACKLIST_APP";
	
	/*从黑名单中取消App的广播*/
    public static final String TWS_REMOVE_BLACKLIST_APP_ACTION = "tws.intent.action.REMOVE_BLACKLIST_APP";
    
    /*添加和删除App时，携带包名的exra*/
    public static final String TWS_BLACK_APP_PACKAGE_NAME = "tws.intent.extra.BLACKAPP_PACKAGENAME";
    
    
    /*系统电源状态发生转换时的消息*/
    public static final String TWS_POWER_STATE_CHANGE = "tws.intent.action.POWERSTATE_CHANGE";
    public static final String TWS_POWER_STATE_CHANGE_EXTRA = "tws.intent.extra.POWERSTATE_CHANGE";
    
    public static final String TWS__FACE_DECTECTED_SUCCESS = "tws.action.face_detected_success";
    
    // flags for setPowerState
    public static final int SCREEN_ON_BIT          = 0x00000001;
    public static final int SCREEN_BRIGHT_BIT      = 0x00000002;
    public static final int BUTTON_BRIGHT_BIT      = 0x00000004;
    public static final int KEYBOARD_BRIGHT_BIT    = 0x00000008;
    
    // SCREEN_OFF == everything off
    public static final int SCREEN_OFF         = 0x00000000;

    // SCREEN_DIM == screen on, screen backlight dim
    public static final int SCREEN_DIM         = SCREEN_ON_BIT;

    // SCREEN_BRIGHT == screen on, screen backlight bright
    public static final int SCREEN_BRIGHT      = SCREEN_ON_BIT | SCREEN_BRIGHT_BIT;

    // SCREEN_BUTTON_BRIGHT == screen on, screen and button backlights bright
    public static final int SCREEN_BUTTON_BRIGHT  = SCREEN_BRIGHT | BUTTON_BRIGHT_BIT;

    // SCREEN_BUTTON_BRIGHT == screen on, screen, button and keyboard backlights bright
    public static final int ALL_BRIGHT         = SCREEN_BUTTON_BRIGHT | KEYBOARD_BRIGHT_BIT;
 
  	/**
     * The internal version code of NANJI, in integer.
     * added by hendysu, 2011-03-07
     */
    public static final int NANJI_VER_INT = SystemProperties.getInt("ro.build.version.nanji", 0);
    
    /**
     * The internal version time of NANJI, in long.
     * added, 2011-05-30
     */
    public static final long NANJI_RELEASE_LONG_TIME = SystemProperties.getLong("ro.build.nanji.releaseTime", 0);
    /**
    /**
     * The internal version of NANJI, in string.
     * added by hendysu, 2011-03-07
     */
    public static final String NANJI_VER_STR = getString("ro.build.version.nanji.display");
	/**
     * The internal DEVICE of NANJI, in string.
     * added 2012-05-15
     */
	public static final String NANJI_DEVICE_STR = getString("ro.tws.device");
	
	public static String getString(String property) {
        return SystemProperties.get(property, Build.UNKNOWN);
    }
	
	public static final int TWS_BRIGHTNESS_DIM = 20;

	private static Canvas mCanvas;
	private static Paint mPaint;
	public static void init() {
		if(mCanvas == null){
			mCanvas = new Canvas();
		}
		if(mPaint == null){
			mPaint = new Paint();
			mPaint.setFilterBitmap(false);
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		}
	}
	public static Bitmap getBitmapWithNoScale(Drawable background, Drawable mIcon) {
		init();
		Bitmap background_bit = ((BitmapDrawable) background).getBitmap();
		Bitmap icon = ((BitmapDrawable) mIcon).getBitmap();
		background_bit = background_bit.copy(Bitmap.Config.ARGB_8888, true);
		Bitmap copy = Bitmap.createScaledBitmap(background_bit, background_bit.getWidth(),
				background_bit.getHeight(), true);
		
		if ((icon.getWidth() >= background.getIntrinsicWidth()*0.8f) || (icon.getHeight() >= background.getIntrinsicHeight()*0.8f)) {
			icon = Bitmap.createScaledBitmap(icon, background.getIntrinsicWidth(), background.getIntrinsicHeight(),
					true);
			mCanvas.setBitmap(icon);
			mCanvas.drawBitmap(copy, 0, 0, mPaint);
			mCanvas.setBitmap(null);

			icon = Bitmap.createScaledBitmap(icon, (int)(background.getIntrinsicWidth()*0.9f), (int)(background.getIntrinsicHeight()*0.9f),
					true);
		}
		
		mCanvas.setBitmap(background_bit);
		mCanvas.drawBitmap(icon, ((background_bit.getWidth() - icon.getWidth()) / 2.0f), ((background_bit.getHeight() - icon.getHeight()) / 2.0f), null);
		mCanvas.setBitmap(null);
	
		return background_bit;
	}
	public static BitmapDrawable getDrawableWithNoScale(Drawable background, Drawable mIcon) {
		Bitmap bp = getBitmapWithNoScale(background,mIcon);
		BitmapDrawable bpd = new BitmapDrawable(bp);
		return bpd;
	}
}
