package com.tencent.tws.assistant.widget;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.policy.PolicyManager;
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.internal.app.ActionBarImpl;
import com.tencent.tws.sharelib.R;

//tws-start add global float view::2014-09-13
public class FloatView implements FloatInterface, Window.Callback, KeyEvent.Callback, OnCreateContextMenuListener, OnGestureListener {
	private static final String TAG = "FloatView";
	private Activity mOwnerActivity;
	private Context mContext;
    final WindowManager mWindowManager;
    Window mWindow;
    View mDecor;
    private ActionBarImpl mActionBar;
    protected boolean mCancelable = true;
    
    private String mCancelAndDismissTaken;
    private Message mCancelMessage;
    private Message mDismissMessage;
    private Message mShowMessage;

    private OnKeyListener mOnKeyListener;
    
    private boolean mCreated = false;
    private boolean mShowing = false;
    private boolean mCanceled = false;
    
    private final Thread mUiThread;
    private final Handler mHandler = new Handler();
    
    private static final int DISMISS = 0x43;
    private static final int CANCEL = 0x44;
    private static final int SHOW = 0x45;
    
    private Handler mListenersHandler;
    private ActionMode mActionMode;
    
    private boolean showFromTop;
    private GestureDetector detector;
    private boolean isDefaultView = true;
    private View mDefaultFloatView;
    private View mDefaultContent;
    private ImageView mDefaultIcon;
    private TextView mDefaultTitle;
    private TextView mDefaultSubTitle;
    private TextView mDefaultTime;
    private View mCustomFloatView;
    private boolean isClick = true;
	private float mLastDownPositionX, mLastDownPositionY;
	private float mLastUpPositionX, mLastUpPositionY;
    
    private OnContentClickListener mOnContentClickListener = null;
    
    private final Runnable mDismissAction = new Runnable() {
		
		public void run() {
			dismissFloatView();
		}
	};
	private int viewWidth, viewHeight;
	private int statusBarHeight;
	private float xInScreen, yInScreen;
	private float xDownInScreen, yDownInScreen;
	private float xInView, yInView;
	private int mAnimationStyle = -1;
	
	private static final float DENSITY_H = 1.5f;
	private static final float DENSITY_XH = 2f;
	private static final float DENSITY_XXH = 3f;
	    
	private static final float WIDTH_H = 480f;
	private static final float WIDTH_XH = 720f;
	private static final float WIDTH_XXH = 1080f;
	private static final int FLOATVIEW_HEIGHT = 66;
	
	WindowManager.LayoutParams mDecorLp; 
	public FloatView(Context context) {
		this(context, true); 
	}
	
	public FloatView(Context context, boolean defaultView) {
		this(context, true, defaultView);
	}
	
	public FloatView(Context context, boolean isHoloLight, boolean defaultView) {
		this(context, isHoloLight, true, defaultView);
	}
	
	
	private WindowManager.LayoutParams getMyLayoutParams() {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_DRAG,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
//		lp.alpha = 1.0f;
		lp.dimAmount = 0.0f;
		lp.gravity = Gravity.LEFT | Gravity.TOP;
		lp.x = 0;
		lp.y = 0;
		
		
		return lp;
	}
	
	private WindowManager.LayoutParams getMyLayoutParamsForDecor() {
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				getScreenDensity() * FLOATVIEW_HEIGHT,
				WindowManager.LayoutParams.TYPE_DRAG,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		lp.dimAmount = 0.0f;
		lp.gravity = Gravity.LEFT | Gravity.TOP;
		lp.x = 0;
		lp.y = 0;
		
		return lp;
	}
	 
	FloatView(Context context, boolean isHoloLight, boolean createContextThemeWrapper, boolean defaultView) {
		isDefaultView = defaultView;
		int theme = isHoloLight ? R.style.FloatViewTheme_HoloLight : R.style.FloatViewTheme;
		context.setTheme(theme);
		mContext = createContextThemeWrapper ? new ContextThemeWrapper(context, theme) : context;
		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Window w = PolicyManager.makeNewWindow(mContext);
		mWindow = w;
		mWindow.requestFeature(Window.FEATURE_NO_TITLE);
		mWindow.setAttributes(getMyLayoutParams());
		
		mDecorLp = mWindow.getAttributes();
		mDecorLp = getMyLayoutParamsForDecor();
		
		makeDefaultContentView();
		
		mUiThread = Thread.currentThread();
		mListenersHandler = new ListenersHandler(this);
		detector = new GestureDetector(context, this);
	}
	
