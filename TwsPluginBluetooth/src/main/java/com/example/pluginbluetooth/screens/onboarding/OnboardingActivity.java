package com.example.pluginbluetooth.screens.onboarding;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.MainActivity;
import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.future.AlwaysCallback;
import com.example.pluginbluetooth.future.FailCallback;
import com.example.pluginbluetooth.future.SuccessCallback;
import com.example.pluginbluetooth.provider.ProviderFactory;
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
    protected void onNewIntent(final Intent intent) {
        QRomLog.i(TAG, "onNewIntent");
        super.onNewIntent(intent);
        handleNewIntent(intent);
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
    public void onPause() {
        QRomLog.i(TAG, "onPause");
        mOnboarding.setChangeListener(null);
        mOnboarding.pause();
        super.onPause();
    }

    @Override
    public void onOnboardingStateChanged() {
        QRomLog.i(TAG, "onOnboardingStateChanged");
        updateOrReplaceCurrentFragment();
    }

    @Override
    public void onInternetConnectivityChanged(boolean requiredInState, boolean enabled) {
        QRomLog.i(TAG, "onInternetConnectivityChanged");
//        if (enabled || !requiredInState) {
//            if (mEnableInternetAccessDialogFragment != null) {
//                mEnableInternetAccessDialogFragment.dismiss();
//                mEnableInternetAccessDialogFragment = null;
//            }
//        } else {
//            if (mEnableInternetAccessDialogFragment == null) {
//                mEnableInternetAccessDialogFragment = new EnableInternetAccessDialogFragment();
//                mEnableInternetAccessDialogFragment.show(getSupportFragmentManager(), null);
//            }
//        }
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

    private void handleNewIntent(final Intent intent) {
        if (intent != null) {
            final Uri data = intent.getData();
//            if (data != null && isEmailValidationIntent(data)) {
//                receivedEmailValidationIntent();
//            }
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
                Onboarding.getInstance().setHasPairSuccess(true);
                gotoNextFragment(OnboardingCalibrationInitiationFragment.newInstance());
                break;
            case FINISHED:
                onboardingFinished();
                break;
            default:
                QRomLog.i(TAG, "Weird state in onboarding " + mOnboarding.getState());
                break;
        }
    }

    public static final long RESET_DEVICE_TIMEOUT = 15000;
    public static final String ACTION_FORGET_WATCH = "forget_watch";

    private void forgetWatch() {
        final ProgressDialog progressDialog = android.app.ProgressDialog.show(this, "提示", "正在解除配对...");

        // progressDialog.setIconAttribute(android.R.attr.alertDialogIcon);
        // progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // progressDialog.show();

        //AnalyticsFeatureProvider.getInstance().sendAction(ACTION_FORGET_WATCH);
        ProviderFactory.getWatch().resetDevice().timeout(RESET_DEVICE_TIMEOUT).success(new SuccessCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                QRomLog.i("forgetWatch()", "Resetting device succeeded");
                progressDialog.dismiss();
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(final Throwable error) {
                QRomLog.i("forgetWatch()", "Resetting device failed", error);
                progressDialog.dismiss();
            }
        }).always(new AlwaysCallback() {
            @Override
            public void onFinished() {
//                final LoginProvider loginProvider = ProviderFactory.getCurrentLoginProvider(getApplicationContext());
//                if (loginProvider != null)
//                    loginProvider.logout();

                ProviderFactory.getWatch().forgetDevice();
                Onboarding.getInstance().setHasPairSuccess(false);
                ProviderFactory.getWatch().setOnboardingFinished(false);
                ProviderFactory.getWatch().setWroteOnboardingDeviceSettings(false);
                gotoPairActivity();

                progressDialog.dismiss();
            }
        });
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

    public void calibrationFinished() {
        mOnboarding.finishOnboarding();
    }

    public void onboardingFinished() {
        startMainActivity(true);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final BaseOnboardingFragment baseOnboardingFragment = getCurrentDisplayedBaseOnboardingFragment();
        if (baseOnboardingFragment != null) {
            baseOnboardingFragment.onActivityResult(requestCode, resultCode, data);
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
        QRomLog.i(TAG, "onBackPressed");
        final BaseOnboardingFragment fragment = getCurrentDisplayedBaseOnboardingFragment();

        if (mBackStackStartLevel > 0 && getSupportFragmentManager().getBackStackEntryCount() <= mBackStackStartLevel) {
            finish();
        } else if (fragment == null || fragment.isBackAllowed()) {
            super.onBackPressed();
        }
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
}
