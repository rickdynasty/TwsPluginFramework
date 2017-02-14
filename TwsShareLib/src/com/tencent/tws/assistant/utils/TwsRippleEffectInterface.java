package com.tencent.tws.assistant.utils;

import java.lang.reflect.Method;

import com.tencent.tws.assistant.utils.ReflectUtils;

import android.content.Context;
import android.util.Log;
import android.view.View;

public class TwsRippleEffectInterface {
	
	private static Method mTwsMethod_SetupOverlayWithoutArgs_delay = null;
	private static Method mTwsMethod_SetupOverlayWithArgs_delay = null;
	private static Method mTwsMethod_SetupOverlayWithoutArgs = null;
	private static Method mTwsMethod_SetupOverlayWithArgs = null;
	private static Method mTwsMethod_SetupOverlayTmp = null;
	private static Method mTwsMethod_RemoveOverlay = null;
	public static void setupEffectionOverlay(View v, Context context){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayWithoutArgs == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayWithoutArgs = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class);
			}
			 
			mTwsMethod_SetupOverlayWithoutArgs.invoke(v, context);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithoutArgs = null;
		}
			
	
		
	}
	public static void setupEffectionOverlay(View v, Context context, long delay){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayWithoutArgs_delay == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayWithoutArgs_delay = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class, long.class);
			}
			 
			mTwsMethod_SetupOverlayWithoutArgs_delay.invoke(v, context, delay);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithoutArgs_delay = null;
		}
			
	
		
	}

	public static void setupEffectionOverlay(View v, Context context, long delay, int alpha){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayWithoutArgs == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayWithoutArgs = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class, long.class, int.class);
			}
			 
			mTwsMethod_SetupOverlayWithoutArgs.invoke(v, context, delay, alpha);
		} catch (Exception e) {
 
			Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithoutArgs = null;
		}
			
	
		
	}
	
	//temp
	public static void setupEffectionOverlay(View v, Context context, long delay, int alpha, int animalpha, int color){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayTmp == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayTmp = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class, long.class, int.class, int.class, int.class);
			}
			 
			mTwsMethod_SetupOverlayTmp.invoke(v, context, delay, alpha, animalpha, color);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayTmp = null;
		}
			
	
		
	}
	//
	public static void setupEffectionOverlay(View v, Context context, long delay, int cl, int ct, int cr, int cb){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayWithArgs_delay == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayWithArgs_delay = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class, long.class, int.class, int.class, int.class, int.class);
			}
			 
			mTwsMethod_SetupOverlayWithArgs_delay.invoke(v, context, delay, cl, ct, cr, cb);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithArgs_delay = null;
		}
			
	
		
	}
	
	
	public static void setupEffectionOverlay(View v, Context context, int cl, int ct, int cr, int cb){
		 
		try {
			
			
			
			if(mTwsMethod_SetupOverlayWithArgs == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_SetupOverlayWithArgs = ReflectUtils.getDeclaredMethod(c, "twsSetupEffectionOverlay", android.content.Context.class, int.class, int.class, int.class, int.class);
			}
			 
			mTwsMethod_SetupOverlayWithArgs.invoke(v, context, cl, ct, cr, cb);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithArgs = null;
		}
			
	
		
	}
	
	public static void removeEffectionOverlay(View v){
		 
		try {
			
			
			
			if(mTwsMethod_RemoveOverlay == null){
				Class c = ReflectUtils.forClassName("android.view.View");
				mTwsMethod_RemoveOverlay = ReflectUtils.getDeclaredMethod(c, "twsRemoveEffectionOverlay");
			}
			 
			mTwsMethod_RemoveOverlay.invoke(v);
		} catch (Exception e) {
 
			//Log.e("lzh", "" + e);
			
			mTwsMethod_SetupOverlayWithArgs = null;
		}
			
	
		
	}

}
