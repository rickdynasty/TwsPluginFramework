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

package com.tencent.tws.assistant.internal.view.menu;

import com.tencent.tws.assistant.app.AlertDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuItemImpl;
import com.tencent.tws.assistant.internal.view.menu.MenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuView;
import com.tencent.tws.assistant.internal.view.menu.SubMenuBuilder;
import com.tencent.tws.sharelib.R;

import android.content.DialogInterface;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.content.Context;

/**
 * Helper for menus that appear as Dialogs (context and submenus).
 * 
 * @hide
 */
public class MenuDialogHelper implements DialogInterface.OnKeyListener,
        DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener,
        MenuPresenter.Callback {
    private MenuBuilder mMenu;
    private AlertDialog mDialog;
    ListMenuPresenter mPresenter;
    private MenuPresenter.Callback mPresenterCallback;
    
    public MenuDialogHelper(MenuBuilder menu) {
        mMenu = menu;
    }

    /**
     * Shows menu as a dialog. 
     * 
     * @param windowToken Optional token to assign to the window.
     */
    public void show(IBinder windowToken) {
        // Many references to mMenu, create local reference
        final MenuBuilder menu = mMenu;
        
        // Get the builder for the dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(menu.getContext(), 
                R.style.Theme_tws_Second_Dialog_Alert_Context);

        mPresenter = new ListMenuPresenter(builder.getContext(),
                R.layout.popup_menu_item_layout);

        mPresenter.setCallback(this);
        mMenu.addMenuPresenter(mPresenter);
        builder.setAdapter(mPresenter.getAdapter(), this);

        // Set the title
        /*final View headerView = menu.getHeaderView();
        if (headerView != null) {
            // Menu's client has given a custom header view, use it
            builder.setCustomTitle(headerView);
        } else {
            // Otherwise use the (text) title and icon
            builder.setIcon(menu.getHeaderIcon()).setTitle(menu.getHeaderTitle());
        }*/
        
        // Set the key listener
        builder.setOnKeyListener(this);

        // Show the menu
        mDialog = builder.create();
        mDialog.setOnDismissListener(this);

        mDialog.setCanceledOnTouchOutside(true);
        /*NANJISTART::change the menu to tws style */
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        if (windowToken != null) {
            lp.token = windowToken;
        }
        lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        lp.gravity = Gravity.CENTER | Gravity.BOTTOM;
        lp.dimAmount=0.6f;
        lp.windowAnimations = R.style.Animation_DropDownUp_tws;
        /*NANJIEND::change the menu to tws style::froyohuang 2013.4.7*/

        //QROM-START::set the background of the list menu::hendysu::2013-06-17
        if(mDialog.getListView() != null) {
            mDialog.getListView().setBackgroundResource(R.drawable.list_menu_bg_holo_light);
            mDialog.getListView().useShapedSelector(true);
            mDialog.getListView().setSelector(R.drawable.list_menu_selector,
                R.drawable.list_menu_selector_top,
                R.drawable.list_menu_selector_bottom);
        }
        //QROM-END::set the background of the list menu::hendysu::2013-06-17
     
        mDialog.show();
    }
    
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0) {
                Window win = mDialog.getWindow();
                if (win != null) {
                    View decor = win.getDecorView();
                    if (decor != null) {
                        KeyEvent.DispatcherState ds = decor.getKeyDispatcherState();
                        if (ds != null) {
                            ds.startTracking(event, this);
                            return true;
                        }
                    }
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Window win = mDialog.getWindow();
                if (win != null) {
                    View decor = win.getDecorView();
                    if (decor != null) {
                        KeyEvent.DispatcherState ds = decor.getKeyDispatcherState();
                        if (ds != null && ds.isTracking(event)) {
                            mMenu.twsClose(true);
                            dialog.dismiss();
                            return true;
                        }
                    }
                }
            }
        }

        // Menu shortcut matching
        return mMenu.performShortcut(keyCode, event, 0);

    }

    public void setPresenterCallback(MenuPresenter.Callback cb) {
        mPresenterCallback = cb;
    }

    /**
     * Dismisses the menu's dialog.
     * 
     * @see Dialog#dismiss()
     */
    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mPresenter.onCloseMenu(mMenu, true);
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        if (allMenusAreClosing || menu == mMenu) {
            dismiss();
        }
        if (mPresenterCallback != null) {
            mPresenterCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        if (mPresenterCallback != null) {
            return mPresenterCallback.onOpenSubMenu(subMenu);
        }
        return false;
    }

    public void onClick(DialogInterface dialog, int which) {
        mMenu.performItemAction((MenuItemImpl) mPresenter.getAdapter().getItem(which), 0);
    }
}
