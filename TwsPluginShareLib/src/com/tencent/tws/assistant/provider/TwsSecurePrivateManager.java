package com.tencent.tws.assistant.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.net.Uri;
import android.provider.BaseColumns;

import com.tencent.tws.assistant.provider.TwsPrivateSpaces.Actions;
import com.tencent.tws.assistant.provider.TwsPrivateSpaces.Spaces;

public class TwsSecurePrivateManager extends ContextWrapper {	
	final static String TAG = "twsSecure";
	
	public  class PrivateListItem{
	 public	int privateId;
	 public		String strPassWd;
	 public		String strEmail;
	};

	private final static boolean DEBUG = false; 
	
	private static final String AUTHORITY = "private_spaces";
	private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	private static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "spaces");
	private static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");
	
	public final static  String TWS_SECURE_CHANGE_PRIVATEID = "tws_secure_change_privateid";
	public final static  String TWS_SECURE_DELETE_PRIVATEID = "tws_secure_delelte_privateid";
	public final static  String TWS_PRIVATEID = "tws_privateid";
	public final static  int  INCOMING_SETTING_TYPE = 0;
	public final static  int  INSMSTITLE_SETTING_TYPE = 1;
	public final static  int  INSMSCONTENT_SETTING_TYPE = 2;
	private final static String  TWS_SECURE_RECEIVER_PERMISSION = "com.tencent.twsprivatespaces.permission.READ_PRIVATE_SPACES";
	public static final  int  PRIVATE_PASSWD_MIN_LEN = 4;
	public static final  int  PRIVATE_PASSWD_MAX_LEN = 16;
	public static final  int  PRIVATE_COUNT_MAX = 65535;
	
	private  Context  mContext;
	private  static int mCurrentPrivateId = 0;

	private static PrivateListItem item;
	//private static List<PrivateListItem>mPrivateListItem = new ArrayList<PrivateListItem>();
	
	public TwsSecurePrivateManager(Context context) {
		super(context);
		mContext = context;
		// TODO Auto-generated constructor stub
	}
	
	public  int getPrivateCount(Context  context){
		int count = -1;
		if(context == null){
			return count;
		}
		count = Spaces.getPrivateSpaceCount(context);
		return count;
	}
	
	public  int getCurrentPrivateId(Context  context){
		int privateid = -1;
		if(context == null){
			return privateid;
		}	
//		Log.d(TAG, "#### getCurrentPrivateId mCurrentPrivateId = "+mCurrentPrivateId);
		privateid = Spaces.getCurrentPrivateSpace(context);
//		Log.d(TAG, "getCurrentPrivateId  privateid = "+privateid);
		if(privateid >= 0){
			mCurrentPrivateId = privateid;
		}
//		Log.w(TAG, "getCurrentPrivateId mCurrentPrivateId = "+mCurrentPrivateId);
		return mCurrentPrivateId;
	}
	
	public  int exitCurrentPrivateId(Context context) {
		int reValue = -1;
		if (context == null) {
			return reValue;
		}
//		Log.d(TAG, "exitCurrentPrivateId  ");
		
		Spaces.exitCurrentPrivateSpace(context);
		mCurrentPrivateId = 0;
		Intent intent = new Intent(TWS_SECURE_CHANGE_PRIVATEID);
		intent.putExtra(TWS_PRIVATEID, mCurrentPrivateId);
		//context.sendBroadcast(intent);
		context.sendOrderedBroadcast(intent, TWS_SECURE_RECEIVER_PERMISSION);

		return reValue;
	}
		
	public  int setCurrentPrivateId(Context context, String strPasswd) {
		int prviateId = -1;
		if (TextUtils.isEmpty(strPasswd) || context == null) {
			return prviateId;
		}
		String strText = strPasswd.trim();
		if (strText.length() < PRIVATE_PASSWD_MIN_LEN) {
			return prviateId;
		}
		String strFirstChar = strText.substring(0, 1);
		int len = strText.length();
		String strEndChar = strText.substring(len - 2, len - 1);
		int index = -1;
		index = strText.indexOf("*");
//		Log.w(TAG, "setCurrentPrivateId index = "+index);
		if (index >= 0) {
			strText = strText.substring(index + 1);
		}
//		Log.w(TAG, "setCurrentPrivateId strText = "+strText);
		index = -1;
		index = strText.indexOf("*");
//		Log.w(TAG, "setCurrentPrivateId ##### index = "+index);
		if (index >= 0) {
			strText = strText.substring(0, index);
		}
//		Log.w(TAG, "setCurrentPrivateId ####  strText = "+strText);
		if(TextUtils.isEmpty(strText) || (!TextUtils.isDigitsOnly(strText))){
//			Log.w(TAG, "setCurrentPrivateId #### ############********* ");
			return prviateId;
		}
		
		if (strText.length() < PRIVATE_PASSWD_MIN_LEN || strText.length() > PRIVATE_PASSWD_MAX_LEN) {
			return prviateId;
		}
//		Log.w(TAG, "setCurrentPrivateId strText = "+strText);
		prviateId = Spaces.setCurrentPrivateSpace(context, strText);
//		Log.w(TAG, "setCurrentPrivateId prviateId = "+prviateId);
		if (prviateId > 0) {			
			mCurrentPrivateId = prviateId;
			Intent intent = new Intent(TWS_SECURE_CHANGE_PRIVATEID);
			intent.putExtra(TWS_PRIVATEID, mCurrentPrivateId);
			//context.sendBroadcast(intent);
			context.sendOrderedBroadcast(intent, TWS_SECURE_RECEIVER_PERMISSION);
		}
//		Log.w(TAG, "setCurrentPrivateId mCurrentPrivateId = "+mCurrentPrivateId);
		return mCurrentPrivateId;
	}
	
	public  int modifyPrivateIdPasswd(Context context,
			String strOldPasswd, String strNewPasswd) {
		int reValue = -1;
		if(context == null || TextUtils.isEmpty(strOldPasswd) || TextUtils.isEmpty(strNewPasswd)){
			return reValue;
		}
		reValue = Spaces.setPrivateSpacePassword(context, strOldPasswd, strNewPasswd);
		return reValue;
	}
	
	public  String getPrivateIdPasswd(Context context, int privateId){
		String strPassWd = null ;
		if(context == null || privateId <= 0){
			return strPassWd;
		}
		final ContentResolver resolver = context.getContentResolver();
		String[] prjection = {Spaces._ID, Spaces.PASSWD };
		String where = Spaces._ID + " = " + String.valueOf(privateId);
		
		Cursor cursor = null;
		try {
			cursor = resolver.query(CONTENT_URI, prjection, where, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(1);
			}			
		} finally {
			if (cursor != null) cursor.close();
		}
		return strPassWd; 
	}
	
	public  int setPrivateIdEmail(Context context, String oldEmail, String newEmail){
		int reValue = -1;
		if(context == null || TextUtils.isEmpty(oldEmail) || TextUtils.isEmpty(newEmail)){
			return reValue;
		}
		reValue = Spaces.setPrivateSpaceEmail(context, oldEmail, newEmail);
		return reValue;
	}
	
	public  String getPrivateIdEmail(Context context, int privateId){
		String strEmail = null ;
		if(context == null || privateId <= 0){
			return strEmail;
		}
		final ContentResolver resolver = context.getContentResolver();
		String[] prjection = {Spaces._ID, Spaces.EMAIL };
		String where = Spaces._ID + " = " + String.valueOf(privateId);
		
		Cursor cursor = null;
		try {
			cursor = resolver.query(CONTENT_URI, prjection, where, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(1);
			}			
		} finally {
			if (cursor != null) cursor.close();
		}
		return strEmail; 
	}
	
	public  String getCurrentPrivateIdEmail(Context context){
		String strEmail = null;
		if(context == null){
			return strEmail;
		}
		strEmail = Spaces.getPrivateSpaceEmail(context);
		return strEmail;
	}
	
	public  List<PrivateListItem> getPrivateAllItem(Context context){
		List<PrivateListItem>listItem = new ArrayList<PrivateListItem>();
		if(context == null){
			return null;
		}
		final ContentResolver resolver = context.getContentResolver();
		String[] prjection = {Spaces._ID, Spaces.PASSWD, Spaces.EMAIL };
		Cursor cursor = null;
		try {
			cursor = resolver.query(CONTENT_URI, prjection, null, null, null);
			if(cursor != null){
				cursor.moveToFirst();
				while(!cursor.isAfterLast()){
					PrivateListItem item = new PrivateListItem();
					item.privateId = cursor.getInt(0);
					item.strPassWd = cursor.getString(1);
					item.strEmail  = cursor.getString(2);
					listItem.add(item);
					cursor.moveToNext();
				}
			}
		} finally {
			if (cursor != null) cursor.close();
		}		
		return listItem;
	}
	
	public  int addPrivateId(Context context, String passwd, String strEmail){
		int reValue = -1;
		if(context == null || TextUtils.isEmpty(passwd) || TextUtils.isEmpty(strEmail)){
			return reValue;
		}
		reValue = Spaces.conformPrivateSpace(context, passwd);
		if(reValue > 0){
			return -2;
		}
			
		int privateSum = getPrivateCount(context);
		if(DEBUG){
			if(privateSum > 2){
				return -3; //
			}
		}
		if(privateSum >= PRIVATE_COUNT_MAX){
			return -3; //
		}	
		
		reValue = Spaces.addPrivateSpace(context, passwd, strEmail);
		int privateId = Spaces.getCurrentPrivateSpace(context);
		if(reValue == privateId){
			mCurrentPrivateId = privateId;
			Intent intent = new Intent(TWS_SECURE_CHANGE_PRIVATEID);
			intent.putExtra(TWS_PRIVATEID, mCurrentPrivateId);
			//context.sendBroadcast(intent);	
			context.sendOrderedBroadcast(intent, TWS_SECURE_RECEIVER_PERMISSION);
			reValue = mCurrentPrivateId;
		}
		return reValue;
	}
	
	public  boolean  deletePrivateId(Context context, int privateId){
		boolean reValue = false;
		if(context == null || privateId <= 0){
			return reValue;
		}
		reValue = Spaces.deletePrivateSpace(context, privateId);
		if(reValue){			
			Intent intent = new Intent(TWS_SECURE_DELETE_PRIVATEID);
			intent.putExtra(TWS_PRIVATEID, mCurrentPrivateId);
			//context.sendBroadcast(intent);	
			context.sendOrderedBroadcast(intent, TWS_SECURE_RECEIVER_PERMISSION);
			mCurrentPrivateId = 0;
		}
		return reValue;
	}
	
	public  int getActionValue(Context context, int privateid, int type){
		int reValue = -1;
		if(context == null || privateid <= 0 || type < 0){
			return reValue;
		}
		reValue = Actions.getActionsValue(context, privateid, type);
		return reValue;
	}
	
	public  boolean setActionValue(Context context, int privateid, int type, int value){
		boolean reValue = false;
		if(context == null || value < 0 || type < 0 || privateid <= 0){
			return reValue;
		}
		
		if(privateid != mCurrentPrivateId){
			return reValue;
		}
		reValue = Actions.setActionsValue(context, type, value);
		return reValue;
	}
	
	public boolean setActionTypeValue(Context context, int privateid, int type, String strValue){
		boolean reValue = false;		
		if(context == null || privateid <= 0 || type < 0 || TextUtils.isEmpty(strValue)){
			return reValue;
		}
		if(privateid != mCurrentPrivateId){
			return reValue;
		}
		reValue = Actions.setActionsRemark(context, type, strValue);
		return reValue;
	}
	
	public String getActionTypeValue(Context context, int privateid, int type){
		String strValue = null;
		if(context == null || privateid <= 0 || type < 0){
			return strValue;
		}
		//if(privateid != mCurrentPrivateId){
		//	return strValue;
		//}
		strValue = Actions.getActionsRemark(context, privateid, type);
		return strValue;
	}
}