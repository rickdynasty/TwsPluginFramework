package com.example.pluginbluetooth.screens.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.widget.WatchLayout;

public class OnboardingWatchAnimationsLayout extends WatchLayout {

    private static final String TAG = OnboardingWatchAnimationsLayout.class.getSimpleName();

    private static final float MAX_ANGLE = 360;
    private static final float ANGLE_HOURS_INIT_START = 300;
    private static final float ANGLE_MINUTES_INIT_START = 60;

    private static final int BUTTON_ANIMATION_DURATION = 1500;
    private static final int ARROW_ANIMATION_DURATION = 1200;

    private static final int ARROW_FADE_IN_OUT = 1000;
    private static final int BUTTON_DELAY = 300;
    private static final int ARROW_FADE_OUT_DELAY = 500;

    private static final int BUTTON_OUT_ANIMATIONS = 3;

    private static final int BUTTON_SPAN_DP = 5;
    private static int sButtonOffset;

    private static final int ARROW_SPAN_DP = 5;
    private static int sArrowOffset;

    // Each tick takes about 24 ms and there are 180 steps around the watch
    private static final int WATCH_HAND_MINUTES_ANIMATION_DURATION = 24 * 180;
    private static final int WATCH_HAND_HOURS_ANIMATION_DURATION = 4 * WATCH_HAND_MINUTES_ANIMATION_DURATION;
    public static final long WATCH_HANDS_CANCEL_DURATION = (long) (0.15f * WATCH_HAND_HOURS_ANIMATION_DURATION);

    private boolean mOnSizeChangedCalled;
    private boolean mShouldStartWatchHandAnimations;
    private boolean mShouldStartButtonAndArrowAnimation;

    private ObjectAnimator mWatchHandHoursAnimator;
    private ObjectAnimator mWatchHandMinutesAnimator;

    protected ImageView mImageViewButton;
    protected ImageView mImageViewArrow;

    private boolean mCancelledButtonAndArrowAnimations;

    AnimatorSet mButtonAndArrowInAnimatorSet;
    AnimatorSet mButtonAndArrowOutAnimatorSet;

    private int mButtonOutAnimationsIndex = 0;

    private boolean mShouldCancelMinutesAnimations;
    private boolean mShouldCancelWatchHandAnimations;

    private CancelAnimationsStartedCallback mCancelAnimationsStartedCallback;

    public OnboardingWatchAnimationsLayout(Context context) {
        this(context, null, 0);
    }

    public OnboardingWatchAnimationsLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OnboardingWatchAnimationsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        setScalingEnabled(false);
    }

    private void init() {
        sButtonOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                        BUTTON_SPAN_DP,
                                                        getResources().getDisplayMetrics());
        sArrowOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_SPAN_DP,
                                                       getResources().getDisplayMetrics());
    }

    public void setCancelAnimationsStartedCallback(CancelAnimationsStartedCallback cancelAnimationsStartedCallback) {
        mCancelAnimationsStartedCallback = cancelAnimationsStartedCallback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImageViewButton = (ImageView) findViewById(R.id.imageViewButton);
        mImageViewArrow = (ImageView) findViewById(R.id.imageViewArrow);
        mImageViewWatchHandHours.setRotation(ANGLE_HOURS_INIT_START);
        mImageViewWatchHandMinutes.setRotation(ANGLE_MINUTES_INIT_START);
        mImageViewArrow.setAlpha(0f);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int horizontalPadding = getPaddingRight() + getPaddingLeft();
        int contentWidth = right - left - horizontalPadding;

        int imageViewButtonWidth = mImageViewButton.getMeasuredWidth();
        int imageViewButtonHeight = mImageViewButton.getMeasuredHeight();

        int lb = getPaddingLeft() + (contentWidth - imageViewButtonWidth) / 2;
        int tb = getPaddingTop();
        int rb = lb + imageViewButtonWidth;
        int bb = tb + imageViewButtonHeight;

        mImageViewButton.layout(lb, tb, rb, bb);

        int imageViewArrowWidth = mImageViewArrow.getMeasuredWidth();
        int imageViewArrowHeight = mImageViewArrow.getMeasuredHeight();

        int la = getPaddingLeft() + (contentWidth - imageViewArrowWidth) / 2;
        int ta = getPaddingTop();
        int ra = la + imageViewArrowWidth;
        int ba = ta + imageViewArrowHeight;

        mImageViewArrow.layout(la, ta, ra, ba);

    }

    public void stopAnimations() {
        if (mWatchHandHoursAnimator != null && mWatchHandHoursAnimator.isRunning()) {
            mWatchHandHoursAnimator.cancel();
        }
        if (mWatchHandMinutesAnimator != null && mWatchHandMinutesAnimator.isRunning()) {
            mWatchHandMinutesAnimator.cancel();
        }
        if (mButtonAndArrowInAnimatorSet != null && mButtonAndArrowInAnimatorSet.isRunning()) {
            mButtonAndArrowInAnimatorSet.cancel();
        }
    }

    public void resetAnimations() {
        mImageViewWatchHandHours.setRotation(ANGLE_HOURS_INIT_START);
        mImageViewWatchHandMinutes.setRotation(ANGLE_MINUTES_INIT_START);
        mImageViewButton.setX(0f);
        mImageViewArrow.setX(0f);
        mImageViewArrow.setAlpha(0f);
    }

    public void setHoursProgress(float progress) {
        float totalAngle = MAX_ANGLE;

        float angle = (ANGLE_HOURS_INIT_START + progress * totalAngle) % 360;

        mImageViewWatchHandHours.setRotation(angle);

        if (mShouldCancelWatchHandAnimations) {
            if (mCancelAnimationsStartedCallback != null) {
                mCancelAnimationsStartedCallback.cancelAnimationsStarted();
            }
            mShouldCancelMinutesAnimations = true;
            mWatchHandHoursAnimator.cancel();
            startHoursCancelAnimations();
        }
    }

    public void setMinutesProgress(float progress) {
        float totalAngle = MAX_ANGLE;

        float angle = (ANGLE_MINUTES_INIT_START + progress * totalAngle) % 360;

        mImageViewWatchHandMinutes.setRotation(angle);

        if (mShouldCancelMinutesAnimations) {
            if (mWatchHandMinutesAnimator != null && mWatchHandMinutesAnimator.isRunning()) {
                mWatchHandMinutesAnimator.cancel();
                startMinutesCancelAnimations();
            }
        }
    }

    public void startButtonAndArrowAnimations() {
        cancelButtonAndArrowAnimationsIfNeeded();
        if (mOnSizeChangedCalled) {
            mButtonOutAnimationsIndex = 0;
            mCancelledButtonAndArrowAnimations = false;
            startButtonAndArrowInAnimation(true);
        } else {
            mShouldStartButtonAndArrowAnimation = true;
        }
    }

    private void cancelButtonAndArrowAnimationsIfNeeded() {
        if (mButtonAndArrowInAnimatorSet != null && mButtonAndArrowInAnimatorSet.isRunning()) {
            mButtonAndArrowInAnimatorSet.removeAllListeners();
            mButtonAndArrowInAnimatorSet.cancel();
        }

        if (mButtonAndArrowOutAnimatorSet != null && mButtonAndArrowOutAnimatorSet.isRunning()) {
            mButtonAndArrowOutAnimatorSet.removeAllListeners();
            mButtonAndArrowOutAnimatorSet.cancel();
        }
    }

    private void startButtonAndArrowInAnimation(final boolean fadeInArrow) {
        cancelButtonAndArrowAnimationsIfNeeded();
        if (mButtonAndArrowInAnimatorSet == null || !mButtonAndArrowInAnimatorSet.isRunning()) {
            mImageViewArrow.setX(0f);
            ValueAnimator arrowInAnimator = ObjectAnimator.ofFloat(mImageViewArrow, "x", mImageViewArrow.getX(),
                                                                   mImageViewArrow.getX() - sArrowOffset);
            arrowInAnimator.setDuration(ARROW_ANIMATION_DURATION);
            arrowInAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            ValueAnimator arrowFadeInAnimator = ObjectAnimator.ofFloat(mImageViewArrow, "alpha", 0f, 1f);
            arrowFadeInAnimator.setDuration(ARROW_FADE_IN_OUT);

            mImageViewButton.setX(0f);

            ValueAnimator buttonInAnimator = ObjectAnimator.ofFloat(mImageViewButton, "x", mImageViewButton.getX(),
                                                                    mImageViewButton.getX() - sButtonOffset);
            buttonInAnimator.setDuration(BUTTON_ANIMATION_DURATION);
            buttonInAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            buttonInAnimator.setStartDelay(BUTTON_DELAY);

            mButtonAndArrowInAnimatorSet = new AnimatorSet();

            if (fadeInArrow) {
                mButtonAndArrowInAnimatorSet.playTogether(arrowInAnimator, arrowFadeInAnimator, buttonInAnimator);
            } else {
                mButtonAndArrowInAnimatorSet.playTogether(arrowInAnimator, buttonInAnimator);
            }

            mButtonAndArrowInAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    startButtonAndArrowOutAnimation();
                }
            });

            mButtonAndArrowInAnimatorSet.start();
        }
    }

    private void startButtonAndArrowOutAnimation() {
        if (mButtonAndArrowOutAnimatorSet == null || !mButtonAndArrowOutAnimatorSet.isRunning()) {
            mButtonOutAnimationsIndex++;
            ValueAnimator arrowOutAnimator = ObjectAnimator.ofFloat(mImageViewArrow, "x", mImageViewArrow.getX(),
                                                                    mImageViewArrow.getX() + sButtonOffset);
            arrowOutAnimator.setDuration(ARROW_ANIMATION_DURATION);
            arrowOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            ValueAnimator buttonOutAnimator = ObjectAnimator.ofFloat(mImageViewButton, "x", mImageViewButton.getX(),
                                                                     mImageViewButton.getX() + sButtonOffset);
            buttonOutAnimator.setDuration(BUTTON_ANIMATION_DURATION);
            buttonOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            buttonOutAnimator.setStartDelay(BUTTON_DELAY);

            ValueAnimator arrowFadeOutAnimator = ObjectAnimator.ofFloat(mImageViewArrow, "alpha", 1f, 0f);
            arrowFadeOutAnimator.setDuration(ARROW_FADE_IN_OUT);
            arrowOutAnimator.setStartDelay(ARROW_FADE_OUT_DELAY);

            mButtonAndArrowOutAnimatorSet = new AnimatorSet();

            if (mButtonOutAnimationsIndex == BUTTON_OUT_ANIMATIONS || mCancelledButtonAndArrowAnimations) {
                mButtonAndArrowOutAnimatorSet.playTogether(arrowOutAnimator, buttonOutAnimator, arrowFadeOutAnimator);
            } else {
                mButtonAndArrowOutAnimatorSet.playTogether(arrowOutAnimator, buttonOutAnimator);
            }

            mButtonAndArrowOutAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mCancelledButtonAndArrowAnimations) {
                        startButtonAndArrowInAnimation(false);
                    } else {
                        // If the out animation was cancelled when it was already started the arrow
                        // will not fade out because the fade out animation will not be added
                        if (mImageViewArrow.getAlpha() != 0) {
                            ValueAnimator arrowFadeOutAnimator =
                                    ObjectAnimator.ofFloat(mImageViewArrow, "alpha", 1f, 0f);
                            arrowFadeOutAnimator.setDuration(ARROW_FADE_IN_OUT);
                            arrowFadeOutAnimator.start();
                        }
                    }
                }
            });

            mButtonAndArrowOutAnimatorSet.start();
        }
    }

    public void startWatchHandAnimations() {
        if (mOnSizeChangedCalled) {
            if ((mWatchHandHoursAnimator == null || !mWatchHandHoursAnimator.isRunning()) &&
                (mWatchHandMinutesAnimator == null || !mWatchHandMinutesAnimator.isRunning())) {
                startHoursAnimations();
                startMinutesAnimations();
            }
        } else {
            mShouldStartWatchHandAnimations = true;
        }
    }

    public void cancelButtonAndArrowAnimations() {
        mCancelledButtonAndArrowAnimations = true;
    }

    public boolean isWatchHandAnimationsRunning() {
        return mWatchHandHoursAnimator != null && mWatchHandHoursAnimator.isRunning();
    }

    public void cancelWatchHandAnimations() {
        if (isWatchHandAnimationsRunning()) {
            mShouldCancelWatchHandAnimations = true;
        }
    }

    private void startHoursAnimations() {
        mWatchHandHoursAnimator = ObjectAnimator.ofFloat(this, "hoursProgress", 0.0f, 1.0f);
        mWatchHandHoursAnimator.setRepeatCount(Animation.INFINITE);
        mWatchHandHoursAnimator.setInterpolator(new LinearInterpolator());
        mWatchHandHoursAnimator.setDuration(WATCH_HAND_HOURS_ANIMATION_DURATION).start();
    }

    private void startMinutesAnimations() {
        mWatchHandMinutesAnimator = ObjectAnimator.ofFloat(this, "minutesProgress", 0.0f, 1.0f);
        mWatchHandMinutesAnimator.setRepeatCount(Animation.INFINITE);
        mWatchHandMinutesAnimator.setInterpolator(new LinearInterpolator());
        mWatchHandMinutesAnimator.setDuration(WATCH_HAND_MINUTES_ANIMATION_DURATION).start();
    }

    private void startHoursCancelAnimations() {
        ValueAnimator watchHandHoursCancelAnimator = ObjectAnimator.ofFloat(mImageViewWatchHandHours,
                                                                            "rotation",
                                                                            mImageViewWatchHandHours.getRotation(),
                                                                            MAX_ANGLE);
        watchHandHoursCancelAnimator.setDuration(WATCH_HANDS_CANCEL_DURATION);
        watchHandHoursCancelAnimator.setInterpolator(new DecelerateInterpolator());
        watchHandHoursCancelAnimator.start();
    }

    private void startMinutesCancelAnimations() {
        ValueAnimator watchHandMinutesCancelAnimator = ObjectAnimator.ofFloat(mImageViewWatchHandMinutes,
                                                                              "rotation",
                                                                              mImageViewWatchHandMinutes.getRotation(),
                                                                              MAX_ANGLE);
        watchHandMinutesCancelAnimator.setDuration(WATCH_HANDS_CANCEL_DURATION);
        watchHandMinutesCancelAnimator.setInterpolator(new DecelerateInterpolator());
        watchHandMinutesCancelAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mShouldStartWatchHandAnimations) {
            startHoursAnimations();
            startMinutesAnimations();
            mShouldStartWatchHandAnimations = false;
        }

        if (mShouldStartButtonAndArrowAnimation) {
            mButtonOutAnimationsIndex = 0;
            startButtonAndArrowInAnimation(true);
            mShouldStartButtonAndArrowAnimation = false;
        }

        mOnSizeChangedCalled = true;

    }

    public interface CancelAnimationsStartedCallback {

        void cancelAnimationsStarted();
    }

}