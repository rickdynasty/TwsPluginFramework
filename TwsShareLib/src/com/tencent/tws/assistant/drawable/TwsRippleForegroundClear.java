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
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.view.animation.LinearInterpolator;

import com.tencent.tws.assistant.utils.FloatProperty;
import com.tencent.tws.assistant.utils.MathUtils;

/**
 * Draws a ripple foreground.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class TwsRippleForegroundClear extends TwsRippleForeground {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new LogDecelerateInterpolator(
            400f, 1.4f, 0);

    // Pixel-based accelerations and velocities.
    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 1024 * 6;

    // Bounded ripple animation properties.
    private static final int BOUNDED_ORIGIN_EXIT_DURATION = 200;
    private static final int BOUNDED_RADIUS_EXIT_DURATION = 300;

    private static final int RIPPLE_ENTER_DELAY = 0;

    // Target values for tween animations.
    private float mOuterX = 0;
    private float mOuterY = 0;

    // Software rendering properties.
    private float mOpacity = 0.1f;

    // Values used to tween between the start and end positions.
    private float mTweenRadius_enter = 0.7f;
    private float mTweenX_enter = 0.7f;
    private float mTweenY_enter = 0.7f;

    // Values used to tween between the start and end positions.
    private float mTweenRadius_exit = 0f;
    private float mTweenX_exit = 0f;
    private float mTweenY_exit = 0f;

    /** Whether this ripple has finished its exit animation. */
    private boolean mHasFinishedExit;

    private static final PorterDuffXfermode CLEAR = new PorterDuffXfermode(Mode.CLEAR);

    public TwsRippleForegroundClear(TwsRippleDrawable owner, Rect bounds, float startingX, float startingY,
            boolean isBounded) {
        super(owner, bounds, startingX, startingY, isBounded);
    }

    @Override
    protected boolean drawSoftware(Canvas c, Paint p) {
        boolean hasContent = false;

        final int origAlpha = p.getAlpha();
        final int alpha = (int) (origAlpha * mOpacity + 0.5f);
        final float radius = getCurrentEnterRadius();
        if (radius > 0 && alpha > 0) {
            final float x = getCurrentEnterX();
            final float y = getCurrentEnterY();
            p.setXfermode(null);
            p.setStyle(Style.FILL);
            p.setAlpha(alpha);
            c.drawCircle(x, y, radius, p);
            p.setAlpha(origAlpha);
            hasContent = true;
        }
        final float radiusExit = getCurrentExitRadius();
        if (radiusExit > 0) {
            final float xExit = getCurrentExitX();
            final float yExit = getCurrentExitY();
            p.setXfermode(CLEAR);
            p.setStyle(Style.FILL);
            c.drawCircle(xExit, yExit, radiusExit, p);
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
        int duration = fast ? 120 :(int)
                (1000 * Math.sqrt(mTargetRadius / WAVE_TOUCH_DOWN_ACCELERATION * mDensity) + 0.5);
//        final int duration = 100;

        final ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1);
        opacity.setAutoCancel(true);
        opacity.setDuration(duration);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);

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
        set.play(tweenOriginEnter).with(tweenRadiusEnter).with(opacity);

        return set;
    }

    private float getCurrentEnterX() {
        return MathUtils.lerp(mClampedStartingX - mBounds.exactCenterX(), mOuterX, mTweenX_enter);
    }

    private float getCurrentEnterY() {
        return MathUtils.lerp(mClampedStartingY - mBounds.exactCenterY(), mOuterY, mTweenY_enter);
    }

    private float getCurrentEnterRadius() {
        return MathUtils.lerp(0, mTargetRadius, mTweenRadius_enter);
    }

    private float getCurrentExitX() {
        return MathUtils.lerp(mClampedStartingX - mBounds.exactCenterX(), mOuterX, mTweenX_exit);
    }
    
    private float getCurrentExitY() {
        return MathUtils.lerp(mClampedStartingY - mBounds.exactCenterY(), mOuterY, mTweenY_exit);
    }
    
    private float getCurrentExitRadius() {
        return MathUtils.lerp(0, mTargetRadius, mTweenRadius_exit);
    }

    @Override
    protected Animator createSoftwareExit() {
        final int radiusDuration;
        final int originDuration;
        radiusDuration = BOUNDED_RADIUS_EXIT_DURATION;
        originDuration = BOUNDED_ORIGIN_EXIT_DURATION;

        final ObjectAnimator tweenRadiusExit = ObjectAnimator.ofFloat(this, TWEEN_RADIUS_EXIT, 1);
        tweenRadiusExit.setAutoCancel(true);
        tweenRadiusExit.setDuration(radiusDuration);
        tweenRadiusExit.setInterpolator(DECELERATE_INTERPOLATOR);

        final ObjectAnimator tweenOriginExit = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN_EXIT, 1);
        tweenOriginExit.setAutoCancel(true);
        tweenOriginExit.setDuration(originDuration);
        tweenOriginExit.setInterpolator(DECELERATE_INTERPOLATOR);

        final AnimatorSet set = new AnimatorSet();
        set.play(tweenOriginExit).with(tweenRadiusExit);
        set.addListener(mAnimationListener);

        return set;
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
    private static final FloatProperty<TwsRippleForegroundClear> TWEEN_RADIUS_ENTER = new FloatProperty<TwsRippleForegroundClear>(
            "tweenRadiusEnter") {
        @Override
        public void setValue(TwsRippleForegroundClear object, float value) {
            object.mTweenRadius_enter = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForegroundClear object) {
            return object.mTweenRadius_enter;
        }
    };

    /**
     * Property for animating origin between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForegroundClear> TWEEN_ORIGIN_ENTER = new FloatProperty<TwsRippleForegroundClear>(
            "tweenOriginEnter") {
        @Override
        public void setValue(TwsRippleForegroundClear object, float value) {
            object.mTweenX_enter = value;
            object.mTweenY_enter = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForegroundClear object) {
            return object.mTweenX_enter;
        }
    };

    /**
     * Property for animating radius between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForegroundClear> TWEEN_RADIUS_EXIT = new FloatProperty<TwsRippleForegroundClear>(
            "tweenRadiusExit") {
        @Override
        public void setValue(TwsRippleForegroundClear object, float value) {
            object.mTweenRadius_exit = value;
            object.invalidateSelf();
        }
        
        @Override
        public Float get(TwsRippleForegroundClear object) {
            return object.mTweenRadius_exit;
        }
    };
    
    /**
     * Property for animating origin between its initial and target values.
     */
    private static final FloatProperty<TwsRippleForegroundClear> TWEEN_ORIGIN_EXIT = new FloatProperty<TwsRippleForegroundClear>(
            "tweenOriginExit") {
        @Override
        public void setValue(TwsRippleForegroundClear object, float value) {
            object.mTweenX_exit = value;
            object.mTweenY_exit = value;
            object.invalidateSelf();
        }
        
        @Override
        public Float get(TwsRippleForegroundClear object) {
            return object.mTweenX_exit;
        }
    };

    private static final FloatProperty<TwsRippleForegroundClear> OPACITY = new FloatProperty<TwsRippleForegroundClear>("opacity") {
        @Override
        public void setValue(TwsRippleForegroundClear object, float value) {
            object.mOpacity = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(TwsRippleForegroundClear object) {
            return object.mOpacity;
        }
    };

    @Override
    protected int getRippleStyle() {
        return TwsRippleDrawable.RIPPLE_STYLE_CLEAR;
    }
}
