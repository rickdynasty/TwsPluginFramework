/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.tencent.tws.assistant.drawable.TwsAnimatedStateListDrawable;
import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.support.annotation.DrawableRes;
import com.tencent.tws.assistant.support.annotation.Nullable;
import com.tencent.tws.assistant.support.v4.widget.TintableCompoundButton;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * A {@link RadioButton} which supports compatible features on older version of the platform, including:
 * <ul>
 * <li>Allows dynamic tint of it background via the background tint methods in
 * {@link android.support.v4.widget.CompoundButtonCompat}.</li>
 * <li>Allows setting of the background tint using {@link R.attr#buttonTint} and {@link R.attr#buttonTintMode}
 * .</li>
 * </ul>
 * 
 * <p>
 * This will automatically be used when you use {@link RadioButton} in your layouts. You should only need to
 * manually use this class when writing custom views.
 * </p>
 */
public class RadioButton extends android.widget.RadioButton implements TintableCompoundButton {

    private TintManager mTintManager;
    private CompoundButtonHelper mCompoundButtonHelper;
    private boolean mIsAnimationButton = true;

    final int[] radioOnDisableState = {android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int radioOnDisableResId = R.drawable.btn_radio_on_disabled_holo_light;
    final int[] radioOffDisableState = {-android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int radioOffDisableResId = R.drawable.btn_radio_off_disabled_holo_light;
    final int[] radioOnState = {android.R.attr.state_checked, android.R.attr.state_enabled};
    final int radioOnResId = R.drawable.btn_radio_to_on_mtrl_015;
    final int[] radioOffState = {-android.R.attr.state_checked, android.R.attr.state_enabled};
    final int radioOffResId = R.drawable.btn_radio_to_on_mtrl_000;
    final int radioOnkeyframeId = R.id.on;
    final int radioOffKeyframeId = R.id.off;
    final int radioToOnResId = R.drawable.btn_radio_to_on_material_anim;
    final int radioToOffResId = R.drawable.btn_radio_to_off_material_anim;
    private Drawable mAnimationButtonDrawable = null;
    private boolean mIsSupportTintDrawable = false;
    private int mColorControlActivated = 0;

    public RadioButton(Context context) {
        this(context, null);
    }

    public RadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.radioButtonStyle);
    }

    public RadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTintManager = TintManager.get(context);
        mCompoundButtonHelper = new CompoundButtonHelper(this, mTintManager);
        if (mIsAnimationButton) {
            setAnimationButtonDrawable(mIsAnimationButton);
        } else {
            mCompoundButtonHelper.loadFromAttributes(attrs, defStyleAttr);
        }
    }

    @Override
    public void setButtonDrawable(Drawable buttonDrawable) {
        super.setButtonDrawable(buttonDrawable);
        if (mCompoundButtonHelper != null && !mIsAnimationButton) {
            mCompoundButtonHelper.onSetButtonDrawable();
        }
    }

    @Override
    public void setButtonDrawable(@DrawableRes int resId) {
        setButtonDrawable((mTintManager != null && !mIsAnimationButton) ? mTintManager.getDrawable(resId)
                : getContext().getResources().getDrawable(resId));
    }

    @Override
    public int getCompoundPaddingLeft() {
        final int value = super.getCompoundPaddingLeft();
        return mCompoundButtonHelper != null ? mCompoundButtonHelper.getCompoundPaddingLeft(value) : value;
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * 
     * @hide
     */
    @Override
    public void setSupportButtonTintList(@Nullable ColorStateList tint) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintList(tint);
        }
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * 
     * @hide
     */
    @Nullable
    @Override
    public ColorStateList getSupportButtonTintList() {
        return mCompoundButtonHelper != null ? mCompoundButtonHelper.getSupportButtonTintList() : null;
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * 
     * @hide
     */
    @Override
    public void setSupportButtonTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintMode(tintMode);
        }
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * 
     * @hide
     */
    @Nullable
    @Override
    public PorterDuff.Mode getSupportButtonTintMode() {
        return mCompoundButtonHelper != null ? mCompoundButtonHelper.getSupportButtonTintMode() : null;
    }

    private void setAnimationButtonDrawable(boolean isAnimation) {
        mIsAnimationButton = isAnimation;
        if (isAnimation) {
            final boolean isSupportTintDrawable = ThemeUtils.isSupportTintDrawable(getContext());
            int colorControlNormal = ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorControlNormal);
            int colorControlActivated = ThemeUtils.getThemeAttrColor(getContext(),
                    R.attr.colorControlActivated);
            int colorControlDisabled = ThemeUtils.getDisabledThemeAttrColor(getContext(),
                    R.attr.colorControlNormal);
            int colorControlActivateDisabled = ThemeUtils.getDisabledThemeAttrColor(getContext(),
                    R.attr.colorControlActivated);
            final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

            if (colorControlNormal == 0) {
                colorControlNormal = getContext().getResources().getColor(R.color.control_normal_color);
            }
            if (colorControlActivated == 0) {
                colorControlActivated = getContext().getResources().getColor(R.color.control_activated_color);
            }
            if (colorControlDisabled == 0) {
                colorControlDisabled = getContext().getResources().getColor(R.color.control_disabled_color);
            }
            if (colorControlActivateDisabled == 0) {
                colorControlActivateDisabled = getContext().getResources().getColor(R.color.control_activate_disabled_color);
            }

            TwsAnimatedStateListDrawable drawable = new TwsAnimatedStateListDrawable();
            Drawable radioOffDisableDrawable = getResources().getDrawable(radioOffDisableResId);
            Drawable radioOnDisableDrawable = getResources().getDrawable(radioOnDisableResId);
            Drawable radioOffDrawable = getResources().getDrawable(radioOffResId);
            Drawable radioOnDrawable = getResources().getDrawable(radioOnResId);

            if (isSupportTintDrawable) {
                radioOffDisableDrawable.setColorFilter(colorControlDisabled, DEFAULT_MODE);
                radioOnDisableDrawable.setColorFilter(colorControlActivateDisabled, DEFAULT_MODE);
                radioOffDrawable.setColorFilter(colorControlNormal, DEFAULT_MODE);
                radioOnDrawable.setColorFilter(colorControlActivated, DEFAULT_MODE);
            }

            drawable.addState(radioOffDisableState, radioOffDisableDrawable, 0);
            drawable.addState(radioOnDisableState, radioOnDisableDrawable, 0);
            drawable.addState(radioOffState, radioOffDrawable, radioOffKeyframeId);
            drawable.addState(radioOnState, radioOnDrawable, radioOnkeyframeId);
            AnimationDrawable toOnDrawable = (AnimationDrawable) getResources().getDrawable(radioToOnResId);
            AnimationDrawable toOffDrawable = (AnimationDrawable) getResources().getDrawable(radioToOffResId);

            if (isSupportTintDrawable) {
                toOnDrawable.setColorFilter(colorControlActivated, DEFAULT_MODE);
                toOffDrawable.setColorFilter(colorControlNormal, DEFAULT_MODE);
            }

            drawable.addTransition(radioOffKeyframeId, radioOnkeyframeId, toOnDrawable, false);
            drawable.addTransition(radioOnkeyframeId, radioOffKeyframeId, toOffDrawable, false);
            setButtonDrawable(drawable);
            mIsSupportTintDrawable = isSupportTintDrawable;
            mColorControlActivated = colorControlActivated;
            mAnimationButtonDrawable = drawable;
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        setAnimationButtonDrawable(View.VISIBLE == visibility);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setRippleBackground(mIsSupportTintDrawable, mAnimationButtonDrawable, mColorControlActivated);
    }

    private void setRippleBackground(boolean isSupportTintDrawable, Drawable orgDrawable, int rippleColor) {
        if (orgDrawable != null && isSupportTintDrawable) {
            ColorStateList csl = createNormalStateList(rippleColor);
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int drawableHeight = orgDrawable.getIntrinsicHeight();
            final int drawableWidth = orgDrawable.getIntrinsicWidth();
            final float scale = 0.3f;
            final float drawableScaleHeight = drawableHeight * scale;
            final float drawableScaleWidth = drawableWidth * scale;

            final int top;
            switch (verticalGravity) {
                case Gravity.BOTTOM:
                    top = (int) (getHeight() - drawableHeight - drawableScaleHeight);
                    break;
                case Gravity.CENTER_VERTICAL:
                    top = (int) (((getHeight() - drawableHeight) / 2) - drawableScaleHeight);
                    break;
                default:
                    top = (int) (-drawableScaleHeight);
            }
            final int bottom = (int) (top + drawableHeight + drawableScaleHeight * 2);
            final int left = (int) (isLayoutRtl() ? getWidth() - drawableWidth - drawableScaleWidth : -drawableScaleWidth);
            final int right = (int) (isLayoutRtl() ? getWidth() + drawableScaleWidth : drawableWidth + drawableScaleWidth);

            TwsRippleDrawable rippleDrawable = new TwsRippleDrawable(csl, null, null,
                    TwsRippleDrawable.RIPPLE_STYLE_RING);
            rippleDrawable.twsSetHotspotBounds(left, top, right, bottom);
            setBackgroundDrawable(rippleDrawable);
        }
    }

    private ColorStateList createNormalStateList(int textColor) {
        // Now create a new ColorStateList with the default color
        final int[][] states = new int[1][];
        final int[] colors = new int[1];
        // Default state
        states[0] = EMPTY_STATE_SET;
        colors[0] = textColor;
        return new ColorStateList(states, colors);
    }
}
