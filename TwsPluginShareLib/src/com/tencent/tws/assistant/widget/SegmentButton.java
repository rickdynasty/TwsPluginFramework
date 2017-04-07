package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Checkable;

import com.tencent.tws.sharelib.R;
import android.widget.LinearLayout;

public class SegmentButton extends Button implements Checkable {
    private boolean mChecked;
    private int mButtonResource;
    private boolean mBroadcasting;
    private Drawable mButtonDrawable;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final int[] HORIZONTAL_FIRST_STATE_SET = {R.attr.state_first_h};
    private static final int[] HORIZONTAL_MIDDLE_STATE_SET = {R.attr.state_middle_h};
    private static final int[] HORIZONTAL_LAST_STATE_SET = {R.attr.state_last_h};
    private static final int[] HORIZONTAL_SINGLE_STATE_SET = {R.attr.state_single_h};
    private static final int[] VERTICAL_FIRST_STATE_SET = {R.attr.state_first_v};
    private static final int[] VERTICAL_MIDDLE_STATE_SET = {R.attr.state_middle_v};
    private static final int[] VERTICAL_LAST_STATE_SET = {R.attr.state_last_v};
    private static final int[] VERTICAL_SINGLE_STATE_SET = {R.attr.state_single_v};

    public static final int STATE_HORIZONTAL_SINGLE = 1000;
    public static final int STATE_HORIZONTAL_FIRST = 1001;
    public static final int STATE_HORIZONTAL_MIDDLE = 1002;
    public static final int STATE_HORIZONTAL_LAST = 1003;
    public static final int STATE_VERTICAL_SINGLE = 2000;
    public static final int STATE_VERTICAL_FIRST = 2001;
    public static final int STATE_VERTICAL_MIDDLE = 2002;
    public static final int STATE_VERTICAL_LAST = 2003;
    private int mBackgroundState = STATE_HORIZONTAL_SINGLE;

    public SegmentButton(Context context) {
        this(context, null);
    }

