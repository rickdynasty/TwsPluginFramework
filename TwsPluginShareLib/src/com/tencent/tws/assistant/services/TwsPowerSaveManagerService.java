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

import com.android.internal.os.PowerProfile;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;

import com.tencent.tws.assistant.provider.TwsSettings;

import android.util.Log;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

//tws-start::import library::new feature::braindwang::20120105
//import com.tencent.nanji.rootstub.IRootStub;

import android.os.ServiceManager;
import static com.tencent.tws.assistant.provider.TwsSettings.System.TWS_POWER_SAVE_MODE_SETTING;

public class TwsPowerSaveManagerService {

	private static final String TAG = "powersave";
	private static final String TAG_SLEEP_MODE = "powersave_sleep_mode";
	private static final String TAG_POWER_SAVE = "powersave_mode";
	private static final String TAG_WIFI_POWER_SAVE = "powerSave_wifi";
	private static final String TAG_MOBILEDATA_POWER_SAVE = "powersave_mobiledata";
	private static final String TAG_BT_POWER_SAVE = "powerSave_bt";
	private Context mContext;
	// private static IRootStub mrootCmdBinder = null;
	private static final int CPU_POWER_SAVE_MODE = 0;
	private static final int CPU_BALANCE_MODE = 1;
	private static final int CPU_PERFORMANCE_MODE = 2;
	private static String mszGovernor = "ondemand";
	private static int mSleepModeStatus = 0;
	private static final String POWER_SAVE_ACTION = "android.powersave.action";
	private static final String REQUEST_ALARM_TYPE = "request_alarm_type";
	private static boolean mbScreenOn = true;
	private AlarmManager mAmm = null;
	private WifiManager mwifimgr = null;
	private ConnectivityManager mCmgr = null;
	private BluetoothAdapter mBtAdapter = null;
	private final static int SLEEP_MODE_ALARM_REQUESTCODE = 0;
	private final static int WIFI_AUTO_CLOSE_ALARM_REQUESTCODE = 1;
	private final static int MOBILEDATA_AUTO_CLOSE_ALARM_REQUESTCODE = 2;
	private final static int BT_AUTO_CLOSE_ALARM_REQUESTCODE = 3;
	private final static int POWER_CONSUME_ALARM_REQUESTCODE = 4;
	private static boolean mbPowerSaveModeEnabled = false;
	private static boolean mbCpuFreqEnabled = false;
	private final static long SLEEPMODE_EXTEND_TIME = AlarmManager.INTERVAL_HALF_HOUR;

	private static boolean mbMobiledataPowerSaveEnabled = false; /* 数据网络智能控制的开关 */
	private static final long MOBILEDATA_AUTO_DISABLE_EXTEND_TIME = 15 * 60 * 1000; /* 数据网络自动关闭的延时时间 */
	private static boolean mbMobiledataUserSet = true; /* 用户选择的数据网络状态，true为开 */
	private static int mMobileDataAutoDisableState = 0; /*
														 * 数据网络自动关闭的状态，0表示没有启动自动关闭
														 * ，
														 * 1表示启动了延时自动关闭但是还没有生效，2
														 * 表示自动关闭已经生效
														 */

	private static boolean mbBluetoothPowerSaveEnabled = false; /* 蓝牙智能控制的开关 */
	private static int mBluetoothState = BluetoothAdapter.STATE_OFF; /* 蓝牙的打开关闭状态 */
	private static boolean mbInBtSettingsActivity = false; /* 是否在蓝牙设置界面 */
	private static boolean mbFileTransfering = false; /* 是否在通过蓝牙进行文件传输 */
	private static int mBtConnectionState = BluetoothAdapter.STATE_DISCONNECTED; /* 蓝牙的连接状态 */
	private static int mBtAutoDisableState = 0;

	private boolean mIsPowered = false;
	private int mBatteryLevel = 100;
	private int mRecentUnpluggedBatteryLevel = 0;
	private long mbatteryCapacity = 1500;
	private static int mMonth;

