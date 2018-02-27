package com.animaconnected.secondo.screens.onboarding;

import com.animaconnected.secondo.screens.BaseFragment;

/**
 * Created by Administrator on 2018/2/27.
 */

public interface OnboardingViewController {
    Onboarding getOnboarding();

    void gotoNextFragment(BaseFragment fragment, boolean addToBackStack);

    void clearBackStack();
}
