package com.example.pluginbluetooth.app.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Animation logic for the wheel in the {@link }.
 * <p>
 * <p>Intended to be used on the UI thread only.</p>
 */
public final class CalibrationWheelAnimator {

    private static final int FADE_TIME_MS = 1000;

    // y axis points downwards so positive angle is clockwise and angle 0 is horizontal
    private static final float TURN_ANGLE_BEGIN = (float) -Math.PI / 2;
    private static final float TURN_ANGLE_END = 0f;

    /**
     * Time to animate turning the wheel clockwise and back to the starting position.
     */
    private static final int TURN_TIME_MS = 3000;
    private static final int TURNS_COUNT = 3;

    private final ValueAnimator mFadeInAnimator;
    private final ValueAnimator mTurnAnimator;
    private final ValueAnimator mFadeOutAnimator;

    private boolean mRunning;
    private float mWheelAngle;
    private float mFingerAlphaFade;

    private Listener mListener;


    public CalibrationWheelAnimator() {
        mFadeInAnimator = createFadeAnimator(0f, 1.0f);
        mFadeInAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mRunning) {
                    mTurnAnimator.start();
                }
            }
        });

        mTurnAnimator = ValueAnimator.ofFloat(TURN_ANGLE_BEGIN, TURN_ANGLE_END);
        mTurnAnimator.setDuration(TURN_TIME_MS / 2);
        mTurnAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mTurnAnimator.setRepeatCount(TURNS_COUNT);
        mTurnAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mTurnAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mRunning) {
                    mFadeOutAnimator.start();
                }
            }
        });
        mTurnAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mWheelAngle = (Float) animation.getAnimatedValue();
                onAnimation();
            }
        });

        mFadeOutAnimator = createFadeAnimator(1.0f, 0f);
        mFadeOutAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (mRunning && mListener != null) {
                            mListener.onWheelAnimationEnd(CalibrationWheelAnimator.this);
                        }
                    }
                });
    }

    private ValueAnimator createFadeAnimator(float begin, float end) {
        ValueAnimator animator  = ValueAnimator.ofFloat(begin, end);
        animator.setDuration(FADE_TIME_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFingerAlphaFade = (Float) animation.getAnimatedValue();
                onAnimation();
            }
        });

        return animator;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        mWheelAngle = TURN_ANGLE_BEGIN;
        mRunning = true;
        mFadeInAnimator.start();
    }

    /**
     * Immediately cancels the animation.
     * <p>
     * No more call-backs will happen after this method returns.
     */
    public void cancel() {
        mRunning = false;
        if (mFadeInAnimator.isRunning()) {
            mFadeInAnimator.cancel();
        }
        if (mTurnAnimator.isRunning()) {
            mTurnAnimator.cancel();
        }
        if (mFadeOutAnimator.isRunning()) {
            mFadeOutAnimator.cancel();
        }
    }

    private void onAnimation() {
        if (mRunning && mListener != null) {
            mListener.onWheelAnimationUpdate(mWheelAngle, mFingerAlphaFade);
        }
    }


    public interface Listener {

        void onWheelAnimationUpdate(float wheelAngle, float fingerAlphaFade);

        @SuppressWarnings("UnusedParameters") // keep similar to android.animation.Animator.AnimatorListener
        void onWheelAnimationEnd(CalibrationWheelAnimator animator);
    }
}
