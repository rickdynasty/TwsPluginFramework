/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.tencent.tws.assistant.internal.view.menu;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.sharelib.R;


/**
 * @hide
 */
public class ActionMenuItemView extends LinearLayout
        implements MenuView.ItemView, View.OnClickListener, View.OnLongClickListener,
        ActionMenuView.ActionMenuChildView {
    private static final String TAG = "ActionMenuItemView";

    private MenuItemImpl mItemData;
    private CharSequence mTitle;
    private MenuBuilder.ItemInvoker mItemInvoker;

    private ImageView mImageButton;
    private TextView mTextButton;
    private boolean mAllowTextWithIcon;
    private boolean mExpandedFormat;
    private int mMinWidth;
	private Context mContext;

    public ActionMenuItemView(Context context) {
        this(context, null);
    }

    public ActionMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionMenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        final Resources res = context.getResources();
        /*tws-start::modified com.internal to tws 20121011*/
        mAllowTextWithIcon = res.getBoolean(R.bool.config_allowActionMenuItemTextWithIcon);
        /*tws-end::modified com.internal to tws 20121011*/
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionMenuItemView,
                R.attr.actionButtonStyle_menuitem, 0);
        final int defaultWidth = getResources().getDimensionPixelOffset(R.dimen.actionbar_overflow_minwidth);
        mMinWidth = a.getDimensionPixelOffset(R.styleable.ActionMenuItemView_minWidth, defaultWidth);
        // tws-end add actionbar0.2 feature::2014-09-28
        a.recycle();
        // tws-start add for ripple::2014-12-21

        boolean bRipple = ThemeUtils.isShowRipple(mContext);
        if (bRipple) {
            if (android.os.Build.VERSION.SDK_INT > 15) {
                setBackground(TwsRippleUtils.getDefaultDarkDrawable(mContext));
            } else {
                setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(mContext));
            }
        }else{
        	setBackgroundResource(R.color.transparent);
        }
        // tws-end add for ripple::2014-12-21
    }

    @Override
    public void onFinishInflate() {
        /*tws-start::change 2012-10-15*/
		mImageButton = (ImageView) findViewById(R.id.imageButton);
        mTextButton = (TextView) findViewById(R.id.textButton);
		/*tws-start::change 2012-10-15*/
        //mImageButton.setOnClickListener(this);
        //mTextButton.setOnClickListener(this);
        //mImageButton.setOnLongClickListener(this);
        setOnClickListener(this);
        setOnLongClickListener(this);
    }

    public void setTextButtonNewStyle(int textAppearance) {
        if (mTextButton != null) {
            mTextButton.setTextAppearance(getContext(), textAppearance);
        }
    }

    public MenuItemImpl getItemData() {
        return mItemData;
    }

    public void initialize(MenuItemImpl itemData, int menuType) {
        mItemData = itemData;

        setIcon(itemData.getIcon());
        setTitle(itemData.twsGetTitleForItemView(this)); // Title only takes effect if there is no icon
        setId(itemData.getItemId());

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);
        setEnabled(itemData.isEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mImageButton.setEnabled(enabled);
        mTextButton.setEnabled(enabled);
    }

    public void onClick(View v) {
        if (mItemInvoker != null) {
            mItemInvoker.invokeItem(mItemData);
        }
    }

    public void setItemInvoker(MenuBuilder.ItemInvoker invoker) {
        mItemInvoker = invoker;
    }

    public boolean prefersCondensedTitle() {
        return true;
    }

    public void setCheckable(boolean checkable) {
        // TODO Support checkable action items
    }

    public void setChecked(boolean checked) {
        // TODO Support checkable action items
    }
    /*tws-start::add::geofffeng::20120830*/
    @Override
    public void setSelected(boolean enabled) {
        super.setSelected(enabled);
        mImageButton.setSelected(enabled);
        mTextButton.setSelected(enabled);
    }
    /*tws-end::add::geofffeng::20120830*/

    public void setExpandedFormat(boolean expandedFormat) {
        if (mExpandedFormat != expandedFormat) {
            mExpandedFormat = expandedFormat;
            if (mItemData != null) {
                mItemData.actionFormatChanged();
            }
        }
    }

    private void updateTextButtonVisibility() {
        boolean visible = !TextUtils.isEmpty(mTextButton.getText());
        visible &= mImageButton.getDrawable() == null ||
                (mItemData.showsTextAsAction() && (mAllowTextWithIcon || mExpandedFormat));

        mTextButton.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setIcon(Drawable icon) {
        mImageButton.setImageDrawable(icon);
        if (icon != null) {
            mImageButton.setVisibility(VISIBLE);
        } else {
            mImageButton.setVisibility(GONE);
        }

        updateTextButtonVisibility();
    }

    public boolean hasText() {
        return mTextButton.getVisibility() != GONE;
    }

    public void setShortcut(boolean showShortcut, char shortcutKey) {
        // Action buttons don't show text for shortcut keys.
    }

    public void setTitle(CharSequence title) {
        mTitle = title;

        mTextButton.setText(mTitle);

        setContentDescription(mTitle);
        updateTextButtonVisibility();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        final CharSequence cdesc = getContentDescription();
        if (!TextUtils.isEmpty(cdesc)) {
            event.getText().add(cdesc);
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Don't allow children to hover; we want this to be treated as a single component.
        return onHoverEvent(event);
    }

    public boolean showsIcon() {
        return true;
    }

    public boolean needsDividerBefore() {
//        return hasText() && mItemData.getIcon() == null;
    	return false;
    }

    public boolean needsDividerAfter() {
//        return hasText();
    	return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (hasText()) {
            // Don't show the cheat sheet for items that already show text.
            return false;
        }

        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        getLocationOnScreen(screenPos);
        getWindowVisibleDisplayFrame(displayFrame);

        final Context context = getContext();
        final int width = getWidth();
        final int height = getHeight();
        final int midy = screenPos[1] + height / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        Toast.makeText(context, mItemData.getTitle(), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int specSize = MeasureSpec.getSize(widthMeasureSpec);
        final int oldMeasuredWidth = getMeasuredWidth();
        final int targetWidth = widthMode == MeasureSpec.AT_MOST ? Math.min(specSize, mMinWidth)
                : mMinWidth;

        if (widthMode != MeasureSpec.EXACTLY && mMinWidth > 0 && oldMeasuredWidth < targetWidth) {
            // Remeasure at exactly the minimum width.
            super.onMeasure(MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                    heightMeasureSpec);
        }
    }
}
