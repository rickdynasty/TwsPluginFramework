package com.tencent.tws.assistant.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class TwsPrivateSpaces {
	private static final String TAG = "TwsPrivateSpaces";
	
	public static final String AUTHORITY = "private_spaces";
	
    /**
     * The content:// style URL for this provider
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    
    /**
     * no public constructor since this is a utility class
     */
    private TwsPrivateSpaces() {}
    
	public static final class Spaces implements BaseColumns {
        /**
         * no public constructor since this is a utility class
         */
		private Spaces() {}
		
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "spaces");
        public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");
        
		public static final String ACTION_PRIVATE_SPACES_DELETED = "android.intent.action.PRIVATE_SPACES_DELETED";

		public static final String PASSWD = "passwd";
		public static final String EMAIL = "email";
		public static final String IS_ACTIVE = "is_active";
		public static final String COUNT = "count";
		public static final String LAST_TIME = "last_time";
		public static final String IS_READ = "is_read";
		public static final String PASSWD_TYPE = "passwd_type";
		public static final String PASSWD_DATA = "passwd_data";
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/spaces";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/spaces";

		public static int getPrivateSpaceCount(Context context) {
			final ContentResolver resolver = context.getContentResolver();
			String[] prjection = { Spaces._ID };
			
			Cursor cursor = null;
            try {
            	cursor = resolver.query(CONTENT_URI, prjection, null, null, null);
                if (cursor == null || !cursor.moveToFirst()) {
                    return 0;
                }
                return cursor.getCount();
            } finally {
                if (cursor != null) cursor.close();
            }
		}
		
		public static int getCurrentPrivateSpace(Context context) {
			final ContentResolver resolver = context.getContentResolver();
			String selection = IS_ACTIVE + " = 1";
			String[] prjection = { Spaces._ID };
			
			Cursor cursor = null;
			try {
				cursor = resolver.query(CONTENT_URI, prjection, selection, null, null);
				if (cursor == null || !cursor.moveToFirst()) {
					return 0;
				}
				return cursor.getInt(0);
			} finally {
				if (cursor != null) cursor.close();
			}
		}
		
		public static void exitCurrentPrivateSpace(Context context) {
			final ContentResolver resolver = context.getContentResolver();
			
			ContentValues values = new ContentValues();
			values.put(IS_ACTIVE, String.valueOf(0));
			String where = IS_ACTIVE + " = 1";
			
			resolver.update(CONTENT_URI, values, where, null);
		}
		
		public static int setCurrentPrivateSpace(Context context, String password) {
			final ContentResolver resolver = context.getContentResolver();
			
			int privateSpace = conformPrivateSpace(context, password);
			if (privateSpace <= 0) return 0;

			exitCurrentPrivateSpace(context);
			
			ContentValues values = new ContentValues();
			values.put(IS_ACTIVE, String.valueOf(1));
			String where = BaseColumns._ID + " = " + String.valueOf(privateSpace);
			
			int ret = resolver.update(CONTENT_URI, values, where, null);
			
			return (ret > 0) ? privateSpace : 0;
		}
		
		public static int conformPrivateSpace(Context context, String password) {
			final ContentResolver resolver = context.getContentResolver();
			String[] prjection = { Spaces._ID };
			
			Cursor cursor = null;
			try {
				cursor = resolver.query(Uri.withAppendedPath(CONTENT_FILTER_URI, password), prjection, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getInt(0);
				}
				return 0; 
			} finally {
				if (cursor != null) cursor.close();
			}
		}
		
		public static int setPrivateSpacePassword(Context context, String oldPassword, 
				String newPassword) {
			final ContentResolver resolver = context.getContentResolver();
			
			int oldPrivateSpace = conformPrivateSpace(context, oldPassword);
			if (oldPrivateSpace <= 0) return -1;

			int current = getCurrentPrivateSpace(context);
			if (current != oldPrivateSpace) return -1;
			
			int newPrivateSpace = conformPrivateSpace(context, newPassword);
			if (newPrivateSpace > 0) return -1;
			
			ContentValues values = new ContentValues();
			values.put(PASSWD, newPassword);
			String where = BaseColumns._ID + " = " + String.valueOf(oldPrivateSpace);
			
			int ret = resolver.update(CONTENT_URI, values, where, null);
			
			return (ret > 0) ? 1 : 0;
		}
		
		public static String getPrivateSpaceEmail(Context context) {
			final ContentResolver resolver = context.getContentResolver();
			
			int privateSpace = getCurrentPrivateSpace(context);
			if (privateSpace <= 0) return null;
			String[] projection = { BaseColumns._ID, EMAIL };
			
			String selection = BaseColumns._ID + " = " + String.valueOf(privateSpace);
			
			Cursor cursor = null;
			try {
				cursor = resolver.query(CONTENT_URI, projection, selection, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getString(1);
				}
				return null;
			} finally {
				if (cursor != null) cursor.close();
			}
		}
		
		public static int setPrivateSpaceEmail(Context context, String oldEmail,
				String newEmail) {
			final ContentResolver resolver = context.getContentResolver();

			int privateSpace = getCurrentPrivateSpace(context);
			if (privateSpace <= 0) return -1;
			
			String email = getPrivateSpaceEmail(context);
			if (!email.equals(oldEmail)) return -1;
			
			ContentValues values = new ContentValues();
			values.put(EMAIL, newEmail);
			String where = BaseColumns._ID + " = " + String.valueOf(privateSpace);
			
			return resolver.update(CONTENT_URI, values, where, null);
		}
		
		public static int addPrivateSpace(Context context, String password, String email) {
			final ContentResolver resolver = context.getContentResolver();
			
			exitCurrentPrivateSpace(context);

			ContentValues values = new ContentValues();
			values.put(PASSWD, password);
			values.put(EMAIL, email);
			values.put(IS_ACTIVE, String.valueOf(1));
			Uri uri = resolver.insert(CONTENT_URI, values);
			
			if (uri != null && uri.getPathSegments().size() >= 2) {
				return Integer.valueOf(uri.getPathSegments().get(1));
			}
				
			return 0;
		}
		
		public static boolean deletePrivateSpace(Context context, int privateSpace) { 
			final ContentResolver resolver = context.getContentResolver();
			
//			int privateSpace = Spaces.getCurrentPrivateSpace(context);
			if (privateSpace <= 0) return false;
			
			resolver.delete(Uri.withAppendedPath(CONTENT_URI, String.valueOf(privateSpace)), 
					null, null);
			resolver.delete(Uri.withAppendedPath(Actions.CONTENT_URI, String.valueOf(privateSpace)), 
					null, null);
			
			return true;
		}
	}
	
	public static final class Actions {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "actions");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/actions";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/actions";
		
		public static final String SPACES_ID = "spaces_id";
		public static final String TYPE = "type";
		public static final String VALUE = "value";
		public static final String REMARK = "remark";
		
		public static int getActionsValue(Context context, int privateSpace, int type) {
			final ContentResolver resolver = context.getContentResolver();

			String selection = "(" + SPACES_ID + " = " + String.valueOf(privateSpace) + ")";
			selection += "AND (" + TYPE + " = " + String.valueOf(type) + ")";
			String[] projection = { SPACES_ID, VALUE };
			
			Cursor cursor = null;
			try {
				cursor = resolver.query(CONTENT_URI, projection, selection, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getInt(1);
				}
				return -1;
			} finally {
				if (cursor != null) cursor.close();
			}
		}
		
		public static boolean setActionsValue(Context context, int type, int value) {
			final ContentResolver resolver = context.getContentResolver();
			
			int privateSpace =  Spaces.getCurrentPrivateSpace(context);
			if (privateSpace <= 0) return false;
			
			int oldValue = getActionsValue(context, privateSpace, type);
			
			ContentValues values = new ContentValues();
			values.put(VALUE, String.valueOf(value));
		
			if (oldValue < 0) {
				values.put(SPACES_ID, String.valueOf(privateSpace));
				values.put(TYPE, String.valueOf(type));
				resolver.insert(CONTENT_URI, values);
			} else {
				String where = "(" + SPACES_ID + " = " + String.valueOf(privateSpace) + ")";
				where += "AND (" + TYPE + " = " + String.valueOf(type) + ")";
				resolver.update(CONTENT_URI, values, where, null);
			}
			
			return true;
		}
		
		public static String getActionsRemark(Context context, int privateSpace, int type) {
			final ContentResolver resolver = context.getContentResolver();

			String selection = "(" + SPACES_ID + " = " + String.valueOf(privateSpace) + ")";
			selection += "AND (" + TYPE + " = " + String.valueOf(type) + ")";
			String[] projection = { SPACES_ID, REMARK };
			
			Cursor cursor = null;
			try {
				cursor = resolver.query(CONTENT_URI, projection, selection, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getString(1);
				}
				return null;
			} finally {
				if (cursor != null) cursor.close();
			}
		}
		
		public static boolean setActionsRemark(Context context, int type, String remark) {
			final ContentResolver resolver = context.getContentResolver();
			
			int privateSpace =  Spaces.getCurrentPrivateSpace(context);
			if (privateSpace <= 0) return false;
			int oldValue = getActionsValue(context, privateSpace, type);
			
			ContentValues values = new ContentValues();
			values.put(REMARK, remark);
			
			if (oldValue < 0) {
				values.put(SPACES_ID, String.valueOf(privateSpace));
				values.put(TYPE, String.valueOf(type));
				resolver.insert(CONTENT_URI, values);
			} else {
				String where = "(" + SPACES_ID + " = " + String.valueOf(privateSpace) + ")";
				where += "AND (" + TYPE + " = " + String.valueOf(type) + ")";
				resolver.update(CONTENT_URI, values, where, null);
			}
			
			return true;
		}
	}
}
