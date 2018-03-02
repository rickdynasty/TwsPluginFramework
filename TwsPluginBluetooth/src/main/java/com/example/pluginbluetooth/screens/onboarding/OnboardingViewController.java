package com.example.pluginbluetooth.screens.onboarding;

import com.example.pluginbluetooth.screens.BaseFragment;

/**
 * Created by Administrator on 2018/3/2.
 */

interface OnboardingViewController {

    Onboarding getOnboarding();

    void gotoNextFragment(BaseFragment fragment, boolean addToBackStack);

    void clearBackStack();
}
