/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.tencent.tws.assistant.drawable.TwsAnimatedStateListDrawable;
import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.support.v4.widget.TintableCompoundButton;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * A {@link CheckBox} which supports compatible features on older version of the platform, including:
 * <ul>
 * <li>Allows dynamic tint of it background via the background tint methods in
 * {@link android.support.v4.widget.CompoundButtonCompat}.</li>
 * <li>Allows setting of the background tint using {@link R.attr#buttonTint} and {@link R.attr#buttonTintMode}
 * .</li>
 * </ul>
 * 
 * <p>
 * This will automatically be used when you use {@link CheckBox} in your layouts. You should only need to
 * manually use this class when writing custom views.
 * </p>
 */
public class CheckBox extends android.widget.CheckBox implements TintableCompoundButton {

    private TintManager mTintManager;
    private CompoundButtonHelper mCompoundButtonHelper;
    private boolean mIsAnimationButton = true;

    final int[] checkOnDisableState = {android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int checkOnDisableResId = R.drawable.btn_check_on_disabled_holo_light;
    final int[] checkOffDisableState = {-android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int checkOffDisableResId = R.drawable.btn_check_off_disabled_holo_light;
    final int[] checkOnState = {android.R.attr.state_checked, android.R.attr.state_enabled};
    final int checkOnResId = R.drawable.btn_check_to_on_mtrl_007;
    final int[] checkOffState = {-android.R.attr.state_checked, android.R.attr.state_enabled};
    final int checkOffResId = R.drawable.btn_check_to_on_mtrl_000;
    final int checkOnkeyframeId = R.id.on;
    final int checkOffKeyframeId = R.id.off;
    final int checkToOnResId = R.drawable.btn_check_to_on_material_anim;
    final int checkToOffResId = R.drawable.btn_check_to_off_material_anim;
    private Drawable mAnimationButtonDrawable = null;
    private boolean mIsSupportTintDrawable = false;
    private int mColorControlActivated = 0;

    public CheckBox(Context context) {
        this(context, null);
    }

    public CheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkboxStyle);
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (mIsAnimationButton) {
            setAnimationButtonDrawable(mIsAnimationButton);
        } else {
            mTintManager = TintManager.get(context);
            mCompoundButtonHelper = new CompoundButtonHelper(this, mTintManager);
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
        setButtonDrawable(mTintManager != null ? mTintManager.getDrawable(resId) : getContext()
                .getResources().getDrawable(resId));
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

    public void setAnimationButtonDrawable(boolean isAnimation) {
        if (isAnimation) {
            int colorControlNormal = ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorControlNormal);
            int colorControlActivated = ThemeUtils.getThemeAttrColor(getContext(),
                    R.attr.colorControlActivated);
            int colorControlDisabled = ThemeUtils.getDisabledThemeAttrColor(getContext(),
                    R.attr.colorControlNormal);
            int colorControlActivateDisabled = ThemeUtils.getDisabledThemeAttrColor(getContext(),
                    R.attr.colorControlActivated);
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
                colorControlActivateDisabled = getContext().getResources().getColor(
                        R.color.control_activate_disabled_color);
            }

            setTintButtonDrawable(colorControlNormal, colorControlDisabled, colorControlActivated,
                    colorControlActivateDisabled);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        setAnimationButtonDrawable(View.VISIBLE == visibility);
    }

    public void setTintButtonDrawable(int colorStateList) {
        ColorStateList stateList = getContext().getResources().getColorStateList(colorStateList);
        if (stateList != null) {
            setTintButtonDrawable(stateList);
        }
    }

    public void setTintButtonDrawable(ColorStateList colorStateList) {
        mIsAnimationButton = true;
        int colorControlNormal = 0;
        int colorControlDisabled = 0;
        int colorControlActivated = 0;
        int colorControlActivateDisabled = 0;
        if (colorStateList.isStateful()) {
            colorControlNormal = colorStateList.getColorForState(checkOffState,
                    colorStateList.getDefaultColor());
            colorControlDisabled = colorStateList.getColorForState(checkOffDisableState,
                    colorStateList.getDefaultColor());
            colorControlActivated = colorStateList.getColorForState(checkOnState,
                    colorStateList.getDefaultColor());
            colorControlActivateDisabled = colorStateList.getColorForState(checkOnDisableState,
                    colorStateList.getDefaultColor());
        }
        setTintButtonDrawable(colorControlNormal, colorControlDisabled, colorControlActivated,
                colorControlActivateDisabled);
    }

    private void setTintButtonDrawable(int colorControlNormal, int colorControlDisabled,
            int colorControlActivated, int colorControlActivateDisabled) {
        final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
        final boolean isSupportTintDrawable = ThemeUtils.isSupportTintDrawable(getContext());
        TwsAnimatedStateListDrawable drawable = new TwsAnimatedStateListDrawable();
        Drawable checkOffDisableDrawable = getResources().getDrawable(checkOffDisableResId);
        Drawable checkOnDisableDrawable = getResources().getDrawable(checkOnDisableResId);
        Drawable checkOffDrawable = getResources().getDrawable(checkOffResId);
        Drawable checkOnDrawable = getResources().getDrawable(checkOnResId);

        if (isSupportTintDrawable) {
            checkOffDisableDrawable.setColorFilter(colorControlDisabled, DEFAULT_MODE);
            checkOnDisableDrawable.setColorFilter(colorControlActivateDisabled, DEFAULT_MODE);
            checkOffDrawable.setColorFilter(colorControlNormal, DEFAULT_MODE);
            checkOnDrawable.setColorFilter(colorControlActivated, DEFAULT_MODE);
        }

        drawable.addState(checkOffDisableState, checkOffDisableDrawable, 0);
        drawable.addState(checkOnDisableState, checkOnDisableDrawable, 0);
        drawable.addState(checkOffState, checkOffDrawable, checkOffKeyframeId);
        drawable.addState(checkOnState, checkOnDrawable, checkOnkeyframeId);
        AnimationDrawable toOnDrawable = (AnimationDrawable) getResources().getDrawable(checkToOnResId);
        AnimationDrawable toOffDrawable = (AnimationDrawable) getResources().getDrawable(checkToOffResId);

        if (isSupportTintDrawable) {
            toOnDrawable.setColorFilter(colorControlActivated, DEFAULT_MODE);
            toOffDrawable.setColorFilter(colorControlNormal, DEFAULT_MODE);
        }

        drawable.addTransition(checkOffKeyframeId, checkOnkeyframeId, toOnDrawable, false);
        drawable.addTransition(checkOnkeyframeId, checkOffKeyframeId, toOffDrawable, false);
        setButtonDrawable(drawable);
        mIsSupportTintDrawable = isSupportTintDrawable;
        mColorControlActivated = colorControlActivated;
        mAnimationButtonDrawable = drawable;
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
