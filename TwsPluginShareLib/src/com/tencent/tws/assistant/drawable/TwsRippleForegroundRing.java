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
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Build;
import android.view.animation.LinearInterpolator;

import com.tencent.tws.assistant.utils.FloatProperty;
import com.tencent.tws.assistant.utils.MathUtils;

/**
 * Draws a ripple foreground.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class TwsRippleForegroundRing extends TwsRippleForeground {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    // Pixel-based accelerations and velocities.
    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 1024 * 6f;

    private static final int RIPPLE_ENTER_DELAY = 0;

    private static final float DEFAULT_TWEEN_RADIUS = 0.5f;
    private static final float DEFAULT_TWEEN_X = 0.5f;
    private static final float DEFAULT_TWEEN_Y = 0.5f;

    // Target values for tween animations.
    private float mOuterX = 0;
    private float mOuterY = 0;

    // Values used to tween between the start and end positions.
    private float mTweenRadius = DEFAULT_TWEEN_RADIUS;
    private float mTweenX = DEFAULT_TWEEN_X;
    private float mTweenY = DEFAULT_TWEEN_Y;

    /** Whether this ripple has finished its exit animation. */
    private boolean mHasFinishedExit;

    public TwsRippleForegroundRing(TwsRippleDrawable owner, Rect bounds, float startingX, float startingY,
            boolean isBounded) {
        super(owner, bounds, startingX, startingY, isBounded);
    }

    @Override
    protected boolean drawSoftware(Canvas c, Paint p) {
        boolean hasContent = false;

        final float radius = getCurrentEnterRadius();
        final float strokeWidth = getCurrentStrokeWidth();
        if (radius > 0) {
            final float x = getCurrentEnterX();
            final float y = getCurrentEnterY();
            p.setStyle(Style.STROKE);
            p.setStrokeWidth(strokeWidth);
            c.drawCircle(x, y, radius, p);
            hasContent = true;
        }

        return hasContent;
    }

    /**
     * @return {@code true} if this ripple has finished its exit animation
     */
    public boolean hasFinishedExit() {
        return mHasFinishedExit;
    }

    @Override
    protected Animator createSoftwareEnter(boolean fast) {
//        int duration = (int) (1000 * Math.sqrt(mTargetRadius / WAVE_TOUCH_DOWN_ACCELERATION * mDensity) + 0.5);
        int duration = 120;

        final ObjectAnimator tweenRadiusEnter = ObjectAnimator.ofFloat(this, TWEEN_RADIUS_ENTER, 1);
        tweenRadiusEnter.setAutoCancel(true);
        tweenRadiusEnter.setDuration(duration);
        tweenRadiusEnter.setInterpolator(LINEAR_INTERPOLATOR);
        tweenRadiusEnter.setStartDelay(RIPPLE_ENTER_DELAY);

        final ObjectAnimator tweenOriginEnter = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN_ENTER, 1);
        tweenOriginEnter.setAutoCancel(true);
        tweenOriginEnter.setDuration(duration);
        tweenOriginEnter.setInterpolator(LINEAR_INTERPOLATOR);
        tweenOriginEnter.setStartDelay(RIPPLE_ENTER_DELAY);

        final AnimatorSet set = new AnimatorSet();
        set.play(tweenOriginEnter).with(tweenRadiusEnter);
        set.addListener(mAnimationListener);

        return set;
    }

    private float getCurrentEnterX() {
        return MathUtils.lerp(mClampedStartingX - mBounds.exactCenterX(), mOuterX, mTweenX);
    }

    private float getCurrentEnterY() {
        return MathUtils.lerp(mClampedStartingY - mBounds.exactCenterY(), mOuterY, mTweenY);
    }

    private float getCurrentEnterRadius() {
        return MathUtils.lerp(0, mTargetRadius, mTweenRadius);
    }

    private float getCurrentStrokeWidth() {
        return MathUtils.lerp(mTargetRadius / 3, 0, mTweenRadius);
    }

    @Override
    protected Animator createSoftwareExit() {
        final AnimatorSet set = new AnimatorSet();
        return set;
    }

    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            mTweenRadius = DEFAULT_TWEEN_RADIUS;
            mTweenX = DEFAULT_TWEEN_X;
            mTweenY = DEFAULT_TWEEN_Y;
        };

        @Override
        public void onAnimationEnd(Animator animator) {
            mHasFinishedExit = true;
            mTweenRadius = 0;
            mTweenX = 0;
            mTweenY = 0;
            removeSelf();
        }
    };

    /**
     * Property for animating radius between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForegroundRing> TWEEN_RADIUS_ENTER = new FloatProperty<TwsRippleForegroundRing>(
            "tweenRadiusEnter") {
        @Override
        public void setValue(TwsRippleForegroundRing object, float value) {
            object.mTweenRadius = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForegroundRing object) {
            return object.mTweenRadius;
        }
    };

    /**
     * Property for animating origin between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForegroundRing> TWEEN_ORIGIN_ENTER = new FloatProperty<TwsRippleForegroundRing>(
            "tweenOriginEnter") {
        @Override
        public void setValue(TwsRippleForegroundRing object, float value) {
            object.mTweenX = value;
            object.mTweenY = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForegroundRing object) {
            return object.mTweenX;
        }
    };

    @Override
    protected int getRippleStyle() {
        return TwsRippleDrawable.RIPPLE_STYLE_RING;
    }

    private void removeSelf() {
        // The owner will invalidate itself.
        mOwner.removeRipple(this);
    }
}
