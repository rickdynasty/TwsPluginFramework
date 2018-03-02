package com.example.pluginbluetooth.screens.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Store onboarding in shared preferences
 */
class OnboardingStorage {

    private static final String INITIAL_ONBOARDING_STORAGE = "initialOnboardingStorage";
    private static final String KEY_DENIED_LOCATION_PERMISSION = "denied-location-permission";

    public static boolean getHasBeenDeniedLocationPermission(final Context context) {
        return getSharedPreferences(context).getBoolean(KEY_DENIED_LOCATION_PERMISSION, false);
    }

    public static void setHasBeenDeniedLocationPermission(final Context context, final boolean hasBeenDenied) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_DENIED_LOCATION_PERMISSION, hasBeenDenied)
                .apply();
    }

    private static SharedPreferences getSharedPreferences(final Context context) {
        return context.getSharedPreferences(INITIAL_ONBOARDING_STORAGE, Context.MODE_PRIVATE);
    }
}
