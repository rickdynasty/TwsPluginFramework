/*
 * Copyright (C) 2011 The Android Open Source Project
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
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuItemImpl;
import com.tencent.tws.assistant.internal.view.menu.MenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuView;
import com.tencent.tws.assistant.internal.view.menu.SubMenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuView.ActionMenuChildView;
import com.tencent.tws.assistant.internal.widget.ActionBarView;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils.TruncateAt;
import android.util.SparseBooleanArray;
import android.view.ActionProvider;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Slog;

import java.util.ArrayList;

/**
 * MenuPresenter for building action menus as seen in the action bar and action modes.
 */
public class ActionMenuPresenter extends BaseMenuPresenter
        implements ActionProvider.SubUiVisibilityListener {
    private static final String TAG = "ActionMenuPresenter";

    private View mOverflowButton;
    private boolean mReserveOverflow;
    private boolean mReserveOverflowSet;
    private int mWidthLimit;
    private int mActionItemWidthLimit;
    private int mMaxItems;
    private boolean mMaxItemsSet;
    private boolean mStrictWidthLimit;
    private boolean mWidthLimitSet;
    private boolean mExpandedActionViewsExclusive;

    private int mMinCellSize;
	
    // Group IDs that have been added as actions - used temporarily, allocated here for reuse.
    private final SparseBooleanArray mActionButtonGroups = new SparseBooleanArray();

    private View mScrapActionButtonView;

    private OverflowPopup mOverflowPopup;
    private ActionButtonSubmenu mActionButtonPopup;

    private OpenOverflowRunnable mPostedOpenRunnable;
    //tws-start Overflow Button::2014-8-29
    private boolean mHasOverflowButton = false;
    //tws-end Overflow Button::2014-8-29

    final PopupPresenterCallback mPopupPresenterCallback = new PopupPresenterCallback();
    int mOpenSubMenuId;

    public ActionMenuPresenter(Context context) {
		/*NANJI-START::change::haoranma::2012-10-11*/
        super(context, R.layout.action_menu_layout,
                R.layout.action_menu_item_layout);
		/*NANJI-END::change::haoranma::2012-10-11*/
		//Slog.v(TAG, "ActionMenuPresenter ActionMenuPresenter 0");
        /*NANJISTART::add::geofffeng::201200508,make sure item width is the same*/
    }

    public ActionMenuPresenter(Context context,int actionMod) {
		/*NANJI-START::change::haoranma::2012-10-11*/
		super(context, R.layout.action_menu_layout,
                R.layout.actionmod_menu_item_layout);
		/*NANJI-END::change::haoranma::2012-10-11*/
       // Slog.v(TAG, "ActionMenuPresenter ActionMenuPresenter 1");
      /*NANJISTART::add::geofffeng::201200508,make sure item width is the same*/
    }
    
    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        super.initForMenu(context, menu);

        final Resources res = context.getResources();

        if (!mReserveOverflowSet) {
            //tws-start ActionMenu PopupWindow::2014-8-20
            //mReserveOverflow = !ViewConfiguration.get(context).hasPermanentMenuKey();
            mReserveOverflow = true;
            //tws-end ActionMenu PopupWindow::2014-8-20
        }

        if (!mWidthLimitSet) {
            mWidthLimit = res.getDisplayMetrics().widthPixels / 2;
        }

        // Measure for initial configuration
        if (!mMaxItemsSet) {
			/*NANJISTART::modified com.internal to tws 20121011*/
            mMaxItems = res.getInteger(R.integer.max_action_buttons);
			/*NANJIEND::modified com.internal to tws 20121011*/
        }

        int width = mWidthLimit;
        if (mReserveOverflow) {
            if (mOverflowButton == null) {
            	if (mWidthLimitSet) {
					mOverflowButton = new OverflowMenuButton(mSystemContext);
				}
            	else {
					mOverflowButton = new OverflowMenuButtonTop(mSystemContext);
				}
                final int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                mOverflowButton.measure(spec, spec);
            }
            width -= mOverflowButton.getMeasuredWidth();
        } else {
            mOverflowButton = null;
        }

        mActionItemWidthLimit = width;

        mMinCellSize = (int) (ActionMenuView.MIN_CELL_SIZE * res.getDisplayMetrics().density);

        // Drop a scrap view as it may no longer reflect the proper context/config.
        mScrapActionButtonView = null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (!mMaxItemsSet) {
			/*NANJISTART::modified com.internal to tws 20121011*/
            mMaxItems = mContext.getResources().getInteger(
                    R.integer.max_action_buttons);
			/*NANJIEND::modified com.internal to tws 20121011*/
            if (mMenu != null) {
                mMenu.twsOnItemsChanged(true);
            }
        }
    }

    public void setWidthLimit(int width, boolean strict) {
        mWidthLimit = width;
        mStrictWidthLimit = strict;
        mWidthLimitSet = true;
    }

    public void setReserveOverflow(boolean reserveOverflow) {
        mReserveOverflow = reserveOverflow;
        mReserveOverflowSet = true;
    }

    public void setItemLimit(int itemCount) {
        mMaxItems = itemCount;
        mMaxItemsSet = true;
    }

    public void setExpandedActionViewsExclusive(boolean isExclusive) {
        mExpandedActionViewsExclusive = isExclusive;
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        MenuView result = super.getMenuView(root);
        ((ActionMenuView) result).setPresenter(this);
        return result;
    }

    @Override
    public View getItemView(MenuItemImpl item, View convertView, ViewGroup parent) {
        View actionView = item.getActionView();
        if (actionView == null || item.hasCollapsibleActionView()) {
            if (!(convertView instanceof ActionMenuItemView)) {
                convertView = null;
            }
            actionView = super.getItemView(item, convertView, parent);
        }
        actionView.setVisibility(item.isActionViewExpanded() ? View.GONE : View.VISIBLE);

        final ActionMenuView menuParent = (ActionMenuView) parent;
        final ViewGroup.LayoutParams lp = actionView.getLayoutParams();
        if (!menuParent.checkLayoutParams(lp)) {
            actionView.setLayoutParams(menuParent.generateLayoutParams(lp));
        }
        return actionView;
    }

    @Override
    public void bindItemView(MenuItemImpl item, MenuView.ItemView itemView) {
        itemView.initialize(item, 0);

        final ActionMenuView menuView = (ActionMenuView) mMenuView;
        ActionMenuItemView actionItemView = (ActionMenuItemView) itemView;
        actionItemView.setItemInvoker(menuView);
        //tws-start add for split actionBar theme::2015-6-11
        boolean isActionBarSplitTheme = ThemeUtils.isActionBarSplitTheme(mContext);
        if (isActionBarSplitTheme && mWidthLimitSet) {
            TypedArray a = mContext.obtainStyledAttributes(R.styleable.ActionBar);
            int textResId = a
                    .getResourceId(
                            R.styleable.ActionBar_menuitemBottomStyle,
                            R.style.TextAppearance_tws_Second_twsTextLargerLightTitleMenuItem_Bottom);
            if (textResId > 0) {
                actionItemView.setTextButtonNewStyle(textResId);
            }
            boolean bRipple = ThemeUtils.isShowRipple(mContext);
            if (bRipple) {
                if (android.os.Build.VERSION.SDK_INT > 15) {
                    actionItemView.setBackground(TwsRippleUtils.getDefaultDrawable(mContext));
                } else {
                    actionItemView.setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(mContext));
                }
            }else{
            	actionItemView.setBackgroundResource(R.color.transparent);
            }
            a.recycle();
        }
        //tws-end add for split actionBar theme::2015-6-11
    }

    @Override
    public boolean shouldIncludeItem(int childIndex, MenuItemImpl item) {
        return item.isActionButton();
    }

    @Override
    public void updateMenuView(boolean cleared) {
        super.updateMenuView(cleared);

        if (mMenu != null) {
            final ArrayList<MenuItemImpl> actionItems = mMenu.twsGetActionItems();
            final int count = actionItems.size();
            for (int i = 0; i < count; i++) {
                final ActionProvider provider = actionItems.get(i).getActionProvider();
                if (provider != null) {
                    provider.setSubUiVisibilityListener(this);
                }
            }
        }

        final ArrayList<MenuItemImpl> nonActionItems = mMenu != null ?
                mMenu.twsGetNonActionItems() : null;

        boolean hasOverflow = false;
        if (mReserveOverflow && nonActionItems != null) {
            final int count = nonActionItems.size();
            if (count == 1) {
                hasOverflow = !nonActionItems.get(0).isActionViewExpanded();
            } else {
                hasOverflow = count > 0;
            }
        }
        //tws-start Overflow Button::2014-8-29
        mHasOverflowButton = hasOverflow;
        //tws-end Overflow Button::2014-8-29

        if (hasOverflow) {
            if (mOverflowButton == null) {
                mOverflowButton = new OverflowMenuButton(mSystemContext);
            }
            ViewGroup parent = (ViewGroup) mOverflowButton.getParent();
            if (parent != mMenuView) {
                if (parent != null) {
                    parent.removeView(mOverflowButton);
                }
                ActionMenuView menuView = (ActionMenuView) mMenuView;
                // 0.2
                menuView.addView(mOverflowButton, menuView.generateOverflowButtonLayoutParams());
            }
        } else if (mOverflowButton != null && mOverflowButton.getParent() == mMenuView) {
            ((ViewGroup) mMenuView).removeView(mOverflowButton);
        }

        ((ActionMenuView) mMenuView).setOverflowReserved(mReserveOverflow);
    }

    @Override
    public boolean filterLeftoverView(ViewGroup parent, int childIndex) {
        if (parent.getChildAt(childIndex) == mOverflowButton) return false;
        return super.filterLeftoverView(parent, childIndex);
    }

    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        if (!subMenu.hasVisibleItems()) return false;

        SubMenuBuilder topSubMenu = subMenu;
        while (topSubMenu.getParentMenu() != mMenu) {
            topSubMenu = (SubMenuBuilder) topSubMenu.getParentMenu();
        }
        View anchor = findViewForItem(topSubMenu.getItem());
        if (anchor == null) {
            if (mOverflowButton == null) return false;
            anchor = mOverflowButton;
        }

        mOpenSubMenuId = subMenu.getItem().getItemId();
        mActionButtonPopup = new ActionButtonSubmenu(mContext, subMenu);
        mActionButtonPopup.setAnchorView(anchor);
        mActionButtonPopup.show();
        super.onSubMenuSelected(subMenu);
        return true;
    }

    private View findViewForItem(MenuItem item) {
        final ViewGroup parent = (ViewGroup) mMenuView;
        if (parent == null) return null;

        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            if (child instanceof MenuView.ItemView &&
                    ((MenuView.ItemView) child).getItemData() == item) {
                return child;
            }
        }
        return null;
    }

    /**
     * Display the overflow menu if one is present.
     * @return true if the overflow menu was shown, false otherwise.
     */
    public boolean showOverflowMenu() {
        if (mReserveOverflow && !isOverflowMenuShowing() && mMenu != null && mMenuView != null &&
                mPostedOpenRunnable == null) {
        	// tws-start add overflow click listener::2014-12-18
        	if (mOverflowClickListener != null) {
        		mOverflowClickListener.doClick();
			}
        	// tws-end add overflow click listener::2014-12-18
            OverflowPopup popup = new OverflowPopup(mContext, mMenu, mOverflowButton, true);
            mPostedOpenRunnable = new OpenOverflowRunnable(popup);
            // Post this for later; we might still need a layout for the anchor to be right.
            ((View) mMenuView).post(mPostedOpenRunnable);

            // ActionMenuPresenter uses null as a callback argument here
            // to indicate overflow is opening.
            super.onSubMenuSelected(null);

            return true;
        }
        return false;
    }

    /**
     * Hide the overflow menu if it is currently showing.
     *
     * @return true if the overflow menu was hidden, false otherwise.
     */
    public boolean hideOverflowMenu() {
        if (mPostedOpenRunnable != null && mMenuView != null) {
            ((View) mMenuView).removeCallbacks(mPostedOpenRunnable);
            mPostedOpenRunnable = null;
            return true;
        }

        MenuPopupHelper popup = mOverflowPopup;
        if (popup != null) {
            popup.dismiss();
            return true;
        }
        return false;
    }

    /**
     * Dismiss all popup menus - overflow and submenus.
     * @return true if popups were dismissed, false otherwise. (This can be because none were open.)
     */
    public boolean dismissPopupMenus() {
        boolean result = hideOverflowMenu();
        result |= hideSubMenus();
        return result;
    }

    /**
     * Dismiss all submenu popups.
     *
     * @return true if popups were dismissed, false otherwise. (This can be because none were open.)
     */
    public boolean hideSubMenus() {
        if (mActionButtonPopup != null) {
            mActionButtonPopup.dismiss();
            return true;
        }
        return false;
    }

    /**
     * @return true if the overflow menu is currently showing
     */
    public boolean isOverflowMenuShowing() {
        return mOverflowPopup != null && mOverflowPopup.isShowing();
    }

    /**
     * @return true if space has been reserved in the action menu for an overflow item.
     */
    public boolean isOverflowReserved() {
        return mReserveOverflow;
    }

    public boolean flagActionItems() {
        final ArrayList<MenuItemImpl> visibleItems = mMenu.twsGetVisibleItems();
        final int itemsSize = visibleItems.size();
        int maxActions = mMaxItems;
        int widthLimit = mActionItemWidthLimit;
        final int querySpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final ViewGroup parent = (ViewGroup) mMenuView;

        int requiredItems = 0;
        int requestedItems = 0;
        int firstActionWidth = 0;
        boolean hasOverflow = false;
        for (int i = 0; i < itemsSize; i++) {
            MenuItemImpl item = visibleItems.get(i);
            if (item.requiresActionButton()) {
                requiredItems++;
            } else if (item.requestsActionButton()) {
                requestedItems++;
            } else {
                hasOverflow = true;
            }
            if (mExpandedActionViewsExclusive && item.isActionViewExpanded()) {
                // Overflow everything if we have an expanded action view and we're
                // space constrained.
                maxActions = 0;
            }
        }

        // Reserve a spot for the overflow item if needed.
        if (mReserveOverflow &&
                (hasOverflow || requiredItems + requestedItems > maxActions)) {
            maxActions--;
        }
        maxActions -= requiredItems;

        final SparseBooleanArray seenGroups = mActionButtonGroups;
        seenGroups.clear();

        int cellSize = 0;
        int cellsRemaining = 0;
        if (mStrictWidthLimit) {
            cellsRemaining = widthLimit / mMinCellSize;
            final int cellSizeRemaining = widthLimit % mMinCellSize;
            cellSize = mMinCellSize + cellSizeRemaining / cellsRemaining;
        }

        // Flag as many more requested items as will fit.
        for (int i = 0; i < itemsSize; i++) {
            MenuItemImpl item = visibleItems.get(i);

            if (item.requiresActionButton()) {
                View v = getItemView(item, mScrapActionButtonView, parent);
                if (mScrapActionButtonView == null) {
                    mScrapActionButtonView = v;
                }
                if (mStrictWidthLimit) {
                    cellsRemaining -= ActionMenuView.measureChildForCells(v,
                            cellSize, cellsRemaining, querySpec, 0);
                } else {
                    v.measure(querySpec, querySpec);
                }
                final int measuredWidth = v.getMeasuredWidth();
                widthLimit -= measuredWidth;
                if (firstActionWidth == 0) {
                    firstActionWidth = measuredWidth;
                }
                final int groupId = item.getGroupId();
                if (groupId != 0) {
                    seenGroups.put(groupId, true);
                }
                item.setIsActionButton(true);
            } else if (item.requestsActionButton()) {
                // Items in a group with other items that already have an action slot
                // can break the max actions rule, but not the width limit.
                final int groupId = item.getGroupId();
                final boolean inGroup = seenGroups.get(groupId);
                boolean isAction = (maxActions > 0 || inGroup) && widthLimit > 0 &&
                        (!mStrictWidthLimit || cellsRemaining > 0);

                if (isAction) {
                    View v = getItemView(item, mScrapActionButtonView, parent);
                    if (mScrapActionButtonView == null) {
                        mScrapActionButtonView = v;
                    }
                    if (mStrictWidthLimit) {
                        final int cells = ActionMenuView.measureChildForCells(v,
                                cellSize, cellsRemaining, querySpec, 0);
                        cellsRemaining -= cells;
                        if (cells == 0) {
                            isAction = false;
                        }
                    } else {
                        v.measure(querySpec, querySpec);
                    }
                    final int measuredWidth = v.getMeasuredWidth();
                    widthLimit -= measuredWidth;
                    if (firstActionWidth == 0) {
                        firstActionWidth = measuredWidth;
                    }

                    if (mStrictWidthLimit) {
                        isAction &= widthLimit >= 0;
                    } else {
                        // Did this push the entire first item past the limit?
                        isAction &= widthLimit + firstActionWidth > 0;
                    }
                }

                if (isAction && groupId != 0) {
                    seenGroups.put(groupId, true);
                } else if (inGroup) {
                    // We broke the width limit. Demote the whole group, they all overflow now.
                    seenGroups.put(groupId, false);
                    for (int j = 0; j < i; j++) {
                        MenuItemImpl areYouMyGroupie = visibleItems.get(j);
                        if (areYouMyGroupie.getGroupId() == groupId) {
                            // Give back the action slot
                            if (areYouMyGroupie.isActionButton()) maxActions++;
                            areYouMyGroupie.setIsActionButton(false);
                        }
                    }
                }

                if (isAction) maxActions--;

                item.setIsActionButton(isAction);
            }
        }
        return true;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        dismissPopupMenus();
        super.onCloseMenu(menu, allMenusAreClosing);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.openSubMenuId = mOpenSubMenuId;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState saved = (SavedState) state;
        if (saved.openSubMenuId > 0) {
            MenuItem item = mMenu.findItem(saved.openSubMenuId);
            if (item != null) {
                SubMenuBuilder subMenu = (SubMenuBuilder) item.getSubMenu();
                onSubMenuSelected(subMenu);
            }
        }
    }

    @Override
    public void onSubUiVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            // Not a submenu, but treat it like one.
            super.onSubMenuSelected(null);
        } else {
            mMenu.twsClose(false);
        }
    }

    private static class SavedState implements Parcelable {
        public int openSubMenuId;

        SavedState() {
        }

        SavedState(Parcel in) {
            openSubMenuId = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(openSubMenuId);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class OverflowMenuButtonTop extends ImageButton implements ActionMenuChildView {
        public OverflowMenuButtonTop(Context context) {
            super(context, null, R.attr.actionOverflowButtonStyle);
            setClickable(true);
            setFocusable(true);
            setVisibility(VISIBLE);
            setEnabled(true);
            setContentDescription("OverflowMenuButton");
            // tws-start add for ripple::2014-12-21
            boolean bRipple = ThemeUtils.isShowRipple(context);
            if (bRipple) {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		setBackground(TwsRippleUtils.getDefaultDarkDrawable(context));
            	}
            	else {
            		setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(context));
            	}
            }else{
            	setBackgroundResource(R.color.transparent);
            }
            // tws-end add for ripple::2014-12-21
        }

        @Override
        public boolean performClick() {
            if (super.performClick()) {
                return true;
            }

            playSoundEffect(SoundEffectConstants.CLICK);
            postDelayed(new Runnable() {

                public void run() {
                    // TODO Auto-generated method stub
                    showOverflowMenu();
                }
            }, mIsTopOverflowDelay ? 200 : 0);
            return true;
        }

        public boolean needsDividerBefore() {
            return false;
        }

        public boolean needsDividerAfter() {
            return false;
        }

    }

    private class OverflowMenuButton extends ImageButton implements ActionMenuChildView {
        public OverflowMenuButton(Context context) {
            super(context, null, R.attr.actionOverflowButtonBottomStyle);
            boolean isActionBarSplitTheme = ThemeUtils.isActionBarSplitTheme(mContext);
            if (isActionBarSplitTheme) {
                TypedArray array = context.obtainStyledAttributes(R.styleable.ActionBar);
                int buttonStyleId = array.getResourceId(R.styleable.ActionBar_actionOverflowButtonBottomStyle, 
                        R.style.Widget_tws_ActionButton_Overflow_Bottom);
                if (buttonStyleId == R.style.Widget_tws_ActionButton_Overflow_Bottom) {
                    setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_moreoverflow_bottom));
                } else if (buttonStyleId == R.style.Widget_tws_ActionButton_Overflow_Bottom_Holo) {
                    setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_moreoverflow_bottom_holo));
                }
                array.recycle();
            }
            setClickable(true);
            setFocusable(true);
            setVisibility(VISIBLE);
            setEnabled(true);
            setPadding(
                    0,
                    0,
                    0 - (int) context.getResources().getDimension(
                            R.dimen.actionbar_bottom_overflow_padding), 0);
            setContentDescription("OverflowMenuButton");
            // tws-start add for ripple::2014-12-21
            boolean bRipple = ThemeUtils.isShowRipple(context);
            if (bRipple) {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		setBackground(TwsRippleUtils.getDefaultDrawable(context));
            	}
            	else {
            		setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(mContext));
            	}
            }else{
            	setBackgroundResource(R.color.dm_common_item_normal);
            }
            // tws-end add for ripple::2014-12-21
        }

        @Override
        public boolean performClick() {
            if (super.performClick())
                return true;
            playSoundEffect(SoundEffectConstants.CLICK);
            postDelayed(new Runnable() {

                public void run() {
                    // TODO Auto-generated method stub
                    showOverflowMenu();
                }
            }, mIsOverflowDelay ? 200 : 0);
            return true;
        }

        public boolean needsDividerBefore() {
            return false;
        }

        public boolean needsDividerAfter() {
            return false;
        }
    }

    private class OverflowPopup extends MenuPopupHelper {
        public OverflowPopup(Context context, MenuBuilder menu, View anchorView, boolean overflowOnly) {
            super(context, menu, anchorView, overflowOnly);
            setCallback(mPopupPresenterCallback);
        }

        @Override
        public void onDismiss() {
            super.onDismiss();
            mMenu.close();
            mOverflowPopup = null;
        }
    }

    private class ActionButtonSubmenu extends MenuPopupHelper {
        private SubMenuBuilder mSubMenu;

        public ActionButtonSubmenu(Context context, SubMenuBuilder subMenu) {
            super(context, subMenu);
            mSubMenu = subMenu;

            MenuItemImpl item = (MenuItemImpl) subMenu.getItem();
            if (!item.isActionButton()) {
                // Give a reasonable anchor to nested submenus.
                setAnchorView(mOverflowButton == null ? (View) mMenuView : mOverflowButton);
            }

            setCallback(mPopupPresenterCallback);

            boolean preserveIconSpacing = false;
            final int count = subMenu.size();
            for (int i = 0; i < count; i++) {
                MenuItem childItem = subMenu.getItem(i);
                if (childItem.isVisible() && childItem.getIcon() != null) {
                    preserveIconSpacing = true;
                    break;
                }
            }
            setForceShowIcon(preserveIconSpacing);
        }

        @Override
        public void onDismiss() {
            super.onDismiss();
            mActionButtonPopup = null;
            mOpenSubMenuId = 0;
        }
    }

    private class PopupPresenterCallback implements MenuPresenter.Callback {

        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null)
                return false;

            mOpenSubMenuId = ((SubMenuBuilder) subMenu).getItem().getItemId();
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (menu instanceof SubMenuBuilder) {
                ((SubMenuBuilder) menu).getRootMenu().twsClose(false);
            }
        }
    }

    private class OpenOverflowRunnable implements Runnable {
        private OverflowPopup mPopup;

        public OpenOverflowRunnable(OverflowPopup popup) {
            mPopup = popup;
        }

        public void run() {
            mMenu.changeMenuMode();
            final View menuView = (View) mMenuView;
            if (menuView != null && menuView.getWindowToken() != null
                    && mPopup.tryShow(mIsTransPopup, false, mIsMarks, mTextColors)) {
                mOverflowPopup = mPopup;
            }
            mPostedOpenRunnable = null;
        }
    }

    // tws-start Overflow Button::2014-8-29
    public boolean isOverflowButtonShowing() {
        if (mOverflowButton != null && mOverflowButton.getVisibility() == View.VISIBLE && mHasOverflowButton) {
            return true;
        }
        return false;
    }

    // tws-end Overflow Button::2014-8-29

    public void setOverflowButtonState(boolean enabled) {
        if (mWidthLimitSet && mOverflowButton != null) {
            mOverflowButton.setEnabled(enabled);
        }
    }

    public void setTopOverflowButtonState(boolean enabled) {
        if (!mWidthLimitSet && mOverflowButton != null) {
            mOverflowButton.setEnabled(enabled);
        }
    }

    // tws-start add overflow click listener::2014-12-18
    private ActionBar.OverflowClickListener mOverflowClickListener;

    public void setOverflowClickListener(ActionBar.OverflowClickListener listener) {
        mOverflowClickListener = listener;
    }

    // tws-end add overflow click listener::2014-12-18

    // tws-start add Overflow interface::2015-2-9
    public View getOverflowButton() {
        return mOverflowButton;
    }

    // tws-end add Overflow interface::2015-2-9

    // tws-start add transPopup interface::2015-3-10
    private boolean mIsTransPopup;

    public void setIsTransPopup(boolean isTransPopup) {
        mIsTransPopup = isTransPopup;
    }

    // tws-end add transPopup interface::2015-3-10

    // tws-start add PopupMenuRedPoint interface::2015-3-12
    private boolean[] mIsMarks;
    private int[] mTextColors;

    public void setPopupMenuMarks(boolean[] isMarks) {
        mIsMarks = isMarks;
    }
    
    public void setPopupTextColors(int[] textColors) {
    	mTextColors = textColors;
    }

    // tws-end add PopupMenuRedPoint interface::2015-3-12

    // tws-start add Overflow clickDelay interface::2015-3-19
    private boolean mIsOverflowDelay, mIsTopOverflowDelay;

    public void setOverflowDelay(boolean isDelay) {
        mIsOverflowDelay = isDelay;
    }

    public void setTopOverflowDelay(boolean isDelay) {
        mIsTopOverflowDelay = isDelay;
    }
    // tws-end add Overflow clickDelay interface::2015-3-19
}
