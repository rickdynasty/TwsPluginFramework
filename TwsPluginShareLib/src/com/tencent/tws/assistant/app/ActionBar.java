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

package com.tencent.tws.assistant.app;

import com.tencent.tws.assistant.internal.widget.ActionBarView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SpinnerAdapter;
import android.app.FragmentTransaction;

/**
 * A window feature at the top of the activity that may display the activity title, navigation
 * modes, and other interactive items.
 * <p>Beginning with Android 3.0 (API level 11), the action bar appears at the top of an
 * activity's window when the activity uses the system's {@link
 * android.R.style#Theme_Holo Holo} theme (or one of its descendant themes), which is the default.
 * You may otherwise add the action bar by calling {@link
 * android.view.Window#requestFeature requestFeature(FEATURE_ACTION_BAR)} or by declaring it in a
 * custom theme with the {@link android.R.styleable#Theme_windowActionBar windowActionBar} property.
 * <p>By default, the action bar shows the application icon on
 * the left, followed by the activity title. If your activity has an options menu, you can make
 * select items accessible directly from the action bar as "action items". You can also
 * modify various characteristics of the action bar or remove it completely.</p>
 * <p>From your activity, you can retrieve an instance of {@link ActionBar} by calling {@link
 * android.app.Activity#getActionBar getActionBar()}.</p>
 * <p>In some cases, the action bar may be overlayed by another bar that enables contextual actions,
 * using an {@link android.view.ActionMode}. For example, when the user selects one or more items in
 * your activity, you can enable an action mode that offers actions specific to the selected
 * items, with a UI that temporarily replaces the action bar. Although the UI may occupy the
 * same space, the {@link android.view.ActionMode} APIs are distinct and independent from those for
 * {@link ActionBar}.
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about how to use the action bar, including how to add action items, navigation
 * modes and more, read the <a href="{@docRoot}guide/topics/ui/actionbar.html">Action
 * Bar</a> developer guide.</p>
 * </div>
 */
public abstract class ActionBar {
    /**
     * Standard navigation mode. Consists of either a logo or icon
     * and title text with an optional subtitle. Clicking any of these elements
     * will dispatch onOptionsItemSelected to the host Activity with
     * a MenuItem with item ID android.R.id.home.
     */
    public static final int NAVIGATION_MODE_STANDARD = 0;
    
    /**
     * List navigation mode. Instead of static title text this mode
     * presents a list menu for navigation within the activity.
     * e.g. this might be presented to the user as a dropdown list.
     */
    public static final int NAVIGATION_MODE_LIST = 1;
    
    /**
     * Tab navigation mode. Instead of static title text this mode
     * presents a series of tabs for navigation within the activity.
     */
    public static final int NAVIGATION_MODE_TABS = 2;

    /**
     * Use logo instead of icon if available. This flag will cause appropriate
     * navigation modes to use a wider logo in place of the standard icon.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public static final int DISPLAY_USE_LOGO = 0x1;
    
    /**
     * Show 'home' elements in this action bar, leaving more space for other
     * navigation elements. This includes logo and icon.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public static final int DISPLAY_SHOW_HOME = 0x2;

    /**
     * Display the 'home' element such that it appears as an 'up' affordance.
     * e.g. show an arrow to the left indicating the action that will be taken.
     *
     * Set this flag if selecting the 'home' button in the action bar to return
     * up by a single level in your UI rather than back to the top level or front page.
     *
     * <p>Setting this option will implicitly enable interaction with the home/up
     * button. See {@link #setHomeButtonEnabled(boolean)}.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public static final int DISPLAY_HOME_AS_UP = 0x4;

    /**
     * Show the activity title and subtitle, if present.
     *
     * @see #setTitle(CharSequence)
     * @see #setTitle(int)
     * @see #setSubtitle(CharSequence)
     * @see #setSubtitle(int)
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public static final int DISPLAY_SHOW_TITLE = 0x8;

    /**
     * Show the custom view if one has been set.
     * @see #setCustomView(View)
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public static final int DISPLAY_SHOW_CUSTOM = 0x10;

    /**
     * Set the action bar into custom navigation mode, supplying a view
     * for custom navigation.
     *
     * Custom navigation views appear between the application icon and
     * any action buttons and may use any space available there. Common
     * use cases for custom navigation views might include an auto-suggesting
     * address bar for a browser or other navigation mechanisms that do not
     * translate well to provided navigation modes.
     *
     * @param view Custom navigation view to place in the ActionBar.
     */
    public abstract void setCustomView(View view);

