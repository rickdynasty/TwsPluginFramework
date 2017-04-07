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


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuItemImpl;
import com.tencent.tws.assistant.internal.view.menu.MenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuView;
import com.tencent.tws.assistant.internal.view.menu.SubMenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder.ItemInvoker;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.sharelib.R;

/**
 * The expanded menu view is a list-like menu with all of the available menu items.  It is opened
 * by the user clicking no the 'More' button on the icon menu view.
 */
public final class ExpandedMenuView extends ListView implements ItemInvoker, MenuView, OnItemClickListener {
    private MenuBuilder mMenu;

    /** Default animations for this menu */
    private int mAnimations;
    
    /**
     * Instantiates the ExpandedMenuView that is linked with the provided MenuBuilder.
     * @param menu The model for the menu which this MenuView will display
     */
    public ExpandedMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MenuView, 0, 0);
        mAnimations = a.getResourceId(R.styleable.MenuView_windowAnimationStyle, 0);
        a.recycle();

        //QROM-START::set the background of the list menu::hendysu::2013-06-17
        setBackgroundResource(R.drawable.list_menu_bg_holo_light);
        //QROM-END::set the background of the list menu::hendysu::2013-06-17

        //QROM-START::set the list menu selector::hendysu::2013-06-17
        //setSelector(R.drawable.second_list_selector_holo_light, true);
        setSelector(R.drawable.list_menu_selector,
            R.drawable.list_menu_selector_top,
            R.drawable.list_menu_selector_bottom);
        //QROM-END::set the list menu selector::hendysu::2013-06-17

        setOnItemClickListener(this);
    }

    public void initialize(MenuBuilder menu) {
        mMenu = menu;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        // Clear the cached bitmaps of children
        setChildrenDrawingCacheEnabled(false);
    }

    public boolean invokeItem(MenuItemImpl item) {
        return mMenu.performItemAction(item, 0);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        invokeItem((MenuItemImpl) getAdapter().getItem(position));
    }

    public int getWindowAnimations() {
        return mAnimations;
    }
    
}
