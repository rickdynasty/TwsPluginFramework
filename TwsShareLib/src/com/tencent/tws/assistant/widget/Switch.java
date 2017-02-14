package com.tencent.tws.assistant.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;

import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.support.v4.graphics.drawable.DrawableCompat;
import com.tencent.tws.assistant.utils.FloatProperty;
import com.tencent.tws.assistant.utils.MathUtils;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

public class Switch extends CompoundButton {
    private static final int THUMB_ANIMATION_DURATION = 250;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    private Drawable mThumbDrawable;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private Drawable mTrackDrawable;
    private ColorStateList mTrackTintList = null;
    private PorterDuff.Mode mTrackTintMode = null;
    private boolean mHasTrackTint = false;
    private boolean mHasTrackTintMode = false;

    //alpha effect
    private Drawable mOverlayThumbDrawable;
    private Drawable mOverlayTrackDrawable;
    private int mOverlayAlpha;

    private int mThumbTextPadding;
    private int mSwitchMinWidth;
    private int mSwitchPadding;

    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private int mMinFlingVelocity;

    private float mThumbPosition;

    /**
     * Width required to draw the switch track and thumb. Includes padding and optical bounds for both the
     * track and thumb.
     */
    private int mSwitchWidth;

    /**
     * Height required to draw the switch track and thumb. Includes padding and optical bounds for both the
     * track and thumb.
     */
    private int mSwitchHeight;

    /**
     * Width of the thumb's content region. Does not include padding or optical bounds.
     */
    private int mThumbWidth;

    /** Left bound for drawing the switch track and thumb. */
    private int mSwitchLeft;

    /** Top bound for drawing the switch track and thumb. */
    private int mSwitchTop;

    /** Right bound for drawing the switch track and thumb. */
    private int mSwitchRight;

    /** Bottom bound for drawing the switch track and thumb. */
    private int mSwitchBottom;

    private ObjectAnimator mPositionAnimator;

    @SuppressWarnings("hiding")
    private final Rect mTempRect = new Rect();

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

    private TintManager mTintManager;

    private boolean mIsSupportTintDrawable;
    private int mNormalColor = 0;
    private int mCheckedColor = 0;
    private int mDisabledColor = 0;
    private int mCheckedDisableColor = 0;
    private int mCurrentColor = 0;
    
    /**
     * Construct a new Switch with default styling.
     * 
     * @param context
     *            The Context that will determine this widget's theming.
     */
    public Switch(Context context) {
        this(context, null);
    }

    /**
     * Construct a new Switch with default styling, overriding specific style attributes as requested.
     * 
     * @param context
     *            The Context that will determine this widget's theming.
     * @param attrs
     *            Specification of attributes that should deviate from default styling.
     */
    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.switchStyle);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute, overriding
     * specific style attributes as requested.
     * 
     * @param context
     *            The Context that will determine this widget's theming.
     * @param attrs
     *            Specification of attributes that should deviate from the default styling.
     * @param defStyleAttr
     *            An attribute in the current theme that contains a reference to a style resource that
     *            supplies default values for the view. Can be 0 to not look for defaults.
     */
    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        mIsSupportTintDrawable = ThemeUtils.isSupportTintDrawable(context);
        mIsSupportTintDrawable = true;

//        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.Switch,
//                defStyleAttr, 0);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Switch,
                defStyleAttr, 0);
        if (mIsSupportTintDrawable) {
            mThumbDrawable = getResources().getDrawable(R.drawable.switch_frame_levellist);
            if (mThumbDrawable != null) {
                mThumbDrawable.setCallback(this);
            }
        } else {
            mThumbDrawable = a.getDrawable(R.styleable.Switch_thumb);
            if (mThumbDrawable != null) {
                mThumbDrawable.setCallback(this);
            }
        }

        if (!mIsSupportTintDrawable) {
//            mOverlayThumbDrawable = a.getDrawable(R.styleable.Switch_thumbOverLayer);
            if (mOverlayThumbDrawable != null) {
                mOverlayThumbDrawable.setCallback(this);
            }
        }

        mOverlayAlpha = 0;

        mNormalColor = ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorControlNormal);
        mCheckedColor = ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorControlActivated);
        mDisabledColor = ThemeUtils.getDisabledThemeAttrColor(getContext(), R.attr.colorControlNormal);
        mCheckedDisableColor = ThemeUtils.getDisabledThemeAttrColor(getContext(), R.attr.colorControlActivated);

        if (mNormalColor == 0) {
            mNormalColor = getContext().getResources().getColor(R.color.control_normal_color);
        }
        if (mCheckedColor == 0) {
            mCheckedColor = getContext().getResources().getColor(R.color.control_activated_color);
        }
        if (mDisabledColor == 0) {
            mDisabledColor = getContext().getResources().getColor(R.color.control_disabled_color);
        }
        if (mCheckedDisableColor == 0) {
            mCheckedDisableColor = getContext().getResources().getColor(R.color.control_activate_disabled_color);
        }


