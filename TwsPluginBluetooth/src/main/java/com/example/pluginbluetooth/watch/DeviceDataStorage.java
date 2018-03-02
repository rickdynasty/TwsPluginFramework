package com.example.pluginbluetooth.watch;

import android.content.Context;
import android.content.SharedPreferences;

class DeviceDataStorage {

    private static final String SHARED_PREFS_NAME = "deviceDataStorage";
    private static final String KEY_STEPS_GOAL = "stepGoal";
    private static final String KEY_STEPS_ACTIVE = "stepsActive";
    private static final String KEY_STEPS_RESET = "stepsReset";
    private static final String KEY_STILLNESS_ACTIVE = "stillnessActive";
    private static final String KEY_TIMEZONE = "timeZone";
    private static final String KEY_TIMEZONE_CITY = "timeZone_city";
    private static final String KEY_TIMEZONE_COUNTRY = "timeZone_country";
    private static final String KEY_ONBOARDING_DONE = "onboarding";
    private static final String KEY_WROTE_ONBOARDING_DEVICE_SETTINGS = "wroteOnboardingDeviceSettings";
    private static final String KEY_DIRTY = "dirty";
    private static final String KEY_COMPLICATION_DIRTY = "complication-dirty";
    private static final String KEY_ALARMS_DIRTY = "alarms-dirty";
    private static final String KEY_STILLNESS_DIRTY = "stillness-dirty";
    private static final String KEY_STEPS_GOAL_DIRTY = "stepsgoalactive-dirty";
    private static final String KEY_ONBOARDING_DIRTY = "onboarding-dirty";
    private static final String KEY_DEMO = "demo";
    private static final String KEY_RSSI_NOTIFICATION = "rssiNotification";
    private static final String KEY_RESET_STEPS = "resetSteps";
    private static final String KEY_REMOTE_CONFIG_VERSION = "remoteConfigVersion";
    private static final String KEY_LAST_SYNC = "last-sync-time";
    private static final String KEY_ALL_DIRTY = "all-dirty";

    private final SharedPreferences mSharedPreferences;

    /**
     * Keeps track of whether we have "dirty" changes, that have not been sent to the device.
     * <p/>
     * The dirty flag exists both in memory here and on disk. It's updated as follows:
     * - On each write: flagged both in memory and on disk (in the same write as the changed data)
     * - When starting a device sync: set to false in memory
     * - When a device sync ends:
     * -- No writes during sync: the dirty flag is still false in memory and is updated on disk.
     * -- Writes during sync: we're dirty again, so time to do another sync...
     * <p/>
     * The important invariant to always keep true is:
     * - The dirty flag on disk must be true whenever there are any "dirty" changes.
     * <p/>
     * For good performance we also want the dirty flag to be false whenever possible, but it's
     * ok to do an unnecessary sync occasionally so this is not an absolute requirement.
     * <p/>
     * The dirty flag only indicates that something has changed, not what. The all-dirty flag
     * tells us that all the settings should be written.
     */
    private boolean mDirty;
    private boolean mAllDirty;

    private boolean mComplicationDirty;
    private boolean mStepsDirty;
    private boolean mAlarmsDirty;
    private boolean mStillnessDirty;
    private boolean mStepsGoalDirty;
    private boolean mOnboardingDirty;

    private boolean mForceTimeWrite = true;
    private boolean mBaseConfigDirty = true;
    private boolean mDebugConfigDirty = true;

    private long mRemoteConfigVersion = -1;

    public DeviceDataStorage(final Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mDirty = mSharedPreferences.getBoolean(KEY_DIRTY, true);
        mAllDirty = mSharedPreferences.getBoolean(KEY_ALL_DIRTY, true);
        mComplicationDirty = mSharedPreferences.getBoolean(KEY_COMPLICATION_DIRTY, true);
        mAlarmsDirty = mSharedPreferences.getBoolean(KEY_ALARMS_DIRTY, true);
        mStillnessDirty = mSharedPreferences.getBoolean(KEY_STILLNESS_DIRTY, true);
        mStepsGoalDirty = mSharedPreferences.getBoolean(KEY_STEPS_GOAL_DIRTY, true);
        mOnboardingDirty = mSharedPreferences.getBoolean(KEY_ONBOARDING_DIRTY, true);
        mRemoteConfigVersion = mSharedPreferences.getLong(KEY_REMOTE_CONFIG_VERSION, -1);
    }

	public boolean getStepsActive() {
		return true;// mSharedPreferences.getBoolean(KEY_STEPS_ACTIVE, false);
	}

	public void setStepsActive(final boolean active) {
		// mBaseConfigDirty = true;
		// apply(mSharedPreferences.edit().putBoolean(KEY_STEPS_ACTIVE,
		// active));
	}

    public int getStepsToday() {
        return mSharedPreferences.getInt(KEY_STEPS_RESET, 0);
    }

    public void setStepsToday(final int steps) {
        mStepsDirty = true;
        apply(mSharedPreferences.edit().putInt(KEY_STEPS_RESET, steps));
    }

