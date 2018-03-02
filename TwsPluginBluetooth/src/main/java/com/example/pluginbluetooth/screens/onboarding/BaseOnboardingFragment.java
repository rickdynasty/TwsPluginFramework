package com.example.pluginbluetooth.screens.onboarding;

import android.support.annotation.AnimRes;
import android.support.v4.app.FragmentActivity;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.screens.onboarding.Onboarding.State;

import com.example.pluginbluetooth.screens.BaseFragment;

public abstract class BaseOnboardingFragment extends BaseFragment {

    public OnboardingViewController getOnboardingViewController() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof OnboardingViewController) {
            return (OnboardingViewController) activity;
        } else {
            throw new RuntimeException("Containing Activity needs to implement OnboardingController");
        }
    }

    protected void updateUI() {

    }

    protected void foundOneDeviceWhenScanning() {

    }

    protected Onboarding getOnboarding() {
        return getOnboardingViewController().getOnboarding();
    }

    /**
     * Returns true if the fragment handles a specific state
     *
     * @param state the state to check for
     * @return true if the fragment handles the state, false otherwise
     */
    abstract boolean handlesState(State state);

    /**
     * Returns the anim resource for entering this fragment.
     *
     * @param fromState the state that will be left
     * @return the anim resource that will be used for animate in the fragment
     */
    @AnimRes
    int getEnterAnimRes(State fromState) {
        return R.anim.onboarding_enter;
    }

    /**
     * Returns the anim resource for leaving this fragment.
     *
     * @param toState       the state that will be displayed instead of this
     * @param isJustResumed means that fragment will be exited just when it goes from paused to resumed
     * @return the anim resource that will be used for animate out the fragment
     */
    @AnimRes
    int getExitAnimRes(State toState, boolean isJustResumed) {
        return R.anim.onboarding_exit;
    }

    /**
     * Returns the anim resource for entering this fragment from the back stack using a pop
     *
     * @return the anim resource that will be used for animate in this fragment from the back stack
     */
    @AnimRes
    int getPopEnterAnimRes() {
        return R.anim.none;
    }

    /**
     * Returns the anim resource for exiting this fragment using a back stack pop.
     *
     * @return the anim resource that will be used for animate out the fragment after a back stack pop
     */
    @AnimRes
    int getPopExitAnimRes() {
        return R.anim.none;
    }

    /**
     * Returns a value for allowing back press or not.
     *
     * @return boolean flag if pressing back is allowed
     */
    boolean isBackAllowed() {
        return true;
    }
}