//        mTrackDrawable = a.getDrawable(R.styleable.Switch_track);
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(this);
        }
        if (!mIsSupportTintDrawable) {
            mOverlayTrackDrawable = a.getDrawable(R.styleable.Switch_trackOverLayer);
            if (mOverlayTrackDrawable != null) {
                mOverlayTrackDrawable.setCallback(this);
            }
        }

        updateCurrentColor(isChecked(), isEnabled());
        setRippleBackground(mIsSupportTintDrawable, mTrackDrawable, mCheckedColor);

        mSwitchMinWidth = a.getDimensionPixelSize(R.styleable.Switch_switchMinWidth, 0);
        mSwitchPadding = a.getDimensionPixelSize(R.styleable.Switch_switchPadding, 0);

        ColorStateList thumbTintList = a.getColorStateList(R.styleable.Switch_thumbTint);
        if (thumbTintList != null) {
            mThumbTintList = thumbTintList;
            mHasThumbTint = true;
        }
        PorterDuff.Mode thumbTintMode = TintManager.parseTintMode(
                a.getInt(R.styleable.Switch_thumbTintMode, -1), null);
        if (mThumbTintMode != thumbTintMode) {
            mThumbTintMode = thumbTintMode;
            mHasThumbTintMode = true;
        }
        if (mHasThumbTint || mHasThumbTintMode) {
            applyThumbTint();
        }

        ColorStateList trackTintList = a.getColorStateList(R.styleable.Switch_trackTint);
        if (trackTintList != null) {
            mTrackTintList = trackTintList;
            mHasTrackTint = true;
        }
        PorterDuff.Mode trackTintMode = TintManager.parseTintMode(
                a.getInt(R.styleable.Switch_trackTintMode, -1), null);
        if (mTrackTintMode != trackTintMode) {
            mTrackTintMode = trackTintMode;
            mHasTrackTintMode = true;
        }
        if (mHasTrackTint || mHasTrackTintMode) {
            applyTrackTint();
        }

