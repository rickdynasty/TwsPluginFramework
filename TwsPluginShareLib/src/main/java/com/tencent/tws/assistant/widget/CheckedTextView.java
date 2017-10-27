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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewDebug;
import android.widget.Checkable;
import android.widget.TextView;

import com.tencent.tws.assistant.drawable.TwsAnimatedStateListDrawable;
import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * A {@link CheckedTextView2} which supports compatible features on older version of the platform.
 *
 * <p>This will automatically be used when you use {@link CheckedTextView2} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class CheckedTextView extends TextView implements Checkable {
    private boolean mChecked;

    private int mCheckMarkResource;
    private Drawable mCheckMarkDrawable;
    private ColorStateList mCheckMarkTintList = null;
    private PorterDuff.Mode mCheckMarkTintMode = null;
    private boolean mHasCheckMarkTint = false;
    private boolean mHasCheckMarkTintMode = false;

    private int mBasePadding;
    private int mCheckMarkWidth;
    private int mCheckMarkGravity = Gravity.END;

    private boolean mNeedRequestlayout;

    private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };

    private TintManager mTintManager;
    private TextHelper mTextHelper;

    private boolean mIsAnimationButton = true;
    public static final int STYLE_SYSTEM = 0;
    public static final int STYLE_SINGLE = 1;
    public static final int STYLE_MULTI = 2;
    //multiple
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

    //single
    final int[] radioOnDisableState = {android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int radioOnDisableResId = R.drawable.btn_radio_on_disabled_holo_light;
    final int[] radioOffDisableState = {-android.R.attr.state_checked, -android.R.attr.state_enabled};
    final int radioOffDisableResId = R.drawable.btn_radio_off_disabled_holo_light;
    final int[] radioOnState = {android.R.attr.state_checked, android.R.attr.state_enabled};
    final int radioOnResId = R.drawable.btn_radio_to_on_mtrl_010;
    final int[] radioOffState = {-android.R.attr.state_checked, android.R.attr.state_enabled};
    final int radioOffResId = R.drawable.btn_radio_to_on_mtrl_000;
    final int radioOnkeyframeId = R.id.on;
    final int radioOffKeyframeId = R.id.off;
    final int radioToOnResId = R.drawable.btn_radio_to_on_material_anim;
    final int radioToOffResId = R.drawable.btn_radio_to_off_material_anim;
    private Drawable mAnimationButtonDrawable = null;
    private boolean mIsSupportTintDrawable = false;
    private int mColorControlActivated = 0;

    public CheckedTextView(Context context) {
        this(context, null);
    }

    public CheckedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkedTextViewStyle);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (mIsAnimationButton) {
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.CheckedTextView, defStyleAttr, 0);

            final Drawable d = a.getDrawable(R.styleable.CheckedTextView_android_checkMark);
            if (d != null) {
                setCheckMarkDrawable(d);
            }

            if (a.hasValue(R.styleable.CheckedTextView_checkMarkTintMode)) {
                mCheckMarkTintMode = TintManager.parseTintMode(
                        a.getInt(R.styleable.CheckedTextView_checkMarkTintMode, -1), mCheckMarkTintMode);
                mHasCheckMarkTintMode = true;
            }

            if (a.hasValue(R.styleable.CheckedTextView_checkMarkTint)) {
                mCheckMarkTintList = a.getColorStateList(R.styleable.CheckedTextView_checkMarkTint);
                mHasCheckMarkTint = true;
            }

            mCheckMarkGravity = a.getInt(R.styleable.CheckedTextView_checkMarkGravity, Gravity.END);

            final boolean checked = a.getBoolean(R.styleable.CheckedTextView_android_checked, false);
            setChecked(checked);

            int checkStyle = a.getInt(R.styleable.CheckedTextView_twsCheckStyle, STYLE_SYSTEM);
            twsSetCheckStyle(checkStyle, d);

            a.recycle();
            applyCheckMarkTint();
        } else {
            mTextHelper = TextHelper.create(this);
            mTextHelper.loadFromAttributes(attrs, defStyleAttr);
            mTextHelper.applyCompoundDrawablesTints();
            
            if (TintManager.SHOULD_BE_USED) {
                TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                        R.styleable.CheckedTextView, defStyleAttr, 0);
                Drawable d = a.getDrawable(R.styleable.CheckedTextView_android_checkMark);
                if (d != null) {
                    setCheckMarkDrawable(d);
                }

                boolean checked = a.getBoolean(R.styleable.CheckedTextView_android_checked, false);
                setChecked(checked);
                a.recycle();
                
                mTintManager = a.getTintManager();
            }
        }
    }

    public void toggle() {
        setChecked(!mChecked);
    }

    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the checked state of this view.
     *
     * @param checked {@code true} set the state to checked, {@code false} to
     *                uncheck
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
//            notifyViewAccessibilityStateChangedIfNeeded(
//                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
        }
    }

    /**
     * Sets the check mark to the drawable with the specified resource ID.
     * <p>
     * When this view is checked, the drawable's state set will include
     * {@link android.R.attr#state_checked}.
     *
     * @param resId the resource identifier of drawable to use as the check
     *              mark
     * @attr ref android.R.styleable#CheckedTextView_checkMark
     * @see #setCheckMarkDrawable(Drawable)
     * @see #getCheckMarkDrawable()
     */
    public void setCheckMarkDrawable(@DrawableRes int resId) {
        if (resId != 0 && resId == mCheckMarkResource) {
            return;
        }

        mCheckMarkResource = resId;

        Drawable d = null;
        if (mCheckMarkResource != 0) {
            if (mTintManager != null) {
                d = mTintManager.getDrawable(mCheckMarkResource);
            } else {
                d = getContext().getResources().getDrawable(mCheckMarkResource);
            }
        }
        setCheckMarkDrawable(d);
    }

    /**
     * Set the check mark to the specified drawable.
     * <p>
     * When this view is checked, the drawable's state set will include
     * {@link android.R.attr#state_checked}.
     *
     * @param d the drawable to use for the check mark
     * @attr ref android.R.styleable#CheckedTextView_checkMark
     * @see #setCheckMarkDrawable(int)
     * @see #getCheckMarkDrawable()
     */
    public void setCheckMarkDrawable(Drawable d) {
        if (mCheckMarkDrawable != null) {
            mCheckMarkDrawable.setCallback(null);
            unscheduleDrawable(mCheckMarkDrawable);
        }
        mNeedRequestlayout = (d != mCheckMarkDrawable);
        if (d != null) {
            d.setCallback(this);
            d.setVisible(getVisibility() == VISIBLE, false);
            d.setState(CHECKED_STATE_SET);
            setMinHeight(d.getIntrinsicHeight());

            mCheckMarkWidth = d.getIntrinsicWidth();
            d.setState(getDrawableState());
            applyCheckMarkTint();
        } else {
            mCheckMarkWidth = 0;
        }
        mCheckMarkDrawable = d;

        // Do padding resolution. This will call internalSetPadding() and do a
        // requestLayout() if needed.
        resolvePadding();
    }

    /**
     * Applies a tint to the check mark drawable. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setCheckMarkDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#CheckedTextView_checkMarkTint
     * @see #getCheckMarkTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setCheckMarkTintList(@Nullable ColorStateList tint) {
        mCheckMarkTintList = tint;
        mHasCheckMarkTint = true;

        applyCheckMarkTint();
    }

    /**
     * Returns the tint applied to the check mark drawable, if specified.
     *
     * @return the tint applied to the check mark drawable
     * @attr ref android.R.styleable#CheckedTextView_checkMarkTint
     * @see #setCheckMarkTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getCheckMarkTintList() {
        return mCheckMarkTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setCheckMarkTintList(ColorStateList)} to the check mark
     * drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#CheckedTextView_checkMarkTintMode
     * @see #setCheckMarkTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setCheckMarkTintMode(@Nullable PorterDuff.Mode tintMode) {
        mCheckMarkTintMode = tintMode;
        mHasCheckMarkTintMode = true;

        applyCheckMarkTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the check mark
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the check mark
     *         drawable
     * @attr ref android.R.styleable#CheckedTextView_checkMarkTintMode
     * @see #setCheckMarkTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getCheckMarkTintMode() {
        return mCheckMarkTintMode;
    }

    private void applyCheckMarkTint() {
        if (mCheckMarkDrawable != null && (mHasCheckMarkTint || mHasCheckMarkTintMode)) {
            mCheckMarkDrawable = mCheckMarkDrawable.mutate();

//            if (mHasCheckMarkTint) {
//                mCheckMarkDrawable.setTintList(mCheckMarkTintList);
//            }
//
//            if (mHasCheckMarkTintMode) {
//                mCheckMarkDrawable.setTintMode(mCheckMarkTintMode);
//            }

            TintInfo tintInfo = new TintInfo();
            tintInfo.mHasTintList = mHasCheckMarkTint;
            tintInfo.mHasTintMode = mHasCheckMarkTintMode;
            tintInfo.mTintList = mCheckMarkTintList;
            tintInfo.mTintMode = mCheckMarkTintMode;
            TintManager.tintDrawable(mCheckMarkDrawable, tintInfo, getDrawableState());
            mCheckMarkDrawable.invalidateSelf();

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mCheckMarkDrawable.isStateful()) {
                mCheckMarkDrawable.setState(getDrawableState());
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (mCheckMarkDrawable != null) {
            mCheckMarkDrawable.setVisible(visibility == VISIBLE, false);
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mCheckMarkDrawable != null) {
            mCheckMarkDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mCheckMarkDrawable || super.verifyDrawable(who);
    }

    /**
     * Gets the checkmark drawable
     *
     * @return The drawable use to represent the checkmark, if any.
     *
     * @see #setCheckMarkDrawable(Drawable)
     * @see #setCheckMarkDrawable(int)
     *
     * @attr ref android.R.styleable#CheckedTextView_checkMark
     */
    public Drawable getCheckMarkDrawable() {
        return mCheckMarkDrawable;
    }

    /**
     * @hide
     */
    @Override
    protected void internalSetPadding(int left, int top, int right, int bottom) {
        super.internalSetPadding(left, top, right, bottom);
        setBasePadding(isCheckMarkAtStart());
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updatePadding();
    }

    private void updatePadding() {
        resetPaddingToInitialValues();
        int newPadding = (mCheckMarkDrawable != null) ?
                mCheckMarkWidth + mBasePadding : mBasePadding;
        if (isCheckMarkAtStart()) {
            mNeedRequestlayout |= (mPaddingLeft != newPadding);
            mPaddingLeft = newPadding;
        } else {
            mNeedRequestlayout |= (mPaddingRight != newPadding);
            mPaddingRight = newPadding;
        }
        if (mNeedRequestlayout) {
            requestLayout();
            mNeedRequestlayout = false;
        }
    }

    private void setBasePadding(boolean checkmarkAtStart) {
        if (checkmarkAtStart) {
            mBasePadding = mPaddingLeft;
        } else {
            mBasePadding = mPaddingRight;
        }
    }

    private boolean isCheckMarkAtStart() {
        final int gravity = Gravity.getAbsoluteGravity(mCheckMarkGravity, LAYOUT_DIRECTION_LTR);
        final int hgrav = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        return hgrav == Gravity.LEFT;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final Drawable checkMarkDrawable = mCheckMarkDrawable;
        if (checkMarkDrawable != null) {
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int height = checkMarkDrawable.getIntrinsicHeight();
            final int width = checkMarkDrawable.getIntrinsicWidth();

            int y = 0;

            switch (verticalGravity) {
                case Gravity.BOTTOM:
                    y = getHeight() - height;
                    break;
                case Gravity.CENTER_VERTICAL:
                    y = (getHeight() - height) / 2;
                    break;
            }
            
            final boolean checkMarkAtStart = isCheckMarkAtStart();
            final int top = y;
            final int bottom = top + height;
            final int left;
            final int right;
            if (checkMarkAtStart) {
                left = mBasePadding;
                right = left + width;
            } else {
                right = getWidth() - mBasePadding;
                left = right - width;
            }
            checkMarkDrawable.setBounds(mScrollX + left, top, mScrollX + right, bottom);
            checkMarkDrawable.draw(canvas);
        }
    }
    
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        
        if (mCheckMarkDrawable != null) {
            int[] myDrawableState = getDrawableState();
            
            // Set the state of the Drawable
            mCheckMarkDrawable.setState(myDrawableState);
            
            invalidate();
        }
    }

    /*@Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mCheckMarkDrawable != null) {
            mCheckMarkDrawable.setHotspot(x, y);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CheckedTextView.class.getName();
    }

    *//** @hide *//*
    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        super.onInitializeAccessibilityEventInternal(event);
        event.setChecked(mChecked);
    }

    *//** @hide *//*
    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        info.setCheckable(true);
        info.setChecked(mChecked);
    }

    *//** @hide *//*
    @Override
    protected void encodeProperties(@NonNull ViewHierarchyEncoder stream) {
        super.encodeProperties(stream);
        stream.addProperty("text:checked", isChecked());
    }*/

    @Deprecated
    public void twsSetCheckStyle(int checkStyle){
        twsSetCheckStyle(checkStyle, null);
    }

    public void twsSetCheckStyle(int checkStyle, Drawable d){
        if (checkStyle == STYLE_SYSTEM) {
            mIsAnimationButton = false;
            setCheckMarkDrawable(d);
            return;
        }

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

        if (checkStyle == STYLE_SINGLE) {
            mIsAnimationButton = true;
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
            setCheckMarkDrawable(drawable);
            mIsSupportTintDrawable = isSupportTintDrawable;
            mColorControlActivated = colorControlActivated;
            mAnimationButtonDrawable = drawable;
        } else if (checkStyle == STYLE_MULTI) {
            mIsAnimationButton = true;
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
            setCheckMarkDrawable(drawable);
            mIsSupportTintDrawable = isSupportTintDrawable;
            mColorControlActivated = colorControlActivated;
            mAnimationButtonDrawable = drawable;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setRippleBackground(mIsSupportTintDrawable, mAnimationButtonDrawable, mColorControlActivated);
    }

    private void setRippleBackground(boolean isSupportTintDrawable, Drawable orgDrawable, int rippleColor) {
        if (orgDrawable != null && isSupportTintDrawable) {
            ColorStateList csl = createNormalStateList(rippleColor);
            final int drawableHeight = orgDrawable.getIntrinsicHeight();
            final int drawableWidth = orgDrawable.getIntrinsicWidth();
            final float scale = 0.3f;
            final float drawableScaleHeight = drawableHeight * scale;
            final float drawableScaleWidth = drawableWidth * scale;
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;

            int y = 0;

            switch (verticalGravity) {
                case Gravity.BOTTOM:
                    y = (int) (getHeight() - drawableHeight - drawableScaleHeight);
                    break;
                case Gravity.CENTER_VERTICAL:
                    y = (int) (((getHeight() - drawableHeight) / 2) - drawableScaleHeight);
                    break;
                default:
                    y = (int) (-drawableScaleHeight);
            }
            
            final boolean checkMarkAtStart = isCheckMarkAtStart();
            final int width = getWidth();
            final int top = y;
            final int bottom = (int) (top + drawableHeight + drawableScaleHeight * 2);
            final int left;
            final int right;
            if (checkMarkAtStart) {
                left = (int) (mBasePadding - drawableScaleWidth);
                right = (int) (left + mCheckMarkWidth + drawableScaleWidth * 2);
            } else {
                right = (int) (width - mBasePadding + drawableScaleWidth);
                left = (int) (right - mCheckMarkWidth - drawableScaleWidth * 2);
            }

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