    /**
     * Set the action bar into custom navigation mode, supplying a view
     * for custom navigation.
     * 
     * <p>Custom navigation views appear between the application icon and
     * any action buttons and may use any space available there. Common
     * use cases for custom navigation views might include an auto-suggesting
     * address bar for a browser or other navigation mechanisms that do not
     * translate well to provided navigation modes.</p>
     *
     * <p>The display option {@link #DISPLAY_SHOW_CUSTOM} must be set for
     * the custom view to be displayed.</p>
     * 
     * @param view Custom navigation view to place in the ActionBar.
     * @param layoutParams How this custom view should layout in the bar.
     *
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setCustomView(View view, LayoutParams layoutParams);

    /**
     * Set the action bar into custom navigation mode, supplying a view
     * for custom navigation.
     *
     * <p>Custom navigation views appear between the application icon and
     * any action buttons and may use any space available there. Common
     * use cases for custom navigation views might include an auto-suggesting
     * address bar for a browser or other navigation mechanisms that do not
     * translate well to provided navigation modes.</p>
     *
     * <p>The display option {@link #DISPLAY_SHOW_CUSTOM} must be set for
     * the custom view to be displayed.</p>
     *
     * @param resId Resource ID of a layout to inflate into the ActionBar.
     *
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setCustomView(int resId);

    /**
     * Set the icon to display in the 'home' section of the action bar.
     * The action bar will use an icon specified by its style or the
     * activity icon by default.
     *
     * Whether the home section shows an icon or logo is controlled
     * by the display option {@link #DISPLAY_USE_LOGO}.
     *
     * @param resId Resource ID of a drawable to show as an icon.
     *
     * @see #setDisplayUseLogoEnabled(boolean)
     * @see #setDisplayShowHomeEnabled(boolean)
     */
    public abstract void setIcon(int resId);

    /**
     * Set the icon to display in the 'home' section of the action bar.
     * The action bar will use an icon specified by its style or the
     * activity icon by default.
     *
     * Whether the home section shows an icon or logo is controlled
     * by the display option {@link #DISPLAY_USE_LOGO}.
     *
     * @param icon Drawable to show as an icon.
     *
     * @see #setDisplayUseLogoEnabled(boolean)
     * @see #setDisplayShowHomeEnabled(boolean)
     */
    public abstract void setIcon(Drawable icon);

    /**
     * Set the logo to display in the 'home' section of the action bar.
     * The action bar will use a logo specified by its style or the
     * activity logo by default.
     *
     * Whether the home section shows an icon or logo is controlled
     * by the display option {@link #DISPLAY_USE_LOGO}.
     *
     * @param resId Resource ID of a drawable to show as a logo.
     *
     * @see #setDisplayUseLogoEnabled(boolean)
     * @see #setDisplayShowHomeEnabled(boolean)
     */
    public abstract void setLogo(int resId);

    /**
     * Set the logo to display in the 'home' section of the action bar.
     * The action bar will use a logo specified by its style or the
     * activity logo by default.
     *
     * Whether the home section shows an icon or logo is controlled
     * by the display option {@link #DISPLAY_USE_LOGO}.
     *
     * @param logo Drawable to show as a logo.
     *
     * @see #setDisplayUseLogoEnabled(boolean)
     * @see #setDisplayShowHomeEnabled(boolean)
     */
    public abstract void setLogo(Drawable logo);

    /**
     * Set the adapter and navigation callback for list navigation mode.
     *
     * The supplied adapter will provide views for the expanded list as well as
     * the currently selected item. (These may be displayed differently.)
     *
     * The supplied OnNavigationListener will alert the application when the user
     * changes the current list selection.
     *
     * @param adapter An adapter that will provide views both to display
     *                the current navigation selection and populate views
     *                within the dropdown navigation menu.
     * @param callback An OnNavigationListener that will receive events when the user
     *                 selects a navigation item.
     */
    public abstract void setListNavigationCallbacks(SpinnerAdapter adapter,
            OnNavigationListener callback);

    /**
     * Set the selected navigation item in list or tabbed navigation modes.
     *
     * @param position Position of the item to select.
     */
    public abstract void setSelectedNavigationItem(int position);

    /**
     * Get the position of the selected navigation item in list or tabbed navigation modes.
     *
     * @return Position of the selected item.
     */
    public abstract int getSelectedNavigationIndex();

