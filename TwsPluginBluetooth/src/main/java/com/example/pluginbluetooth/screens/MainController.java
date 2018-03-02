package com.example.pluginbluetooth.screens;

import android.support.annotation.AnimRes;

public interface MainController {

    void gotoNextFragment(BaseFragment fragment);

    void gotoNextFragment(final BaseFragment fragment, final boolean leaveRevealedBackStack);

    void goBack();

    void popUntilDashboard();

    void gotoOnboarding();

    void gotoNextFragmentWithAnimations(BaseFragment fragment, @AnimRes int enter,
                                        @AnimRes int exit, @AnimRes int popEnter, @AnimRes int popExit);

    void gotoRevealedFragment(BaseFragment fragment);
}
