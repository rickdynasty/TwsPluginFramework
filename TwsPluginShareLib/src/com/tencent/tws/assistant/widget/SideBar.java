package com.tencent.tws.assistant.widget;

import com.tencent.tws.sharelib.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.PixelFormat;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class SideBar extends View {
	public class Entrie {
		public Entrie(CharSequence ch) {
			letter = ch;
		}

		CharSequence letter = " ";
		boolean exist = true;
	}

	private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
	private final String[] DEFAULT_ENTRIES_CSP = { "★", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
			"X", "Y", "Z", "#" };
	private final String[] DEFAULT_ENTRIES = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
			"Z", "#" };

	public static final String TAG = "SideBar";

	private int mCurrentLetterIndex = 0;
	private Paint mNormalPaint = new Paint();
	private Paint mSelectedPaint = new Paint();
	private Paint mNonExistPaint = new Paint();
	private int mNormalColor;
	private int mSelectedColor;
	private int mNonExistColor;

	private int mItemHeight = 1;
	private int mSpace = 0;

	private Drawable mChildCheckedBackground = null;
	private Entrie[] mSideBarEntries = null;
	private Rect mChildRect = new Rect();
	private boolean mShowing;
	private boolean mReady;

	private AddWindow mAddWindow = new AddWindow();
	private HideWindow mHideWindow = new HideWindow();
	private RemoveWindow mRemoveWindow = new RemoveWindow();
	private Handler mHandler = new Handler();
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mLayoutParams;

	private boolean mHasDialog = true;
	private boolean mUpdateEnable = true;

	private boolean mIsCustomChanger = false;
	private int mRealCurrentIndex = -1;
	private boolean mCurrentIndexNotExist = false;

	// hubble
	private SidebarHubbleView mHubbleView = null;
	private int mDelayTime = 200;

	private final int fSideBarHubbleViewWith = (int) getResources().getDimension(R.dimen.sidbar_hubble_w);
	private final int fSideBarHubbleViewHeigh = (int) getResources().getDimension(R.dimen.sidbar_hubble_h);
	private final float SIDEBAR_LETTER_SIZE = getResources().getDimension(R.dimen.sidbar_letter_size);
	private final int SIDEBAR_PADDING_RIGHT = (int) getResources().getDimension(R.dimen.sidbar_padding_right);
	private final int SIDEBAR_PADDING_TOP = (int) getResources().getDimension(R.dimen.sidbar_padding_top);
	private final int SIDEBAR_PADDING_BOTTOM = (int) getResources().getDimension(R.dimen.sidbar_padding_bottom);

	// property
	private int mScreenH;
	// in Screen
	private int mSideBarGlobalVisibleTop = 0;
	private FontMetricsInt mFontMetricsInt = null;

	// value
	private int mSidebarPaddingTop = 0;
	private int mSidebarPaddingBottom = 0;
	private boolean mIsCSP = false;

	public SideBar(Context context) {
		this(context, null);
	}

	public SideBar(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.SideBarStyle);
	}

	@SuppressWarnings("deprecation")
	public SideBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParams = new WindowManager.LayoutParams(fSideBarHubbleViewWith, fSideBarHubbleViewHeigh, WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
		mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		mLayoutParams.x = mWindowManager.getDefaultDisplay().getWidth() - fSideBarHubbleViewWith;

		TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.SideBar, defStyle, 0);
		mChildCheckedBackground = attributesArray.getDrawable(R.styleable.SideBar_childSelectedBackground);
		mNormalColor = attributesArray.getColor(R.styleable.SideBar_childNormalTextColor, 0xcc000000);
		mSelectedColor = attributesArray.getColor(R.styleable.SideBar_childSelectedTextColor, 0xff22b2b6);
		mNonExistColor = attributesArray.getColor(R.styleable.SideBar_childNonExistTextColor, 0x4c000000);

		int hubbleNormalBgColor = attributesArray.getColor(R.styleable.SideBar_hubbleNormalBackground, 0xFF000000);
		int hubbleNonExistBgColor = attributesArray.getColor(R.styleable.SideBar_hubbleNonExistBackground, 0xFFe5e5e5);
		int hubbleNormalTextColor = attributesArray.getColor(R.styleable.SideBar_hubbleNormalTextColor, 0xFFFFFFFF);
		int hubbleNonExistTextColor = attributesArray.getColor(R.styleable.SideBar_hubbleNonExistTextColor, 0xFFFFFFFF);

		CharSequence[] sideBarEntries = attributesArray.getTextArray(R.styleable.SideBar_android_entries);
		initSideBarEntries();
		if (sideBarEntries != null) {
			updateEntriesPropWithContentArray(sideBarEntries);
		}
		attributesArray.recycle();

		mHubbleView = new SidebarHubbleView(context);
		mHubbleView.setNormalTextColor(hubbleNormalTextColor);
		mHubbleView.setNormalBgColor(hubbleNormalBgColor);
		mHubbleView.setNonExistTextColor(hubbleNonExistTextColor);
		mHubbleView.setNonExistBgColor(hubbleNonExistBgColor);

		mNormalPaint.setAntiAlias(true);
		mNormalPaint.setTextAlign(Align.CENTER);
		mNormalPaint.setColor(mNormalColor);
		mNormalPaint.setTextSize(SIDEBAR_LETTER_SIZE);
		mSelectedPaint.setAntiAlias(true);
		mSelectedPaint.setTextAlign(Align.CENTER);
		mSelectedPaint.setColor(mSelectedColor);
		mSelectedPaint.setTextSize(SIDEBAR_LETTER_SIZE);
		mNonExistPaint.setAntiAlias(true);
		mNonExistPaint.setTextAlign(Align.CENTER);
		mNonExistPaint.setColor(mNonExistColor);
		mNonExistPaint.setTextSize(SIDEBAR_LETTER_SIZE);

		Log.d(TAG, "SIDEBAR_LETTER_SIZE=" + SIDEBAR_LETTER_SIZE + " SIDEBAR_PADDING_RIGHT=" + SIDEBAR_PADDING_RIGHT + " SIDEBAR_PADDING_TOP=" + SIDEBAR_PADDING_TOP
				+ " SIDEBAR_PADDING_BOTTOM=" + SIDEBAR_PADDING_BOTTOM);
	}

	public void setIsCSP(boolean isCSP) {
		if (mIsCSP != isCSP) {
			mIsCSP = isCSP;
			initSideBarEntries();
		}
	}

	public void setNormalColor(int color) {
		if (mNormalColor != color) {
			mNormalColor = color;
			mNormalPaint.setColor(mNormalColor);
		}
	}

	public void setSelectedColor(int color) {
		if (mSelectedColor != color) {
			mSelectedColor = color;
			mSelectedPaint.setColor(mSelectedColor);
		}
	}

	public void setNonExistColor(int color) {
		if (mNonExistColor != color) {
			mNonExistColor = color;
			mNonExistPaint.setColor(mNonExistColor);
		}
	}

	public void setHubbleNormalTextColor(int color) {
		if (mHubbleView != null)
			mHubbleView.setNormalTextColor(color);
	}

	public void setHubbleNonExistTextColor(int color) {
		if (mHubbleView != null)
			mHubbleView.setNonExistTextColor(color);
	}

	public void setHubbleNormalBgColor(int color) {
		if (mHubbleView != null)
			mHubbleView.setNormalBgColor(color);
	}

	public void setHubbleNonExistBgColor(int color) {
		if (mHubbleView != null)
			mHubbleView.setNonExistBgColor(color);
	}

	private void initSideBarEntries() {
		mSideBarEntries = null;
		if (mIsCSP) {
			mSideBarEntries = new Entrie[DEFAULT_ENTRIES_CSP.length];
			for (int index = 0; index < DEFAULT_ENTRIES_CSP.length; index++) {
				mSideBarEntries[index] = new Entrie(DEFAULT_ENTRIES_CSP[index]);
			}
		} else {
			mSideBarEntries = new Entrie[DEFAULT_ENTRIES.length];
			for (int index = 0; index < DEFAULT_ENTRIES.length; index++) {
				mSideBarEntries[index] = new Entrie(DEFAULT_ENTRIES[index]);
			}
		}
	}

	public void updateEntriesPropWithContentArray(CharSequence[] contentLetters) {
		changeEntriesToNotExist();
		char ch;
		for (int index = 0; index < contentLetters.length; index++) {
			if (contentLetters[index] != null && 0 < contentLetters[index].length()) {
				ch = Character.toUpperCase(contentLetters[index].charAt(0));
				updateEntriesPropWithCharValue(ch);
			}
		}
	}

	public void updateEntriesPropForAddOneContent(CharSequence content) {
		if (content != null && 0 < content.length()) {
			updateEntriesPropWithCharValue(content.charAt(0));
		}
	}

	private final char CHAR_A = 'A';
	private final char CHAR_Z = 'Z';

	private void updateEntriesPropWithCharValue(char ch) {
		if (ch < CHAR_A) {
			updateExistEntries(mIsCSP ? DEFAULT_ENTRIES_CSP.length - 1 : DEFAULT_ENTRIES.length - 1);
		} else if (CHAR_Z < ch) {
			updateExistEntries((mIsCSP && ch == DEFAULT_ENTRIES_CSP[0].charAt(0)) ? 0 : DEFAULT_ENTRIES.length - 1);
		} else {
			updateExistEntries(mIsCSP ? 1 + ch - CHAR_A : ch - CHAR_A);
		}
	}

	private void updateExistEntries(int index) {
		if (0 <= index && index < mSideBarEntries.length) {
			mSideBarEntries[index].exist = true;
		}
	}

	private void changeEntriesToNotExist() {
		for (int index = 0; index < mSideBarEntries.length; index++)
			mSideBarEntries[index].exist = false;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mReady = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		hideHubbleWithAnimation(false);
		mReady = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		final int width = getWidth();
		if (mFontMetricsInt == null)
			mFontMetricsInt = mNormalPaint.getFontMetricsInt();

		// mChildRect.set(0, 0, width, getHeight() - mSidebarPaddingTop - mSidebarPaddingBottom);
		for (int i = 0; i < mSideBarEntries.length; i++) {
			float baseLine = mSidebarPaddingTop + mSpace + i * mItemHeight + (mItemHeight - mFontMetricsInt.bottom - mFontMetricsInt.top) / 2;
			if (i == mCurrentLetterIndex && mUpdateEnable && mSideBarEntries[i].exist) {
				if (mChildCheckedBackground != null) {
					mChildRect.set((width - mItemHeight) / 2, (int) (mSpace + mItemHeight * i), (width + mItemHeight) / 2, (int) (mSpace + mItemHeight * (i + 1)));
					mChildCheckedBackground.setBounds(mChildRect);
					mChildCheckedBackground.draw(canvas);
				}

				canvas.drawText(mSideBarEntries[i].letter + "", width / 2, baseLine, mSelectedPaint);
			} else {
				if (mSideBarEntries[i].exist)
					canvas.drawText(mSideBarEntries[i].letter + "", width / 2, baseLine, mNormalPaint);
				else
					canvas.drawText(mSideBarEntries[i].letter + "", width / 2, baseLine, mNonExistPaint);
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		super.dispatchTouchEvent(event);
		hideSoftKeyboard();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			setPressed(true);
			handleTouchIndex(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			setPressed(false);
			hideWindow();
			if (onTouchingLetterChangedListener != null) {
				onTouchingLetterChangedListener.onTouchUp();
			}
			break;
		default:
			break;
		}

		return true;
	}

	private void handleTouchIndex(MotionEvent event) {
		int index = Math.max(0, Math.min((int) ((event.getY() - mSpace - mSidebarPaddingTop) / mItemHeight), mSideBarEntries.length - 1));
		String letterString = mSideBarEntries[index].letter + "";

		if (mFontMetricsInt == null)
			mFontMetricsInt = mNormalPaint.getFontMetricsInt();

		float baseLine = mSidebarPaddingTop + mSpace + index * mItemHeight + (mItemHeight - mFontMetricsInt.bottom - mFontMetricsInt.top) / 2;
		mLayoutParams.y = mSideBarGlobalVisibleTop + (int) baseLine - fSideBarHubbleViewHeigh / 2 - (mFontMetricsInt.bottom - mFontMetricsInt.top) / 3;
		if (mIsCustomChanger) {
			if (onTouchingLetterChangedListener != null) {
				onTouchingLetterChangedListener.onTouchingLetterChanged(index);
				onTouchingLetterChangedListener.onTouchingLetterChanged(letterString);
			}
			if (mCurrentIndexNotExist) {
				mCurrentIndexNotExist = false;
			} else {
				mRealCurrentIndex = index;
			}
			if (mReady && mHasDialog) {
				updateHubble(mLayoutParams, index);
			}
		} else {
			if (mReady && mHasDialog) {
				updateHubble(mLayoutParams, index);
			}
			if (onTouchingLetterChangedListener != null) {
				onTouchingLetterChangedListener.onTouchingLetterChanged(index);
				onTouchingLetterChangedListener.onTouchingLetterChanged(letterString);
			}
		}
		mCurrentLetterIndex = index;

		invalidate();
	}

	private void hideWindow() {
		if (mHandler != null && mHubbleView != null) {
			mHandler.removeCallbacks(mHideWindow);
			int restInTime = (int) (mHubbleView.theRestOfInAnim() * mHubbleView.getHubbleInPerFrame() * SidebarHubbleView.IN_FRAME_COUNT);
			mHandler.postDelayed(mHideWindow, restInTime + mDelayTime);
		}
	}

	@Override
	public void layout(int l, int t, int r, int b) {
		super.layout(l, t, r, b);
		getDisplayInfo();
	}

	@SuppressWarnings("deprecation")
	private void getDisplayInfo() {
		mScreenH = mWindowManager.getDefaultDisplay().getHeight();
		Rect outRect = new Rect();
		getGlobalVisibleRect(outRect);
		mSidebarPaddingTop = Math.max(SIDEBAR_PADDING_TOP - outRect.top, 0);
		mSideBarGlobalVisibleTop = outRect.top;
		mSidebarPaddingBottom = Math.max(SIDEBAR_PADDING_BOTTOM + outRect.bottom - mScreenH, 0);

		int height = getHeight() - mSidebarPaddingTop - mSidebarPaddingBottom;
		int size = mSideBarEntries.length;
		mItemHeight = Math.max(height / size, 1);
		mSpace = (height - mItemHeight * size) / 2;
	}

	private void updateHubble(WindowManager.LayoutParams layoutParams, int index) {
		if (mHubbleView != null) {
			String letterString = mSideBarEntries[index].letter + "";
			mHubbleView.setText(letterString);
			mHubbleView.setIndexIsExsit(mSideBarEntries[index].exist);
		}

		clearCallbacks();
		if (mHandler != null) {
			mHubbleView.stopOut();

			mHandler.post(mAddWindow);
		}
	}

	private void hideSoftKeyboard() {
		// Hide soft keyboard, if visible
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
		this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
	}

	public void updateCurrentIndex(int index) {
		mCurrentLetterIndex = index;
		invalidate();
	}

	public void updateCurrentIndex(String indexString) {
		if (TextUtils.isEmpty(indexString)) {
			return;
		}

		for (int i = 0; i < mSideBarEntries.length; i++) {
			if (TextUtils.equals(mSideBarEntries[i].letter, indexString)) {
				mCurrentLetterIndex = i;
				break;
			}
		}
		invalidate();
	}

	public void setUpdateEnable(boolean enable) {
		mUpdateEnable = enable;
	}

	public void setSideBarEntries(CharSequence[] sideBarEntries) {
		updateEntriesPropWithContentArray(sideBarEntries);
		// mSideBarEntries = sideBarEntries;
		invalidate();
	}

	public CharSequence[] getSideBarEntries() {
		if (mIsCSP)
			return DEFAULT_ENTRIES_CSP;
		else
			return DEFAULT_ENTRIES;
	}

	public void setHasDialog(boolean hasDialog) {
		mHasDialog = hasDialog;
		if (!hasDialog) {
			hideHubbleWithAnimation(false);
		}
	}

	public boolean getHasDialog() {
		return mHasDialog;
	}

	private final class RemoveWindow implements Runnable {
		public void run() {
			if (mHubbleView != null) {
				mHubbleView.setVisibility(View.INVISIBLE);
				try {
					if (mWindowManager != null) {
						mShowing = false;
						mWindowManager.removeView(mHubbleView);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class HideWindow implements Runnable {
		public void run() {
			hideHubbleWithAnimation(true);
		}
	}

	private final class AddWindow implements Runnable {
		public void run() {
			if (mWindowManager != null && mHubbleView != null && mLayoutParams != null) {
				mShowing = mReady = true;

				ViewParent viewParent = mHubbleView.getParent();

				mHubbleView.setVisibility(View.VISIBLE);
				if (viewParent != null) {
					mWindowManager.updateViewLayout(mHubbleView, mLayoutParams);
				} else {
					mWindowManager.addView(mHubbleView, mLayoutParams);
					mHubbleView.showHubbleWithAnimation();
				}
			}
		}

	}

	public int getDialogDelayTime() {
		return mDelayTime;
	}

	public void setDialogDelayTime(int delayTime) {
		mDelayTime = delayTime;
	}

	private void hideHubbleWithAnimation(boolean hasAnimation) {
		clearCallbacks();

		if (!hasAnimation || !mShowing) {
			if (mHubbleView != null)
				mHubbleView.stopOut();

			if (mHandler != null)
				mHandler.post(mRemoveWindow);

			return;
		}

		if (hasAnimation) {
			if (mHubbleView != null && mHandler != null){
				mHubbleView.hidHubbleWithAnimation();
				mHandler.postDelayed(mRemoveWindow, mHubbleView.getHubbleOutPerFrame() * SidebarHubbleView.OUT_FRAME_COUNT);
			}
		} else {
			if (mHubbleView != null)
				mHubbleView.stopOut();

			if (mHandler != null) {
				mHandler.post(mRemoveWindow);
			}
		}
	}
	
	private void clearCallbacks() {
		if (mHandler != null) {
			mHandler.removeCallbacks(mHideWindow);
			mHandler.removeCallbacks(mRemoveWindow);
			mHandler.removeCallbacks(mAddWindow);
		}
	}

	// Compatible with the old V
	public void hideDialog(boolean hasAnimation) {
		hideHubbleWithAnimation(hasAnimation);
	}

	public interface OnTouchingLetterChangedListener {
		public void onTouchUp();

		public void onTouchingLetterChanged(String touchIndexString);

		public void onTouchingLetterChanged(int letterIndex);
	}

	public void setCustomChanger(boolean isCustom) {
		mIsCustomChanger = isCustom;
	}

	public void setRealCurrentIndex(int realCurrentIndex) {
		mCurrentIndexNotExist = true;
		mRealCurrentIndex = realCurrentIndex;
	}
}
