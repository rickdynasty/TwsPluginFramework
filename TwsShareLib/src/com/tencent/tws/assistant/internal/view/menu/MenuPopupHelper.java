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
import java.util.ArrayList;

import com.tencent.tws.assistant.gaussblur.JNIBlur;
import com.tencent.tws.assistant.widget.AbsListView;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.ListPopupWindow;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.PopupWindow;
import com.tencent.tws.sharelib.R;
import android.app.Activity;
import android.app.TwsActivity;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;


/**
 * Presents a menu as a small, simple popup anchored to another view.
 * @hide
 */
public class MenuPopupHelper implements AdapterView.OnItemClickListener, View.OnKeyListener,
        ViewTreeObserver.OnGlobalLayoutListener, PopupWindow.OnDismissListener,
        View.OnAttachStateChangeListener, MenuPresenter {
    private static final String TAG = "MenuPopupHelper";

    static final int ITEM_LAYOUT = R.layout.popup_menu_item_layout;

    private Context mContext;
    private LayoutInflater mInflater;
    private ListPopupWindow mPopup;
    private MenuBuilder mMenu;
    private int mPopupMaxWidth;
    private View mAnchorView;
    private boolean mOverflowOnly;
    private ViewTreeObserver mTreeObserver;

    private MenuAdapter mAdapter;

    private Callback mPresenterCallback;

    boolean mForceShowIcon;

    private ViewGroup mMeasureParent;
    
    public MenuPopupHelper(Context context, MenuBuilder menu) {
        this(context, menu, null, false);
    }

    public MenuPopupHelper(Context context, MenuBuilder menu, View anchorView) {
        this(context, menu, anchorView, false);
    }

    public MenuPopupHelper(Context context, MenuBuilder menu, View anchorView, boolean overflowOnly) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mMenu = menu;
        mOverflowOnly = overflowOnly;

        final Resources res = context.getResources();
		mPopupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
				res.getDimensionPixelSize(R.dimen.config_prefDialogWidth));

        mAnchorView = anchorView;

        menu.addMenuPresenter(this);
    }

    public void setAnchorView(View anchor) {
        mAnchorView = anchor;
    }

    public void setForceShowIcon(boolean forceShow) {
        mForceShowIcon = forceShow;
    }

    public void show() {
        if (!tryShow(false, false, null, null)) {
            throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");
        }
    }

    // tws-start modify transPopup blur interface::2015-3-17
    public Bitmap takeMenuBGBlur(int count) {
        try {
            Resources res = mContext.getResources();
            View view = ((TwsActivity) mContext).getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bitmap = view.getDrawingCache();

            Drawable topDrawable = res.getDrawable(R.drawable.menu_dropdown_panel_holo_dark);
            topDrawable.setBounds(0, 0, res.getDimensionPixelSize(R.dimen.tws_popup_menu_min_width),
                    res.getDimensionPixelSize(R.dimen.tws_listview_item_height) * count);

            Matrix matrix = new Matrix();
            matrix.postScale(0.1f, 0.1f);

            int startY = mPopup.isPopupFromTop ? (res
                    .getDimensionPixelSize(R.dimen.tws_action_bar_height))
                    : (res.getDisplayMetrics().heightPixels
                            - res.getDimensionPixelSize(R.dimen.tws_listview_item_height) * count - res
                            .getDimensionPixelSize(R.dimen.tws_actionbar_split_height));

            Bitmap bitmap2 = Bitmap.createBitmap(
                    bitmap,
                    res.getDisplayMetrics().widthPixels
                            - res.getDimensionPixelSize(R.dimen.tws_popup_menu_min_width) + 50, startY + 50,
                    res.getDimensionPixelSize(R.dimen.tws_popup_menu_min_width) - 100,
                    res.getDimensionPixelSize(R.dimen.tws_listview_item_height) * count - 100, matrix, true);
            Canvas canvas = new Canvas(bitmap2);
            topDrawable.draw(canvas);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
            view.destroyDrawingCache();

            JNIBlur blur = new JNIBlur(mContext);
            Bitmap bitmap3 = blur.blur(bitmap2, true);
            return bitmap3;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // tws-end modify transPopup blur interface::2015-3-17

    public boolean tryShow(boolean isTransparent, boolean isBlurBG, boolean[] isRedPoint, int[] textColors) {
        mPopup = new ListPopupWindow(mContext, null, R.attr.popupMenuStyle, isTransparent, isBlurBG,
                isRedPoint, textColors);
        mPopup.setOnDismissListener(this);
        mPopup.setOnItemClickListener(this);

        WindowManager mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metric = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metric);
        float density = metric.density;
        int location[] = new int[2];
        mAnchorView.getLocationOnScreen(location);
        int y = location[1];
        int mActionBarHeight = (int) mContext.getResources().getDimension(
                R.dimen.tws_action_bar_height);
        if (y < (int) (mActionBarHeight * density)) {
            mPopup.isPopupFromTop = true;
//            mPopup.setVerticalOffset(-6);// shadow padding
        } else {
            mPopup.isPopupFromTop = false;
//            mPopup.setVerticalOffset(-6);
        }

        // tws-start add transPopup interface::2015-3-10
        if (isTransparent) {
        	if (isBlurBG) {
        		Bitmap blurBGBitmap = takeMenuBGBlur(mMenu.getVisibleItems().size()
                        - mMenu.getActionItems().size());
                if (blurBGBitmap != null && !blurBGBitmap.isRecycled()) {
                    BitmapDrawable bd = new BitmapDrawable(mContext.getResources(), blurBGBitmap);
                    mPopup.setBackgroundDrawable(bd);
                }
        	}
        	else {
        		mPopup.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.menu_dropdown_panel_holo_light_trans));
        	}
        }
        // tws-end add transPopup interface::2015-3-10
        // mPopup.setHorizontalOffset((int)(10*density));
        mAdapter = new MenuAdapter(mMenu);
        mPopup.setAdapter(mAdapter);
        mPopup.setModal(true);

        View anchor = mAnchorView;
        if (anchor != null) {
            final boolean addGlobalListener = mTreeObserver == null;
            mTreeObserver = anchor.getViewTreeObserver(); // Refresh to latest
            if (addGlobalListener)
                mTreeObserver.addOnGlobalLayoutListener(this);
            anchor.addOnAttachStateChangeListener(this);
            mPopup.setAnchorView(anchor);
        } else {
            return false;
        }

        /*NANJI-START::change::haoranma::2012-11-29*/
        // mPopup.setContentWidth(Math.min(measureContentWidth(mAdapter), mPopupMaxWidth));
        twsSetWidth();
        /*NANJI-END::change::haoranma::2012-11-29*/
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopup.show();
//        mPopup.getListView().setSelector(R.color.transparent);
        mPopup.mDrawBottomDivider = false;
        mPopup.getListView().setDivider(
                mContext.getResources().getDrawable(R.drawable.list_divider_holo_light));
        mPopup.getListView().setOnKeyListener(this);
        mPopup.getListView().setVerticalScrollBarEnabled(false);
        mPopup.getListView().setOverScrollMode(ListView.OVER_SCROLL_NEVER);
        return true;
    }

    public void dismiss() {
        int delayTime = mContext.getResources().getInteger(R.integer.default_start_activity_delay);
        if (isShowing()) {
            mPopup.setRemoveView(false);
            mPopup.dismiss();
        }
    }

    public void onDismiss() {
        mPopup = null;
        mMenu.close();
        if (mTreeObserver != null) {
            if (!mTreeObserver.isAlive())
                mTreeObserver = mAnchorView.getViewTreeObserver();
            mTreeObserver.removeGlobalOnLayoutListener(this);
            mTreeObserver = null;
        }
        mAnchorView.removeOnAttachStateChangeListener(this);
    }

    public boolean isShowing() {
        return mPopup != null && mPopup.isShowing();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MenuAdapter adapter = mAdapter;
        adapter.mAdapterMenu.performItemAction(adapter.getItem(position), 0);
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss();
            return true;
        }
        return false;
    }

    private int measureContentWidth(ListAdapter adapter) {
        // Menus don't tend to be long, so this is more sane than it looks.
        int width = 0;
        View itemView = null;
        int itemType = 0;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(mContext);
            }
            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }
        return width;
    }

    @Override
    public void onGlobalLayout() {
        if (isShowing()) {
            final View anchor = mAnchorView;
            if (anchor == null || !anchor.isShown()) {
                dismiss();
            } else if (isShowing()) {
                // Recompute window size and position
                mPopup.show();
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        if (mTreeObserver != null) {
            if (!mTreeObserver.isAlive())
                mTreeObserver = v.getViewTreeObserver();
            mTreeObserver.removeGlobalOnLayoutListener(this);
        }
        v.removeOnAttachStateChangeListener(this);
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        // Don't need to do anything; we added as a presenter in the constructor.
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        throw new UnsupportedOperationException("MenuPopupHelpers manage their own views");
    }

    @Override
    public void updateMenuView(boolean cleared) {
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setCallback(Callback cb) {
        mPresenterCallback = cb;
    }

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        if (subMenu.hasVisibleItems()) {
            MenuPopupHelper subPopup = new MenuPopupHelper(mContext, subMenu, mAnchorView, false);
            subPopup.setCallback(mPresenterCallback);

            boolean preserveIconSpacing = false;
            final int count = subMenu.size();
            for (int i = 0; i < count; i++) {
                MenuItem childItem = subMenu.getItem(i);
                if (childItem.isVisible() && childItem.getIcon() != null) {
                    preserveIconSpacing = true;
                    break;
                }
            }
            subPopup.setForceShowIcon(preserveIconSpacing);

            if (subPopup.tryShow(false, false, new boolean[] {false}, new int[] {0})) {
                if (mPresenterCallback != null) {
                    mPresenterCallback.onOpenSubMenu(subMenu);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        // Only care about the (sub)menu we're presenting.
        if (menu != mMenu)
            return;

        dismiss();
        if (mPresenterCallback != null) {
            mPresenterCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    @Override
    public boolean flagActionItems() {
        return false;
    }

    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return null;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
    }

    private class MenuAdapter extends BaseAdapter {
        private MenuBuilder mAdapterMenu;
        private int mExpandedIndex = -1;

        public MenuAdapter(MenuBuilder menu) {
            mAdapterMenu = menu;
            registerDataSetObserver(new ExpandedIndexObserver());
            findExpandedIndex();
        }

        public int getCount() {
            ArrayList<MenuItemImpl> items = mOverflowOnly ? mAdapterMenu.twsGetNonActionItems()
                    : mAdapterMenu.twsGetVisibleItems();
            if (mExpandedIndex < 0) {
                return items.size();
            }
            return items.size() - 1;
        }

        public MenuItemImpl getItem(int position) {
            ArrayList<MenuItemImpl> items = mOverflowOnly ? mAdapterMenu.twsGetNonActionItems()
                    : mAdapterMenu.twsGetVisibleItems();
            if (mExpandedIndex >= 0 && position >= mExpandedIndex) {
                position++;
            }
            return items.get(position);
        }

        public long getItemId(int position) {
            // Since a menu item's ID is optional, we'll use the position as an
            // ID for the item in the AdapterView
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(ITEM_LAYOUT, parent, false);
            }

            MenuView.ItemView itemView = (MenuView.ItemView) convertView;
            if (mForceShowIcon) {
                ((ListMenuItemView) convertView).setForceShowIcon(true);
            }
            itemView.initialize(getItem(position), 0);
            int count = getCount();
            if (count > 1) {
                if (position == 0) {
//                    convertView.setBackgroundResource(R.drawable.second_menu_fullwidth_selector_top);
                } else if (position == count - 1) {
//                    convertView.setBackgroundResource(R.drawable.second_menu_fullwidth_selector_bottom);
                } else {
//                    convertView.setBackgroundResource(R.drawable.second_menu_fullwidth_selector_middle);
                }
            } else {
//                convertView.setBackgroundResource(R.drawable.second_menu_fullwidth_selector_full);
            }
            return convertView;
        }

        void findExpandedIndex() {
            final MenuItemImpl expandedItem = mMenu.getExpandedItem();
            if (expandedItem != null) {
                final ArrayList<MenuItemImpl> items = mMenu.twsGetNonActionItems();
                final int count = items.size();
                for (int i = 0; i < count; i++) {
                    final MenuItemImpl item = items.get(i);
                    if (item == expandedItem) {
                        mExpandedIndex = i;
                        return;
                    }
                }
            }
            mExpandedIndex = -1;
        }
    }

    private class ExpandedIndexObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mAdapter.findExpandedIndex();
        }
    }

    /*NANJI-START::add::haoranma::2012-12-03*/
    private void twsSetWidth() {
        int minWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.tws_popup_menu_min_width);
        int contentWidth = measureContentWidth(mAdapter);
        if (mAnchorView.getWidth() < minWidth) {
            if (contentWidth < minWidth) {
                mPopup.setContentWidth(minWidth);
            } else {
                mPopup.setContentWidth(Math.min(contentWidth, mPopupMaxWidth));
            }
        }
    }

    /*NANJI-END::add::haoranma::2012-12-03*/
}
