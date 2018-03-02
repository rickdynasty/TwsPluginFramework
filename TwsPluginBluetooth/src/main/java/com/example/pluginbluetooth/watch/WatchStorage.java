package com.example.pluginbluetooth.watch;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pluginbluetooth.behaviour.Behaviour;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.runner.BackgroundRunner;
import com.example.pluginbluetooth.future.runner.SequentialBackgroundRunner;

import java.util.concurrent.Callable;

public class WatchStorage {

	public static final String SHARED_PREFS_NAME = "watch_store";
	public static final String DEVICE_ADDRESS = "device-address";
	
	private static final String BEHAVIOUR_SETTINGS_PREFIX = "settings";
	private static final String BEHAVIOUR_TYPE_PREFIX = "type_v2";
	private static final String BEHAVIOUR_TYPE_MAGIC_KEY_PREFIX = "type_magic_key_";
	private static final String DEVICE_TYPE = "device-type";
	private static final String DEVICE_ITEMID = "device-itemid";
	private static final String LAST_TIME_DAILY = "last-time-daily";
	private static final String TIME_WHEN_DIAGNOSTICS_SENT = "time-when-diagnostics-sent";

	private final Context mContext;
	private final SharedPreferences mPreferences;
	private final BackgroundRunner mRunner = new SequentialBackgroundRunner();

	WatchStorage(final Context context) {
		mPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		mContext = context;
	}

	String getSettingsForBehaviourType(final String behaviourType) {
		return mPreferences.getString(BEHAVIOUR_SETTINGS_PREFIX + behaviourType, null);
	}

//	String getBehaviourTypeForSlot(final int slot) {
//		return mPreferences.getString(BEHAVIOUR_TYPE_PREFIX + slot, Empty.TYPE);
//	}
//
//	String getBehaviourTypeForMagicKey(final int magicKey) {
//		return mPreferences.getString(BEHAVIOUR_TYPE_MAGIC_KEY_PREFIX + magicKey, Empty.TYPE);
//	}

	void setBehaviourTypeForMagicKey(final String behaviourType, final int magicKey) {
		final SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(BEHAVIOUR_TYPE_MAGIC_KEY_PREFIX + magicKey, behaviourType);
		editor.apply();
	}

	public void save(final Behaviour behaviour) {
		final SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(BEHAVIOUR_SETTINGS_PREFIX + behaviour.getType(), behaviour.getSettingsAsJson());
		editor.apply();
	}

	public void save(final Behaviour behaviour, final int slot) {
		final SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(BEHAVIOUR_TYPE_PREFIX + slot, behaviour.getType());
		editor.putString(BEHAVIOUR_SETTINGS_PREFIX + behaviour.getType(), behaviour.getSettingsAsJson());
		editor.apply();
	}

	Future<GattDevice> loadDevice() {
		return mRunner.submit(new Callable<GattDevice>() {
			@Override
			public GattDevice call() throws Exception {
				final String address = mPreferences.getString(DEVICE_ADDRESS, null);
				final int deviceType = mPreferences.getInt(DEVICE_TYPE, 0);
				final int itemId = mPreferences.getInt(DEVICE_ITEMID, -1);

				if (address == null) {
					return null;
				}

				// Note that GattDevice's constructor is thread safe (and this
				// is run in a worker thread)
				return new GattDevice(mContext, address, deviceType, itemId);
			}
		});
	}

	void saveDevice(GattDevice device) {
		if (device == null) {
			mPreferences.edit().remove(DEVICE_ADDRESS).remove(DEVICE_TYPE).remove(DEVICE_ITEMID).apply();
		} else {
			mPreferences.edit().putString(DEVICE_ADDRESS, device.getAddress()).putInt(DEVICE_TYPE, device.getType())
					.putInt(DEVICE_ITEMID, device.getItemId()).apply();
		}
	}

	long getTimeSinceDaily() {
		return mPreferences.getLong(LAST_TIME_DAILY, 0);
	}

	void setTimeSinceDaily(final long timeSinceDaily) {
		mPreferences.edit().putLong(LAST_TIME_DAILY, timeSinceDaily).apply();
	}

	long getTimeDiagnosticsSent() {
		return mPreferences.getLong(TIME_WHEN_DIAGNOSTICS_SENT, 0);
	}

	void setTimeDiagnosticsSent(final long timeWhenDiagnosticsSent) {
		mPreferences.edit().putLong(TIME_WHEN_DIAGNOSTICS_SENT, timeWhenDiagnosticsSent).apply();
	}
}