	public void setFloatViewWindowBk() {
		mWindow.setBackgroundDrawableResource(R.color.transparent);
	}
	
	public void setFloatViewHeight() {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_DRAG,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		lp.dimAmount = 0.0f;
		lp.gravity = Gravity.LEFT | Gravity.TOP;
		lp.x = 0;
		lp.y = 0;
		
		mDecorLp = lp;
	}
	
	protected FloatView(Context context, boolean cancelable, OnCancelListener cancelListener) {
		this(context);
		mCancelable = cancelable;
		setOnCancelListener(cancelListener);
	}
	
	
	public final Context getContext() {
		return mContext;
	}
	
	public ActionBar getActionBar() {
		return mActionBar;
	}
	
	public final void setOwnerActivity(Activity activity) {
		mOwnerActivity = activity;
		getWindow().setVolumeControlStream(mOwnerActivity.getVolumeControlStream());
	}
	
	public final Activity getOwnerActivity() {
		return mOwnerActivity;
	}
	
	public boolean isShowing() {
		return mShowing;
	}
	
	public void show(boolean isAutoDismiss) {
		if (mShowing) {
			if (mDecor != null) {
				if (mWindow.hasFeature(Window.FEATURE_ACTION_BAR)) {
					mWindow.invalidatePanelMenu(Window.FEATURE_ACTION_BAR);
				}
				mDecor.setVisibility(View.VISIBLE);
			}
			return;
		}
		
		mCanceled = false;
		
		if (!mCreated) {
			dispatchOnCreate(null);
		}
		
		onStart();
		mDecor = mWindow.getDecorView();
		
		if (mActionBar == null && mWindow.hasFeature(Window.FEATURE_ACTION_BAR)) {
			mActionBar = new ActionBarImpl(this);
		}
		
		WindowManager.LayoutParams l = mWindow.getAttributes();
		if ((l.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) == 0) {
			WindowManager.LayoutParams nl = new WindowManager.LayoutParams();
			nl.copyFrom(l);
			nl.softInputMode |= WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
			l = nl;
			Log.d("floatview", "l.softInputMode SOFT_INPUT_IS_FORWARD_NAVIGATION ");
		}
		
		try {
			if (l.y < mContext.getResources().getDisplayMetrics().heightPixels / 2)
				showFromTop = true;
//			l = getMyLayoutParamsForDecor();
			l = mDecorLp;
			l.windowAnimations = computeAnimationResource();
			viewWidth = l.width;
			viewHeight = l.height;
//			Log.e(TAG, "viewWidth = " + viewWidth + "; viewHeight = " + viewHeight); 
			mWindowManager.addView(mDecor, l);
			mShowing = true;
			sendShowMessage();
		} finally {
		}
		
		if (isAutoDismiss) {
			dismissDelayed(5000);
		}
	}
	
	
	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		if (Thread.currentThread() != mUiThread) {
		    mHandler.removeCallbacks(mDismissAction);
			mHandler.post(mDismissAction);
		}
		else {
			mHandler.removeCallbacks(mDismissAction);
			mDismissAction.run();
		}
	}
	
	public void dismissDelayed(long delay) {
		if (Thread.currentThread() != mUiThread) {
		    mHandler.removeCallbacks(mDismissAction);
			mHandler.postDelayed(mDismissAction, delay);
		}
		else {
			mHandler.removeCallbacks(mDismissAction);
			TimerTask dismissTask = new TimerTask() {
				public void run() {
					dismissFloatView();
				}
			};
			Timer timer = new Timer();
			timer.schedule(dismissTask, delay);
		}
	}
	
	void dismissFloatView() {
		if (mDecor == null || !mShowing) {
			return;
		}
		
		if (mWindow.isDestroyed()) {
			return;
		}
		try {
			mWindowManager.removeView(mDecor);
		} finally {
			if (mActionMode != null) {
				mActionMode.finish();
			}
			mDecor = null;
			mWindow.closeAllPanels();
			onStop();
			mShowing = false;
			
			sendDismissMessage();
		}
	}
	
	public void hide() {
		if (mDecor != null) {
			mDecor.setVisibility(View.GONE);
		}
	}
	
	private void sendDismissMessage() {
		if (mDismissMessage != null) {
			Message.obtain(mDismissMessage).sendToTarget();
		}
	}
	
	private void sendShowMessage() {
		if (mShowMessage != null) {
			Message.obtain(mShowMessage).sendToTarget();
		}
	}
	
	public void dispatchOnCreate(Bundle savedInstanceState) {
		if (!mCreated) {
			onCreate(savedInstanceState);
			mCreated = true;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		
	}
	
	protected void onStart() {
		if (mActionBar != null)
			mActionBar.setShowHideAnimationEnabled(true);
	}
	
	protected void onStop() {
		if (mActionBar != null)
			mActionBar.setShowHideAnimationEnabled(false);
	}

	private static final String FLOATVIEW_SHOWING_TAG = "android:floatviewShowing";
	private static final String FLOATVIEW_HIERARCHY_TAG = "android:floatviewHierachy";
	
	public Bundle onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putBoolean(FLOATVIEW_SHOWING_TAG, mShowing);
		if (mCreated) {
			bundle.putBundle(FLOATVIEW_HIERARCHY_TAG, mWindow.saveHierarchyState());
		}
		return bundle;
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		final Bundle floatviewHierarchyState = savedInstanceState.getBundle(FLOATVIEW_HIERARCHY_TAG);
		if (floatviewHierarchyState == null) {
			return;
		}
		dispatchOnCreate(savedInstanceState);
		mWindow.restoreHierarchyState(floatviewHierarchyState);
		if (savedInstanceState.getBoolean(FLOATVIEW_SHOWING_TAG)) {
			show(false);
		}
	}
	
	public Window getWindow() {
        return mWindow;
    }

	public View getCurrentFocus() {
		return mWindow != null ? mWindow.getCurrentFocus() : null;
	}

	public View findViewById(int id) {
		return mWindow.findViewById(id);
	}

	private void makeDefaultContentView() {
		if(isDefaultView) {
			getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			mDefaultFloatView = LayoutInflater.from(mContext).inflate(R.layout.floatview_template_base, null);
			setContentView(mDefaultFloatView);
			initDefaultFloatView();
		}
	}
	
	private void initDefaultFloatView() {
		mDefaultContent = mDefaultFloatView.findViewById(R.id.float_view_content);
		mDefaultContent.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				int action = event.getAction();
				switch (action) { 
				case MotionEvent.ACTION_DOWN:
					mLastDownPositionX = event.getX();
					mLastDownPositionY = event.getY();
					break;
				case MotionEvent.ACTION_UP:
					mLastUpPositionX = event.getX();
					mLastUpPositionY = event.getY();
					Log.i(TAG, "mLastDownPositionX = " + mLastDownPositionX + "; mLastUpPositionX = " + mLastUpPositionX
							+ "; mLastDownPositionY = " + mLastDownPositionY + "; mLastUpPositionY = " + mLastUpPositionY);
					if(Math.abs(mLastDownPositionX - mLastUpPositionX) > 6 
							|| Math.abs(mLastDownPositionY - mLastUpPositionY) > 20) {
						isClick = false;
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
		mDefaultContent.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				Log.i(TAG, "mDefaultContent mOnContentClickListener isClick = " + isClick);
				if(mOnContentClickListener != null && isClick) {
					Log.i(TAG, "mOnContentClickListener not null");
					mOnContentClickListener.onClick(view);
				}
				dismiss();
			}
		});
		mDefaultIcon =(ImageView) mDefaultFloatView.findViewById(R.id.float_view_app_icon);
		mDefaultTitle = (TextView)mDefaultFloatView.findViewById(R.id.float_view_app_title);
		mDefaultSubTitle = (TextView)mDefaultFloatView.findViewById(R.id.float_view_app_subtitle);
		mDefaultTime = (TextView) mDefaultFloatView.findViewById(R.id.float_view_app_time);
	}
	public void setIcon(int resId) {
		mDefaultIcon.setImageResource(resId);
	};
	
	public void setIcon(Drawable resDrawable){
		mDefaultIcon.setImageDrawable(resDrawable);
	}
	public void setIcon(Bitmap bitmap) {
		mDefaultIcon.setImageBitmap(bitmap);
	}
	
	public void setSubTitle(int resId) {
		mDefaultSubTitle.setText(resId);
	}
	
	public void setSubTitle(CharSequence text) {
		mDefaultSubTitle.setText(text);
	}
	
	public void setContentView(int layoutResID) {
		View view = LayoutInflater.from(mContext).inflate(layoutResID, null);
		setContentView(view);
	}

	public void setContentView(View view) {
		mCustomFloatView = view;
		mWindow.setContentView(mCustomFloatView);
	}

	public void addContentView(View view, ViewGroup.LayoutParams params) {
		mWindow.addContentView(view, params);
	}

	public void setTitle(CharSequence title) {
		if(isDefaultView) {
			mDefaultTitle.setText(title);
		}else {
			mWindow.setTitle(title);
			mWindow.getAttributes().setTitle(title);
		}
	}

	public void setTitle(int titleId) {
		setTitle(mContext.getText(titleId));
	}
	
	public void setTime(CharSequence time){
		mDefaultTime.setText(time);
	}
	
	public void setTime(int timeId) {
		mDefaultTime.setText(timeId);
	}

	public boolean onKeyDown(int keyCode,KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			event.startTracking();
			return true;
		}
		return false;
	}

	public boolean onKeyLongPress(int keyCode,KeyEvent event) {
		return false;
	}

	public boolean onKeyUp(int keyCode,KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()) {
			onBackPressed();
			return true;
		}
		return false;
	}

	public boolean onKeyMultiple(int keyCode,int count,KeyEvent event) {
		return false;
	}

	public void onBackPressed() {
		if (mCancelable) {
			cancel();
		}
	}

	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		
		if (mCancelable && mShowing && mWindow.shouldCloseOnTouch(mContext, event)) {
			cancel();
			return true;
		}
		return detector.onTouchEvent(event);
	}
	
	private int getStatusBarHeight() {  
		return 0;
	}
	
	private void updateFloatViewPosition() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.x = (int)(xInScreen - xInView);
		lp.y = (int)(yInScreen - yInView);
		lp.width = viewWidth;
		lp.height = viewHeight;
		onWindowAttributesChanged(lp);
	}
	
	public void setPosition(int x, int y) {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.x = x;
		lp.y = y;
		onWindowAttributesChanged(lp);
	}
	
	public int getX() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		return lp.x;
	}
	
	public int getY() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		return lp.y;
	}
	
	public void setSize(int width, int height) {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.width = width;
		lp.height = height;
		viewWidth = width;
		viewHeight = height;
		onWindowAttributesChanged(lp);
	}
	
	public int getWidth() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		return lp.width;
	}
	
	public int getHeight() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		return lp.height;
	}
	
	public void setAlpha(float alpha) {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.alpha = alpha;
		onWindowAttributesChanged(lp);
	}
	
	public float getAlpha() {
		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		return lp.alpha;
	}
	
	public boolean onTrackballEvent(MotionEvent event) {
		return false;
	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		return false;
	}

	public void setCancelable(boolean flag) {
		mCancelable = flag;
	}

	public void setCanceledOnTouchOutside(boolean cancel) {
		if (cancel && !mCancelable) {
			mCancelable = true;
		}
		mWindow.setCloseOnTouchOutside(cancel);
	}
	
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		if (!mCanceled && mCancelMessage != null) {
			mCanceled = true;
			Message.obtain(mCancelMessage).sendToTarget();
		}
		dismiss();
	}

	public void setOnCancelListener(final OnCancelListener listener) {
		if (mCancelAndDismissTaken != null) {
			throw new IllegalStateException("OnCancelListener is already taken by " 
						+ mCancelAndDismissTaken + " and can not be replaced");
		}
		if (listener != null) {
			mCancelMessage = mListenersHandler.obtainMessage(CANCEL, listener);
		}
		else {
			mCancelMessage = null;
		}
	}

	public void setCancelMessage(final Message msg) {
		mCancelMessage = msg;
	}

	public void setOnContentClickListener(final OnContentClickListener listener) {
		
		mOnContentClickListener = listener;
	}
	public void setOnDismissListener(final OnDismissListener listener) {
		if (mCancelAndDismissTaken != null) {
			throw new IllegalStateException("OnDismissListener is already taken by "
						+ mCancelAndDismissTaken + " and can not be replaced");
		}
		if (listener != null) {
			mDismissMessage = mListenersHandler.obtainMessage(DISMISS, listener);
		}
		else {
			mDismissMessage = null;
		}
	}
	
	public void setDismissMessage(final Message msg) {
		mDismissMessage = msg;
	}
	
	public void setOnShowListener(final onShowListener listener) {
		if (listener != null) {
			mShowMessage = mListenersHandler.obtainMessage(SHOW, listener);
		}
		else {
			mShowMessage = null;
		}
	}

	public boolean takeCancelAndDismissListeners(String msg, final OnCancelListener cancel,
            final OnDismissListener dismiss) {
        if (mCancelAndDismissTaken != null) {
            mCancelAndDismissTaken = null;
        } else if (mCancelMessage != null || mDismissMessage != null) {
            return false;
        }
        
        setOnCancelListener(cancel);
        setOnDismissListener(dismiss);
        mCancelAndDismissTaken = msg;
        
        return true;
    }

	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (mWindow.superDispatchGenericMotionEvent(event)) {
			return true;
		}
		return onGenericMotionEvent(event);
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		// TODO Auto-generated method stubo
		if ((mOnKeyListener != null) && (mOnKeyListener.onKey(this, event.getKeyCode(), event))) {
			return true;
		}
		if (mWindow.superDispatchKeyEvent(event)) {
			return true;
		}
		return event.dispatch(this, mDecor != null ? mDecor.getKeyDispatcherState() : null, this);
	}

	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		// TODO Auto-generated method stub
		if (mWindow.superDispatchKeyShortcutEvent(event)) {
			return true;
		}
		return onKeyShortcut(event.getKeyCode(), event);
	}

	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		// TODO Auto-generated method stub
		event.setClassName(getClass().getName());
		event.setPackageName(mContext.getPackageName());

		LayoutParams params = getWindow().getAttributes();
		boolean isFullScreen = (params.width == LayoutParams.MATCH_PARENT) && (params.height == LayoutParams.MATCH_PARENT);
		event.setFullScreen(isFullScreen);
		
		return false;
	}

	public boolean dispatchTouchEvent(MotionEvent event) {
		if (mWindow.superDispatchTouchEvent(event)) {
			return true;
		}
		return onTouchEvent(event);
	}

	public boolean dispatchTrackballEvent(MotionEvent event) {
		if (mWindow.superDispatchTrackballEvent(event)) {
			return true;
		}
		return onTrackballEvent(event);
	}

	public void onActionModeFinished(ActionMode mode) {
		if (mode == mActionMode) {
			mActionMode = null;
		}
	}

	public void onActionModeStarted(ActionMode mode) {
		mActionMode = mode;
	}

	public void onAttachedToWindow() {
		// TODO Auto-generated method stub
		
	}

	public void onContentChanged() {
		// TODO Auto-generated method stub
		
	}

	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			return onCreateOptionsMenu(menu);
		}
		return false;
	}

	public View onCreatePanelView(int featureId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onPanelClosed(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		
	}

	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onSearchRequested() {
		// TODO Auto-generated method stub
		final SearchManager searchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);	
		final ComponentName appName = getAssociatedActivity();
		if (appName != null && searchManager.getSearchableInfo(appName) != null) {
			searchManager.startSearch(null, false, appName, null, false);
			dismiss();
			return true;
		}
		else {
			return false;
		}
	}

	private ComponentName getAssociatedActivity() {
		Activity activity = mOwnerActivity;
		Context context = getContext();
		while (activity == null && context != null) {
			if (context instanceof Activity) {
				activity = (Activity)mContext;
			}
			else {
				context = (context instanceof ContextWrapper) ? ((ContextWrapper)context).getBaseContext() : null;
			}
		}
		return activity == null ? null : activity.getComponentName();
	}

	public void takeKeyEvents(boolean get) {
		mWindow.takeKeyEvents(get);
	}

	public final boolean requestWindowFeature(int featureId) {
		return getWindow().requestFeature(featureId);
	}

	public final void setFeatureDrawableResource(int featureId, int resId) {
		getWindow().setFeatureDrawableResource(featureId, resId);
	}

	public final void setFeatureDrawableUri(int featureId, Uri uri) {
		getWindow().setFeatureDrawableUri(featureId, uri);
	}

	public final void setFeatureDrawable(int featureId, Drawable drawable) {
		getWindow().setFeatureDrawable(featureId, drawable);
	}

	public final void setFeatureDrawableAlpha(int featureId, int alpha) {
		getWindow().setFeatureDrawableAlpha(featureId, alpha);
	}

	public LayoutInflater getLayoutInflater() {
		return getWindow().getLayoutInflater();
	}

	public void onWindowAttributesChanged(LayoutParams params) {
		// TODO Auto-generated method stub
		if (mDecor != null) {
			mWindowManager.updateViewLayout(mDecor, params);
		}
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		
	}

	public ActionMode onWindowStartingActionMode(Callback callback) {
		// TODO Auto-generated method stub
		if (mActionBar != null) {
			return mActionBar.startActionMode(callback);
		}
		return null;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	public void onOptionsMenuClosed(Menu menu) {
		
	}

	public void openOptionsMenu() {
		mWindow.openPanel(Window.FEATURE_OPTIONS_PANEL, null);
	}

	public void closeOptionsMenu() {
		mWindow.closePanel(Window.FEATURE_OPTIONS_PANEL);
	}

	public void invalidateOptionsMenu() {
		mWindow.invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
	}

	public void registerForContextMenu(View view) {
		view.setOnCreateContextMenuListener(this);
	}

	public void unregisterForContextMenu(View view) {
		view.setOnCreateContextMenuListener(null);
	}

	public void openContextMenu(View view) {
		view.showContextMenu();
	}

	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}

	public void onContextMenuClosed(Menu menu) {
		
	}

	public final void setVolumeControlStream(int streamType) {
		getWindow().setVolumeControlStream(streamType);
	}

	public final int getVolumeControlStream() {
		return getWindow().getVolumeControlStream();
	}

	public void setOnKeyListener(final OnKeyListener onKeyListener) {
		mOnKeyListener = onKeyListener;
	}
	
	private static final class ListenersHandler extends Handler {
		private WeakReference<FloatInterface> mFloatView;
		
		public ListenersHandler(FloatView view) {
			mFloatView = new WeakReference<FloatInterface>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DISMISS:
				((OnDismissListener)msg.obj).onDismiss(mFloatView.get());
				break;
			case CANCEL:
				((OnCancelListener)msg.obj).onCancel(mFloatView.get());
				break;
			case SHOW:
				((onShowListener)msg.obj).onShow(mFloatView.get());
				break;
			}
		}
	}
	
	public int getAnimationStyle() {
		return mAnimationStyle;
	}

	public void setAnimationStyle(int mAnimationStyle) {
		this.mAnimationStyle = mAnimationStyle;
	}
	
	private int computeAnimationResource() {
        if (mAnimationStyle == -1) {
        	return showFromTop ? R.style.Animation_DropDownUp_FloatView : R.style.Animation_tws_DropDownDown_FloatView;
        }
        return mAnimationStyle;
    }

	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		if (Math.abs(velocityX) > 2000.0f || Math.abs(velocityY) > 2000.0f) {
			dismiss();
		}
		return false;
	}

	
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		switch (e1.getAction()) {
		case MotionEvent.ACTION_DOWN:
			xInView = e1.getX();
			yInView = e1.getY();
			xDownInScreen = e1.getRawX();
			yDownInScreen = e1.getRawY() - getStatusBarHeight();
			xInScreen = e1.getRawX();
			yInScreen = e1.getRawY() - getStatusBarHeight();
			break;
		}
		switch (e2.getAction()) {
		case MotionEvent.ACTION_MOVE:
			xInScreen = e2.getRawX();
			yInScreen = e2.getRawY() - getStatusBarHeight();
			updateFloatViewPosition();
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null && mWindow.getCurrentFocus() != null) {
				imm.hideSoftInputFromWindow(mWindow.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
			break;
		}
		return false;
	}

	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	// update 
	public void update(int resId, String title, String subtitle) {
		Drawable drawable = mContext.getResources().getDrawable(resId);
		setSlideOutAnimator(drawable, title, subtitle);
	}
	
	public void update(Drawable drawable, String title, String subtitle) {
		setSlideOutAnimator(drawable, title, subtitle);
	}
	
	public void update(int layoutId){
		View view = LayoutInflater.from(mContext).inflate(layoutId, null);
		update(view);
	}
	
	public void update(View customView) {
		dismiss();
		setContentView(customView);
		show(true);
	}
	
	private boolean isAnimatorFinish = false;
	public boolean animatorIsFinish() {
		return isAnimatorFinish;
	}
	
	public void setSlideOutAnimator(final Drawable drawable, final String title, final String subtitle) {
		Log.i(TAG, "setSlideOutAnimator");
		
		Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.floatview_shrink_fade_out);