    public SegmentButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.segmentButtonStyle);
    }

    public SegmentButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentButton, defStyle, 0);

        Drawable d = a.getDrawable(R.styleable.SegmentButton_button);
        if (d != null) {
            setButtonDrawable(d);
        }

        boolean checked = a.getBoolean(R.styleable.SegmentButton_checked, false);
        setChecked(checked);

        a.recycle();
    }

    public void toggle() {
        if (!isChecked()) {
            setChecked(!mChecked);
        }
    }

    @Override
    public boolean performClick() {
        /*
         * XXX: These are tiny, need some surrounding 'expanded touch area',
         * which will need to be implemented in Button if we only override
         * performClick()
         */

        /* When clicked, toggle the state */
        toggle();
        return super.performClick();
    }

    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * <p>
     * Changes the checked state of this button.
     * </p>
     * 
     * @param checked
     *            true to check the button, false to uncheck it
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
//            notifyViewAccessibilityStateChangedIfNeeded(
//                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }
            if (mOnCheckedChangeWidgetListener != null) {
                mOnCheckedChangeWidgetListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     * 
     * @param listener
     *            the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes. This callback is used
     * for internal purpose only.
     * 
     * @param listener
     *            the callback to call on checked state change
     * @hide
     */
    void setOnCheckedChangeWidgetListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeWidgetListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when the checked state of a segment button changed.
     */
    public static interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a segment button has changed.
         * 
         * @param buttonView
         *            The segment button view whose state has changed.
         * @param isChecked
         *            The new checked state of buttonView.
         */
        void onCheckedChanged(SegmentButton buttonView, boolean isChecked);
    }

    /**
     * Set the background to a given Drawable, identified by its resource id.
     * 
     * @param resid
     *            the resource id of the drawable to use as the background
     */
    public void setButtonDrawable(int resid) {
        if (resid != 0 && resid == mButtonResource) {
            return;
        }

        mButtonResource = resid;

        Drawable d = null;
        if (mButtonResource != 0) {
            d = getResources().getDrawable(mButtonResource);
        }
        setButtonDrawable(d);
    }

    /**
     * Set the background to a given Drawable
     * 
     * @param d
     *            The Drawable to use as the background
     */
    public void setButtonDrawable(Drawable d) {
        if (d != null) {
            if (mButtonDrawable != null) {
                mButtonDrawable.setCallback(null);
                unscheduleDrawable(mButtonDrawable);
            }
            d.setCallback(this);
            d.setVisible(getVisibility() == VISIBLE, false);
            mButtonDrawable = d;
            setMinHeight(mButtonDrawable.getIntrinsicHeight());
        }

        refreshDrawableState();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(SegmentButton.class.getName());
        event.setChecked(mChecked);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(SegmentButton.class.getName());
        info.setCheckable(true);
        info.setChecked(mChecked);
    }

    @Override
    public int getCompoundPaddingLeft() {
        int padding = super.getCompoundPaddingLeft();
        if (!isLayoutRtl()) {
            final Drawable buttonDrawable = mButtonDrawable;
            if (buttonDrawable != null) {
                padding += buttonDrawable.getIntrinsicWidth();
            }
        }
        return padding;
    }

    @Override
    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight();
        if (isLayoutRtl()) {
            final Drawable buttonDrawable = mButtonDrawable;
            if (buttonDrawable != null) {
                padding += buttonDrawable.getIntrinsicWidth();
            }
        }
        return padding;
    }

    /**
     * @hide
     */
    @Override
    public int getHorizontalOffsetForDrawables() {
        final Drawable buttonDrawable = mButtonDrawable;
        return (buttonDrawable != null) ? buttonDrawable.getIntrinsicWidth() : 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean hasText = false;
        CharSequence text = getText();
        if (text != null && !TextUtils.isEmpty(text)) {
            hasText = true;
        } else {
            hasText = false;
        }
        final Drawable buttonDrawable = mButtonDrawable;
        if (buttonDrawable != null) {
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int drawableHeight = buttonDrawable.getIntrinsicHeight();
            final int drawableWidth = buttonDrawable.getIntrinsicWidth();

            int top = 0;
            switch (verticalGravity) {
                case Gravity.BOTTOM:
                    top = getHeight() - drawableHeight;
                    break;
                case Gravity.CENTER_VERTICAL:
                    top = (getHeight() - drawableHeight) / 2;
                    break;
            }
            int bottom = top + drawableHeight;
            int left = isLayoutRtl() ? getWidth() - drawableWidth : 0;
            int right = isLayoutRtl() ? getWidth() : drawableWidth;

            //text is empty, buttondrawable draw center
            if (!hasText) {
                canvas.translate(Math.abs((getWidth() - drawableWidth)) / 2, 0);
            }
            buttonDrawable.setBounds(left, top, right, bottom);
            buttonDrawable.draw(canvas);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        /*final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }*/
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        switch (mBackgroundState) {
            case STATE_HORIZONTAL_SINGLE:
                mergeDrawableStates(drawableState, HORIZONTAL_SINGLE_STATE_SET);
                break;
            case STATE_HORIZONTAL_FIRST:
                mergeDrawableStates(drawableState, HORIZONTAL_FIRST_STATE_SET);
                break;
            case STATE_HORIZONTAL_MIDDLE:
                mergeDrawableStates(drawableState, HORIZONTAL_MIDDLE_STATE_SET);
                break;
            case STATE_HORIZONTAL_LAST:
                mergeDrawableStates(drawableState, HORIZONTAL_LAST_STATE_SET);
                break;
            case STATE_VERTICAL_SINGLE:
                mergeDrawableStates(drawableState, VERTICAL_SINGLE_STATE_SET);
                break;
            case STATE_VERTICAL_FIRST:
                mergeDrawableStates(drawableState, VERTICAL_FIRST_STATE_SET);
                break;
            case STATE_VERTICAL_MIDDLE:
                mergeDrawableStates(drawableState, VERTICAL_MIDDLE_STATE_SET);
                break;
            case STATE_VERTICAL_LAST:
                mergeDrawableStates(drawableState, VERTICAL_LAST_STATE_SET);
                break;
            default:
                mergeDrawableStates(drawableState, HORIZONTAL_SINGLE_STATE_SET);
                break;
        }
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mButtonDrawable != null) {
            int[] myDrawableState = getDrawableState();

            // Set the state of the Drawable
            mButtonDrawable.setState(myDrawableState);

            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mButtonDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mButtonDrawable != null)
            mButtonDrawable.jumpToCurrentState();
    }

    static class SavedState extends BaseSavedState {
        boolean checked;

        /**
         * Constructor called from {@link segmentButton#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
        }

        @Override
        public String toString() {
            return "SegmentButton.SavedState{" + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + checked + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        setFreezesText(true);
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);

        ss.checked = isChecked();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked);
        requestLayout();
    }

    public void setBackgroundState (int backgroundState) {
        mBackgroundState = backgroundState;
    }
}