    public int getStepGoal() {
        return mSharedPreferences.getInt(KEY_STEPS_GOAL, 10000);
    }

    public void setStepGoal(final int steps) {
        mStepsGoalDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_STEPS_GOAL_DIRTY, true).apply();
        apply(mSharedPreferences.edit().putInt(KEY_STEPS_GOAL, steps));
    }

//    public boolean updateAlertTypes(int alarmAlertType,
//                                    int stillnessAlertType,
//                                    int stepGoalReachedAlertType,
//                                    int callsAlertType) {
//        boolean didUpdate = DeviceAlertConfig.updateAlertTypes(mSharedPreferences, alarmAlertType,
//                stillnessAlertType, stepGoalReachedAlertType, callsAlertType);
//        if (didUpdate) {
//            apply(mSharedPreferences.edit());
//        }
//        return didUpdate;
//    }
//
//    public boolean isAlertConfigDirty() {
//        return DeviceAlertConfig.isAlertConfigDirty(mSharedPreferences) || mAllDirty;
//    }
//
//    public int[] getAlertConfigBitmasks(boolean includeCalls) {
//        return new int[] {DeviceAlertConfig.getBitmaskForAlertType(mSharedPreferences, 1, includeCalls),
//                DeviceAlertConfig.getBitmaskForAlertType(mSharedPreferences, 2, includeCalls),
//                DeviceAlertConfig.getBitmaskForAlertType(mSharedPreferences, 3, includeCalls)};
//    }

    public boolean getStillnessActive() {
        return mSharedPreferences.getBoolean(KEY_STILLNESS_ACTIVE, false);
    }

    public void setStillnessActive(final boolean active) {
        mStillnessDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_STILLNESS_DIRTY, true).apply();
        apply(mSharedPreferences.edit().putBoolean(KEY_STILLNESS_ACTIVE, active));
    }

    public void setOnboardingFinished(final boolean isFinished) {
        mOnboardingDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_ONBOARDING_DIRTY, true).apply();
        apply(mSharedPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, isFinished));
    }

    public boolean getIsOnboardingFinished() {
        return mSharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setWroteOnboardingDeviceSettings(final boolean wroteOnboardingDeviceSettings) {
        mSharedPreferences.edit().putBoolean(KEY_WROTE_ONBOARDING_DEVICE_SETTINGS, wroteOnboardingDeviceSettings)
                .apply();
    }

    public boolean getWroteOnboardingDeviceSettings() {
        return mSharedPreferences.getBoolean(KEY_WROTE_ONBOARDING_DEVICE_SETTINGS, false);
    }

    public String getTimeZoneId() {
        return mSharedPreferences.getString(KEY_TIMEZONE, WatchConstants.DEFAULT_TIMEZONE);
    }

    public void setTimeZoneId(final String timeZoneId) {
        mComplicationDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_COMPLICATION_DIRTY, true).apply();
        apply(mSharedPreferences.edit().putString(KEY_TIMEZONE, timeZoneId));
    }
    
    public String getTimeZoneCity() {
    	return mSharedPreferences.getString(KEY_TIMEZONE_CITY, WatchConstants.getDefaultCity());
    }
    
    public void setTimeZoneCity(final String timeZoneCity) {
    	mComplicationDirty = true;
    	mSharedPreferences.edit().putBoolean(KEY_COMPLICATION_DIRTY, true).apply();
    	apply(mSharedPreferences.edit().putString(KEY_TIMEZONE_CITY, timeZoneCity));
    }
    
    public String getTimeZoneCountry() {
    	return mSharedPreferences.getString(KEY_TIMEZONE_COUNTRY, WatchConstants.getDefaultCountry());
    }
    
    public void setTimeZoneCountry(final String timeZoneCountry) {
    	mComplicationDirty = true;
    	mSharedPreferences.edit().putBoolean(KEY_COMPLICATION_DIRTY, true).apply();
    	apply(mSharedPreferences.edit().putString(KEY_TIMEZONE_COUNTRY, timeZoneCountry));
    }

    /**
     * Activate or deactivate demo mode on the device
     * <p/>
     * When this is active, then the device generates random step data for demo and test purposes.
     */
    public void setDemoMode(final boolean enabled) {
        mDebugConfigDirty = true;
        apply(mSharedPreferences.edit().putBoolean(KEY_DEMO, enabled));
    }

    public boolean getDemoMode() {
        return mSharedPreferences.getBoolean(KEY_DEMO, false);
    }

    /**
     * Activate or deactivate rssi notification mode on the device
     * <p/>
     * When this is active, then the device sends continously rssi values.
     */
    public void setRssiNotification(final boolean enabled) {
        mDebugConfigDirty = true;
        apply(mSharedPreferences.edit().putBoolean(KEY_RSSI_NOTIFICATION, enabled));
    }

    public boolean getRssiNotification() {
        return mSharedPreferences.getBoolean(KEY_RSSI_NOTIFICATION, false);
    }

    /**
     * This is run when we know that all settings have been successfully sent to the device
     * <p/>
     * All one-shot actions should be disabled here, so they aren't sent again. Note that
     * there's no way to guarantee that a "one-shot" action will not be sent more than once
     * before we reach this... It is therefore necessary to tolerate that. Everything is strictly
     * "best effort" and we only guarantee that it will happen at least once.
     * <p/>
     * There are a few ways that something we want to trigger once will be sent multiple times to
     * the device and we handle them all the same for simplicity (shouldn't happen often):
     * - The individual write or writes to trigger the one-shot thing fails in the middle. In
     * this case we can't know if it "happened" or not.
     * - The whole device settings sync fails (ie at least one unrelated write fails).
     * - Some setting is changed during the sync (it could be one of these or not).
     * <p/>
     * In all cases where this isn't called, a new sync will run. Think of this as a primitive
     * transaction that we know is committed at this point and otherwise we'll keep re-running.
     * <p/>
     * Be careful not to set the dirty flag here! Otherwise, we'll loop forever.
     */
    private void onSyncSuccessful() {
        mForceTimeWrite = false;
        mSharedPreferences.edit()
                .putBoolean(KEY_RESET_STEPS, false)
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply();
    }

    /**
     * Update dirty flag and apply the editor's changes
     */
    private void apply(final SharedPreferences.Editor editor) {
        editor.putBoolean(KEY_DIRTY, true).apply();
        mDirty = true;
    }

    /**
     * Are there changes that might not have been sent to the device?
     */
    public boolean isDirty() {
        return mDirty || mAllDirty;
    }

    public void setForceTimeWrite() {
        mForceTimeWrite = true;
    }

    public boolean getForceTimeWrite() {
        return mForceTimeWrite;
    }

    /**
     * Time of last sync in milliseconds (or -1)
     */
    public long getLastSyncTime() {
        return mSharedPreferences.getLong(KEY_LAST_SYNC, -1);
    }

    /**
     * Called when starting a device sync, to clear the dirty flag in memory
     * <p/>
     * We still keep the dirty flag set on disk until we're finished with the sync.
     * <p/>
     * Note: it's important that only one device sync is done at a time for this to work!
     */
    public void setSyncPending() {
        mDirty = false;
    }

    /**
     * Called when a device sync has finished
     * <p/>
     * Clears the dirty flag on disk, if it's still unset in memory (i.e. there has been no new
     * writes during the device sync).
     */
    public void setSyncDone() {
        if (!mDirty) {
            mComplicationDirty = false;
            mOnboardingDirty = false;
            mStillnessDirty = false;
            mStepsGoalDirty = false;
            mAlarmsDirty = false;
            mBaseConfigDirty = false;
            mDebugConfigDirty = false;
            mStepsDirty = false;
            mAllDirty = false;
            // DeviceAlertConfig.setAlertConfigNotDirty(mSharedPreferences);
            mSharedPreferences.edit()
                    .putBoolean(KEY_ALL_DIRTY, false)
                    .putBoolean(KEY_DIRTY, false)
                    .putBoolean(KEY_ALARMS_DIRTY, false)
                    .putBoolean(KEY_COMPLICATION_DIRTY, false)
                    .putBoolean(KEY_ONBOARDING_DIRTY, false)
                    .putBoolean(KEY_STILLNESS_DIRTY, false)
                    .putLong(KEY_REMOTE_CONFIG_VERSION, mRemoteConfigVersion)
                    .apply();
            onSyncSuccessful();
        }
    }

    /**
     * Mark the devices settings (as a whole) as dirty
     * <p/>
     * This means that they need to be (re)sent to the device.
     */
    public void setDirty() {
        apply(mSharedPreferences.edit()); // empty write to mark settings as dirty
    }

    public void setAllDirty() {
        mAllDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_ALL_DIRTY, true).apply();
    }

    public boolean isComplicationDirty() {
        return mComplicationDirty || mAllDirty;
    }

    public boolean isAlarmsDirty() {
        return mAlarmsDirty || mAllDirty;
    }

    public void setAlarmsDirty() {
        mAlarmsDirty = true;
        mSharedPreferences.edit().putBoolean(KEY_ALARMS_DIRTY, true).apply();
    }

    public boolean isStillnessDirty() {
        return mStillnessDirty || mAllDirty;
    }

    public boolean isStepsGoalDirty() {
        return mStepsGoalDirty || mAllDirty;
    }

    public boolean isOnboardingDirty() {
        return mOnboardingDirty || mAllDirty;
    }

    public boolean isBaseConfigDirty() {
        return mBaseConfigDirty || mAllDirty;
    }

    public boolean isDebugConfigDirty() {
        return mDebugConfigDirty || mAllDirty;
    }

    public boolean isStepsDirty() {
        return mStepsDirty;
    }

    public boolean isRemoteConfigDirty() {
        return mSharedPreferences.getLong(KEY_REMOTE_CONFIG_VERSION, -1) != mRemoteConfigVersion ||
                mAllDirty;
    }

    public void setRemoteConfigVersion(long configVersion) {
        mRemoteConfigVersion = configVersion;
        if (isRemoteConfigDirty()) {
            setDirty();
        }
    }
}