    /**
     * Get the number of navigation items present in the current navigation mode.
     *
     * @return Number of navigation items.
     */
    public abstract int getNavigationItemCount();

    /**
     * Set the action bar's title. This will only be displayed if
     * {@link #DISPLAY_SHOW_TITLE} is set.
     *
     * @param title Title to set
     *
     * @see #setTitle(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setTitle(CharSequence title);

    /**
     * Set the action bar's title. This will only be displayed if
     * {@link #DISPLAY_SHOW_TITLE} is set.
     *
     * @param resId Resource ID of title string to set
     *
     * @see #setTitle(CharSequence)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setTitle(int resId);

    /**
     * Set the action bar's subtitle. This will only be displayed if
     * {@link #DISPLAY_SHOW_TITLE} is set. Set to null to disable the
     * subtitle entirely.
     *
     * @param subtitle Subtitle to set
     *
     * @see #setSubtitle(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setSubtitle(CharSequence subtitle);

    /**
     * Set the action bar's subtitle. This will only be displayed if
     * {@link #DISPLAY_SHOW_TITLE} is set.
     *
     * @param resId Resource ID of subtitle string to set
     *
     * @see #setSubtitle(CharSequence)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setSubtitle(int resId);

    /**
     * Set display options. This changes all display option bits at once. To change
     * a limited subset of display options, see {@link #setDisplayOptions(int, int)}.
     * 
     * @param options A combination of the bits defined by the DISPLAY_ constants
     *                defined in ActionBar.
     */
    public abstract void setDisplayOptions(int options);
    
    /**
     * Set selected display options. Only the options specified by mask will be changed.
     * To change all display option bits at once, see {@link #setDisplayOptions(int)}.
     * 
     * <p>Example: setDisplayOptions(0, DISPLAY_SHOW_HOME) will disable the
     * {@link #DISPLAY_SHOW_HOME} option.
     * setDisplayOptions(DISPLAY_SHOW_HOME, DISPLAY_SHOW_HOME | DISPLAY_USE_LOGO)
     * will enable {@link #DISPLAY_SHOW_HOME} and disable {@link #DISPLAY_USE_LOGO}.
     * 
     * @param options A combination of the bits defined by the DISPLAY_ constants
     *                defined in ActionBar.
     * @param mask A bit mask declaring which display options should be changed.
     */
    public abstract void setDisplayOptions(int options, int mask);

    /**
     * Set whether to display the activity logo rather than the activity icon.
     * A logo is often a wider, more detailed image.
     *
     * <p>To set several display options at once, see the setDisplayOptions methods.
     *
     * @param useLogo true to use the activity logo, false to use the activity icon.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setDisplayUseLogoEnabled(boolean useLogo);

    /**
     * Set whether to include the application home affordance in the action bar.
     * Home is presented as either an activity icon or logo.
     *
     * <p>To set several display options at once, see the setDisplayOptions methods.
     *
     * @param showHome true to show home, false otherwise.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setDisplayShowHomeEnabled(boolean showHome);

    /**
     * Set whether home should be displayed as an "up" affordance.
     * Set this to true if selecting "home" returns up by a single level in your UI
     * rather than back to the top level or front page.
     *
     * <p>To set several display options at once, see the setDisplayOptions methods.
     *
     * @param showHomeAsUp true to show the user that selecting home will return one
     *                     level up rather than to the top level of the app.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setDisplayHomeAsUpEnabled(boolean showHomeAsUp);

    /**
     * Set whether an activity title/subtitle should be displayed.
     *
     * <p>To set several display options at once, see the setDisplayOptions methods.
     *
     * @param showTitle true to display a title/subtitle if present.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setDisplayShowTitleEnabled(boolean showTitle);

    /**
     * Set whether a custom view should be displayed, if set.
     *
     * <p>To set several display options at once, see the setDisplayOptions methods.
     *
     * @param showCustom true if the currently set custom view should be displayed, false otherwise.
     *
     * @see #setDisplayOptions(int)
     * @see #setDisplayOptions(int, int)
     */
    public abstract void setDisplayShowCustomEnabled(boolean showCustom);

    /**
     * Set the ActionBar's background. This will be used for the primary
     * action bar.
     * 
     * @param d Background drawable
     * @see #setStackedBackgroundDrawable(Drawable)
     * @see #setSplitBackgroundDrawable(Drawable)
     */
    public abstract void setBackgroundDrawable(Drawable d);

