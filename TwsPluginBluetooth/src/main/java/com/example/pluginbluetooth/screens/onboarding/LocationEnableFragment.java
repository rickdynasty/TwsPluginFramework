package com.example.pluginbluetooth.screens.onboarding;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.screens.BaseFragment;

public class LocationEnableFragment extends BaseOnboardingFragment {

    /*
     * Needed since it is possible to change location setting via the status bar
     */
    private BroadcastReceiver mLocationChangedBroadcastReceiver;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                setLocationEnabledIfNeeded();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mLocationChangedBroadcastReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mLocationChangedBroadcastReceiver);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_location_enable, container, false);

        view.findViewById(R.id.onboarding_location_hint_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(viewIntent, 0);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        setLocationEnabledIfNeeded();
    }

    @Override
    boolean handlesState(final Onboarding.State state) {
        return state == Onboarding.State.LOCATION_ENABLING;
    }

    @Override
    public String getName() {
        return "Location enabling fragment";
    }

    public static BaseFragment newInstance() {
        return new LocationEnableFragment();
    }

    private void setLocationEnabledIfNeeded() {
        if (getOnboarding().isLocationEnabled()) {
            getOnboarding().onLocationEnabled();
        }
    }

    @Override
    int getExitAnimRes(Onboarding.State toState, boolean isJustResumed) {
        return isJustResumed ? R.anim.onboarding_instant : R.anim.onboarding_pause;
    }
}