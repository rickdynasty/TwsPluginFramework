package com.example.pluginbluetooth.screens.onboarding;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.pluginbluetooth.R;

import static com.example.pluginbluetooth.screens.onboarding.Onboarding.State.CANCEL;

public class OnboardingFailFragment extends BaseOnboardingFragment {



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_onboarding_fail, container, false);

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Onboarding.getInstance().gotoState(CANCEL);
            }
        });


        view.findViewById(R.id.btn_turn_on_bluetooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Onboarding.getInstance().updateState();

            }
        });


        view.findViewById(R.id.textView_title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Onboarding.getInstance().gotoState(Onboarding.State.FQA);
                Toast.makeText(getActivity(),"OnClick textView_title",Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }


    @Override
    boolean handlesState(final Onboarding.State state) {
        return state == Onboarding.State.FAIL;
    }

    @Override
    public String getName() {
        return "OnboardingFailFragment";
    }

    @Override
    boolean isBackAllowed() {
        return true;
    }

    @Override
    int getEnterAnimRes(final Onboarding.State fromState) {
        return R.anim.enter_from_right;
    }

    @Override
    int getPopEnterAnimRes() {
        return R.anim.enter_from_left;
    }

    @Override
    int getPopExitAnimRes() {
        return R.anim.exit_to_right;
    }

    public static OnboardingFailFragment newInstance() {
        return new OnboardingFailFragment();
    }
}
