package com.example.pluginbluetooth.screens.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.example.pluginbluetooth.R;

import qrom.component.log.QRomLog;

public class OnboardingWatchFragment extends BaseOnboardingFragment implements
        OnboardingWatchAnimationsLayout.CancelAnimationsStartedCallback {
    private static final String TAG = "rick_Print:OnboardingWatchFragment";

    private static final int FADE_DURATION = 500;
    private static final int SCAN_TEXT_DURATION_MS = 11000;
    private static final int SCAN_TEXT_HELP_DURATION_MS = 6000;
    private static final int TEXT_CONTAINER_ANIMATION_IN_DELAY = 100;
    private static final int CONNECTED_DURATION_MS = 5000;
    private static final long CONNECTED_DURATION_FROM_CONNECTING_MS = OnboardingWatchAnimationsLayout.WATCH_HANDS_CANCEL_DURATION + 750;

    private static final int TEXT_CONTAINER_ANIMATION_OFFSET_DP = 10;
    private static final int WELCOME_TEXT_START_OFFSET_DP = 15;
    private float mWelcomeTextStartOffset;
    private static int mTextContainerOffset;

    private OnboardingWatchAnimationsLayout mOnboardingWatchAnimationsLayout;

    private TextView mTitleTextView;
    private TextView mDescriptionTextView;
    private View mTextContainerView;
    private TextView mCancel;

    private boolean mShowingScanningHelp;

    private final Handler mHandler = new Handler();

    private final Runnable mScannningRunnable = new Runnable() {
        @Override
        public void run() {
            startScanningTextTransition();
        }
    };

    private final Runnable mConnectedFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            getOnboarding().startFinishingOnboarding();
        }
    };

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_animations, container, false);

        mTitleTextView = (TextView) view.findViewById(R.id.title_text_view);
        mDescriptionTextView = (TextView) view.findViewById(R.id.description_text_view);
        mTextContainerView = view.findViewById(R.id.description_text_view);
        mCancel = (TextView) view.findViewById(R.id.cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Onboarding.getInstance().gotoState(Onboarding.State.CANCEL);
            }
        });
        QRomLog.i(TAG, "onCreateView");

        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOnboardingWatchAnimationsLayout = (OnboardingWatchAnimationsLayout) view.findViewById(
                R.id.imageViewMeterWatchHandContainer);
        mOnboardingWatchAnimationsLayout.setCancelAnimationsStartedCallback(this);

        mTextContainerOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_CONTAINER_ANIMATION_OFFSET_DP, getResources().getDisplayMetrics());

        mWelcomeTextStartOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                WELCOME_TEXT_START_OFFSET_DP, getResources().getDisplayMetrics());
        QRomLog.i(TAG, "onViewCreated");

        initUI();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mScannningRunnable);
        mHandler.removeCallbacks(mConnectedFinishedRunnable);
        mOnboardingWatchAnimationsLayout.stopAnimations();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        QRomLog.i(TAG, "onResume");
        mOnboardingWatchAnimationsLayout.resetAnimations();
    }

    @Override
    public String getName() {
        return "OnboardingAnimations";
    }

    protected void initUI() {
        final Onboarding.State state = getOnboarding().getState();
        QRomLog.i(TAG, "initUI state = " + state);
        if (state == Onboarding.State.SCANNING) {
            mHandler.postDelayed(mScannningRunnable, SCAN_TEXT_DURATION_MS);
            if (getOnboarding().foundOneDeviceWhenScanning()) {
                QRomLog.i(TAG, "foundOneDeviceWhenScanning");
                mOnboardingWatchAnimationsLayout.cancelButtonAndArrowAnimations();
                mOnboardingWatchAnimationsLayout.startWatchHandAnimations();
                mHandler.removeCallbacks(mScannningRunnable);

            } else {
                QRomLog.i(TAG, "foundOneDeviceWhenScanning false");
                mTitleTextView.setVisibility(View.VISIBLE);
                mTitleTextView.setText(getString(R.string.welcome));
                mDescriptionTextView.setVisibility(View.VISIBLE);
                mDescriptionTextView.setText(R.string.welcome_content);
                startWelcomeTextInTransition();
                mOnboardingWatchAnimationsLayout.startButtonAndArrowAnimations();
            }
//            mTitleTextView.setText("");
//            mTitleTextView.setVisibility(View.GONE);
//            mDescriptionTextView.setText(getString(R.string.scanning_description));
        } else if (state == Onboarding.State.CONNECTING) {
            mHandler.removeCallbacks(mScannningRunnable);
            mOnboardingWatchAnimationsLayout.startWatchHandAnimations();
            mTitleTextView.setVisibility(View.VISIBLE);
            mTitleTextView.setText(getString(R.string.onboarding_connecting));
            mDescriptionTextView.setText(getString(R.string.onboarding_connecting_desc));
        } else if (state == Onboarding.State.CONNECTED) {
            mHandler.removeCallbacks(mScannningRunnable);
            if (mOnboardingWatchAnimationsLayout.isWatchHandAnimationsRunning()) {
                mOnboardingWatchAnimationsLayout.cancelWatchHandAnimations();
            } else {
                goToConnectedUIFromInit();
            }
            mTitleTextView.setVisibility(View.VISIBLE);
            mTitleTextView.setText(getString(R.string.connection_successful));
            mDescriptionTextView.setText("");
        }
    }

    @Override
    protected void updateUI() {
        QRomLog.i(TAG, "updateUI state = " + getOnboarding().getState() + " getPreviousState = " + getOnboarding().getPreviousState());
        if (getOnboarding().getPreviousState() == Onboarding.State.PAUSED) {
            initUI();
        } else {
            final Onboarding.State state = getOnboarding().getState();
            if (state == Onboarding.State.SCANNING) {
                mHandler.postDelayed(mScannningRunnable, SCAN_TEXT_DURATION_MS);
                if (getOnboarding().foundOneDeviceWhenScanning()) {
                    mOnboardingWatchAnimationsLayout.cancelButtonAndArrowAnimations();
                    mOnboardingWatchAnimationsLayout.startWatchHandAnimations();
                    mHandler.removeCallbacks(mScannningRunnable);
                } else {
                    mOnboardingWatchAnimationsLayout.startButtonAndArrowAnimations();
                }
                startScanningInTransition();
            } else if (state == Onboarding.State.CONNECTING) {
                mHandler.removeCallbacks(mScannningRunnable);
                mOnboardingWatchAnimationsLayout.startWatchHandAnimations();
                startConnectingInTransition();
            } else if (state == Onboarding.State.CONNECTED) {
                mHandler.removeCallbacks(mScannningRunnable);
                if (mOnboardingWatchAnimationsLayout.isWatchHandAnimationsRunning()) {
                    mOnboardingWatchAnimationsLayout.cancelWatchHandAnimations();
                } else {
                    goToConnectedUI();
                }
            }
        }

    }

    private void goToConnectedUI() {
        startConnectedInTransition();
        mHandler.postDelayed(mConnectedFinishedRunnable, CONNECTED_DURATION_FROM_CONNECTING_MS);
    }

    private void goToConnectedUIFromInit() {
        mHandler.postDelayed(mConnectedFinishedRunnable, CONNECTED_DURATION_MS);
    }

    @Override
    protected void foundOneDeviceWhenScanning() {
        if (getOnboarding().getState() == Onboarding.State.SCANNING) {
            mOnboardingWatchAnimationsLayout.cancelButtonAndArrowAnimations();
            mOnboardingWatchAnimationsLayout.startWatchHandAnimations();
        }
    }

    @Override
    boolean handlesState(final Onboarding.State state) {
        return state == Onboarding.State.SCANNING ||
                state == Onboarding.State.CONNECTING ||
                state == Onboarding.State.CONNECTED;
    }

    private void startTextContainerInTransition() {
        mTextContainerView.setAlpha(0f);
        mTextContainerView.setY(mTextContainerView.getY() + mTextContainerOffset);
        mTextContainerView.animate()
                .alpha(1f)
                .yBy(-mTextContainerOffset)
                .setDuration(FADE_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(TEXT_CONTAINER_ANIMATION_IN_DELAY)
                .setListener(null);
    }

    private void startWelcomeTextInTransition() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillBefore(true);
        animationSet.setDuration(1000);
        animationSet.setInterpolator(new DecelerateInterpolator());

        Animation translateAnimation = new TranslateAnimation(0, 0, mWelcomeTextStartOffset, 0);
        mTitleTextView.startAnimation(translateAnimation);
        animationSet.addAnimation(translateAnimation);

        Animation alphaAnimation = new AlphaAnimation(0f, 1f);
        animationSet.addAnimation(alphaAnimation);
        mTitleTextView.startAnimation(animationSet);
    }

    private void startScanningInTransition() {
        mCancel.setVisibility(View.VISIBLE);
        mTextContainerView.animate()
                .alpha(0f)
                .yBy(-mTextContainerOffset)
                .setDuration(FADE_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isAdded()) {

                            mTextContainerView.setY(mTextContainerView.getY() + mTextContainerOffset);

                            mTitleTextView.setText(getString(R.string.welcome));
                            mTitleTextView.setVisibility(View.VISIBLE);
                            mDescriptionTextView.setText(getString(R.string.welcome_content));

                            startTextContainerInTransition();
                        }
                    }
                });

    }

    private void startScanningTextTransition() {

        mTextContainerView.animate()
                .alpha(0f)
                .yBy(-mTextContainerOffset)
                .setDuration(FADE_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isAdded()) {
                            mTextContainerView.setY(mTextContainerView.getY() + mTextContainerOffset);

                            if (!mShowingScanningHelp) {
                                mOnboardingWatchAnimationsLayout.cancelButtonAndArrowAnimations();
                                mTitleTextView.setText(getString(R.string.welcome));
                                mTitleTextView.setVisibility(View.VISIBLE);
                                mDescriptionTextView.setText(getString(R.string.welcome_content));
                                mHandler.removeCallbacks(mScannningRunnable);
                                mHandler.postDelayed(mScannningRunnable, SCAN_TEXT_HELP_DURATION_MS);
                                mShowingScanningHelp = true;
                            } else {
                                mOnboardingWatchAnimationsLayout.startButtonAndArrowAnimations();
                                mTitleTextView.setText(getString(R.string.welcome));
                                mTitleTextView.setVisibility(View.VISIBLE);
                                mDescriptionTextView.setText(getString(R.string.welcome_content));
                                mHandler.removeCallbacks(mScannningRunnable);
                                mHandler.postDelayed(mScannningRunnable, SCAN_TEXT_DURATION_MS);
                                mShowingScanningHelp = false;
                            }

                            startTextContainerInTransition();
                        }
                    }
                });

    }

    private void startConnectingInTransition() {
        QRomLog.i(TAG, "startConnectingInTransition()");
        mTextContainerView.animate()
                .alpha(0f)
                .yBy(-mTextContainerOffset)
                .setDuration(FADE_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isAdded()) {

                            mTextContainerView.setY(mTextContainerView.getY() + mTextContainerOffset);

                            mTitleTextView.setVisibility(View.VISIBLE);
                            mTitleTextView.setText(getString(R.string.onboarding_connecting));
                            mDescriptionTextView.setText(getString(R.string.onboarding_connecting_desc));

                            startTextContainerInTransition();
                        }
                    }
                });

    }

    private void startConnectedInTransition() {
        mCancel.setVisibility(View.GONE);
        mTextContainerView.animate()
                .alpha(0f)
                .yBy(-mTextContainerOffset)
                .setDuration(FADE_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isAdded()) {

                            mTextContainerView.setY(mTextContainerView.getY() + mTextContainerOffset);

                            mTitleTextView.setVisibility(View.VISIBLE);
                            mTitleTextView.setText(getString(R.string.connection_successful));
                            mDescriptionTextView.setText("");

                            startTextContainerInTransition();
                        }
                    }
                });

    }

    public static BaseOnboardingFragment newInstance() {
        return new OnboardingWatchFragment();
    }

    @Override
    public void cancelAnimationsStarted() {
        goToConnectedUI();
    }

    @Override
    int getEnterAnimRes(final Onboarding.State fromState) {
        if (fromState == Onboarding.State.PAUSED) {
            return R.anim.onboarding_enter;
        }
        return R.anim.onboarding_resume;
    }


    @Override
    int getExitAnimRes(final Onboarding.State toState, final boolean isJustResumed) {
        return R.anim.onboarding_pause;
    }
}