    /**
     * Set the ActionBar's stacked background. This will appear
     * in the second row/stacked bar on some devices and configurations.
     *
     * @param d Background drawable for the stacked row
     */
    public void setStackedBackgroundDrawable(Drawable d) { }

    /**
     * Set the ActionBar's split background. This will appear in
     * the split action bar containing menu-provided action buttons
     * on some devices and configurations.
     * <p>You can enable split action bar with {@link android.R.attr#uiOptions}
     *
     * @param d Background drawable for the split bar
     */
    public void setSplitBackgroundDrawable(Drawable d) { }

    /**
     * @return The current custom view.
     */
    public abstract View getCustomView();

    /**
     * Returns the current ActionBar title in standard mode.
     * Returns null if {@link #getNavigationMode()} would not return
     * {@link #NAVIGATION_MODE_STANDARD}. 
     *
     * @return The current ActionBar title or null.
     */
    public abstract CharSequence getTitle();
    
    /**
     * Returns the current ActionBar subtitle in standard mode.
     * Returns null if {@link #getNavigationMode()} would not return
     * {@link #NAVIGATION_MODE_STANDARD}. 
     *
     * @return The current ActionBar subtitle or null.
     */
    public abstract CharSequence getSubtitle();
    
    /**
     * Returns the current navigation mode. The result will be one of:
     * <ul>
     * <li>{@link #NAVIGATION_MODE_STANDARD}</li>
     * <li>{@link #NAVIGATION_MODE_LIST}</li>
     * <li>{@link #NAVIGATION_MODE_TABS}</li>
     * </ul>
     *
     * @return The current navigation mode.
     */
    public abstract int getNavigationMode();

    /**
     * Set the current navigation mode.
     *
     * @param mode The new mode to set.
     * @see #NAVIGATION_MODE_STANDARD
     * @see #NAVIGATION_MODE_LIST
     * @see #NAVIGATION_MODE_TABS
     */
    public abstract void setNavigationMode(int mode);

    /**
     * @return The current set of display options. 
     */
    public abstract int getDisplayOptions();

    /**
     * Create and return a new {@link Tab}.
     * This tab will not be included in the action bar until it is added.
     *
     * <p>Very often tabs will be used to switch between {@link Fragment}
     * objects.  Here is a typical implementation of such tabs:</p>
     *
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/FragmentTabs.java
     *      complete}
     *
     * @return A new Tab
     *
     * @see #addTab(Tab)
     */
    public abstract Tab newTab();

    /**
     * Add a tab for use in tabbed navigation mode. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public abstract void addTab(Tab tab);

    /**
     * Add a tab for use in tabbed navigation mode. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public abstract void addTab(Tab tab, boolean setSelected);

    /**
     * Add a tab for use in tabbed navigation mode. The tab will be inserted at
     * <code>position</code>. If this is the first tab to be added it will become
     * the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public abstract void addTab(Tab tab, int position);

    /**
     * Add a tab for use in tabbed navigation mode. The tab will be insterted at
     * <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public abstract void addTab(Tab tab, int position, boolean setSelected);

    /**
     * Remove a tab from the action bar. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public abstract void removeTab(Tab tab);

    /**
     * Remove a tab from the action bar. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public abstract void removeTabAt(int position);

    /**
     * Remove all tabs from the action bar and deselect the current tab.
     */
    public abstract void removeAllTabs();

    /**
     * Select the specified tab. If it is not a child of this action bar it will be added.
     *
     * <p>Note: If you want to select by index, use {@link #setSelectedNavigationItem(int)}.</p>
     *
     * @param tab Tab to select
     */
    public abstract void selectTab(Tab tab);

    /**
     * Returns the currently selected tab if in tabbed navigation mode and there is at least
     * one tab present.
     *
     * @return The currently selected tab or null
     */
    public abstract Tab getSelectedTab();

    /**
     * Returns the tab at the specified index.
     *
     * @param index Index value in the range 0-get
     * @return
     */
    public abstract Tab getTabAt(int index);

    /**
     * Returns the number of tabs currently registered with the action bar.
     * @return Tab count
     */
    public abstract int getTabCount();

    /**
     * Retrieve the current height of the ActionBar.
     *
     * @return The ActionBar's height
     */
    public abstract int getHeight();

