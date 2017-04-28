package android.app;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.internal.view.menu.ContextMenuBuilder;
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.gaussblur.JNIBlur;
import com.tencent.tws.assistant.internal.app.ActionBarImpl;
import com.tencent.tws.assistant.internal.view.menu.ListMenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuDialogHelper;
import com.tencent.tws.assistant.internal.view.menu.MenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuView;
import com.tencent.tws.assistant.internal.view.menu.StandaloneActionMode;
import com.tencent.tws.assistant.internal.widget.ActionBarContainer;
import com.tencent.tws.assistant.internal.widget.ActionBarContextView;
import com.tencent.tws.assistant.internal.widget.ActionBarView;
import com.tencent.tws.assistant.utils.ReflectUtils;
import com.tencent.tws.assistant.utils.ResIdentifierUtils;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;
import com.tws.plugin.core.android.TwsActivityInterface;

public class TwsActivity extends Activity implements TwsActivityInterface{
	private static final String TAG = "TwsActivity";
	private static int mStatusBarHeight = 0;
	private static int mTwsStatusBarHeight = 0;

	/* package */ActionBarImpl mActionBar = null;
	ActionBarView mActionBarView = null;
	private MenuInflater mMenuInflater;
	ViewGroup mContentParent = null;
	ViewGroup mPhoneWindowContent = null;
	TwsContentView mTwsRootView = null;// tws's root view, handle message for
										// tws
	View mActivityView = null;

	private boolean mTitleReady = false;
	private CharSequence mTitle;

	private boolean mStatusBarOverlay;
	private boolean mActionModeOverLayBgIsBlur = false;
	private Bitmap mTopScreenBlurBitmap = null;
	private Bitmap mBottomScreenBlurBitmap = null;

	// DEFAULT IS false, DO NOT MODIFY.
	private boolean mCustomSplitWhenNarrow = false;

	private static class ManagedDialog {
		TwsDialog mDialog;
		Bundle mArgs;
	}

	private SparseArray<ManagedDialog> mManagedDialogs;

	private boolean mLinearMode;
	private MenuBuilder mMenu;
	public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;

	// tws-start add for statusbar::2015-1-4
	public static final String STATUSBAR_COLOR_WHITE = "black";
	public static final String STATUSBAR_COLOR_BLACK = "white";
	// tws-end add for statusbar::2015-1-4

	private static final boolean IS_SUPPORT_CUSTOM_THEME = true;
	public static final String KEY_CUSTOM_THEME = "CustomTheme";

