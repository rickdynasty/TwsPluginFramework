/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tws.assistant.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.animation.LinearInterpolator;

import com.tencent.tws.assistant.utils.FloatProperty;
import com.tencent.tws.assistant.utils.MathUtils;

/**
 * Draws a ripple foreground.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class TwsRippleForeground extends TwsRippleComponent {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new LogDecelerateInterpolator(
            400f, 1.4f, 0);

    // Pixel-based accelerations and velocities.
    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 2048;
    private static final float WAVE_TOUCH_UP_ACCELERATION = 2048;
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3;

    // Bounded ripple animation properties.
    private static final int BOUNDED_ORIGIN_EXIT_DURATION = 300;
    private static final int BOUNDED_RADIUS_EXIT_DURATION = 800;
    private static final int BOUNDED_OPACITY_EXIT_DURATION = 400;
    private static final float MAX_BOUNDED_RADIUS = 350;

    private static final int RIPPLE_ENTER_DELAY = 30;
    private static final int OPACITY_ENTER_DURATION_FAST = 120;

    // Parent-relative values for starting position.
    protected float mStartingX;
    protected float mStartingY;
    protected float mClampedStartingX;
    protected float mClampedStartingY;

    // Target values for tween animations.
    private float mTargetX = 0;
    private float mTargetY = 0;

    /** Ripple target radius used when bounded. Not used for clamping. */
    private float mBoundedRadius = 0;

    // Software rendering properties.
    private float mOpacity = 1;

    // Values used to tween between the start and end positions.
    private float mTweenRadius = 0;
    private float mTweenX = 0;
    private float mTweenY = 0;

    /** Whether this ripple is bounded. */
    private boolean mIsBounded;

    /** Whether this ripple has finished its exit animation. */
    private boolean mHasFinishedExit;

    public TwsRippleForeground(TwsRippleDrawable owner, Rect bounds, float startingX, float startingY,
            boolean isBounded) {
        super(owner, bounds);

        mIsBounded = isBounded;
        mStartingX = startingX;
        mStartingY = startingY;

        if (isBounded) {
            mBoundedRadius = MAX_BOUNDED_RADIUS * 0.9f
                    + (float) (MAX_BOUNDED_RADIUS * Math.random() * 0.1);
        } else {
            mBoundedRadius = 0;
        }
    }

    @Override
    protected void onTargetRadiusChanged(float targetRadius) {
        clampStartingPosition();
    }

    @Override
    protected boolean drawSoftware(Canvas c, Paint p) {
        boolean hasContent = false;

        final int origAlpha = p.getAlpha();
        final int alpha = (int) (origAlpha * mOpacity + 0.5f);
        final float radius = getCurrentRadius();
        if (alpha > 0 && radius > 0) {
            final float x = getCurrentX();
            final float y = getCurrentY();
            p.setAlpha(alpha);
            c.drawCircle(x, y, radius, p);
            p.setAlpha(origAlpha);
            hasContent = true;
        }

        return hasContent;
    }

    /**
     * Returns the maximum bounds of the ripple relative to the ripple center.
     */
    public void getBounds(Rect bounds) {
        final int outerX = (int) mTargetX;
        final int outerY = (int) mTargetY;
        final int r = (int) mTargetRadius + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    /**
     * Specifies the starting position relative to the drawable bounds. No-op if
     * the ripple has already entered.
     */
    public void move(float x, float y) {
        mStartingX = x;
        mStartingY = y;

        clampStartingPosition();
    }

    /**
     * @return {@code true} if this ripple has finished its exit animation
     */
    public boolean hasFinishedExit() {
        return mHasFinishedExit;
    }

    @Override
    protected Animator createSoftwareEnter(boolean fast) {
        // Bounded ripples don't have enter animations.
        if (mIsBounded) {
            return null;
        }

        final int duration = (int)
                (1000 * Math.sqrt(mTargetRadius / WAVE_TOUCH_DOWN_ACCELERATION * mDensity) + 0.5);

        final ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1);
        tweenRadius.setAutoCancel(true);
        tweenRadius.setDuration(duration);
        tweenRadius.setInterpolator(LINEAR_INTERPOLATOR);
        tweenRadius.setStartDelay(RIPPLE_ENTER_DELAY);

        final ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1);
        tweenOrigin.setAutoCancel(true);
        tweenOrigin.setDuration(duration);
        tweenOrigin.setInterpolator(LINEAR_INTERPOLATOR);
        tweenOrigin.setStartDelay(RIPPLE_ENTER_DELAY);

        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        opacity.setAutoCancel(true);
        opacity.setDuration(OPACITY_ENTER_DURATION_FAST);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

        final AnimatorSet set = new AnimatorSet();
        set.play(tweenOrigin).with(tweenRadius).with(opacity);

        return set;
    }

    private float getCurrentX() {
        return MathUtils.lerp(mClampedStartingX - mBounds.exactCenterX(), mTargetX, mTweenX);
    }

    private float getCurrentY() {
        return MathUtils.lerp(mClampedStartingY - mBounds.exactCenterY(), mTargetY, mTweenY);
    }

    private int getRadiusExitDuration() {
        final float remainingRadius = mTargetRadius - getCurrentRadius();
        return (int) (1000 * Math.sqrt(remainingRadius / (WAVE_TOUCH_UP_ACCELERATION
                + WAVE_TOUCH_DOWN_ACCELERATION) * mDensity) + 0.5);
    }

    private float getCurrentRadius() {
        return MathUtils.lerp(0, mTargetRadius, mTweenRadius);
    }

    private int getOpacityExitDuration() {
        return (int) (1000 * mOpacity / WAVE_OPACITY_DECAY_VELOCITY + 0.5f);
    }

    /**
     * Compute target values that are dependent on bounding.
     */
    private void computeBoundedTargetValues() {
        mTargetX = (mClampedStartingX - mBounds.exactCenterX()) * .7f;
        mTargetY = (mClampedStartingY - mBounds.exactCenterY()) * .7f;
        mTargetRadius = mBoundedRadius;
    }

    @Override
    protected Animator createSoftwareExit() {
        final int radiusDuration;
        final int originDuration;
        final int opacityDuration;
        if (mIsBounded) {
            computeBoundedTargetValues();

            radiusDuration = BOUNDED_RADIUS_EXIT_DURATION;
            originDuration = BOUNDED_ORIGIN_EXIT_DURATION;
            opacityDuration = BOUNDED_OPACITY_EXIT_DURATION;
        } else {
            radiusDuration = getRadiusExitDuration();
            originDuration = radiusDuration;
            opacityDuration = getOpacityExitDuration();
        }

        final ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1);
        tweenRadius.setAutoCancel(true);
        tweenRadius.setDuration(radiusDuration);
        tweenRadius.setInterpolator(DECELERATE_INTERPOLATOR);

        final ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1);
        tweenOrigin.setAutoCancel(true);
        tweenOrigin.setDuration(originDuration);
        tweenOrigin.setInterpolator(DECELERATE_INTERPOLATOR);

        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0);
        opacity.setAutoCancel(true);
        opacity.setDuration(opacityDuration);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

        final AnimatorSet set = new AnimatorSet();
        set.play(tweenOrigin).with(tweenRadius).with(opacity);
        set.addListener(mAnimationListener);

        return set;
    }

    /**
     * Clamps the starting position to fit within the ripple bounds.
     */
    private void clampStartingPosition() {
        final float cX = mBounds.exactCenterX();
        final float cY = mBounds.exactCenterY();
        final float dX = mStartingX - cX;
        final float dY = mStartingY - cY;
        final float r = mTargetRadius;
        if (dX * dX + dY * dY > r * r) {
            // Point is outside the circle, clamp to the perimeter.
            final double angle = Math.atan2(dY, dX);
            mClampedStartingX = cX + (float) (Math.cos(angle) * r);
            mClampedStartingY = cY + (float) (Math.sin(angle) * r);
        } else {
            mClampedStartingX = mStartingX;
            mClampedStartingY = mStartingY;
        }
    }

    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            mHasFinishedExit = true;
        }
    };

    /**
    * Interpolator with a smooth log deceleration.
    */
    private static final class LogDecelerateInterpolator implements TimeInterpolator {
        private final float mBase;
        private final float mDrift;
        private final float mTimeScale;
        private final float mOutputScale;

        public LogDecelerateInterpolator(float base, float timeScale, float drift) {
            mBase = base;
            mDrift = drift;
            mTimeScale = 1f / timeScale;

            mOutputScale = 1f / computeLog(1f);
        }

        private float computeLog(float t) {
            return 1f - (float) Math.pow(mBase, -t * mTimeScale) + (mDrift * t);
        }

        @Override
        public float getInterpolation(float t) {
            return computeLog(t) * mOutputScale;
        }
    }

    /**
     * Property for animating radius between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForeground> TWEEN_RADIUS =
            new FloatProperty<TwsRippleForeground>("tweenRadius") {
        @Override
        public void setValue(TwsRippleForeground object, float value) {
            object.mTweenRadius = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForeground object) {
            return object.mTweenRadius;
        }
    };

    /**
     * Property for animating origin between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForeground> TWEEN_ORIGIN =
            new FloatProperty<TwsRippleForeground>("tweenOrigin") {
                @Override
                public void setValue(TwsRippleForeground object, float value) {
                    object.mTweenX = value;
                    object.mTweenY = value;
                    object.invalidateSelf();
                }

                @Override
                public Float get(TwsRippleForeground object) {
                    return object.mTweenX;
                }
            };

    /**
     * Property for animating opacity between 0 and its target value.
     */
    private static final FloatProperty<TwsRippleForeground> OPACITY =
            new FloatProperty<TwsRippleForeground>("opacity") {
        @Override
        public void setValue(TwsRippleForeground object, float value) {
            object.mOpacity = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForeground object) {
            return object.mOpacity;
        }
    };

    @Override
    protected int getRippleStyle() {
        return TwsRippleDrawable.RIPPLE_STYLE_NORMAL;
    }
}
