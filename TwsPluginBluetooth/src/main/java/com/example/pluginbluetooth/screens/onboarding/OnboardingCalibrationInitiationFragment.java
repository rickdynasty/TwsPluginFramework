package com.example.pluginbluetooth.screens.onboarding;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.pluginbluetooth.R;

public class OnboardingCalibrationInitiationFragment extends BaseOnboardingFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calibration_initiation, container, false);

        final Button initiateCalibration = (Button) view.findViewById(R.id.start_calibration);
        initiateCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //getOnboardingViewController().gotoNextFragment(CalibrationFragment.newInstance(), true);
            }
        });

        return view;
    }

    @Override
    boolean handlesState(final Onboarding.State state) {
        return state == Onboarding.State.FINISHING;
    }

    @Override
    int getExitAnimRes(final Onboarding.State toState, final boolean isJustResumed) {
        return toState == Onboarding.State.FINISHING ? R.anim.exit_to_left : super.getExitAnimRes(toState,
                isJustResumed);
    }

    @Override
    public String getName() {
        return "CalibrationInitiation";
    }

    public static OnboardingCalibrationInitiationFragment newInstance() {
        return new OnboardingCalibrationInitiationFragment();
    }
}