	private static final String TWS_POWER_SAVE_ACTION = "tws.action.POWER_SAVE_ACTION";
	private static final int TWS_SYNCDATA_POWERSAVE_ACTION = 0;
	private static final int TWS_BLUETOOTH_POWERSAVE_ACTION = 1;

	private ScreenStateChangeHandler mHandler = null;

	public TwsPowerSaveManagerService(Context context) {
		tws_log(TAG, "TwsPowerSaveManagerService");
		mContext = context;

		Init();
	}

	/* init */
	private void Init() {
		/*
		 * IBinder binder = ServiceManager.getService("rootstub"); if(binder ==
		 * null){ Log.e(TAG, "rootstub service not exist"); } else{
		 * mrootCmdBinder = IRootStub.Stub.asInterface(binder); }
		 */

		getAvailableGovernor();

		mAmm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		mwifimgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mCmgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		HandlerThread hthread = new HandlerThread("TwsPowerSaveManagerService");
		hthread.start();
		mHandler = new ScreenStateChangeHandler(hthread.getLooper());

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction("tx_bluetooth_setting_state_change");
		filter.addAction("BLUETOOTH_TRANSFER_FILE_REQUEST_ACTION");
		filter.addAction("BLUETOOTH_TRANSFER_COMPLETED_ACTION");
		filter.addAction("WIFI_DEVICE_IDLE");
		filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		mContext.registerReceiver(mIntentReceiver, filter);

		PowerProfile myPowerProfile = new PowerProfile(mContext);
		mbatteryCapacity = Math.round(myPowerProfile.getBatteryCapacity());

		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mContext.registerReceiver(new BatteryReceiver(), filter);

		filter = new IntentFilter();
		filter.addAction(POWER_SAVE_ACTION);
		mContext.registerReceiver(new PowerSaveRecevier(), filter);

		mbPowerSaveModeEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TWS_POWER_SAVE_MODE_SETTING, 0) > 0;
		mbCpuFreqEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_CPU_SETTING, 0) > 0;
		mbMobiledataPowerSaveEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_POWER_SAVE_AUTO_DISABLE_MOBILEDATA, 0) > 0;
		mbMobiledataUserSet = isMobileDataEnable(mContext);
		mbBluetoothPowerSaveEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_AUTO_CLOSE_BT_EBABLE, 0) > 0;
		mbSyncAutoSetting = ContentResolver.getMasterSyncAutomatically();
		mSleepModeStatus = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_SLEEP_MODE_STATUS, 0);

		updatePowerSaveModeLocked(mbPowerSaveModeEnabled);

		Uri PowerSaveModeUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_POWER_SAVE_MODE_SETTING);
		Uri CpuFreqUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_CPU_SETTING);
		Uri AutoDisableWiFiUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_AUTO_CLOSE_WIFI_EBABLE);
		Uri AutoDisableMobileDataUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_POWER_SAVE_AUTO_DISABLE_MOBILEDATA);
		Uri AutoDisableBluetoothUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_AUTO_CLOSE_BT_EBABLE);
		mContext.getContentResolver().registerContentObserver(PowerSaveModeUri, false, mPowerSaveModeObserver);
		Uri sleepModeUri = TwsSettings.System.getUriFor(TwsSettings.System.TWS_SLEEP_MODE_STATUS);
		mContext.getContentResolver().registerContentObserver(CpuFreqUri, false, mCpuFreqObserver);
		mContext.getContentResolver().registerContentObserver(AutoDisableMobileDataUri, false, mAutoDisableMobileDataObserver);
		mContext.getContentResolver().registerContentObserver(AutoDisableBluetoothUri, false, mAutoDisableBtObserver);
		mContext.getContentResolver().registerContentObserver(sleepModeUri, false, mSleepModeObserver);

	}

	private final int MSG_SCREEN_ON = 0;
	private final int MSG_SCREEN_OFF = 1;

	private class ScreenStateChangeHandler extends Handler {
		public ScreenStateChangeHandler(Looper looper) {
			// TODO Auto-generated constructor stub
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);

			switch (msg.what) {
			case MSG_SCREEN_ON:
				// restore the ondemand parms when screen on
				/*
				 * if (mszGovernor.equals("ondemand")){ try {
				 * //mrootCmdBinder.setCpuConfig("ondemand/powersave_bias",
				 * "0"); } catch (RemoteException e) { // TODO Auto-generated
				 * catch block e.printStackTrace(); } }
				 */

				if (mSleepModeStatus == 1) {
					CancelAlarm(SLEEP_MODE_ALARM_REQUESTCODE);
				} else if (mSleepModeStatus == 2) {
					mSleepModeStatus = 1;
				}

				// restore the mobile data when screen on ,if auto disable the
				// mobile data during screen off
				if (mMobileDataAutoDisableState == 2) {
					setMobiledataEnable(true);
				} else if (mMobileDataAutoDisableState == 1) {
					SetAutoDisableMobiledataEnable(false);
				}

				if (mBtAutoDisableState == 2) {
					setBtEnable(true);
				} else if (mBtAutoDisableState == 1) {
					SetAutoDisableBluetooth(false);
				}

				if (mbSyncAutoSetting) {
					// ContentResolver.setMasterSyncAutomatically(true);
				}
				break;

			case MSG_SCREEN_OFF:
				if (mSleepModeStatus == 1) {
					SetAlarm(SLEEP_MODE_ALARM_REQUESTCODE, SLEEPMODE_EXTEND_TIME, AlarmManager.RTC_WAKEUP);
				}

				// Do the power save action, just the power save mode is enabled
				if (mbPowerSaveModeEnabled) {

					// mobile data
					if (mbMobiledataPowerSaveEnabled) {
						mbMobiledataUserSet = isMobileDataEnable(mContext);

						if (mbMobiledataUserSet) {
							SetAutoDisableMobiledataEnable(true);
						}
					}

					// bt
					if (mbBluetoothPowerSaveEnabled) {
						if (mBluetoothState == BluetoothAdapter.STATE_ON) {
							SetAutoDisableBluetooth(true);
						}
					}

					// data sync power save
					if (getAutoDisableDataSyncSetting()) {
						mbSyncAutoSetting = ContentResolver.getMasterSyncAutomatically();
						if (mbSyncAutoSetting) {
							// ContentResolver.setMasterSyncAutomatically(false);

							sendPowerSaveActionIntent(TWS_SYNCDATA_POWERSAVE_ACTION);
						}
					} else {
						/* 如果用户没有打开锁屏关闭同步的开关，则亮屏时不用去操作背景数据同步和自动同步的开关 */
						mbSyncAutoSetting = false;
					}

					/* 如果是ondemand调频模式，则黑屏时设置省电偏置 */
					/*
					 * if (mszGovernor.equals("ondemand") && mbCpuFreqEnabled){
					 * try {
					 * //mrootCmdBinder.setCpuConfig("ondemand/powersave_bias",
					 * "500"); } catch (RemoteException e) { // TODO
					 * Auto-generated catch block e.printStackTrace(); } }
					 */
				}
				break;

			default:
				break;
			}
		}
	}

	private void updatePowerSaveModeLocked(boolean bPowerSaveModeEnabled) {
		updateCpuFreqPowerSaveLocked(bPowerSaveModeEnabled);

		// enable sleep mode when power save mode enable
		updateSleepModeStateLocked(bPowerSaveModeEnabled);
	}

	private void updateSleepModeStateLocked(boolean bEnable) {
		TwsSettings.System.putInt(mContext.getContentResolver(), TwsSettings.System.TWS_SLEEP_MODE_STATUS, bEnable ? 1 : 0);
	}

	private void updateCpuFreqPowerSaveLocked(boolean bPowerSaveModeEnabled) {
		if (mbCpuFreqEnabled && bPowerSaveModeEnabled) {
			try {
				SetCpuFreq(CPU_POWER_SAVE_MODE);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				SetCpuFreq(CPU_BALANCE_MODE);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private ContentObserver mPowerSaveModeObserver = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			final boolean bPowerSaveModeEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_POWER_SAVE_MODE_SETTING, 0) > 0;
			tws_log(TAG_POWER_SAVE, "Power save mode change to " + bPowerSaveModeEnabled);
			if (bPowerSaveModeEnabled != mbPowerSaveModeEnabled) {
				mbPowerSaveModeEnabled = bPowerSaveModeEnabled;
				updatePowerSaveModeLocked(mbPowerSaveModeEnabled);
			}
		};
	};

	private ContentObserver mCpuFreqObserver = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			final boolean bCpuFreqEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_CPU_SETTING, 0) > 0;
			tws_log(TAG_POWER_SAVE, "Cpufreq change to " + bCpuFreqEnabled);
			if (bCpuFreqEnabled != mbCpuFreqEnabled) {
				mbCpuFreqEnabled = bCpuFreqEnabled;
				updateCpuFreqPowerSaveLocked(mbCpuFreqEnabled);
			}
		};
	};

	private ContentObserver mAutoDisableMobileDataObserver = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			final boolean bMobileDataPowerSaveEnabled = TwsSettings.System.getInt(mContext.getContentResolver(),
					TwsSettings.System.TWS_POWER_SAVE_AUTO_DISABLE_MOBILEDATA, 0) > 0;
			tws_log(TAG_MOBILEDATA_POWER_SAVE, "Mobiledata power save mode change to " + bMobileDataPowerSaveEnabled);
			if (bMobileDataPowerSaveEnabled != mbMobiledataPowerSaveEnabled) {
				mbMobiledataPowerSaveEnabled = bMobileDataPowerSaveEnabled;
			}
		};
	};

	private ContentObserver mAutoDisableBtObserver = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			final boolean bAutoDisableBtEnabled = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_AUTO_CLOSE_BT_EBABLE, 0) > 0;
			tws_log(TAG_BT_POWER_SAVE, "Auto disable bt change to " + bAutoDisableBtEnabled);
			if (mbBluetoothPowerSaveEnabled != bAutoDisableBtEnabled) {
				mbBluetoothPowerSaveEnabled = bAutoDisableBtEnabled;
				if (mBluetoothState == BluetoothAdapter.STATE_ON) {
					if (mbBluetoothPowerSaveEnabled && mbPowerSaveModeEnabled) {
						SetAutoDisableBluetooth(true);
					} else {
						SetAutoDisableBluetooth(false);
					}
				}
			}
		};
	};

	private void SetAutoDisableMobiledataEnable(boolean bEnable) {

		// allow auto disable mobile data just when wifi is not connected
		if (bEnable) {
			tws_log(TAG_MOBILEDATA_POWER_SAVE, "SetAutoDisableMobiledataEnable to true");
			SetAlarm(MOBILEDATA_AUTO_CLOSE_ALARM_REQUESTCODE, MOBILEDATA_AUTO_DISABLE_EXTEND_TIME, AlarmManager.RTC);
			mMobileDataAutoDisableState = 1;
		} else if (mMobileDataAutoDisableState == 1) {
			tws_log(TAG_MOBILEDATA_POWER_SAVE, "SetAutoDisableMobiledataEnable to false");
			CancelAlarm(MOBILEDATA_AUTO_CLOSE_ALARM_REQUESTCODE);
			mMobileDataAutoDisableState = 0;
		}
	}

	private void SetAutoDisableBluetooth(boolean bEnable) {

		if (bEnable && mbBluetoothPowerSaveEnabled && mbPowerSaveModeEnabled) {
			if (!mbFileTransfering && !mbInBtSettingsActivity && mBtConnectionState == BluetoothAdapter.STATE_DISCONNECTED
					&& mBluetoothState == BluetoothAdapter.STATE_ON) {
				tws_log(TAG_BT_POWER_SAVE, "SetAutoDisableBluetooth true");
				SetAlarm(BT_AUTO_CLOSE_ALARM_REQUESTCODE, MOBILEDATA_AUTO_DISABLE_EXTEND_TIME, AlarmManager.RTC);
				mBtAutoDisableState = 1;
			}
		} else {
			tws_log(TAG_BT_POWER_SAVE, "SetAutoDisableBluetooth false");
			CancelAlarm(BT_AUTO_CLOSE_ALARM_REQUESTCODE);
			mBtAutoDisableState = 0;
		}
	}

	private void sendPowerSaveActionIntent(int Action) {
		// Log.d(TAG, "sendPowerSaveActionIntent Action = "+Action);
		Intent intent = new Intent(TWS_POWER_SAVE_ACTION);
		intent.putExtra(TWS_POWER_SAVE_ACTION, Action);
		mContext.sendBroadcast(intent);

	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(intent.ACTION_SCREEN_OFF)) {
				mbScreenOn = false;
				mHandler.sendEmptyMessage(MSG_SCREEN_OFF);
			} else if (action.equals(intent.ACTION_SCREEN_ON)) {
				mbScreenOn = true;
				mHandler.sendEmptyMessage(MSG_SCREEN_ON);
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				// bt state changed
				mBluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				tws_log(TAG_BT_POWER_SAVE, "mBluetoothState = " + mBluetoothState);
			} else if (action.equals("tx_bluetooth_setting_state_change")) {
				// be in bt setting activity
				mbInBtSettingsActivity = intent.getBooleanExtra("tx_bluetooth_setting_is_foreground", false);
				tws_log(TAG_BT_POWER_SAVE, "bluetooth settings is foreground? " + mbInBtSettingsActivity);
			} else if (action.equals("BLUETOOTH_TRANSFER_FILE_REQUEST_ACTION")) {
				// bt transfer request
				tws_log(TAG_BT_POWER_SAVE, "bt transfer request ");
				mbFileTransfering = true;
			} else if (action.equals("BLUETOOTH_TRANSFER_COMPLETED_ACTION")) {
				// bt transfer completed
				tws_log(TAG_BT_POWER_SAVE, "bt transfer completed ");
				mbFileTransfering = false;
			} else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
				// bt connection state changed
				mBtConnectionState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
				tws_log(TAG_BT_POWER_SAVE, "mBtConnectionState change to " + mBtConnectionState);
			}

		}
	};

	private ContentObserver mSleepModeObserver = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			int i_sleepmode_status = TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_SLEEP_MODE_STATUS, 0);
			if (mSleepModeStatus != i_sleepmode_status) {
				mSleepModeStatus = i_sleepmode_status;

				if (mSleepModeStatus == 0) {
					CancelAlarm(SLEEP_MODE_ALARM_REQUESTCODE);
					tws_log(TAG_SLEEP_MODE, "sleep mode close");
				} else if (mSleepModeStatus == 1) {
					tws_log(TAG_SLEEP_MODE, "sleep mode open");
					if (!mbScreenOn) {
						SetAlarm(SLEEP_MODE_ALARM_REQUESTCODE, SLEEPMODE_EXTEND_TIME, AlarmManager.RTC_WAKEUP);
					}
				}
			}
		};
	};

	private void SetAlarm(int nRequestCode, long lExtendTime, int nAlarmType) {
		if (mAmm != null) {
			tws_log(TAG_POWER_SAVE, "setAlarm " + nRequestCode + " alarm");
			CancelAlarm(nRequestCode);
			Calendar myCalendar = Calendar.getInstance();

			myCalendar.setTimeInMillis(System.currentTimeMillis() + lExtendTime);
			Intent intent = new Intent(POWER_SAVE_ACTION);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			intent.putExtra(REQUEST_ALARM_TYPE, nRequestCode);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, nRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			mAmm.set(nAlarmType, myCalendar.getTimeInMillis(), pendingIntent);
		}
	}

	private void setAlarm(Context ctx, int nRequestcode, int hourOfDay, int minute) {
		if (mAmm != null) {
			Calendar myCalendar = Calendar.getInstance();
			myCalendar.setTimeInMillis(System.currentTimeMillis());
			int nowHour = myCalendar.get(Calendar.HOUR_OF_DAY);
			int nowMinute = myCalendar.get(Calendar.MINUTE);

			// if alarm is behind current time, advance one day
			if (hourOfDay < nowHour || (hourOfDay == nowHour && minute <= nowMinute)) {
				myCalendar.add(Calendar.DAY_OF_YEAR, 1);
			}

			Intent intent = new Intent(POWER_SAVE_ACTION);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			intent.putExtra(REQUEST_ALARM_TYPE, nRequestcode);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, nRequestcode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
			myCalendar.set(Calendar.MINUTE, minute);
			myCalendar.set(Calendar.SECOND, 0);
			myCalendar.set(Calendar.MILLISECOND, 0);

			mAmm.setRepeating(AlarmManager.RTC_WAKEUP, myCalendar.getTimeInMillis(), 24 * AlarmManager.INTERVAL_HOUR, pendingIntent);
		}

	}

	private void CancelAlarm(int nRequestcode) {
		if (mAmm != null) {
			Intent intent = new Intent(POWER_SAVE_ACTION);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, nRequestcode, intent, PendingIntent.FLAG_NO_CREATE);
			if (pendingIntent != null) {
				mAmm.cancel(pendingIntent);
			}
		}
	}

	private final class PowerSaveRecevier extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			// TODO Auto-generated method stub
			int nRequestCode = intent.getIntExtra(REQUEST_ALARM_TYPE, -1);
			switch (nRequestCode) {
			case SLEEP_MODE_ALARM_REQUESTCODE:
				tws_log(TAG_SLEEP_MODE, "It's time to enable sleep mode");
				TwsSettings.System.putInt(mContext.getContentResolver(), TwsSettings.System.TWS_SLEEP_MODE_STATUS, 2);
				break;

			case MOBILEDATA_AUTO_CLOSE_ALARM_REQUESTCODE:
				tws_log(TAG_MOBILEDATA_POWER_SAVE, "It's time to set mobiledata disable");
				setMobiledataEnable(false);
				break;
			case BT_AUTO_CLOSE_ALARM_REQUESTCODE:
				tws_log(TAG_BT_POWER_SAVE, "It's time to set bt disable");
				setBtEnable(false);
				break;
			default:
				break;
			}
		}
	}

	private void setMobiledataEnable(boolean bEnable) {
		if (mCmgr != null) {
			boolean bMobiledataEnabled = isMobileDataEnable(mContext);

			// if mobile data already disable,don't disable again.
			if (!bEnable && !bMobiledataEnabled) {
				return;
			}

			tws_log(TAG_MOBILEDATA_POWER_SAVE, "Auto set Mobiledata to " + bEnable);
			mCmgr.setMobileDataEnabled(bEnable);
			mMobileDataAutoDisableState = bEnable ? 0 : 2;
		}
	}

	private void setBtEnable(boolean bEnable) {
		if (mBtAdapter != null) {

			// if bt is already OFF,don't disable again
			if (!bEnable && mBluetoothState != BluetoothAdapter.STATE_ON) {
				return;
			}

			if (bEnable) {
				boolean success = mBtAdapter.enable();
				if (success) {
					mBtAutoDisableState = 0;
				}
			} else {
				boolean success = mBtAdapter.disable();
				if (success) {
					mBtAutoDisableState = 2;
					sendPowerSaveActionIntent(TWS_BLUETOOTH_POWERSAVE_ACTION);
				}
			}
		}
	}

	private boolean isMobileDataEnable(Context ctx) {
		boolean bRet = true;
		if (mCmgr != null) {
			bRet = mCmgr.getMobileDataEnabled();
		}
		return bRet;
	}

	/**
	 * get available cpu freq governor
	 */
	private void getAvailableGovernor() {
		String result = "ondemand";
		char[] buffer = new char[128];
		FileReader file = null;
		try {
			file = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
			try {
				int len = file.read(buffer, 0, 128);

				if (len != -1) {
					// Log.d(TAG, "read governor success");
					result = new String(buffer, 0, len).trim();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {

			// When reading the file failed or can not find the ondemand mode,
			// remove cpusetting
			mszGovernor = result;

			// Log.d(TAG, "mszGovernor = "+mszGovernor);

			if (file != null) {
				try {
					file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * set cpu freq interface for app
	 * 
	 * @param flag
	 */
	public void setCpuConfiguration(int flag) {
		switch (flag) {
		case CPU_PERFORMANCE_MODE:
			/*
			 * if (mrootCmdBinder != null){ try { // Log.d(TAG,
			 * "App set to ondemand governor");
			 * mrootCmdBinder.setCpuConfig("scaling_governor", "ondemand");
			 * SetCpuFreq(CPU_PERFORMANCE_MODE); } catch (RemoteException e) {
			 * // TODO Auto-generated catch block e.printStackTrace(); } }
			 */

			break;
		case CPU_BALANCE_MODE:
		case CPU_POWER_SAVE_MODE:
		default:
			if (mbPowerSaveModeEnabled && mbCpuFreqEnabled) {
				try {
					SetCpuFreq(CPU_POWER_SAVE_MODE);
					// Log.d(TAG, "App reset to powersave governor");
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					// Log.d(TAG, "App reset to balance governor");
					SetCpuFreq(CPU_BALANCE_MODE);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			break;
		}
	}

	/**
	 * set cpu freq config
	 * 
	 * @param i_cpu_mode
	 * @throws RemoteException
	 */
	private void SetCpuFreq(int i_cpu_mode) throws RemoteException {
		/*
		 * if (mrootCmdBinder != null){
		 * 
		 * if (mszGovernor.equals("lulzactive")){ switch (i_cpu_mode) { case
		 * CPU_POWER_SAVE_MODE:
		 * mrootCmdBinder.setCpuConfig("lulzactive/inc_cpu_load", "90");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_up_step", "2");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_down_step", "1");
		 * mrootCmdBinder.setCpuConfig("lulzactive/up_sample_time", "20000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/down_sample_time", "40000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/screen_off_min_step", "4");
		 * break; case CPU_BALANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("lulzactive/inc_cpu_load", "80");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_up_step", "2");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_down_step", "1");
		 * mrootCmdBinder.setCpuConfig("lulzactive/up_sample_time", "10000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/down_sample_time", "40000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/screen_off_min_step", "3");
		 * break; case CPU_PERFORMANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("lulzactive/inc_cpu_load", "60");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_up_step", "4");
		 * mrootCmdBinder.setCpuConfig("lulzactive/pump_down_step", "1");
		 * mrootCmdBinder.setCpuConfig("lulzactive/up_sample_time", "10000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/down_sample_time", "70000");
		 * mrootCmdBinder.setCpuConfig("lulzactive/screen_off_min_step", "0");
		 * break; default: break; } } else if (mszGovernor.equals("ondemand")){
		 * switch (i_cpu_mode) { case CPU_POWER_SAVE_MODE:
		 * mrootCmdBinder.setCpuConfig("ondemand/up_threshold", "90");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_rate", "100000");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_down_factor", "1");
		 * mrootCmdBinder.setCpuConfig("ondemand/down_differential", "5");
		 * mrootCmdBinder.setCpuConfig("ondemand/powersave_bias", "0"); break;
		 * case CPU_BALANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("ondemand/up_threshold", "85");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_rate", "60000");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_down_factor", "2");
		 * mrootCmdBinder.setCpuConfig("ondemand/down_differential", "10");
		 * mrootCmdBinder.setCpuConfig("ondemand/powersave_bias", "0"); break;
		 * case CPU_PERFORMANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("ondemand/up_threshold", "75");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_rate", "30000");
		 * mrootCmdBinder.setCpuConfig("ondemand/sampling_down_factor", "2");
		 * mrootCmdBinder.setCpuConfig("ondemand/down_differential", "15");
		 * mrootCmdBinder.setCpuConfig("ondemand/powersave_bias", "0"); break;
		 * default: break; } } else if (mszGovernor.equals("interactive")){
		 * switch (i_cpu_mode) { case CPU_POWER_SAVE_MODE:
		 * mrootCmdBinder.setCpuConfig("interactive/go_hispeed_load", "80");
		 * mrootCmdBinder.setCpuConfig("interactive/hispeed_freq", "700000");
		 * mrootCmdBinder.setCpuConfig("interactive/min_sample_time", "40000");
		 * mrootCmdBinder.setCpuConfig("interactive/timer_rate", "20000");
		 * break; case CPU_BALANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("interactive/go_hispeed_load", "70");
		 * mrootCmdBinder.setCpuConfig("interactive/hispeed_freq", "700000");
		 * mrootCmdBinder.setCpuConfig("interactive/min_sample_time", "60000");
		 * mrootCmdBinder.setCpuConfig("interactive/timer_rate", "20000");
		 * break; case CPU_PERFORMANCE_MODE:
		 * mrootCmdBinder.setCpuConfig("interactive/go_hispeed_load", "50");
		 * mrootCmdBinder.setCpuConfig("interactive/hispeed_freq", "700000");
		 * mrootCmdBinder.setCpuConfig("interactive/min_sample_time", "60000");
		 * mrootCmdBinder.setCpuConfig("interactive/timer_rate", "10000");
		 * break; default: break; } } else if (mszGovernor.equals("smartass")){
		 * 
		 * }
		 * 
		 * }
		 */
	}

	/**
	 * when battery info changed,get the battery level. if the plug state
	 * changed,record the power consume info to TwsSettings
	 * 
	 * @author yongchen
	 */
	private final class BatteryReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean wasPowered = mIsPowered;
			mIsPowered = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) > 0;
			mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
			ContentResolver cr = mContext.getContentResolver();
			TwsSettings.System.putInt(cr, TwsSettings.System.TWS_CURRENT_BATTERY_LEVEL, mBatteryLevel);

			// if battery charge state changed
			if (mIsPowered != wasPowered) {

				TwsSettings.System.putInt(cr, TwsSettings.System.TWS_BATTERY_PLUGGED_TYPE, intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

				if (!mIsPowered) {
					// not plugged
					mRecentUnpluggedBatteryLevel = mBatteryLevel;
					TwsSettings.System.putInt(cr, TwsSettings.System.TWS_RECENT_UNPLUGGED_BATTERYLEVEL, mRecentUnpluggedBatteryLevel);
				}
			}
		}
	}

	private static boolean mbSyncAutoSetting = false;

	private boolean getAutoDisableDataSyncSetting() {
		return TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_SYNC_POWER_SAVE_SETTING, 0) > 0
				&& TwsSettings.System.getInt(mContext.getContentResolver(), TwsSettings.System.TWS_POWER_SAVE_MODE_SETTING, 0) > 0;
	}

	private final static boolean mb_isDebug = false;

	private void tws_log(String tag, String msg) {
		if (mb_isDebug) {
			Log.d(tag, msg);
		}
	}
}
