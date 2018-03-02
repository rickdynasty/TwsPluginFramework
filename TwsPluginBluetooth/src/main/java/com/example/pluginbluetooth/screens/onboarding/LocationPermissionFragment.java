package com.example.pluginbluetooth.screens.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.screens.BaseFragment;

public class LocationPermissionFragment extends BaseOnboardingFragment {

    private static final String TAG = LocationPermissionFragment.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_location_permission, container, false);

        view.findViewById(R.id.allow_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                showPermissionDialog();
            }
        });

        if (getOnboarding().hasVisitedState(Onboarding.State.LOCATION_ENABLING)) {
            ((TextView) view.findViewById(R.id.onboarding_location_permission_description)).
                    setText(getText(R.string.onboarding_permission_description1_simple));
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission was granted");
                getOnboarding().onLocationPermissionGranted();
            } else {
                Log.d(TAG, "Location permission was denied");
                OnboardingStorage.setHasBeenDeniedLocationPermission(getContext(), true);
            }
        }
    }


    @Override
    boolean handlesState(final Onboarding.State state) {
        return state == Onboarding.State.LOCATION_PERMISSION;
    }

    @Override
    public String getName() {
        return "OnboardingPermission";
    }


    private void showPermissionDialog() {
        final boolean canShowPermissionDialog = canShowLocationPermissionDialog();

        // Always try to show a permission dialog the first time. After that, look at the "should show
        // rationale" from Android. If we can't show a dialog, then we send the user to the Android
        // settings instead, as we can't do much else.
        if (canShowPermissionDialog) {
            Log.d(TAG, "Triggering Android's system permission dialog");
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            // Open app settings instead of trying to show the permission dialog
            Log.d(TAG, "Opening Android app settings so the user can grant permissions...");
            final Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            final Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            getContext().startActivity(intent);
        }
    }

    private boolean canShowLocationPermissionDialog() {
        final boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION);
        final boolean hasBeenDeniedLocationPermission =
                OnboardingStorage.getHasBeenDeniedLocationPermission(getContext());
        final boolean canShowPermissionDialog = !hasBeenDeniedLocationPermission || showRationale;

        Log.d(TAG, "Should show permission rationale: " + showRationale);
        Log.d(TAG, "Possible to trigger a permission dialog: " + canShowPermissionDialog);

        return canShowPermissionDialog;
    }

    public static BaseFragment newInstance() {
        return new LocationPermissionFragment();
    }
}