//        mTintManager = a.getTintManager();

        a.recycle();

        final ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // Refresh display with current params
        refreshDrawableState();
        setChecked(isChecked());
    }

    /**
     * Set the amount of horizontal padding between the switch and the associated text.
     * 
     * @param pixels
     *            Amount of padding in pixels
     * 
     * @attr ref android.R.styleable#Switch_switchPadding
     */
    public void setSwitchPadding(int pixels) {
        mSwitchPadding = pixels;
        requestLayout();
    }

    /**
     * Get the amount of horizontal padding between the switch and the associated text.
     * 
     * @return Amount of padding in pixels
     * 
     * @attr ref android.R.styleable#Switch_switchPadding
     */
    public int getSwitchPadding() {
        return mSwitchPadding;
    }

    /**
     * Set the minimum width of the switch in pixels. The switch's width will be the maximum of this value and
     * its measured width as determined by the switch drawables and text used.
     * 
     * @param pixels
     *            Minimum width of the switch in pixels
     * 
     * @attr ref android.R.styleable#Switch_switchMinWidth
     */
    public void setSwitchMinWidth(int pixels) {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    /**
     * Get the minimum width of the switch in pixels. The switch's width will be the maximum of this value and
     * its measured width as determined by the switch drawables and text used.
     * 
     * @return Minimum width of the switch in pixels
     * 
     * @attr ref android.R.styleable#Switch_switchMinWidth
     */
    public int getSwitchMinWidth() {
        return mSwitchMinWidth;
    }

    /**
     * Set the horizontal padding around the text drawn on the switch itself.
     * 
     * @param pixels
     *            Horizontal padding for switch thumb text in pixels
     * 
     * @attr ref android.R.styleable#Switch_thumbTextPadding
     */
    public void setThumbTextPadding(int pixels) {
        mThumbTextPadding = pixels;
        requestLayout();
    }

    /**
     * Get the horizontal padding around the text drawn on the switch itself.
     * 
     * @return Horizontal padding for switch thumb text in pixels
     * 
     * @attr ref android.R.styleable#Switch_thumbTextPadding
     */
    public int getThumbTextPadding() {
        return mThumbTextPadding;
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     * 
     * @param track
     *            Track drawable
     * 
     * @attr ref android.R.styleable#Switch_track
     */
    public void setTrackDrawable(Drawable track) {
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(null);
        }
        mTrackDrawable = track;
        if (track != null) {
            track.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     * 
     * @param resId
     *            Resource ID of a track drawable
     * 
     * @attr ref android.R.styleable#Switch_track
     */
    public void setTrackResource(int resId) {
        Drawable d = null;
        if (mTintManager != null) {
            d = mTintManager.getDrawable(resId);
        } else {
            d = getContext().getResources().getDrawable(resId);
        }
        setTrackDrawable(d);
    }

    /**
     * Get the drawable used for the track that the switch slides within.
     * 
     * @return Track drawable
     * 
     * @attr ref android.R.styleable#Switch_track
     */
    public Drawable getTrackDrawable() {
        return mTrackDrawable;
    }

    /**
     * Applies a tint to the track drawable. Does not modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setTrackDrawable(Drawable)} will automatically mutate the drawable and
     * apply the specified tint and tint mode using {@link Drawable#setTintList(ColorStateList)}.
     * 
     * @param tint
     *            the tint to apply, may be {@code null} to clear tint
     * 
     * @attr ref android.R.styleable#Switch_trackTint
     * @see #getTrackTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTrackTintList(ColorStateList tint) {
        mTrackTintList = tint;
        mHasTrackTint = true;

        applyTrackTint();
    }

    /**
     * @return the tint applied to the track drawable
     * @attr ref android.R.styleable#Switch_trackTint
     * @see #setTrackTintList(ColorStateList)
     */
    public ColorStateList getTrackTintList() {
        return mTrackTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTrackTintList(ColorStateList)} to the track drawable. The default mode is
     * {@link PorterDuff.Mode#SRC_IN}.
     * 
     * @param tintMode
     *            the blending mode used to apply the tint, may be {@code null} to clear tint
     * @attr ref android.R.styleable#Switch_trackTintMode
     * @see #getTrackTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setTrackTintMode(PorterDuff.Mode tintMode) {
        mTrackTintMode = tintMode;
        mHasTrackTintMode = true;

        applyTrackTint();
    }

    /**
     * @return the blending mode used to apply the tint to the track drawable
     * @attr ref android.R.styleable#Switch_trackTintMode
     * @see #setTrackTintMode(PorterDuff.Mode)
     */
    public PorterDuff.Mode getTrackTintMode() {
        return mTrackTintMode;
    }

    private void applyTrackTint() {
        if (mTrackDrawable != null && (mHasTrackTint || mHasTrackTintMode)) {
            mTrackDrawable = mTrackDrawable.mutate();

//            if (mHasTrackTint) {
//                mTrackDrawable.setTintList(mTrackTintList);
//            }
//
//            if (mHasTrackTintMode) {
//                mTrackDrawable.setTintMode(mTrackTintMode);
//            }

            TintInfo tintInfo = new TintInfo();
            tintInfo.mHasTintList = mHasTrackTint;
            tintInfo.mHasTintMode = mHasTrackTintMode;
            tintInfo.mTintList = mTrackTintList;
            tintInfo.mTintMode = mTrackTintMode;
            TintManager.tintDrawable(mTrackDrawable, tintInfo, getDrawableState());

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTrackDrawable.isStateful()) {
                mTrackDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user can physically touch and drag
     * along the track.
     * 
     * @param thumb
     *            Thumb drawable
     * 
     * @attr ref android.R.styleable#Switch_thumb
     */
    public void setThumbDrawable(Drawable thumb) {
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(null);
        }
        mThumbDrawable = thumb;
        if (thumb != null) {
            thumb.setCallback(this);
        }
        requestLayout();
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user can physically touch and drag
     * along the track.
     * 
     * @param resId
     *            Resource ID of a thumb drawable
     * 
     * @attr ref android.R.styleable#Switch_thumb
     */
    public void setThumbResource(int resId) {
        Drawable d = null;
        if (mTintManager != null) {
            d = mTintManager.getDrawable(resId);
        } else {
            d = getContext().getResources().getDrawable(resId);
        }
        setThumbDrawable(d);
    }

    /**
     * Get the drawable used for the switch "thumb" - the piece that the user can physically touch and drag
     * along the track.
     * 
     * @return Thumb drawable
     * 
     * @attr ref android.R.styleable#Switch_thumb
     */
    public Drawable getThumbDrawable() {
        return mThumbDrawable;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setThumbDrawable(Drawable)} will automatically mutate the drawable and
     * apply the specified tint and tint mode using {@link Drawable#setTintList(ColorStateList)}.
     * 
     * @param tint
     *            the tint to apply, may be {@code null} to clear tint
     * 
     * @attr ref android.R.styleable#Switch_thumbTint
     * @see #getThumbTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    /**
     * @return the tint applied to the thumb drawable
     * @attr ref android.R.styleable#Switch_thumbTint
     * @see #setThumbTintList(ColorStateList)
     */
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)} to the thumb drawable. The default mode is
     * {@link PorterDuff.Mode#SRC_IN}.
     * 
     * @param tintMode
     *            the blending mode used to apply the tint, may be {@code null} to clear tint
     * @attr ref android.R.styleable#Switch_thumbTintMode
     * @see #getThumbTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setThumbTintMode(PorterDuff.Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;

        applyThumbTint();
    }

    /**
     * @return the blending mode used to apply the tint to the thumb drawable
     * @attr ref android.R.styleable#Switch_thumbTintMode
     * @see #setThumbTintMode(PorterDuff.Mode)
     */
    public PorterDuff.Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        if (mThumbDrawable != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumbDrawable = mThumbDrawable.mutate();

//            if (mHasThumbTint) {
//                mThumbDrawable.setTintList(mThumbTintList);
//            }
//
//            if (mHasThumbTintMode) {
//                mThumbDrawable.setTintMode(mThumbTintMode);
//            }

            TintInfo tintInfo = new TintInfo();
            tintInfo.mHasTintList = mHasThumbTint;
            tintInfo.mHasTintMode = mHasThumbTintMode;
            tintInfo.mTintList = mThumbTintList;
            tintInfo.mTintMode = mThumbTintMode;
            TintManager.tintDrawable(mThumbDrawable, tintInfo, getDrawableState());
            mThumbDrawable.invalidateSelf();

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumbDrawable.isStateful()) {
                mThumbDrawable.setState(getDrawableState());
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final Rect padding = mTempRect;
        final int thumbWidth;
        final int thumbHeight;
        if (mThumbDrawable != null) {
            // Cached thumb width does not include padding.
            mThumbDrawable.getPadding(padding);
            thumbWidth = mThumbDrawable.getIntrinsicWidth() - padding.left - padding.right;
            thumbHeight = mThumbDrawable.getIntrinsicHeight();
        } else {
            thumbWidth = 0;
            thumbHeight = 0;
        }

        mThumbWidth = Math.max(0, thumbWidth);

        final int trackHeight;
        final int trackWidth;
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);
            trackHeight = mTrackDrawable.getIntrinsicHeight();
            trackWidth = mTrackDrawable.getIntrinsicWidth();
        } else {
            padding.setEmpty();
            trackHeight = 0;
            trackWidth = 0;
        }

        // Adjust left and right padding to ensure there's enough room for the
        // thumb's padding (when present).
        int paddingLeft = padding.left;
        int paddingRight = padding.right;
        if (mThumbDrawable != null) {
            final Rect inset = DrawableUtils.getOpticalBounds(mThumbDrawable);
            paddingLeft = Math.max(paddingLeft, inset.left);
            paddingRight = Math.max(paddingRight, inset.right);
        }

        int switchWidth = Math.max(mSwitchMinWidth,
                2 * mThumbWidth + paddingLeft + paddingRight);
        if (mIsSupportTintDrawable) {
            switchWidth = Math.max(mThumbWidth, trackWidth);
        }
        final int switchHeight = Math.max(trackHeight, thumbHeight);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int measuredHeight = getMeasuredHeight();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(getMeasuredWidthAndState(), switchHeight);
        }
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        if (mThumbDrawable == null) {
            return false;
        }

        // Relies on mTempRect, MUST be called first!
        final int thumbOffset = getThumbOffset();

        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        int thumbLeft = mSwitchLeft + thumbOffset - mTouchSlop;
        if (mIsSupportTintDrawable) {
            thumbLeft = mSwitchLeft - mTouchSlop;
        }
        int thumbRight = thumbLeft + mThumbWidth + mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE:
                        // Didn't target the thumb, treat normally.
                        break;

                    case TOUCH_MODE_DOWN: {
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop || Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        final float x = ev.getX();
                        final int thumbScrollRange = getThumbScrollRange();
                        final float thumbScrollOffset = x - mTouchX;
                        float dPos;
                        if (thumbScrollRange != 0) {
                            dPos = thumbScrollOffset / thumbScrollRange;
                        } else {
                            // If the thumb scroll range is empty, just use the
                            // movement direction to snap on or off.
                            dPos = thumbScrollOffset > 0 ? 1 : -1;
                        }
                        if (isLayoutRtl()) {
                            dPos = -dPos;
                        }
                        final float newPos = MathUtils.constrain(mThumbPosition + dPos, 0, 1);
                        if (newPos != mThumbPosition) {
                            mTouchX = x;
                            setThumbPosition(newPos);
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    // Allow super class to handle pressed state, etc.
                    super.onTouchEvent(ev);
                    return true;
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     * 
     * @param ev
     *            Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;

        // Commit the change if the event is up and not canceled and the switch
        // has not been disabled during the drag.
        final boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();
        final boolean oldState = isChecked();
        final boolean newState;
        if (commitChange) {
            mVelocityTracker.computeCurrentVelocity(1000);
            final float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                newState = isLayoutRtl() ? (xvel < 0) : (xvel > 0);
            } else {
                newState = getTargetCheckedState();
            }
        } else {
            newState = oldState;
        }

        if (newState != oldState) {
            playSoundEffect(SoundEffectConstants.CLICK);
            setChecked(newState);
        }

        cancelSuperTouch(ev);
    }

    private void animateThumbToCheckedState(final boolean newCheckedState) {
        final float targetPosition = newCheckedState ? 1 : 0;
        mPositionAnimator = ObjectAnimator.ofFloat(this, THUMB_POS, targetPosition);
        mPositionAnimator.setDuration(THUMB_ANIMATION_DURATION);
        if (android.os.Build.VERSION.SDK_INT > 17) {
            mPositionAnimator.setAutoCancel(true);
        }
        if (mIsSupportTintDrawable) {
            final AnimatorListenerAdapter animatorListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    startCheckedRipple(newCheckedState);
                }
            };
            mPositionAnimator.addListener(animatorListener);
        }
        mPositionAnimator.start();
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
        }
    }

    private boolean getTargetCheckedState() {
        return mThumbPosition > 0.5f;
    }

    /**
     * Sets the thumb position as a decimal value between 0 (off) and 1 (on).
     * 
     * @param position
     *            new position between [0,1]
     */
    private void setThumbPosition(float position) {
        mThumbPosition = position;
        if (mIsSupportTintDrawable) {
            ArgbEvaluator evaluator = new ArgbEvaluator();
            if (isEnabled()) {
                mCurrentColor = (Integer) (evaluator.evaluate(position, mNormalColor, mCheckedColor));
            } else {
                mCurrentColor = (Integer) (evaluator.evaluate(position, mDisabledColor, mCheckedDisableColor));
            }
            int index = (int) (mThumbPosition * 10);
            if (mThumbDrawable != null) {
                mThumbDrawable.setLevel(index);
            }
            if (mThumbDrawable != null) {
//                mThumbDrawable.setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_IN);
            }
            if (mTrackDrawable != null) {
//                mTrackDrawable.setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_IN);
            }
        }

        invalidate();
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        // Calling the super method may result in setChecked() getting called
        // recursively with a different value, so load the REAL value...
        checked = isChecked();

//        if (getWindowToken() != null && isLaidOut(this) && isShown()) {
        if (android.os.Build.VERSION.SDK_INT > 18 && isAttachedToWindow() && isLaidOut()) {
            animateThumbToCheckedState(checked);
        } else {
            // Immediately move the thumb to the new position.
            cancelPositionAnimator();
            setThumbPosition(checked ? 1 : 0);
            startCheckedRipple(checked);
        }
    }

    @Deprecated
    public void setCheckedAnima(boolean checked) {
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int opticalInsetLeft = 0;
        int opticalInsetRight = 0;
        if (mThumbDrawable != null) {
            final Rect trackPadding = mTempRect;
            if (mTrackDrawable != null) {
                mTrackDrawable.getPadding(trackPadding);
            } else {
                trackPadding.setEmpty();
            }

            final Rect insets = DrawableUtils.getOpticalBounds(mThumbDrawable);
            opticalInsetLeft = Math.max(0, insets.left - trackPadding.left);
            opticalInsetRight = Math.max(0, insets.right - trackPadding.right);
        }

        final int switchRight;
        final int switchLeft;
        if (isLayoutRtl()) {
            switchLeft = getPaddingLeft() + opticalInsetLeft;
            switchRight = switchLeft + mSwitchWidth - opticalInsetLeft - opticalInsetRight;
        } else {
            switchRight = getWidth() - getPaddingRight() - opticalInsetRight;
            switchLeft = switchRight - mSwitchWidth + opticalInsetLeft + opticalInsetRight;
        }

        final int switchTop;
        final int switchBottom;
        switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                switchTop = getPaddingTop();
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.CENTER_VERTICAL:
                switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 - mSwitchHeight / 2;
                switchBottom = switchTop + mSwitchHeight;
                break;

            case Gravity.BOTTOM:
                switchBottom = getHeight() - getPaddingBottom();
                switchTop = switchBottom - mSwitchHeight;
                break;
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
    }

    @Override
    public void draw(Canvas c) {
        final Rect padding = mTempRect;
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        int thumbInitialLeft = switchLeft + getThumbOffset();

        final Rect thumbInsets;
        if (mThumbDrawable != null) {
            thumbInsets = DrawableUtils.getOpticalBounds(mThumbDrawable);
        } else {
            thumbInsets = DrawableUtils.INSETS_NONE;
        }

        // Layout the track.
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);

            // Adjust thumb position for track padding.
            thumbInitialLeft += padding.left;

            // If necessary, offset by the optical insets of the thumb asset.
            int trackLeft = switchLeft;
            int trackTop = switchTop;
            int trackRight = switchRight;
            //TODO
            if (mIsSupportTintDrawable) {
                trackRight = mTrackDrawable.getIntrinsicWidth();
            }
            int trackBottom = switchBottom;
            if (thumbInsets != null) {
                if (thumbInsets.left > padding.left) {
                    trackLeft += thumbInsets.left - padding.left;
                }
                if (thumbInsets.top > padding.top) {
                    trackTop += thumbInsets.top - padding.top;
                }
                if (thumbInsets.right > padding.right) {
                    trackRight -= thumbInsets.right - padding.right;
                }
                if (thumbInsets.bottom > padding.bottom) {
                    trackBottom -= thumbInsets.bottom - padding.bottom;
                }
            }
            mTrackDrawable.setBounds(trackLeft, trackTop, trackRight, trackBottom);
        }

        // Layout the track overlayer.
        if (mOverlayTrackDrawable != null) {
            mOverlayTrackDrawable.getPadding(padding);

            // Adjust thumb position for track padding.
            thumbInitialLeft += padding.left;

            // If necessary, offset by the optical insets of the thumb asset.
            int trackLeft = switchLeft;
            int trackTop = switchTop;
            int trackRight = (int) (switchRight * mThumbPosition);
            int trackBottom = switchBottom;
            if (thumbInsets != null) {
                if (thumbInsets.left > padding.left) {
                    trackLeft += thumbInsets.left - padding.left;
                }
                if (thumbInsets.top > padding.top) {
                    trackTop += thumbInsets.top - padding.top;
                }
                if (thumbInsets.right > padding.right) {
                    trackRight -= thumbInsets.right - padding.right;
                }
                if (thumbInsets.bottom > padding.bottom) {
                    trackBottom -= thumbInsets.bottom - padding.bottom;
                }
            }
            mOverlayTrackDrawable.setBounds(trackLeft, trackTop, trackRight, trackBottom);
        }

        // Layout the thumb.
        if (mThumbDrawable != null) {
            mThumbDrawable.getPadding(padding);

            int thumbLeft = thumbInitialLeft - padding.left;
            int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
            if (mIsSupportTintDrawable) {
                thumbLeft = switchLeft - padding.left;
                thumbRight = mThumbWidth + padding.right;
            }
            mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);

            final Drawable background = getBackground();
            if (background != null) {
                DrawableCompat.setHotspotBounds(background, thumbLeft, switchTop,
                        thumbRight, switchBottom);
            }
        }

        // Layout the thumb overlayer.
        if (mOverlayThumbDrawable != null) {
            mOverlayThumbDrawable.getPadding(padding);
            
            final int thumbLeft = thumbInitialLeft - padding.left;
            final int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
            mOverlayThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);
        }

        // Draw the background.
        super.draw(c);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mOverlayAlpha = calculateAlpha();
        final Drawable overlayThumbDrawable = mOverlayThumbDrawable;
        final Drawable overlayTrackDrawable = mOverlayTrackDrawable;
        
        final Rect padding = mTempRect;
        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null) {
            trackDrawable.getPadding(padding);
        } else {
            padding.setEmpty();
        }

        final int switchTop = mSwitchTop;
        final int switchBottom = mSwitchBottom;
        final int switchInnerTop = switchTop + padding.top;
        final int switchInnerBottom = switchBottom - padding.bottom;

        final Drawable thumbDrawable = mThumbDrawable;
        if (trackDrawable != null) {
            trackDrawable.draw(canvas);
        }

        if (overlayTrackDrawable != null) {
            overlayTrackDrawable.draw(canvas);
        }

        final int saveCount = canvas.save();

        if (thumbDrawable != null) {
            thumbDrawable.draw(canvas);
        }

        if (overlayThumbDrawable != null) {
            overlayThumbDrawable.setAlpha(mOverlayAlpha);
            overlayThumbDrawable.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getCompoundPaddingLeft() {
        if (!isLayoutRtl()) {
            return super.getCompoundPaddingLeft();
        }
        int padding = super.getCompoundPaddingLeft() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    @Override
    public int getCompoundPaddingRight() {
        if (isLayoutRtl()) {
            return super.getCompoundPaddingRight();
        }
        int padding = super.getCompoundPaddingRight() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    /**
     * Translates thumb position to offset according to current RTL setting and thumb scroll range. Accounts
     * for both track and thumb padding.
     * 
     * @return thumb offset
     */
    private int getThumbOffset() {
        final float thumbPosition;
        if (isLayoutRtl()) {
            thumbPosition = 1 - mThumbPosition;
        } else {
            thumbPosition = mThumbPosition;
        }
        return (int) (thumbPosition * getThumbScrollRange() + 0.5f);
    }

    private int getThumbScrollRange() {
        if (mTrackDrawable != null) {
            final Rect padding = mTempRect;
            mTrackDrawable.getPadding(padding);

            final Rect insets;
            if (mThumbDrawable != null) {
                insets = DrawableUtils.getOpticalBounds(mThumbDrawable);
            } else {
                insets = DrawableUtils.INSETS_NONE;
            }

            if (mIsSupportTintDrawable) {
                return mSwitchWidth - padding.left - padding.right
                        - insets.left - insets.right;
            }
            return mSwitchWidth - mThumbWidth - padding.left - padding.right
                    - insets.left - insets.right;
        } else {
            return 0;
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

        final int[] myDrawableState = getDrawableState();

        if (mThumbDrawable != null) {
            mThumbDrawable.setState(myDrawableState);
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.setState(myDrawableState);
        }

        if (mOverlayThumbDrawable != null) {
            mOverlayThumbDrawable.setState(myDrawableState);
        }

        if (mOverlayTrackDrawable != null) {
            mOverlayTrackDrawable.setState(myDrawableState);
        }

        updateCurrentColor(isChecked(), isEnabled());

        invalidate();
    }

    /*public void drawableHotspotChanged(float x, float y) {
        if (Build.VERSION.SDK_INT >= 21) {
            super.drawableHotspotChanged(x, y);
        }

        if (mThumbDrawable != null) {
            DrawableCompat.setHotspot(mThumbDrawable, x, y);
        }

        if (mTrackDrawable != null) {
            DrawableCompat.setHotspot(mTrackDrawable, x, y);
        }
    }*/

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable
                || who == mOverlayThumbDrawable || who == mOverlayTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumbDrawable != null) {
            mThumbDrawable.jumpToCurrentState();
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.jumpToCurrentState();
        }

        if (mOverlayThumbDrawable != null) {
            mOverlayThumbDrawable.jumpToCurrentState();
        }
        
        if (mOverlayTrackDrawable != null) {
            mOverlayTrackDrawable.jumpToCurrentState();
        }

        if (mPositionAnimator != null && mPositionAnimator.isRunning()) {
            mPositionAnimator.end();
            mPositionAnimator = null;
        }
    }

    private static final FloatProperty<Switch> THUMB_POS = new FloatProperty<Switch>("thumbPos") {
        @Override
        public Float get(Switch object) {
            return object.mThumbPosition;
        }

        @Override
        public void setValue(Switch object, float value) {
            object.setThumbPosition(value);
        }
    };

    private boolean isLaidOut(View view){
        return view.getWidth() > 0 && view.getHeight() > 0;
    }

    private int calculateAlpha() {
        int alpha = (int) (mThumbPosition * 255);
        if (alpha <= 0) {
            alpha = 0;
        } else if (alpha >= 255) {
            alpha = 255;
        }
        return alpha;
    }

    private void updateCurrentColor(boolean checked, boolean enabled) {
        if (mIsSupportTintDrawable) {
            if (checked) {
                if (enabled) {
                    mCurrentColor = mCheckedColor;
                } else {
                    mCurrentColor = mCheckedDisableColor;
                }
            } else {
                if (enabled) {
                    mCurrentColor = mNormalColor;
                } else {
                    mCurrentColor = mDisabledColor;
                }
            }
            if (mThumbDrawable != null) {
//                mThumbDrawable.setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_IN);
            }
            if (mTrackDrawable != null) {
//                mTrackDrawable.setColorFilter(mCurrentColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private void setRippleBackground(boolean isSupportTintDrawable, Drawable orgDrawable, int rippleColor) {
        if (orgDrawable != null && isSupportTintDrawable) {
            ColorStateList csl = createNormalStateList(rippleColor);
            final float scale = 0.3f;
            int width = orgDrawable.getIntrinsicWidth();
            int height = orgDrawable.getIntrinsicHeight();
            int left = (int) (width - (height * (1 + scale)));
            int top = (int) (-height * scale);
            int right = (int) (width + height * scale);
            int bottom = height - top;

            TwsRippleDrawable rippleDrawable = new TwsRippleDrawable(csl, null, null,
                    TwsRippleDrawable.RIPPLE_STYLE_RING);
            rippleDrawable.twsSetHotspotBounds(left, top, right, bottom);
            rippleDrawable.setSupportCheckedRipple(false);
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

    private void startCheckedRipple(boolean newCheckedState){
        final Drawable drawable = getBackground();
        if (drawable instanceof TwsRippleDrawable) {
            ((TwsRippleDrawable) drawable).startCheckedRipple(newCheckedState);
        }
    }
}
