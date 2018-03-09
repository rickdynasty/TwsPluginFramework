package com.example.pluginbluetooth.screens.onboarding;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.MainActivity;
import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.screens.BaseActivity;
import com.example.pluginbluetooth.screens.BaseFragment;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2018/3/1.
 */

public class OnboardingActivity extends BaseActivity implements Onboarding.OnboardingChangeListener, OnboardingViewController {
    private static final String TAG = "rick_Print:OnboardingActivity";

    private Onboarding mOnboarding;
    private boolean mIsResuming;

    private int mBackStackStartLevel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QRomLog.i(TAG, "onCreate");
        mBackStackStartLevel = 0;

        setContentView(R.layout.activity_onboarding);
        setTitle("Bluetooth");

        mOnboarding = Onboarding.getInstance();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        QRomLog.i(TAG, "onResume");
        mIsResuming = true;
        super.onResume();
        mOnboarding.resume();
        mOnboarding.setChangeListener(this);
        updateOrReplaceCurrentFragment();
        mOnboarding.updateInternetConnectivityEnabled(true);
        mIsResuming = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        QRomLog.i(TAG, "onNewIntent");
        super.onNewIntent(intent);
    }

    // OnboardingChangeListener
    @Override
    public void onOnboardingStateChanged() {
        QRomLog.i(TAG, "onOnboardingStateChanged");

    }

    @Override
    public void foundOneDeviceWhenScanning() {
        QRomLog.i(TAG, "foundOneDeviceWhenScanning");
        BaseOnboardingFragment onboardingFragment = (BaseOnboardingFragment) getSupportFragmentManager()
                .findFragmentById(R.id.contentOnboarding);
        if (onboardingFragment != null) {
            onboardingFragment.foundOneDeviceWhenScanning();
        }
    }

    @Override
    public void onInternetConnectivityChanged(boolean requiredInState, boolean enabled) {
        QRomLog.i(TAG, "onInternetConnectivityChanged");

    }

    public Onboarding getOnboarding() {
        QRomLog.i(TAG, "getOnboarding");
        return mOnboarding;
    }

    private void updateOrReplaceCurrentFragment() {
        QRomLog.i(TAG, "updateOrReplaceCurrentFragment");
        BaseOnboardingFragment baseOnboardingFragment = getCurrentDisplayedBaseOnboardingFragment();
        if (baseOnboardingFragment != null && baseOnboardingFragment.handlesState(mOnboarding.getState())) {
            baseOnboardingFragment.updateUI();
        } else {
            displayNextFragment();
        }
    }

    private BaseOnboardingFragment getCurrentDisplayedBaseOnboardingFragment() {
        QRomLog.i(TAG, "getCurrentDisplayedBaseOnboardingFragment");
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.contentOnboarding);
        if (fragment != null && fragment instanceof BaseOnboardingFragment) {
            return (BaseOnboardingFragment) fragment;
        } else {
            return null;
        }
    }

    private void displayNextFragment() {
        QRomLog.i(TAG, "displayNextFragment mState:" + mOnboarding.getState());
        switch (mOnboarding.getState()) {
            case SCANNING:
            case CONNECTING:
            case CONNECTED:
                gotoNextFragment(OnboardingWatchFragment.newInstance());
                break;
            case BLUETOOTH_ENABLING:
                gotoNextFragment(EnableBluetoothFragment.newInstance());
                break;
            case LOCATION_ENABLING:
                gotoNextFragment(LocationEnableFragment.newInstance());
                break;
            case LOCATION_PERMISSION:
                gotoNextFragment(LocationPermissionFragment.newInstance());
                break;
            case FAIL:
                gotoNextFragment(OnboardingFailFragment.newInstance());
                break;
            case CANCEL:
                gotoPairActivity();
                break;
            case FINISHING:
                // debug 留后门
                if (BuildConfig.DEBUG) {
                    Onboarding.getInstance().setHasPairSuccess(true);
                    //HealthDataProcessor.getInstance().updateMacAddress();
                    //gotoNextFragment(OnboardingCalibrationInitiationFragment.newInstance());
                } else {
                    // 检查是否为中国区发行的表
                    // checoutWatchNumber();
                }
                break;
            case FINISHED:
                onboardingFinished();
                break;
            default:
                QRomLog.i(TAG, "Weird state in onboarding " + mOnboarding.getState());
                break;
        }
    }

    private void gotoNextFragment(final BaseFragment fragment) {
        QRomLog.i(TAG, "gotoNextFragment fragment:" + fragment);
        gotoNextFragment(fragment, false);
    }

    @Override
    public void gotoNextFragment(final BaseFragment fragment, boolean addToBackStack) {
        int animExitRes = R.anim.exit_to_left;
        BaseOnboardingFragment currentOnboardingFragment = getCurrentDisplayedBaseOnboardingFragment();
        if (currentOnboardingFragment != null) {
            animExitRes = currentOnboardingFragment.getExitAnimRes(mOnboarding.getState(), mIsResuming);
        }

        int animEnterRes = R.anim.enter_from_right;
        int animPopEnterRes;
        int animPopExitRes;
        if (fragment instanceof BaseOnboardingFragment) {
            animEnterRes = ((BaseOnboardingFragment) fragment).getEnterAnimRes(mOnboarding.getPreviousState());
            animPopExitRes = ((BaseOnboardingFragment) fragment).getPopExitAnimRes();
            animPopEnterRes = ((BaseOnboardingFragment) fragment).getPopEnterAnimRes();
        } else {
            animPopEnterRes = R.anim.enter_from_left;
            animPopExitRes = R.anim.exit_to_right;
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(animEnterRes, animExitRes, animPopEnterRes, animPopExitRes)
                .replace(R.id.contentOnboarding, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void clearBackStack() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount > 0) {
            mBackStackStartLevel = backStackEntryCount;
        }
    }

    public void onboardingFinished() {
        startMainActivity(true);
    }

    private void gotoPairActivity() {
        QRomLog.i(TAG, "gotoPairActivity");
        // TODO Auto-generated method stub
        Intent intent = new Intent("com.tencent.tws.gdevicemanager.action.READY_PAIR");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private void startMainActivity(boolean removeCurrentFragmentFirst) {
        QRomLog.i(TAG, "startMainActivity");
        if (!removeCurrentFragmentFirst) {
            MainActivity.start(this);
            finish();
        } else {
            final Intent intent = MainActivity.createStartIntent(this, R.anim.onboarding_enter_fast);
            int removeTime = removeCurrentFragment();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                    finish();
                }
            }, removeTime);
        }
    }

    /**
     * Removes the current contentOnboarding fragment and returns the time it
     * will take
     *
     * @return the time in ms it will take to remove the fragment
     */
    private int removeCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contentOnboarding);
        if (currentFragment == null) {
            return 0;
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.onboarding_enter, R.anim.onboarding_exit).remove(currentFragment)
                    .commit();
            return getResources().getInteger(R.integer.screen_transition_onboarding_exit);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        QRomLog.i(TAG, "onBackPressed");
        final BaseOnboardingFragment fragment = getCurrentDisplayedBaseOnboardingFragment();

        if (mBackStackStartLevel > 0 && getSupportFragmentManager().getBackStackEntryCount() <= mBackStackStartLevel) {
            finish();
        } else if (fragment == null || fragment.isBackAllowed()) {
            super.onBackPressed();
        }
    }
}