    /**
     * Show the ActionBar if it is not currently showing.
     * If the window hosting the ActionBar does not have the feature
     * {@link Window#FEATURE_ACTION_BAR_OVERLAY} it will resize application
     * content to fit the new space available.
     */
    public abstract void show();
    /*NANJISTART::add::geofffeng::20120208*/
    public void setHomeButtonEnabledQQ(boolean enable) {};
    public void setHomeButtonIconEnabled(boolean enable) {};
    public void setActionMenuItemCount(int itemCount) {};
    public void splitActionbar_show(){};
    public void splitActionbar_Init(boolean isShow){};
    public void setEmbededTabEnable(boolean tabEnable){};
    public void splitActionbar_hide(){};
    public boolean splitActionbarIsHide(){return false;};
    public void setUserCloseImageType(int imageType){};
    public void setActionModeSimple(int modeType){};
    public void actionModeManualFinish(){};
    public static final int DISPLAY_DEFAULT_IMAGE = 0;
    public static final int DISPLAY_USER_IMAGE = 1;
    public static final int ACTIONMODE_DEFAULT = 0;
    public static final int ACTIONMODE_SIMPLE = 1;
    //tws-start add for actionTab::2015-1-7
    /**
     * ACTIONBAR_TAB_STANDARD statusBar is opaque and no title
     * ACTIONBAR_TAB_STANDARD_SECOND statusBar is opaque and has title
     * ACTIONBAR_TAB_OVERLAY statusBar is transparent and no title
     * ACTIONBAR_TAB_OVERLAY_SECOND statusBar is transparent and has title
     */
    public static final int ACTIONBAR_TAB_STANDARD = 1;
    public static final int ACTIONBAR_TAB_STANDARD_SECOND = 2;
    public static final int ACTIONBAR_TAB_OVERLAY = 3;
    public static final int ACTIONBAR_TAB_OVERLAY_SECOND = 4;
    //tws-end add for actionTab::2015-1-7

    public interface Callback {
       //
        public void onCreatActionMode();
        public void onDestroyActionMode();
    }
	/*NANJI-START::add::haoranma::2012-09-25*/
	public void twsSetCustomListHeight(int height){};
	/*NANJI-END::add::haoranma::2012-09-25*/
    public void setActionModeCallback(ActionBar.Callback callback){};

    //NANJISTART alanhuang 20130411
    public View getMultiChoiceView(){
    	return null;
    }
    
    public ImageView getRightButtonView(){
    	return null;
    }
    
    // tws-start add smooth scroll feature to tab::2014-11-18
    public void twsSetPageScroll(int position, float positionOffset){};
	public void twsSetScrollEnd(){};
    // tws-end add smooth scroll feature to tab ::2014-11-18
    
    // tws-start add actionbar0.2 feature::2014-09-28
    public View getCloseView(){
    	return null;
    }
    public EditText getEditView() {
    	return null;
    }
    public View getMultiChoiceView(boolean isActionMode){
    	return null;
    }
    public View getCloseView(boolean isActionMode){
    	return null;
    }
    public EditText getEditView(boolean isActionMode) {
    	return null;
    }
    public void setActionBarBGColor(int resId) {
    	return;
    }
    public void setActionBarBGColor2(int color){
    	return;
    }
    public void setActionModeBGColor(int resId) {
    	return;
    }
	public TextView getTitleView(boolean isActionMode) {
    	return null;
    }
    public TextView getSubtitleView(boolean isActionMode) {
    	return null;
    }

	//tws-start ActionBar homeLayout interface::2014-12-26
    public ImageButton getActionBarHome() {
    	return null;
    }
    public ActionBarView getActionBarView() {
    	return null;
    }
	//tws-end ActionBar homeLayout interface::2014-12-26
    
    public boolean startMiniMode() {
    	return false;
    }
    public boolean exitMiniMode() {
    	return false;
    }

	public int getBackgroundResId() {
    	return 0;
    }
	
	public void setOverflowButtonState(boolean isActionMode, boolean enable) {
		return;
	}
	public void setTopOverflowButtonState(boolean enable) {
		return;
	}
    // tws-end add actionbar0.2 feature::2014-09-28
    
