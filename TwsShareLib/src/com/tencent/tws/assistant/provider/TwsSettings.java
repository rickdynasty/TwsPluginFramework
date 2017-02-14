/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.tencent.tws.assistant.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.BaseColumns;
import android.util.AndroidException;
import android.util.Log;
import java.util.HashMap;


/**
 * The Settings provider contains global system-level device preferences.
 */
public final class TwsSettings {

     /**
     * @hide - Private call() method on SettingsProvider to read from 'system' table.
     */
    public static final String CALL_METHOD_GET_SYSTEM = "GET_system";

    public static final String AUTHORITY = "twssettings";

    private static final String TAG = "TwsSettings";
    private static final boolean LOCAL_LOGV = false;;

    public static class SettingNotFoundException extends AndroidException {
        public SettingNotFoundException(String msg) {
            super(msg);
        }
    }

    /**
     * Common base for tables of name/value settings.
     */
    public static class NameValueTable implements BaseColumns {
        public static final String NAME = "name";
        public static final String VALUE = "value";

        protected static boolean putString(ContentResolver resolver, Uri uri,
                String name, String value) {
            // The database will take care of replacing duplicates.
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, name);
                values.put(VALUE, value);
                resolver.insert(uri, values);
                return true;
            } catch (SQLException e) {
                Log.w(TAG, "Can't set key " + name + " in " + uri, e);
                return false;
            }
        }

        public static Uri getUriFor(Uri uri, String name) {
            return Uri.withAppendedPath(uri, name);
        }
    }

    // Thread-safe.
    private static class NameValueCache {
        private final String mVersionSystemProperty;
        private final Uri mUri;

        private static final String[] SELECT_VALUE = new String[] { TwsSettings.NameValueTable.VALUE };
        private static final String NAME_EQ_PLACEHOLDER = "name=?";

        // Must synchronize on 'this' to access mValues and mValuesVersion.
        private final HashMap<String, String> mValues = new HashMap<String, String>();
        private long mValuesVersion = 0;

        // Initially null; set lazily and held forever.  Synchronized on 'this'.
        private IContentProvider mContentProvider = null;

        // The method we'll call (or null, to not use) on the provider
        // for the fast path of retrieving settings.
        private final String mCallCommand;

        public NameValueCache(String versionSystemProperty, Uri uri, String callCommand) {
            mVersionSystemProperty = versionSystemProperty;
            mUri = uri;
            mCallCommand = callCommand;
        }

        public String getString(ContentResolver cr, String name) {
            long newValuesVersion = SystemProperties.getLong(mVersionSystemProperty, 0);
			
            synchronized (this) {
                if (mValuesVersion != newValuesVersion) {
                    if (LOCAL_LOGV) {
                        Log.v(TAG, "invalidate [" + mUri.getLastPathSegment() + "]: current " +
                                newValuesVersion + " != cached " + mValuesVersion);
                    }

                    mValues.clear();
                    mValuesVersion = newValuesVersion;
                }

                if (mValues.containsKey(name)) {
                    return mValues.get(name);  // Could be null, that's OK -- negative caching
                }
            }

            IContentProvider cp = null;
            synchronized (this) {
                cp = mContentProvider;
                if (cp == null) {
                    cp = mContentProvider = cr.acquireProvider(mUri.getAuthority());
                }
            }

            // Try the fast path first, not using query().  If this
            // fails (alternate Settings provider that doesn't support
            // this interface?) then we fall back to the query/table
            // interface.
            if (mCallCommand != null) {
                try {
                    Bundle b = cp.call(cr.getPackageName(),mCallCommand, name, null);
                    if (b != null) {
                        String value = b.getPairValue();
                        synchronized (this) {
                            mValues.put(name, value);
                        }
                        return value;
                    }
                    // If the response Bundle is null, we fall through
                    // to the query interface below.
                } catch (RemoteException e) {
                    // Not supported by the remote side?  Fall through
                    // to query().
                }
            }

            Cursor c = null;
            try {
                c = cp.query(cr.getPackageName(),mUri, SELECT_VALUE, NAME_EQ_PLACEHOLDER,
                             new String[]{name}, null,null);
                if (c == null) {
                    Log.w(TAG, "Can't get key " + name + " from " + mUri);
                    return null;
                }

                String value = c.moveToNext() ? c.getString(0) : null;
                synchronized (this) {
                    mValues.put(name, value);
                }
                if (LOCAL_LOGV) {
                    Log.v(TAG, "cache miss [" + mUri.getLastPathSegment() + "]: " +
                            name + " = " + (value == null ? "(null)" : value));
                }
                return value;
            } catch (RemoteException e) {
                Log.w(TAG, "Can't get key " + name + " from " + mUri, e);
                return null;  // Return null, but don't cache it.
            } finally {
                if (c != null) c.close();
            }
        }
    }

    /**
     * System settings, containing miscellaneous system preferences.  This
     * table holds simple name/value pairs.  There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class System extends NameValueTable {
        public static final String SYS_PROP_SETTING_VERSION = "sys.tws_system_version";

        // Populated lazily, guarded by class object:
        private static NameValueCache sNameValueCache = null;
        
        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public synchronized static String getString(ContentResolver resolver, String name) {
            if (sNameValueCache == null) {
                sNameValueCache = new NameValueCache(SYS_PROP_SETTING_VERSION, CONTENT_URI, CALL_METHOD_GET_SYSTEM);
            }
            return sNameValueCache.getString(resolver, name);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putString(resolver, CONTENT_URI, name, value);
        }

        /**
         * Construct the content URI for a particular name/value pair,
         * useful for monitoring changes with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI, or null if not present
         */
        public static Uri getUriFor(String name) {
            return getUriFor(CONTENT_URI, name);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            String v = getString(cr, name);
            try {
                return v != null ? Integer.parseInt(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putString(cr, name, Integer.toString(value));
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            String valString = getString(cr, name);
            long value;
            try {
                value = valString != null ? Long.parseLong(valString) : def;
            } catch (NumberFormatException e) {
                value = def;
            }
            return value;
        }


        /**
         * Convenience function for updating a single settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putString(cr, name, Long.toString(value));
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            String v = getString(cr, name);
            try {
                return v != null ? Float.parseFloat(v) : def;
            } catch (NumberFormatException e) {
                return def;
            }
        }


        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putString(cr, name, Float.toString(value));
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/system");
        public static final String QROM_POWER_SAVE_MODE = "power_save_mode";	
        public static final String QROM_POWER_SAVE_ENABLE_LEVEL = "power_save_enable_level";
        public static final String QROM_POWER_SAVE_MODE_SETTING = "power_save_mode_setting";      

		/**add for sleep plan
			0:close
			1:open
			2:running
		*/
		public static final String QROM_SLEEP_PLAN_STATUS = "sleep_plan_status";
		public static final String QROM_MOBILE_DATA_USER_SET = "mobile_data_user_set";
		public static final String QROM_NETWORK_TYPE_USER_SET = "network_type_user_set";
		public static final String QROM_AIRPLANE_USER_SET = "airplane_user_set";
        
        /**
         * Control cpu power save settings
         */
        public static final String QROM_CPU_SETTING = "cpu_setting"; 
        
        /**
         * Control whether enable night mode
         */
        public static final String QROM_NIGHT_MODE_SETTING = "night_mode_setting";
        
        /**
         * Control whether enable auto kill bk proc when screen off
         */
        public static final String QROM_AUTO_KILL_BK_PROC_SETTING = "auto_kill_bk_proc";

        /**
         * Control whether enable auto close wifi
         */
        public static final String QROM_AUTO_CLOSE_WIFI_EBABLE = "auto_close_wifi_enable";
        
        /**
         * Control whether enable auto close bluetooth
         */
        public static final String QROM_AUTO_CLOSE_BT_EBABLE = "auto_close_bt_enable";
        
        /**
         * Control whether enable auto close bluetooth
         */
        public static final String QROM_AUTO_DISABLE_GPS = "auto_disable_gps";
        
        /**
         * Control whether enable auto close sync 
         */
        public static final String QROM_SYNC_POWER_SAVE_SETTING = "sync_power_save_setting";
        
        public static final String QROM_POWER_SAVE_AUTO_DISABLE_MOBILEDATA = "power_save_auto_disable_mobiledata";
        
        /**
         * night mode disable option 
         */
        public static final String QROM_NIGHT_MODE_DISABLE_OPTION = "night_mode_disable_option";


		public static final String QROM_BTN_LIGHT_SETTINGS = "btn_light_settings";
		

		/**Control sleep mode */
		public static final String QROM_SLEEP_MODE_STATUS = "tws_sleep_mode_status";
		
		public static final String QROM_SHOW_SLEEP_MODE_ON_LOCKSCREEN = "show_sleep_mode_on_lockscreen";
        
        public static final String QROM_SAFETY_INTERCEPT_SETTING = "safety_intercept_setting";

        
        
    	/**
    	 * settings to power consume
    	 */
    	/**
    	 * Total unplugged duration time
    	 */
    	public final static String QROM_TOTAL_BATTERY_DURATION = "total_battery_duration";
    	
    	/**
    	 * total power consume
    	 */
    	public final static String QROM_TOTAL_POWER_CONSUME = "total_power_consume";	
    	
    	/**
    	 * total Screen on duration
    	 */
    	public final static String QROM_TOTAL_SCREEN_ON_DURATION = "total_screen_on_duration";
    	
    	/**
    	 * total suspend duration
    	 */
    	public final static String QROM_TOTAL_SUSPEND_DURATION = "total_suspend_duration";
    	
    	
    	/**
    	 * today unplugged duration time
    	 */
    	public final static String QROM_TODAY_BATTERY_DURATION = "today_unplugged_duration";

    	/**
    	 * today power consume 
    	 */
    	public final static String QROM_TODAY_POWER_CONSUME = "today_power_consume";
    	
    	/**
    	 * today Screen on duration
    	 */
    	public final static String QROM_TODAY_SCREEN_ON_DURATION = "today_screen_on_duration";
    	
    	/**
    	 * today suspend duration
    	 */
    	public final static String QROM_TODAY_SUSPEND_DURATION = "today_suspend_duration";
    	
    	
    	/**
    	 * Yesterday Midnight Elapsed real time
    	 */
    	public final static String QROM_YESTERDAY_ELAPSEDREALTIME = "yesterday_elapsedrealtime";
    	
    	/**
    	 * recent unplugged to yesterday midnight screen on duration 
    	 */
    	public final static String QROM_YESTERDAY_SCREENON_DURATION = "yesterdaymidnight_screenon_duration";
    	
    	/**
    	 * power consume when yesterday mid night
    	 */
    	public final static String QROM_YESTERDAY_POWER_CONSUME = "yesterday_power_consume";
    	
    	/**
    	 * suspend duration when yesterday mid night
    	 */
    	public final static String QROM_YESTERDAY_SUSPEND_DURATION = "yesterday_suspend_duration";
    	
    	/**
    	 * recent unplugged batterylevel
    	 */
    	public final static String QROM_RECENT_UNPLUGGED_BATTERYLEVEL = "recent_unplugged_batterylevel";
    	
    	/**
    	 * the battery plugged type
    	 */
    	public final static String QROM_BATTERY_PLUGGED_TYPE = "battery_plugged_type";
    	
    	/**
    	 * the current battery level
    	 */
    	public final static String QROM_CURRENT_BATTERY_LEVEL = "current_battery_level"; 
    	
    	
    	/**
    	 * the face detect 
    	 */
    	public final static String QROM_FACE_DETECT_ENABLE = "tws_face_detect_enable";
    	
    	/**
    	 * the mms priority 
    	 */
    	public final static String QROM_MMS_PRIORITY = "tws_mms_priority";
    	
    	/**
    	 * the tws screen brightness
    	 */
    	public final static String QROM_SCREEN_BRIGHTNEE = "tws_screen_brightness";
    	
    	
    	/**
    	 * the tws permission root
    	 */
    	public final static String QROM_PERMISSION_ROOT = "tws_permission_root";


	/**
	* the Virtual Key Vibe Pattern
	*/
		public final static String QROM_VIRTUAL_KEY_VIBERATE_PATTERN = "tws_virtual_key_viberate_pattern";

	/**
	* the sms notification sound
	*/
		public final static String QROM_SMS_NOTIFICATION_SOUND = "tws_sms_notification_sound";

	public final static String QROM_CURRENT_POWER_STATE = "tws_current_power_state";

    public static final String QROM_WIFI_SLEEP_POLICY = "tws_wifi_sleep_policy";

    public static final int QROM_WIFI_SLEEP_POLICY_DEFAULT = 0;

    public static final int QROM_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED = 1;

    public static final int QROM_WIFI_SLEEP_POLICY_NEVER = 2;

	/**
    	 * the sync setting of tws accunt 
    	 */
    public final static String QROM_ACCOUNT_SYNC_FORBID_2G3G = "tws_forbid_sync_in_2g3g";
    	

     /**
         * Settings to backup. This is here so that it's in the same place as the settings
         * keys and easy to update.
         *
         * NOTE: Settings are backed up and restored in the order they appear
         *       in this array. If you have one setting depending on another,
         *       make sure that they are ordered appropriately.
         *
         * @hide
         */
        public static final String[] SETTINGS_TO_BACKUP = {
        	QROM_POWER_SAVE_MODE,
            QROM_POWER_SAVE_ENABLE_LEVEL,
        	QROM_POWER_SAVE_MODE_SETTING,
        	QROM_SLEEP_PLAN_STATUS,
        	QROM_MOBILE_DATA_USER_SET,
        	QROM_NETWORK_TYPE_USER_SET,
        	QROM_AIRPLANE_USER_SET, 	
            QROM_CPU_SETTING,
            QROM_NIGHT_MODE_SETTING,
            QROM_AUTO_KILL_BK_PROC_SETTING,
            QROM_AUTO_CLOSE_WIFI_EBABLE,
            QROM_AUTO_CLOSE_BT_EBABLE,
            QROM_AUTO_DISABLE_GPS,
            QROM_SYNC_POWER_SAVE_SETTING,
            QROM_POWER_SAVE_AUTO_DISABLE_MOBILEDATA,
            QROM_NIGHT_MODE_DISABLE_OPTION,
            QROM_BTN_LIGHT_SETTINGS,
            QROM_SLEEP_MODE_STATUS,
            QROM_SHOW_SLEEP_MODE_ON_LOCKSCREEN,
            QROM_SAFETY_INTERCEPT_SETTING,
            QROM_TOTAL_BATTERY_DURATION,
            QROM_TOTAL_POWER_CONSUME,
            QROM_TOTAL_SCREEN_ON_DURATION,
            QROM_TOTAL_SUSPEND_DURATION,
            QROM_MMS_PRIORITY,
            QROM_VIRTUAL_KEY_VIBERATE_PATTERN,
            QROM_PERMISSION_ROOT,
        };
    }
}