	// if is setContentView removeOldView must be true,addContentView must be
	// false
	private void twsGenerateContentParent() {
		if (mContentParent == null) {
			LayoutInflater layoutinflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			boolean hasOverlayActionbar = getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
			boolean hasOverlayActionMode = getWindow().hasFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
			if (!mLinearMode) {
				if (hasOverlayActionbar) {
					mActivityView = layoutinflater.inflate(R.layout.content_action_bar_overlay, null);
				} else if (hasOverlayActionMode) {
					mActivityView = layoutinflater.inflate(R.layout.content_overlay_action_mode, null);
				} else {
					mActivityView = layoutinflater.inflate(R.layout.content_action_bar, null);
				}
			} else {
				mActivityView = layoutinflater.inflate(R.layout.content_action_bar_linearmode, null);
			}

			if (mActivityView == null) {
				throw new AndroidRuntimeException("twsAddContentView twsActionbar creat fail");
			}
			mContentParent = (ViewGroup) mActivityView.findViewById(android.R.id.content);
			if (mContentParent == null) {
				throw new AndroidRuntimeException("twsAddContentView no contenParent");
			}
			mActionBarView = (ActionBarView) mActivityView.findViewById(R.id.tws_action_bar);
			final int tws_action_bar_height = (int) getResources().getDimension(R.dimen.tws_action_bar_height);
			mActionBarView
					.setContentHeight(getResources().getBoolean(R.bool.config_statusbar_state) ? tws_action_bar_height
							+ getStatusBarHeight() : tws_action_bar_height);
			mActionBarView.setPadding(0, mStatusBarOverlay ? getStatusBarHeight() : 0, 0, 0);
			// if (mActionBarView == null) {
			// throw new
			// AndroidRuntimeException("twsAddContentView no mActionBarView");
			// }

			// make TwsActivity support NoActionBar theme
			TypedArray a = obtainStyledAttributes(R.styleable.Theme);
			boolean withActionBar = a.getBoolean(R.styleable.Theme_windowActionBar, true);
			Log.i(TAG, "withActionBar = " + withActionBar);
			if (!withActionBar) {
				mActionBarView.setVisibility(View.GONE);
			}

		} else {
			mContentParent.removeAllViews();
		}

		if (mTwsRootView == null) {
			mTwsRootView = new TwsContentView(getWindow().getContext());
			if (mTwsRootView == null) {
				throw new AndroidRuntimeException("twsAddContentView mTwsRootView creat fail");
			}
		} else {
			mTwsRootView.removeAllViews();
		}
		mTwsRootView.addView(mActivityView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		if (mPhoneWindowContent == null) {
			mPhoneWindowContent = (ViewGroup) getWindow().getDecorView();
			if (mPhoneWindowContent == null) {
				throw new AndroidRuntimeException("twsAddContentView contentView creat fail");
			}
		}
		mPhoneWindowContent.removeAllViews();
		mPhoneWindowContent.addView(mTwsRootView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		mActionBarView.setWindowCallback(getWindow().getCallback());
		boolean splitActionBar = false;
		boolean splitWhenNarrow = false;
		ActivityInfo info = getActivityInfo();
		if (info != null) {
			splitWhenNarrow = (info.uiOptions & ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW) != 0;
		}
		if (splitWhenNarrow || mCustomSplitWhenNarrow) {
			splitActionBar = getResources().getBoolean(R.bool.split_action_bar_is_narrow);
		} else {
			splitActionBar = false;
		}
		final ActionBarContainer splitView = (ActionBarContainer) mActivityView.findViewById(R.id.tws_split_action_bar);
		if (splitView != null) {
			mActionBarView.setSplitView(splitView);
			mActionBarView.setSplitActionBar(splitActionBar);
			mActionBarView.setSplitWhenNarrow(splitWhenNarrow);
			final ActionBarContextView cab = (ActionBarContextView) mActivityView
					.findViewById(R.id.tws_action_context_bar);
			cab.setSplitView(splitView);
			cab.setSplitActionBar(splitActionBar);
			cab.setSplitWhenNarrow(splitWhenNarrow);
		} else {
			Log.e(TAG, "Requested split action bar with " + "incompatible window decor! Ignoring request.");
		}
	}

	public static int getStatusBarHeight() {
		return mStatusBarHeight == 0 ? mTwsStatusBarHeight : mStatusBarHeight;
	}

	private void getSysStatusBarHeight() {
		if (mStatusBarHeight == 0) {
			Rect rect = new Rect();
			getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
			mStatusBarHeight = rect.top;
			if (0 == mStatusBarHeight) {
				try {
					Class<?> localClass = Class.forName("com.android.internal.R$dimen");
					Object object = localClass.newInstance();
					int height = Integer.parseInt(localClass.getField("status_bar_height").get(object).toString());
					mStatusBarHeight = getResources().getDimensionPixelSize(height);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (mTwsStatusBarHeight == 0) {
			mTwsStatusBarHeight = (int) getResources().getDimension(R.dimen.status_bar_height);
		}
	}

	public final ViewGroup getTwsContentView() {
		if (mContentParent == null) {
			twsGenerateContentParent();
		}
		return mContentParent;
	}

	@Override
	public void setContentView(int layoutResID) {
		if (mContentParent == null) {
			twsGenerateContentParent();
		} else {
			mContentParent.removeAllViews();
		}
		getWindow().getLayoutInflater().inflate(layoutResID, mContentParent);
		if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
			getWindow().getCallback().onContentChanged();
		}

		initActionBar();
	}

	@Override
	public void setContentView(View view) {
		setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
	}

	@Override
	public void setContentView(View view, ViewGroup.LayoutParams params) {
		if (mContentParent == null) {
			twsGenerateContentParent();
		} else {
			mContentParent.removeAllViews();
		}
		mContentParent.addView(view, params);
		if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
			getWindow().getCallback().onContentChanged();
		}

		initActionBar();
	}

	@Override
	public void addContentView(View view, ViewGroup.LayoutParams params) {
		if (mContentParent == null) {
			twsGenerateContentParent();
		}
		mContentParent.addView(view, params);
		if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
			getWindow().getCallback().onContentChanged();
		}

		initActionBar();
	}

	@Override
	public void onDetachedFromWindow() {
		if (mActionBarView != null) {
			mActionBarView.dismissPopupMenus();
		}
		super.onDetachedFromWindow();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (!isChild()) {
			mTitleReady = true;
			onTitleChanged(getTitle(), getTitleColor());
		}
	}

	@Override
	protected void onTitleChanged(CharSequence title, int color) {
		super.onTitleChanged(title, color);
		if (mTitleReady) {
			final Window win = getWindow();
			if (win != null) {
				win.setTitle(title);
				if (mActionBarView != null) {
					mActionBarView.setWindowTitle(title);
				}
			}
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		onTitleChanged(title, getTitleColor());
		super.setTitle(title);
	}

	@Override
	public void setTitle(int titleId) {
		setTitle(getText(titleId));
	}

	private void initActionBar() {
		ViewGroup twsContentView = getTwsContentView();
		if (mActionBar == null) {
			mActionBar = new ActionBarImpl(this, mStatusBarOverlay);
		}
		if (mActionBarView != null) {
			mActionBarView.setWindowTitle(getTitle());
		}
	}

	public ActionBar getTwsActionBar() {
		initActionBar();
		return mActionBar;
	}

	public boolean onMenuOpened(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_ACTION_BAR) {
			initActionBar();
			if (mActionBar != null) {
				mActionBar.dispatchMenuVisibilityChanged(true);
			} else {
				Log.e(TAG, "Tried to open action bar menu with no action bar");
			}
		}
		return true;
	}

	public void onPanelClosed(int featureId, Menu menu) {
		mActionBar.dispatchMenuVisibilityChanged(false);
		super.onPanelClosed(featureId, menu);
	}

	public MenuInflater getMenuInflater() {
		// Make sure that action views can get an appropriate theme.
		if (mMenuInflater == null) {
			initActionBar();
			if (mActionBar != null) {
				mMenuInflater = new MenuInflater(mActionBar.getThemedContext());
			} else {
				mMenuInflater = new MenuInflater(this);
			}
		}
		return mMenuInflater;
	}

	public ActionMode startActionMode(ActionMode.Callback callback) {
		return mTwsRootView.startActionMode(callback);
	}

	@Override
	public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
		initActionBar();
		if (mActionBar != null) {
			if (mActionBarView != null && !mActionBarView.mIsMarksPointFlag) {
				invalidateOptionsMenu();
			}
			return mActionBar.startActionMode(callback);
		}
		return null;
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		getMenu().clear();
		return super.onCreatePanelMenu(featureId, menu);
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		filterMenu(menu);
		return super.onPreparePanel(featureId, view, menu);
	}

	@Override
	protected void onStop() {
		super.onStop();
		closePopupMenu();// panel menu
		closeContextMenu();// context menu
	}

	void filterMenu(Menu menu) {
		mMenu.setCallback(mMenuCallback);
		if (mActionBarView != null) {
			mActionBarView.setMenu(mMenu, null);
		}
	}

	public Menu getMenu() {
		if (mMenu == null) {
			mMenu = new MenuBuilder(this);
		}
		return mMenu;
	}

	protected void onCreate(Bundle savedInstanceState) {
		if (android.os.Build.VERSION.SDK_INT > 18 && getResources().getBoolean(R.bool.config_statusbar_state)) {
			Window window = getWindow();
			window.addFlags(FLAG_TRANSLUCENT_STATUS);
			setStatusBarOverlay();
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		getSysStatusBarHeight();
	}

	protected void onResume() {
		super.onResume();
		boolean isThemeChanged = false;
		boolean isDefaultTheme = SystemProperties.getBoolean("persist.sys.theme", true);
		if (!isDefaultTheme) {
			ActivityInfo info = getActivityInfo();
			if (info != null) {
				isThemeChanged = false;
			} else {
				isThemeChanged = false;
			}
		}

		invalidateOptionsMenu();
		ActionBar actionBar = getTwsActionBar();
		if (actionBar != null && android.os.Build.VERSION.SDK_INT > 18) {
			String customColor = changeTwsStatusBarColor();
			if (customColor != null) {
				if (STATUSBAR_COLOR_WHITE.equals(customColor)) {
					sendStatusBarBroadcast(STATUSBAR_COLOR_WHITE, isThemeChanged);
				} else if (STATUSBAR_COLOR_BLACK.equals(customColor)) {
					sendStatusBarBroadcast(STATUSBAR_COLOR_BLACK, isThemeChanged);
				} else {
					sendStatusBarBroadcast(STATUSBAR_COLOR_BLACK, isThemeChanged);
				}
			} else {
				int id = actionBar.getBackgroundResId();
				if (id == R.drawable.ab_solid_holo_light) {
					if (ThemeUtils.isActionBarBackgroundGradient(this)) {
						sendStatusBarBroadcast(STATUSBAR_COLOR_WHITE, isThemeChanged);
					} else {
						sendStatusBarBroadcast(STATUSBAR_COLOR_BLACK, isThemeChanged);
					}
				} else if (id == R.drawable.ab_solid_holo_dark) {
					sendStatusBarBroadcast(STATUSBAR_COLOR_WHITE, isThemeChanged);
				}
			}
		} else {
			String color = changeTwsStatusBarColor();
			if (null != color && STATUSBAR_COLOR_WHITE.equals(color)) {
				sendStatusBarBroadcast(STATUSBAR_COLOR_WHITE, isThemeChanged);
			} else if (null != color && STATUSBAR_COLOR_BLACK.equals(color)) {
				sendStatusBarBroadcast(STATUSBAR_COLOR_BLACK, isThemeChanged);
			} else {
				sendStatusBarBroadcast(STATUSBAR_COLOR_WHITE, isThemeChanged);
			}
		}
	}

	protected void onPause() {
		super.onPause();
		if (mActionBarView != null && mActionBarView.mIsMarksPointFlag) {
			closePopupMenu();// panel menu
		}
	}

	MenuBuilder.Callback mMenuCallback = new MenuBuilder.Callback() {

		public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
			boolean result = TwsActivity.this.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
			closePopupMenu();
			return result;
		}

		public void onMenuModeChange(MenuBuilder menu) {
			if (!mPopupMenuShow && !mActionBar.isMultiMode()) {
				onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, getMenu());
			}
		}
	};

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			if (event.getRepeatCount() == 0 && !mPopupMenuShow && !mActionBar.isMultiMode()) {
				if (onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, getMenu()) == false)
					return true;
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU: {
			if (mActionBarView != null && mActionBarView.isOverflowReserved() && mActionBar.mOverflowButtonState) {
				if (mActionBarView.getVisibility() == View.VISIBLE && mActionBarView.isOverflowButtonShowing()) {
					if (!mActionBarView.isOverflowMenuShowing()) {
						if (!isDestroyed() && onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, getMenu())) {
							mActionBarView.showOverflowMenu();
						}
					} else {
						mActionBarView.hideOverflowMenu();
					}
				}
			}
			return true;
		}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// for actionmod
		if (mTwsRootView.superDispatchKeyEvent(event)) {
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	private void closePopupMenu() {
		WindowManager wm = this.getWindowManager();
		if (wm == null) {
			return;
		}
		if (mPopupViewContainer != null && mPopupMenuShow) {
			wm.removeView(mPopupViewContainer);
			mPopupMenuShow = false;
		}
		if (mActionBarView != null) {
			mActionBarView.hideOverflowMenu();
		}
	}

	PopupViewContainer getPopupViewContainer() {
		if (mPopupViewContainer == null) {
			mPopupViewContainer = new PopupViewContainer(this);
			mPopupViewContainer.setBackgroundResource(R.drawable.transparent_background);
		}

		return mPopupViewContainer;
	}

	private WindowManager.LayoutParams createPopupLayout(/* IBinder token */) {
		WindowManager wm = this.getWindowManager();

		if (wm == null) {
			return null;
		}
		int width = wm.getDefaultDisplay().getWidth();
		int height = wm.getDefaultDisplay().getHeight();
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		lp.width = width;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.format = getPopupViewContainer().getBackground().getOpacity();
		lp.flags = WindowManager.LayoutParams.FLAG_DITHER | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
				| WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_DIM_BEHIND;

		lp.gravity = Gravity.CENTER | Gravity.BOTTOM;
		lp.dimAmount = 0.6f;
		lp.windowAnimations = R.style.Animation_DropDownUp_tws;

		return lp;
	}

	ListMenuPresenter mListMenuPresenter = new ListMenuPresenter(R.layout.popup_menu_item_layout,
			R.style.Theme_tws_CompactMenu_Second);
	MenuView mMenuView;

	PopupViewContainer mPopupViewContainer;
	boolean mPopupMenuShow = false;

	private class PopupViewContainer extends FrameLayout {
		private static final String TAG = "PopupWindow.PopupViewContainer";

		public PopupViewContainer(Context context) {
			super(context);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
				if (getKeyDispatcherState() == null) {
					return super.dispatchKeyEvent(event);
				}

				if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
					KeyEvent.DispatcherState state = getKeyDispatcherState();
					if (state != null) {
						state.startTracking(event, this);
					}
					return true;
				} else if (event.getAction() == KeyEvent.ACTION_UP) {
					KeyEvent.DispatcherState state = getKeyDispatcherState();
					if (state != null && state.isTracking(event) && !event.isCanceled()) {
						closePopupMenu();
						return true;
					}
				}
				return super.dispatchKeyEvent(event);
			} else {
				return super.dispatchKeyEvent(event);
			}
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			return super.dispatchTouchEvent(ev);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			final int x = (int) event.getX();
			final int y = (int) event.getY();

			if ((event.getAction() == MotionEvent.ACTION_DOWN)
					&& ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
				closePopupMenu();
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				closePopupMenu();
				return true;
			} else {
				return super.onTouchEvent(event);
			}
		}
	}

	@Override
	public void invalidateOptionsMenu() {
		super.invalidateOptionsMenu();
		onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, getMenu());
		onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, getMenu());
	}

	@Override
	public void closeContextMenu() {
		super.closeContextMenu();
		if (mContextMenu != null) {
			mContextMenu.close();
			dismissContextMenu();
		}
	}

	private synchronized void dismissContextMenu() {
		mContextMenu = null;

		if (mContextMenuHelper != null) {
			mContextMenuHelper.dismiss();
			mContextMenuHelper = null;
		}
	}

	final DialogMenuCallback mContextMenuCallback = new DialogMenuCallback(Window.FEATURE_CONTEXT_MENU);
	private ContextMenuBuilder mContextMenu;
	private MenuDialogHelper mContextMenuHelper;

	// getCallback is activity
	public final class TwsContentView extends FrameLayout {
		private ActionMode mActionMode;
		private ActionBarContextView mActionModeView;
		private PopupWindow mActionModePopup;
		private Runnable mShowActionModePopup;
		private View twsmOriginalView = null;

		public TwsContentView(Context context) {
			super(context);
		}

		public boolean superDispatchKeyEvent(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				final int action = event.getAction();
				// Back cancels action modes first.
				if (mActionMode != null) {
					if (action == KeyEvent.ACTION_UP) {
						mActionMode.finish();
					}
					return true;
				}

				// Next collapse any expanded action views.
				if (mActionBarView != null && mActionBarView.hasExpandedActionView()) {
					if (action == KeyEvent.ACTION_UP) {
						mActionBarView.collapseActionView();
					}
					return true;
				}
			}

			return false;
		}

		public View twsGetOriginalView() {
			return twsmOriginalView;
		}

		void twsActionModPopWindow(ActionMode mode, ActionMode.Callback callback) {
			boolean creatValue = callback.onCreateActionMode(mode, mode.getMenu());

			// this is textview
			if (twsGetOriginalView() instanceof TextView) {
				mode.invalidate();
				mActionMode = mode;
			} else {
				if (creatValue) {
					mode.invalidate();
					mActionModeView.initForMode(mode);
					mActionModeView.setVisibility(View.VISIBLE);
					mActionMode = mode;
					if (mActionModePopup != null) {
						post(mShowActionModePopup);
					}

					mActionModeView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
				} else {
					mActionMode = null;
				}
			}
		}

		void twsMakeSureContextView() {
			// must have this, other actionmod finish will crash
			if ((twsmOriginalView instanceof TextView) && mActionModeView == null) {
				mActionModeView = new ActionBarContextView(mContext);
			}
		}

		private ActionMode twsStartActionModeForChild(View originalView, ActionMode.Callback callback) {
			twsmOriginalView = originalView;
			if (mActionMode != null) {
				mActionMode.finish();
			}
			final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
			ActionMode mode = null;
			if (!(twsmOriginalView instanceof TextView)) {
				if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
					try {
						mode = getWindow().getCallback().onWindowStartingActionMode(wrappedCallback);
					} catch (AbstractMethodError ame) {
						// Older apps might not implement this callback method.
					}
				}
			}
			if (mode != null) {
				mActionMode = mode;
			} else {
				if (mActionModeView == null) {
					if (getWindow().isFloating()) {
						mActionModeView = new ActionBarContextView(mContext);
						mActionModePopup = new PopupWindow(mContext, null, R.attr.actionModePopupWindowStyle);
						mActionModePopup.setLayoutInScreenEnabled(true);
						mActionModePopup.setLayoutInsetDecor(true);
						mActionModePopup.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION);
						mActionModePopup.setContentView(mActionModeView);
						mActionModePopup.setWidth(MATCH_PARENT);

						TypedValue heightValue = new TypedValue();
						mContext.getTheme().resolveAttribute(android.R.attr.actionBarSize, heightValue, true);
						final int height = TypedValue.complexToDimensionPixelSize(heightValue.data, mContext
								.getResources().getDisplayMetrics());
						mActionModeView.setContentHeight(height);
						mActionModePopup.setHeight(WRAP_CONTENT);
						mShowActionModePopup = new Runnable() {
							public void run() {
								mActionModePopup.showAtLocation(mActionModeView.getApplicationWindowToken(),
										Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
							}
						};
					}
				}
				twsMakeSureContextView();

				if (mActionModeView != null) {
					mActionModeView.killMode();
					mode = new StandaloneActionMode(getContext(), mActionModeView, wrappedCallback,
							mActionModePopup == null);
					twsActionModPopWindow(mode, callback);
				}
			}
			if (mActionMode != null && getWindow().getCallback() != null && !getWindow().isDestroyed()) {
				try {
					getWindow().getCallback().onActionModeStarted(mActionMode);
				} catch (AbstractMethodError ame) {
					// Older apps might not implement this callback method.
				}
			}
			return mActionMode;
		}

		@Override
		public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
			return twsStartActionModeForChild(originalView, callback);
		}

		@Override
		public ActionMode startActionMode(ActionMode.Callback callback) {
			if (mActionMode != null) {
				mActionMode.finish();
			}

			final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
			ActionMode mode = null;
			if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
				try {
					mode = getWindow().getCallback().onWindowStartingActionMode(wrappedCallback);
				} catch (AbstractMethodError ame) {
					// Older apps might not implement this callback method.
				}
			}
			if (mode != null) {
				mActionMode = mode;
			} else {
				if (mActionModeView == null) {
					if (getWindow().isFloating()) {
						mActionModeView = new ActionBarContextView(mContext);
						mActionModePopup = new PopupWindow(mContext, null, R.attr.actionModePopupWindowStyle);
						mActionModePopup.setLayoutInScreenEnabled(true);
						mActionModePopup.setLayoutInsetDecor(true);
						mActionModePopup.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION);
						mActionModePopup.setContentView(mActionModeView);
						mActionModePopup.setWidth(MATCH_PARENT);

						TypedValue heightValue = new TypedValue();
						mContext.getTheme().resolveAttribute(android.R.attr.actionBarSize, heightValue, true);
						final int height = TypedValue.complexToDimensionPixelSize(heightValue.data, mContext
								.getResources().getDisplayMetrics());
						mActionModeView.setContentHeight(height);
						mActionModePopup.setHeight(WRAP_CONTENT);
						mShowActionModePopup = new Runnable() {
							public void run() {
								mActionModePopup.showAtLocation(mActionModeView.getApplicationWindowToken(),
										Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
							}
						};
					} else {
						int stubID = ResIdentifierUtils.getSysId("action_mode_bar_stub");
						if (0 != stubID) {
							ViewStub stub = (ViewStub) findViewById(stubID);

							if (stub != null) {
								mActionModeView = (ActionBarContextView) stub.inflate();
							}
						}
					}
				}

				if (mActionModeView != null) {
					mActionModeView.killMode();
					mode = new StandaloneActionMode(getContext(), mActionModeView, wrappedCallback,
							mActionModePopup == null);
					if (callback.onCreateActionMode(mode, mode.getMenu())) {
						mode.invalidate();
						mActionModeView.initForMode(mode);
						mActionModeView.setVisibility(View.VISIBLE);
						mActionMode = mode;
						if (mActionModePopup != null) {
							post(mShowActionModePopup);
						}
						mActionModeView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
					} else {
						mActionMode = null;
					}
				}
			}
			if (mActionMode != null && getWindow().getCallback() != null && !getWindow().isDestroyed()) {
				try {
					getWindow().getCallback().onActionModeStarted(mActionMode);
				} catch (AbstractMethodError ame) {
					// Older apps might not implement this callback method.
				}
			}

			boolean hasOverlayActionMode = getWindow().hasFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
			if (hasOverlayActionMode && mActionModeOverLayBgIsBlur) {
				try {
					mBottomScreenBlurBitmap = takeSplitActionBarBlur(TwsActivity.this);
					if (mBottomScreenBlurBitmap != null && !mBottomScreenBlurBitmap.isRecycled()) {
						mActionBar.setSplitBackgroundDrawable(new BitmapDrawable(mBottomScreenBlurBitmap));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return mActionMode;
		}

		private class ActionModeCallbackWrapper implements ActionMode.Callback {
			private ActionMode.Callback mWrapped;

			public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
				mWrapped = wrapped;
			}

			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				return mWrapped.onCreateActionMode(mode, menu);
			}

			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return mWrapped.onPrepareActionMode(mode, menu);
			}

			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				return mWrapped.onActionItemClicked(mode, item);
			}

			public void onDestroyActionMode(ActionMode mode) {
				mWrapped.onDestroyActionMode(mode);
				if (mActionModePopup != null) {
					removeCallbacks(mShowActionModePopup);
					mActionModePopup.dismiss();
				} else if (mActionModeView != null) {
					mActionModeView.setVisibility(GONE);
				}
				if (mActionModeView != null) {
					mActionModeView.removeAllViews();
				}
				if (getWindow().getCallback() != null && !getWindow().isDestroyed()) {
					try {
						getWindow().getCallback().onActionModeFinished(mActionMode);
					} catch (AbstractMethodError ame) {
						// Older apps might not implement this callback method.
					}
				}
				mActionMode = null;
			}
		}
	}

	private final class DialogMenuCallback implements MenuBuilder.Callback, MenuPresenter.Callback {
		private int mFeatureId;
		private MenuDialogHelper mSubMenuHelper;

		public DialogMenuCallback(int featureId) {
			mFeatureId = featureId;
		}

		public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
			if (menu.getRootMenu() != menu) {
				onCloseSubMenu(menu);
			}

			if (allMenusAreClosing) {
				Window.Callback callback = getWindow().getCallback();
				if (callback != null && !getWindow().isDestroyed()) {
					callback.onPanelClosed(mFeatureId, menu);
				}
				// Dismiss the submenu, if it is showing
				if (mSubMenuHelper != null) {
					mSubMenuHelper.dismiss();
					mSubMenuHelper = null;
				}
			}
		}

		public void onCloseSubMenu(MenuBuilder menu) {
			Window.Callback callback = getWindow().getCallback();
			if (callback != null && !getWindow().isDestroyed()) {
				callback.onPanelClosed(mFeatureId, menu.getRootMenu());
			}
		}

		public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
			Window.Callback callback = getWindow().getCallback();
			return (callback != null && !getWindow().isDestroyed()) && callback.onMenuItemSelected(mFeatureId, item);
		}

		public void onMenuModeChange(MenuBuilder menu) {
		}

		public boolean onOpenSubMenu(MenuBuilder subMenu) {
			if (subMenu == null)
				return false;

			// Set a simple callback for the submenu
			subMenu.setCallback(this);

			// The window manager will give us a valid window token
			mSubMenuHelper = new MenuDialogHelper(subMenu);
			mSubMenuHelper.show(null);

			return true;
		}
	}

	private ActivityInfo getActivityInfo() {
		ActivityInfo info = null;
		try {
			info = twsGetPackageManager().getActivityInfo(this.getComponentName(), PackageManager.GET_ACTIVITIES);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return info;
	}

	private PackageManager twsGetPackageManager() {
		PackageManager pm = this.getPackageManager();
		if (pm == null) {
			resetIPackageManager(this);
			pm = this.getPackageManager();
		}
		return pm;
	}

	public void resetIPackageManager(Context context) {
		Context contextImpl;
		if (context instanceof ContextWrapper) {
			contextImpl = ((ContextWrapper) context).getBaseContext();
		} else {
			return;
		}
		try {
			Class<?> clazz1 = ReflectUtils.forClassName("android.app.ActivityThread");
			Field field1 = ReflectUtils.getDeclaredField(clazz1, "sPackageManager");
			ReflectUtils.setFieldValue(null, field1, null);
			Class<?> clazz2 = ReflectUtils.forClassName("android.app.ContextImpl");
			Field field2 = ReflectUtils.getDeclaredField(clazz2, "mPackageManager");
			ReflectUtils.setFieldValue(contextImpl, field2, null);
			Class<?> clazz3 = ReflectUtils.forClassName("android.os.ServiceManager");
			Field field3 = ReflectUtils.getDeclaredField(clazz3, "sServiceManager");
			ReflectUtils.setFieldValue(null, field3, null);
			HashMap<String, IBinder> map = (HashMap<String, IBinder>) ReflectUtils
					.getFieldValue("sCache", null, clazz3);
			if (map != null) {
				IBinder oldValue = map.remove("package");
				if (oldValue != null) {
					Method method1 = ReflectUtils.getDeclaredMethod(clazz3, "getService", String.class);
					IBinder newValue = (IBinder) ReflectUtils.invoke(method1, null, "package");
					if (newValue != null) {
						map.put("package", newValue);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private TwsDialog createTwsDialog(Integer dialogId, Bundle state, Bundle args) {
		final TwsDialog dialog = onCreateTwsDialog(dialogId, args);
		if (dialog == null) {
			return null;
		}
		dialog.dispatchOnCreate(state);
		return dialog;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// dismiss any dialogs we are managing.
		if (mManagedDialogs != null) {
			final int numDialogs = mManagedDialogs.size();
			for (int i = 0; i < numDialogs; i++) {
				final ManagedDialog md = mManagedDialogs.valueAt(i);
				if (md.mDialog.isShowing()) {
					md.mDialog.dismiss();
				}
			}
			mManagedDialogs = null;
		}
		if (mActionModeOverLayBgIsBlur) {
			try {
				if (mBottomScreenBlurBitmap != null && !mBottomScreenBlurBitmap.isRecycled()) {
					mBottomScreenBlurBitmap.recycle();
					mBottomScreenBlurBitmap = null;
					Log.d(TAG, "onDestroy() mBottomScreenBlurBitmap");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public final boolean showTwsDialog(int id, Bundle args) {
		if (mManagedDialogs == null) {
			mManagedDialogs = new SparseArray<ManagedDialog>();
		}
		ManagedDialog md = mManagedDialogs.get(id);
		if (md == null) {
			md = new ManagedDialog();
			md.mDialog = createTwsDialog(id, null, args);
			if (md.mDialog == null) {
				return false;
			}
			mManagedDialogs.put(id, md);
		}

		md.mArgs = args;
		onPrepareTwsDialog(id, md.mDialog, args);
		md.mDialog.show();
		return true;
	}

	public final void dismissTwsDialog(int id) {
		if (mManagedDialogs == null) {
			throw missingTwsDialog(id);
		}

		final ManagedDialog md = mManagedDialogs.get(id);
		if (md == null) {
			throw missingTwsDialog(id);
		}
		md.mDialog.dismiss();
	}

	private IllegalArgumentException missingTwsDialog(int id) {
		return new IllegalArgumentException("no dialog with id " + id + " was ever " + "shown via Activity#showDialog");
	}

	public final void removeTwsDialog(int id) {
		if (mManagedDialogs != null) {
			final ManagedDialog md = mManagedDialogs.get(id);
			if (md != null) {
				md.mDialog.dismiss();
				mManagedDialogs.remove(id);
			}
		}
	}

	public final void showTwsDialog(int id) {
		showTwsDialog(id, null);
	}

	protected TwsDialog onCreateTwsDialog(int id) {
		return null;
	}

	protected TwsDialog onCreateTwsDialog(int id, Bundle args) {
		return onCreateTwsDialog(id);
	}

	protected void onPrepareTwsDialog(int id, TwsDialog dialog) {
		dialog.setBottomButtonsStartAnimation(true);
		dialog.setOwnerActivity(this);
	}

	protected void onPrepareTwsDialog(int id, TwsDialog dialog, Bundle args) {
		onPrepareTwsDialog(id, dialog);
	}

	public void sendStatusBarBroadcast(String color, boolean isThemeChanged) {
		Intent status_intent = new Intent();
		status_intent.setPackage("com.android.systemui");
		status_intent.setAction("tws.systemui.statusbar.theme");
		status_intent.putExtra("color", color);
		status_intent.putExtra("isThemeChanged", isThemeChanged);
		// agneswang 20150310 send the window params of the activity to SystemUI
		status_intent.putExtra("windowType", getWindow().getAttributes().type);
		status_intent.putExtra("windowFlag", getWindow().getAttributes().flags);
		sendBroadcast(status_intent);
	}

	public void setStatusBarOverlay() {
		mStatusBarOverlay = true;
	}

	public void setActionModeOverLayBgBlur(boolean isBlur) {
		mActionModeOverLayBgIsBlur = isBlur;
	}

	public Bitmap takeSplitActionBarBlur(Activity activity) {
		try {
			View view = activity.getWindow().getDecorView();
			view.setDrawingCacheEnabled(true);
			view.buildDrawingCache();
			Bitmap bitmap = view.getDrawingCache();
			Rect rect = new Rect();
			activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
			int statusBarHeight = rect.top;

			int width = activity.getWindowManager().getDefaultDisplay().getWidth();
			int height = activity.getWindowManager().getDefaultDisplay().getHeight();
			int bottomHeight = getResources().getDimensionPixelSize(R.dimen.tws_actionbar_split_height);
			Drawable bottomDrawable = getResources().getDrawable(R.drawable.ab_solid_holo_light_bottom);
			bottomDrawable.setBounds(0, 0, width, bottomHeight);

			Matrix matrix = new Matrix();
			matrix.postScale(0.2f, 0.2f);

			Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, height - bottomHeight, width, bottomHeight, matrix, true);
			Canvas canvas = new Canvas(bitmap2);
			bottomDrawable.draw(canvas);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			canvas.restore();
			view.destroyDrawingCache();

			JNIBlur blur = new JNIBlur(TwsActivity.this);
			Bitmap bitmap3 = blur.blur(bitmap2, true);
			return bitmap3;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Bitmap takeActionBarBlur(Activity activity) {
		try {
			View view = activity.getWindow().getDecorView();
			view.setDrawingCacheEnabled(true);
			view.buildDrawingCache();
			Bitmap bitmap = view.getDrawingCache();
			Rect rect = new Rect();
			activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
			int statusBarHeight = rect.top;

			int width = activity.getWindowManager().getDefaultDisplay().getWidth();
			int height = activity.getWindowManager().getDefaultDisplay().getHeight();
			int topHeight = getResources().getDimensionPixelSize(R.dimen.tws_action_bar_height) - getStatusBarHeight();
			Drawable topDrawable = getResources().getDrawable(R.drawable.ab_solid_holo_light);
			topDrawable.setBounds(0, 0, width, topHeight);

			Matrix matrix = new Matrix();
			matrix.postScale(0.2f, 0.2f);

			Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, width, topHeight, matrix, true);
			Canvas canvas = new Canvas(bitmap2);
			topDrawable.draw(canvas);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			canvas.restore();
			view.destroyDrawingCache();

			JNIBlur blur = new JNIBlur(TwsActivity.this);
			Bitmap bitmap3 = blur.blur(bitmap2, true);
			return bitmap3;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setLinearMode(boolean linearMode) {
		this.mLinearMode = linearMode;
	}

	// tws-start add for statusbar::2015-1-4
	public String changeTwsStatusBarColor() {
		return null;
	}

	@Deprecated
	public void setHoloStatusBar() {
	}

	// tws-end add for statusbar::2015-1-4

	private void useCustomTheme() {
		if (IS_SUPPORT_CUSTOM_THEME) {
			Intent intent = getIntent();
			if (intent != null) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					int customTheme = bundle.getInt(KEY_CUSTOM_THEME, 0);
					if (customTheme != 0) {
						setTheme(customTheme);
					}
				}
			}
		}
	}

	// only before before setContentView valid
	@Override
	public void setSplitActionWhenNarrowOptions(boolean isSplitActionWhenNarrow) {
		if (mContentParent != null) {
			Log.e(TAG, "该接口只能在调用setContent/getTwsActionBar/getTwsContentView之前使用才有效~");
			throw new IllegalAccessError("Only before before init mContentParent valid!");
		}

		mCustomSplitWhenNarrow = isSplitActionWhenNarrow;
	}
}