	//NANJIEND alanhuang 20130411
    //public ActionMode startActionModeSimple(ActionMode.Callback callback) {return null;};
    /*NANJIEND::add::geofffeng::20120208*/
    //tws-start BottomBar Height::2014-7-23
    public abstract int getBottomBarHeight();
    //tws-end BottomBar Height::2014-7-23
    //tws-start ActionBar ShowHideAnimation::2014-7-28
    public void setShowHideAnimationEnabled(boolean enabled) {}
    //tws-end ActionBar ShowHideAnimation::2014-7-28
    //tws-start ActionBar Back Button::2014-7-30
    public void twsSetBackOnclickEnabled(boolean enabled){}
    public boolean twsGetBackOnclickEnabled(){
        return true;
    }
    //tws-end ActionBar Back Button::2014-7-30
    //tws-start ActionMode BackBtn OnClickListener::2014-8-7
    public void twsSetActionModeBackOnClickListener(OnClickListener clickListener){}
    //tws-end ActionMode BackBtn OnClickListener::2014-8-7
    //tws-start add for actionbar tab::2015-1-7
    public void twsSetActionBarTabHasTitle(boolean hasTitle){}
    //tws-end add for actionbar tab::2015-1-7
    /**
     * Hide the ActionBar if it is currently showing.
     * If the window hosting the ActionBar does not have the feature
     * {@link Window#FEATURE_ACTION_BAR_OVERLAY} it will resize application
     * content to fit the new space available.
     */
    public abstract void hide();
    /**
     * @return <code>true</code> if the ActionBar is showing, <code>false</code> otherwise.
     */
    public abstract boolean isShowing();

    /**
     * Add a listener that will respond to menu visibility change events.
     *
     * @param listener The new listener to add
     */
    public abstract void addOnMenuVisibilityListener(OnMenuVisibilityListener listener);