//		anim.setDuration(1000);
		anim.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation arg0) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation arg0) {
//				isAnimatorFinish = true;
				
				mDefaultIcon.setImageDrawable(drawable);
				mDefaultTitle.setText(title);
				mDefaultSubTitle.setText(subtitle);
				
				setSlideInAnimator();
			}
		});
		Log.i(TAG, "setSlideOutAnimator mCustomFloatView before");
		if(mDefaultContent != null){
			Log.d(TAG, "setSlideOutAnimator mCustomFloatView ");
			mDefaultContent.startAnimation(anim);
		}
		Log.i(TAG, "setSlideOutAnimator mCustomFloatView after");
	}
	
	public void setSlideInAnimator() {
		Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.floatview_grow_fade_in);
		anim.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation arg0) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation arg0) {
				
			}
		});
		if(mDefaultContent != null){
			mDefaultContent.startAnimation(anim);
		}
	}
	
	private int getScreenDensity() {
		int widthPixels = 0;
		android.view.WindowManager manager = (android.view.WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		if (manager != null) {
			DisplayMetrics dm = new DisplayMetrics();
			Display display = manager.getDefaultDisplay();
			if (display != null) {
				manager.getDefaultDisplay().getMetrics(dm);
				widthPixels = dm.widthPixels;
				float originDensity = dm.density;
				float adaptDensity = 0;
				if (widthPixels >= WIDTH_XXH) {
					adaptDensity = DENSITY_XXH;
				} else if (widthPixels >= WIDTH_XH) {
					adaptDensity = DENSITY_XH;
				} else if (widthPixels >= WIDTH_H) {
					adaptDensity = DENSITY_H;
				}
//				int temp = (int)(originDensity > adaptDensity ? originDensity : adaptDensity);
//				Log.e("bruce", "widthPixels = " + widthPixels + "; adaptDensity = " + adaptDensity + 
//						"; originDensity = " + originDensity + "; temp = " + temp);
				return (int)(originDensity)/*(originDensity > adaptDensity ? originDensity : adaptDensity)*/;
			}
		}
		return 0;
	}

	private void setFloatViewAnimator() {
		AnimatorSet set = new AnimatorSet();
		ObjectAnimator anim1 = ObjectAnimator.ofFloat(mCustomFloatView, "alpha", 1f, 0f);
		anim1.setDuration(2000);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(mCustomFloatView, "alpha", 0f, 1f);
		anim2.setDuration(2000);
		set.play(anim1).before(anim2);
		set.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationCancel(Animator arg0) {
				
			}
		});
		set.start();
		
	}
}
//tws-end add global float view::2014-09-13