    /**
     * Remove a menu visibility listener. This listener will no longer receive menu
     * visibility change events.
     *
     * @param listener A listener to remove that was previously added
     */
    public abstract void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener);

    /**
     * Enable or disable the "home" button in the corner of the action bar. (Note that this
     * is the application home/up affordance on the action bar, not the systemwide home
     * button.)
     *
     * <p>This defaults to true for packages targeting &lt; API 14. For packages targeting
     * API 14 or greater, the application should call this method to enable interaction
     * with the home/up affordance.
     *
     * <p>Setting the {@link #DISPLAY_HOME_AS_UP} display option will automatically enable
     * the home button.
     *
     * @param enabled true to enable the home button, false to disable the home button.
     */
    public void setHomeButtonEnabled(boolean enabled) { }

    /**
     * Returns a {@link Context} with an appropriate theme for creating views that
     * will appear in the action bar. If you are inflating or instantiating custom views
     * that will appear in an action bar, you should use the Context returned by this method.
     * (This includes adapters used for list navigation mode.)
     * This will ensure that views contrast properly against the action bar.
     *
     * @return A themed Context for creating views
     */
    public Context getThemedContext() { return null; }

    /**
     * Listener interface for ActionBar navigation events.
     */
    public interface OnNavigationListener {
        /**
         * This method is called whenever a navigation item in your action bar
         * is selected.
         *    
         * @param itemPosition Position of the item clicked.
         * @param itemId ID of the item clicked.
         * @return True if the event was handled, false otherwise.
         */
        public boolean onNavigationItemSelected(int itemPosition, long itemId);
    }

    /**
     * Listener for receiving events when action bar menus are shown or hidden.
     */
    public interface OnMenuVisibilityListener {
        /**
         * Called when an action bar menu is shown or hidden. Applications may want to use
         * this to tune auto-hiding behavior for the action bar or pause/resume video playback,
         * gameplay, or other activity within the main content area.
         *
         * @param isVisible True if an action bar menu is now visible, false if no action bar
         *                  menus are visible.
         */
        public void onMenuVisibilityChanged(boolean isVisible);
    }

    /**
     * A tab in the action bar.
     *
     * <p>Tabs manage the hiding and showing of {@link Fragment}s.
     */
    public static abstract class Tab {
        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        /**
         * Return the current position of this tab in the action bar.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
         *         the action bar.
         */
        public abstract int getPosition();

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        public abstract Drawable getIcon();

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        public abstract CharSequence getText();

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        public abstract Tab setIcon(Drawable icon);

        /**
         * Set the icon displayed on this tab.
         *
         * @param resId Resource ID referring to the drawable to use as an icon
         * @return The current instance for call chaining
         */
        public abstract Tab setIcon(int resId);

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not
         * room to display the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        public abstract Tab setText(CharSequence text);

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not
         * room to display the entire string.
         *
         * @param resId A resource ID referring to the text that should be displayed
         * @return The current instance for call chaining
         */
        public abstract Tab setText(int resId);

        /**
         * Set a custom view to be used for this tab. This overrides values set by
         * {@link #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        public abstract Tab setCustomView(View view);

        /**
         * Set a custom view to be used for this tab. This overrides values set by
         * {@link #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param layoutResId A layout resource to inflate and use as a custom tab view
         * @return The current instance for call chaining
         */
        public abstract Tab setCustomView(int layoutResId);

        /**
         * Retrieve a previously set custom view for this tab.
         *
         * @return The custom view set by {@link #setCustomView(View)}.
         */
        public abstract View getCustomView();

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param obj Object to store
         * @return The current instance for call chaining
         */
        public abstract Tab setTag(Object obj);

        /**
         * @return This Tab's tag object.
         */
        public abstract Object getTag();

        /**
         * Set the {@link TabListener} that will handle switching to and from this tab.
         * All tabs must have a TabListener set before being added to the ActionBar.
         *
         * @param listener Listener to handle tab selection events
         * @return The current instance for call chaining
         */
        public abstract Tab setTabListener(TabListener listener);

        /**
         * Select this tab. Only valid if the tab has been added to the action bar.
         */
        public abstract void select();

        /**
         * Set a description of this tab's content for use in accessibility support.
         * If no content description is provided the title will be used.
         *
         * @param resId A resource ID referring to the description text
         * @return The current instance for call chaining
         * @see #setContentDescription(CharSequence)
         * @see #getContentDescription()
         */
        public abstract Tab setContentDescription(int resId);

        /**
         * Set a description of this tab's content for use in accessibility support.
         * If no content description is provided the title will be used.
         *
         * @param contentDesc Description of this tab's content
         * @return The current instance for call chaining
         * @see #setContentDescription(int)
         * @see #getContentDescription()
         */
        public abstract Tab setContentDescription(CharSequence contentDesc);

        /**
         * Gets a brief description of this tab's content for use in accessibility support.
         *
         * @return Description of this tab's content
         * @see #setContentDescription(CharSequence)
         * @see #setContentDescription(int)
         */
        public abstract CharSequence getContentDescription();
    }

    /**
     * Callback interface invoked when a tab is focused, unfocused, added, or removed.
     */
    public interface TabListener {
        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        during a tab switch. The previous tab's unselect and this tab's select will be
         *        executed in a single transaction. This FragmentTransaction does not support
         *        being added to the back stack.
         */
        public void onTabSelected(Tab tab, FragmentTransaction ft);

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        during a tab switch. This tab's unselect and the newly selected tab's select
         *        will be executed in a single transaction. This FragmentTransaction does not
         *        support being added to the back stack.
         */
        public void onTabUnselected(Tab tab, FragmentTransaction ft);

        /**
         * Called when a tab that is already selected is chosen again by the user.
         * Some applications may use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         * @param ft A {@link FragmentTransaction} for queuing fragment operations to execute
         *        once this method returns. This FragmentTransaction does not support
         *        being added to the back stack.
         */
        public void onTabReselected(Tab tab, FragmentTransaction ft);
    }
    
    // tws-start modify ActionMode Blur Listener::2014-12-12
    public interface SplitBlurListener {
    	public void doBlur();
    }
    // tws-end modify ActionMode Blur Listener::2014-12-12
    
    // tws-start add overflow click listener::2014-12-18
    public interface OverflowClickListener {
    	public void doClick();
    }
    // tws-end add overflow click listener::2014-12-18
    // tws-start add showhide actionbar listener::2015-1-14
    protected boolean mActionBarAnimTrans = false;
    protected boolean mActionBarHideNoAnim = false;
    public static boolean mIsInActionMode = false;
    public interface ActionBarHideListener {
    	public void doHideBar();
    }
    public interface ActionBarShowListener {
    	public void doShowBar();
    }
    public interface ActionModeFinishListener {
    	public void doFinish();
    }
    // tws-end add showhide actionbar listener::2015-1-14
    
    public abstract void setSplitBlurListener(ActionBar.SplitBlurListener listener);
    public abstract void setOverflowClickListener(ActionBar.OverflowClickListener listener, boolean isActionMode);
    // tws-start add Overflow interface::2015-2-9
    public abstract View getOverflowButton(boolean isActionMode);
    // tws-end add Overflow interface::2015-2-9
    // tws-start add showhide actionbar listener::2015-1-14
    public abstract void setActionBarShowListener(ActionBar.ActionBarShowListener listener);
    public abstract void setActionBarHideListener(ActionBar.ActionBarHideListener listener);
    public abstract void setActionModeFinishListener(ActionBar.ActionModeFinishListener listener);
    public abstract void setActionBarAnimTrans(boolean animTrans);
    public abstract void setActionBarHideNoAnim(boolean flag);
    // tws-end add showhide actionbar listener::2015-1-14
    // tws-start add transPopup interface::2015-3-10
    public abstract void setIsTransPopup(boolean isActionMode, boolean isTransPopup);
    // tws-end add transPopup interface::2015-3-10
    // tws-start add PopupMenuRedPoint interface::2015-3-12
    public abstract void setPopupMenuMarks(boolean isActionMode, boolean[] isMarks);
    // tws-end add PopupMenuRedPoint interface::2015-3-12
    // tws-start add Overflow clickDelay interface::2015-3-19
    public abstract void setOverflowDelay(boolean isActionMode, boolean isDelay);
    public abstract void setTopOverflowDelay(boolean isDelay);
    // tws-end add Overflow clickDelay interface::2015-3-19
    
    // tws-start add Menu Config flag::2015-3-31
    public abstract void setMenuConfigFlag(boolean isConfig);
    // tws-end add Menu Config flag::2015-3-31
    
    // tws-start add interface for tabclick::2015-5-5
    public abstract void enableTabClick(boolean isEnable);
    // tws-end add interface for tabclick::2015-5-5
    
    // tws-start add PopupMenuText interface::2015-5-18
    public abstract void setPopupTextColors(boolean isActionMode, int[] textColors);
    // tws-end add PopupMenuText interface::2015-5-18
    
    // tws-start add actionbar tab wave interface::2015-9-28
    public abstract void twsSetPageSelected(int position);
    public abstract void twsSetTabWaveEnable(boolean enabled);
    public abstract void twsSetTabTextSelectChange(boolean change);
    public abstract void twsSetTabCustomEnable(boolean enable);
    
    public abstract void twsSetTabLeftView(View view);
    public abstract void twsSetTabRightView(View view);
    public abstract void twsSetTabCustomEnd();
    // tws-end add actionbar tab wave interface::2015-9-28
    
    // tws-start add top actionbar showhide interface::2015-11-19
    public abstract void topActionbar_show();
    public abstract void topActionbar_hide();
    
    public abstract void topActionbar_alphashow();
    public abstract void topActionbar_alphahide();
    
    public abstract void actionbar_alphashow();
    public abstract void actionbar_alphahide();
    // tws-end add top actionbar showhide interface::2015-11-19

    /**
     * Per-child layout information associated with action bar custom views.
     *
     * @attr ref android.R.styleable#ActionBar_LayoutParams_layout_gravity
     */
    public static class LayoutParams extends MarginLayoutParams {
        /**
         * Gravity for the view associated with these LayoutParams.
         *
         * @see android.view.Gravity
         */
        @ViewDebug.ExportedProperty(category = "layout", mapping = {
            @ViewDebug.IntToString(from =  -1,                       to = "NONE"),
            @ViewDebug.IntToString(from = Gravity.NO_GRAVITY,        to = "NONE"),
            @ViewDebug.IntToString(from = Gravity.TOP,               to = "TOP"),
            @ViewDebug.IntToString(from = Gravity.BOTTOM,            to = "BOTTOM"),
            @ViewDebug.IntToString(from = Gravity.LEFT,              to = "LEFT"),
            @ViewDebug.IntToString(from = Gravity.RIGHT,             to = "RIGHT"),
            @ViewDebug.IntToString(from = Gravity.CENTER_VERTICAL,   to = "CENTER_VERTICAL"),
            @ViewDebug.IntToString(from = Gravity.FILL_VERTICAL,     to = "FILL_VERTICAL"),
            @ViewDebug.IntToString(from = Gravity.CENTER_HORIZONTAL, to = "CENTER_HORIZONTAL"),
            @ViewDebug.IntToString(from = Gravity.FILL_HORIZONTAL,   to = "FILL_HORIZONTAL"),
            @ViewDebug.IntToString(from = Gravity.CENTER,            to = "CENTER"),
            @ViewDebug.IntToString(from = Gravity.FILL,              to = "FILL")
        })
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs,
                    com.android.internal.R.styleable.ActionBar_LayoutParams);
            gravity = a.getInt(
                    com.android.internal.R.styleable.ActionBar_LayoutParams_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(WRAP_CONTENT, MATCH_PARENT, gravity);
        }

        public LayoutParams(LayoutParams source) {
            super(source);

            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
