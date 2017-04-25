/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.tencent.tws.sharelib.R;
import com.tencent.tws.assistant.app.AlertDialog.ButtonColor;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.CheckedTextView;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.TwsScrollView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;

public class AlertController {

	private final Context mContext;
	private final DialogInterface mDialogInterface;
	private final Window mWindow;

	private CharSequence mTitle;

	private CharSequence mMessage;

	private ListView mListView;

	private View mView;

	private int mViewSpacingLeft;

	private int mViewSpacingTop;

	private int mViewSpacingRight;

	private int mViewSpacingBottom;

	private boolean mViewSpacingSpecified = false;

	private Button mButtonPositive;

	private CharSequence mButtonPositiveText;

	private Message mButtonPositiveMessage;

	private ButtonColor mPositiveColor;

	private Button mButtonNegative;

	private CharSequence mButtonNegativeText;

	private Message mButtonNegativeMessage;

	private ButtonColor mNegativeColor;

	private Button mButtonNeutral;

	private CharSequence mButtonNeutralText;

	private Message mButtonNeutralMessage;

	private ButtonColor mNeutralColor;

	private TwsScrollView mScrollView;

	private int mIconId = -1;

	private Drawable mIcon;

	private ImageView mIconView;

	private TextView mTitleView;
	private float mTitleTextSize;
	private int mTitleTextColor;
	private boolean mCustomSettitleTextColor = false;
	private boolean mCustomSettitleTextSize = false;

	private TextView mMessageView;

	private View mMsgSpacer;

	private View mCustomTitleView;

	private boolean mForceInverseBackground;

	private boolean mButtonBGSet;

	/* tws-start::add for special background for tws style contextmenu */
	public boolean mIsContextMenu = false;
	private int mListViewHeight = -1;
	/* NANJIEMD::froyohuang::2013.4.7 */

	private ListAdapter mAdapter;

	private int mCheckedItem = -1;

	private int mAlertDialogLayout;
	private int mListLayout;
	private int mMultiChoiceItemLayout;
	private int mSingleChoiceItemLayout;
	private int mListItemLayout;

	private Handler mHandler;
	// tws-start bottom dialog::2014-10-1
	private ListView mBottomButtonsListView;
	private ListAdapter mBottomButtonAdapter;
	private CharSequence[] mBottomButtonItems;
	private LinearLayout mBottomButtonsPanel;
	private View mBottomDivider;
	private boolean mBottomButtonsVisible = true;
	private int mBottomButtonLayout;
	private int mBottomButtonItemLayout;
	private int mBottomButtonSingleChoiceItemLayout;
	private int mBottomButtonMultiChoiceItemLayout;
	private int mBottomButtonCheckedItem = -1;
	private boolean mIsBottomDialog = false;
	// tws-end bottom dialog::2014-10-1
	private static final int DEFAULT_ANIMATION_DUR = 160;
	private static final int DEFAULT_ANIMATION_DELAY_TIME = 60;
	private static final int DEFAULT_ANIMATION_SHORT_DUR = 130;
	private static final int DEFAULT_ANIMATION_SHORT_DELAY_TIME = 30;
	private static final int DEFAULT_ITEM_COUNT = 3;

	private static final int DEFAULT_LISTITEM_COUNT = 6;

	private static int mBottomButtonAnimationDur = DEFAULT_ANIMATION_DUR;
	private static int mBottomButtonAnimationDelayTime = DEFAULT_ANIMATION_DELAY_TIME;
	private static int mBottomButtonAnimationShortDur = DEFAULT_ANIMATION_SHORT_DUR;
	private static int mBottomButtonAnimationShortDelayTime = DEFAULT_ANIMATION_SHORT_DELAY_TIME;

	private static int paddingLeft;
	private static int paddingRight;
	private static int mListItemHeight;
	private static int mListSpace;

	View.OnClickListener mButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			Message m = null;
			if (v == mButtonPositive && mButtonPositiveMessage != null) {
				m = Message.obtain(mButtonPositiveMessage);
			} else if (v == mButtonNegative && mButtonNegativeMessage != null) {
				m = Message.obtain(mButtonNegativeMessage);
			} else if (v == mButtonNeutral && mButtonNeutralMessage != null) {
				m = Message.obtain(mButtonNeutralMessage);
			}
			boolean bRipple = ThemeUtils.isShowRipple(mContext);
			if (bRipple) {
				if (m != null) {
					mHandler.sendMessageDelayed(m, 175);
				}

				// Post a message so we dismiss after the above handlers are
				// executed
				mHandler.sendMessageDelayed(mHandler.obtainMessage(ButtonHandler.MSG_DISMISS_DIALOG, mDialogInterface),
						175);
			} else {
				if (m != null) {
					m.sendToTarget();
				}

				// Post a message so we dismiss after the above handlers are
				// executed
				mHandler.obtainMessage(ButtonHandler.MSG_DISMISS_DIALOG, mDialogInterface).sendToTarget();
			}

		}
	};

	private static final class ButtonHandler extends Handler {
		// Button clicks have Message.what as the BUTTON{1,2,3} constant
		private static final int MSG_DISMISS_DIALOG = 1;

		private WeakReference<DialogInterface> mDialog;

		public ButtonHandler(DialogInterface dialog) {
			mDialog = new WeakReference<DialogInterface>(dialog);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case DialogInterface.BUTTON_POSITIVE:
			case DialogInterface.BUTTON_NEGATIVE:
			case DialogInterface.BUTTON_NEUTRAL:
				((DialogInterface.OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
				break;

			case MSG_DISMISS_DIALOG:
				((DialogInterface) msg.obj).dismiss();
			}
		}
	}

	private static boolean shouldCenterSingleButton(Context context) {
		/*
		 * TypedValue outValue = new TypedValue();
		 * context.getTheme().resolveAttribute
		 * (android.R.attr.alertDialogCenterButtons, outValue, true); return
		 * outValue.data != 0;
		 */
		return false;
	}

	// tws-start bottom dialog::2014-10-3
	public AlertController(Context context, DialogInterface di, Window window) {
		this(context, di, window, false);
	}

	public AlertController(Context context, DialogInterface di, Window window, boolean isBottomDialog) {
		mContext = context;
		mDialogInterface = di;
		mWindow = window;
		mHandler = new ButtonHandler(di);

		// tws-start::added for tws theme::20121002
		mAlertDialogLayout = R.layout.tws_alert_dialog_holo;
		mListLayout = R.layout.select_dialog;
		mMultiChoiceItemLayout = R.layout.select_dialog_multichoice;
		mSingleChoiceItemLayout = R.layout.select_dialog_singlechoice;
		mListItemLayout = R.layout.select_dialog_item;

		paddingLeft = (int) mContext.getResources().getDimension(R.dimen.tws_listview_item_padding_side_left);
		paddingRight = (int) mContext.getResources().getDimension(R.dimen.tws_listview_item_padding_side_right);
		mListItemHeight = (int) mContext.getResources().getDimension(R.dimen.tws_listview_item_height);
		mListSpace = (int) mContext.getResources().getDimension(R.dimen.preference_item_empty_padding);
		// tws-start bottom dialog::2014-10-1
		mBottomButtonLayout = R.layout.bottom_select_dialog;
		mBottomButtonItemLayout = R.layout.bottom_select_dialog_item;
		mBottomButtonSingleChoiceItemLayout = R.layout.bottom_select_dialog_singlechoice;
		mBottomButtonMultiChoiceItemLayout = R.layout.bottom_select_dialog_multichoice;
		mIsBottomDialog = isBottomDialog;
		mBottomButtonsVisible = true;
		// tws-end bottom dialog::2014-10-1
		context.setTheme(AlertDialog.resolveDialogTheme(context, context.getThemeResId()));
		// Log.d("alertcontrol", "alertcontrol-------2--" +
		// context.getThemeResId());
		mBottomButtonAnimationDur = context.getResources().getInteger(R.integer.config_listItemDefaultDur);
		if (mBottomButtonAnimationDur < 1) {
			mBottomButtonAnimationDur = DEFAULT_ANIMATION_DUR;
		}
		mBottomButtonAnimationShortDur = context.getResources().getInteger(R.integer.config_listItemShortDur);
		if (mBottomButtonAnimationShortDur < 1) {
			mBottomButtonAnimationShortDur = DEFAULT_ANIMATION_SHORT_DUR;
		}
		mBottomButtonAnimationDelayTime = context.getResources().getInteger(R.integer.config_listItemDefaultDelayTime);
		if (mBottomButtonAnimationDelayTime < 1) {
			mBottomButtonAnimationDelayTime = DEFAULT_ANIMATION_DELAY_TIME;
		}
		mBottomButtonAnimationShortDelayTime = context.getResources().getInteger(
				R.integer.config_listItemShortDelayTime);
		if (mBottomButtonAnimationShortDelayTime < 1) {
			mBottomButtonAnimationShortDelayTime = DEFAULT_ANIMATION_SHORT_DELAY_TIME;
		}
	}

	// tws-end bottom dialog::2014-10-3

	static boolean canTextInput(View v) {
		if (v.onCheckIsTextEditor()) {
			return true;
		}

		if (!(v instanceof ViewGroup)) {
			return false;
		}

		ViewGroup vg = (ViewGroup) v;
		int i = vg.getChildCount();
		while (i > 0) {
			i--;
			v = vg.getChildAt(i);
			if (canTextInput(v)) {
				return true;
			}
		}

		return false;
	}

	public void installContent() {
		/* We use a custom title so never request a window title */
		mWindow.requestFeature(Window.FEATURE_NO_TITLE);

		if (mView == null || !canTextInput(mView)) {
			mWindow.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
					WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		}
		// tws-start bottom dialog::2014-10-3
		View contentView = LayoutInflater.from(mContext).inflate(mAlertDialogLayout, null);
		if (mIsBottomDialog) {
			mWindow.setContentView(contentView);
		} else {
			mWindow.setContentView(mAlertDialogLayout);
		}
		// tws-end bottom dialog::2014-10-3
		setupView();
	}

	public void setTitle(CharSequence title) {
		mTitle = title;
		if (mTitleView != null) {
			mTitleView.setText(title);
		}
	}

	public void setTitleTextSize(float size) {
		mTitleTextSize = size;
		mCustomSettitleTextSize = true;

		if (mTitleView != null) {
			mTitleView.setTextSize(size);
		}
	}

	public void setTitleTextColor(int color) {
		mTitleTextColor = color;
		mCustomSettitleTextColor = true;

		if (mTitleView != null) {
			mTitleView.setTextColor(color);
		}
	}

	/**
	 * @see AlertDialog.Builder#setCustomTitle(View)
	 */
	public void setCustomTitle(View customTitleView) {
		mCustomTitleView = customTitleView;
	}

	public void setMessage(CharSequence message) {
		mMessage = message;
		if (mMessageView != null) {
			mMessageView.setText(message);
		}
	}

	/**
	 * Set the view to display in the dialog.
	 */
	public void setView(View view) {
		mView = view;
		mViewSpacingSpecified = false;
	}

	/**
	 * Set the view to display in the dialog along with the spacing around that
	 * view
	 */
	public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
		mView = view;
		mViewSpacingSpecified = true;
		mViewSpacingLeft = viewSpacingLeft;
		mViewSpacingTop = viewSpacingTop;
		mViewSpacingRight = viewSpacingRight;
		mViewSpacingBottom = viewSpacingBottom;
	}

	/**
	 * Sets a click listener or a message to be sent when the button is clicked.
	 * You only need to pass one of {@code listener} or {@code msg}.
	 * 
	 * @param whichButton
	 *            Which button, can be one of
	 *            {@link DialogInterface#BUTTON_POSITIVE},
	 *            {@link DialogInterface#BUTTON_NEGATIVE}, or
	 *            {@link DialogInterface#BUTTON_NEUTRAL}
	 * @param text
	 *            The text to display in positive button.
	 * @param listener
	 *            The {@link DialogInterface.OnClickListener} to use.
	 * @param msg
	 *            The {@link Message} to be sent when clicked.
	 */
	public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener, Message msg) {
		setButton(ButtonColor.BTN_NORMAL, whichButton, text, listener, msg);
	}

	public void setButton(ButtonColor buttonColor, int whichButton, CharSequence text,
			DialogInterface.OnClickListener listener, Message msg) {

		if (msg == null && listener != null) {
			msg = mHandler.obtainMessage(whichButton, listener);
		}

		switch (whichButton) {

		case DialogInterface.BUTTON_POSITIVE:
			mButtonPositiveText = text;
			mButtonPositiveMessage = msg;
			mPositiveColor = buttonColor;
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			mButtonNegativeText = text;
			mButtonNegativeMessage = msg;
			mNegativeColor = buttonColor;
			break;

		case DialogInterface.BUTTON_NEUTRAL:
			mButtonNeutralText = text;
			mButtonNeutralMessage = msg;
			mNeutralColor = buttonColor;
			break;

		default:
			throw new IllegalArgumentException("Button does not exist");
		}
	}

	// tws-start bottom dialog::2014-10-14
	public void setBottomButtons(int bottomButtonItemsId, final OnClickListener onClickListener) {
		CharSequence[] bottomButtonItems = mContext.getResources().getTextArray(bottomButtonItemsId);
		setBottomButtons(bottomButtonItems, onClickListener);
	}

	public void setBottomButtons(CharSequence[] bottomButtonItems, final OnClickListener onClickListener) {
		setBottomButtons(bottomButtonItems, null, onClickListener);
	}

	public void setBottomButtons(int bottomButtonItemsId, int[] bottomButtonColorItems,
			final OnClickListener onClickListener) {
		CharSequence[] bottomButtonItems = mContext.getResources().getTextArray(bottomButtonItemsId);
		setBottomButtons(bottomButtonItems, bottomButtonColorItems, onClickListener);
	}

	public void setBottomButtons(CharSequence[] bottomButtonItems, final int[] bottomButtonColorItems,
			final OnClickListener onClickListener) {
		if (bottomButtonItems == null || bottomButtonItems.length <= 0) {
			return;
		}
		mBottomButtonItems = bottomButtonItems;
		final boolean hasTitle = ((mCustomTitleView != null) || (mTitle != null && !TextUtils.isEmpty(mTitle))
				|| (mIcon != null) || (mIconId > 0) || (mMessage != null && !TextUtils.isEmpty(mMessage)) || mView != null);
		final boolean hasButton = false;
		if (mBottomButtonsListView == null) {
			final ListView listView = (ListView) LayoutInflater.from(mContext).inflate(mBottomButtonLayout, null);
			boolean bRipple = ThemeUtils.isShowRipple(mContext);
			if (!bRipple) {
				listView.setSelector(mContext.getResources().getDrawable(R.drawable.dialog_list_selector_holo_light));
			}
			listView.setFooterDividersEnabled(false);
			listView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView parent, View v, int position, long id) {
					onClickListener.onClick(mDialogInterface, position);
					mHandler.sendMessageDelayed(
							mHandler.obtainMessage(ButtonHandler.MSG_DISMISS_DIALOG, mDialogInterface), 80);
					// mDialogInterface.dismiss();
				}
			});
			mBottomButtonsListView = listView;
		}
		ListAdapter listAdapter = new ArrayAdapter<CharSequence>(mContext, mBottomButtonItemLayout, R.id.text1,
				mBottomButtonItems) {
			final List<CharSequence> mList = Arrays.asList(mBottomButtonItems);

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				if (bottomButtonColorItems != null) {
					TextView tv = (TextView) view.findViewById(R.id.text1);
					setButtonColor(mContext, tv, bottomButtonColorItems[position]);
				}
				setListBackground(view, position, mList.size(), hasTitle, hasButton);
				return view;
			}
		};
		mBottomButtonAdapter = listAdapter;
		if (mBottomButtonsPanel != null) {
			boolean hasBottomButtons = setupBottomButtons(mBottomButtonsPanel);
			if (!hasBottomButtons || !mBottomButtonsVisible) {
				mBottomButtonsPanel.setVisibility(View.GONE);
			} else {
				setBottomButtonsStartAnimation(mBottomButtonsListView, mBottomButtonAdapter.getCount());
				mBottomButtonsPanel.setVisibility(View.VISIBLE);
			}
			if (mIsBottomDialog) {
				mWindow.setCloseOnTouchOutside(true);
			}
		}
	}

	public void setBottomButtonVisible(boolean isVisible) {
		mBottomButtonsVisible = isVisible;
		if (mBottomButtonsPanel != null) {
			mBottomButtonsPanel.setVisibility(isVisible ? View.VISIBLE : View.GONE);
		}
	}

	// tws-end bottom dialog::2014-10-14

	/**
	 * Set resId to 0 if you don't want an icon.
	 * 
	 * @param resId
	 *            the resourceId of the drawable to use as the icon or 0 if you
	 *            don't want an icon.
	 */
	public void setIcon(int resId) {
		mIconId = resId;
		if (mIconView != null) {
			if (resId > 0) {
				mIconView.setImageResource(mIconId);
			} else if (resId == 0) {
				mIconView.setVisibility(View.GONE);
			}
		}
	}

	public void setIcon(Drawable icon) {
		mIcon = icon;
		if ((mIconView != null) && (mIcon != null)) {
			mIconView.setImageDrawable(icon);
		}
	}

	public void setInverseBackgroundForced(boolean forceInverseBackground) {
		mForceInverseBackground = forceInverseBackground;
	}

	public ListView getListView() {
		return mListView;
	}

	public Button getButton(int whichButton) {
		switch (whichButton) {
		case DialogInterface.BUTTON_POSITIVE:
			return mButtonPositive;
		case DialogInterface.BUTTON_NEGATIVE:
			return mButtonNegative;
		case DialogInterface.BUTTON_NEUTRAL:
			return mButtonNeutral;
		default:
			return null;
		}
	}

	// tws-start bottom dialog::2014-10-1
	public ListView getBottomButtons() {
		return mBottomButtonsListView;
	}

	// tws-end bottom dialog::2014-10-1

	@SuppressWarnings({ "UnusedDeclaration" })
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return mScrollView != null && mScrollView.executeKeyEvent(event);
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return mScrollView != null && mScrollView.executeKeyEvent(event);
	}

	private void setupView() {
		LinearLayout contentPanel = (LinearLayout) mWindow.findViewById(R.id.contentPanel);
		setupContent(contentPanel);
		boolean hasButtons = setupButtons();

		LinearLayout topPanel = (LinearLayout) mWindow.findViewById(R.id.topPanel);
		// tws-start bottom dialog::2014-10-3
		int alertDialogStyle = com.android.internal.R.attr.alertDialogStyle;
		if (mIsBottomDialog) {
			alertDialogStyle = R.attr.bottomAlertDialogStyle;
		}
		TypedArray array = mContext.obtainStyledAttributes(null, com.android.internal.R.styleable.AlertDialog,
				alertDialogStyle, 0);
		// tws-end bottom dialog::2014-10-3
		boolean hasTitle = setupTitle(topPanel);

		View buttonPanel = mWindow.findViewById(R.id.buttonPanel);
		if (mBottomDivider == null) {
			mBottomDivider = mWindow.findViewById(R.id.bottomDividerTop);
		}
		
		if (!hasButtons) {
			buttonPanel.setVisibility(View.GONE);
			mWindow.setCloseOnTouchOutside(true);
		} else {
			buttonPanel.setVisibility(View.VISIBLE);
			mWindow.setCloseOnTouchOutside(false);
			mBottomDivider.setVisibility(View.VISIBLE);
		}

		LinearLayout customPanel = null;
		if (mView != null) {
			// Log.d("tws alertController","setupView--------1");
			customPanel = (LinearLayout) mWindow.findViewById(R.id.customPanel);
			FrameLayout custom = (FrameLayout) mWindow.findViewById(R.id.custom);
			custom.addView(mView, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
			if (mViewSpacingSpecified) {
				custom.setPadding(mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight, mViewSpacingBottom);
			}

			if (mView instanceof ListView) {
				ViewGroup.LayoutParams lp = custom.getLayoutParams();
				ListAdapter adapter = ((ListView) mView).getAdapter();
				if (adapter != null && !mIsBottomDialog) {
					int num = adapter.getCount();
					if (num > DEFAULT_LISTITEM_COUNT) {
						int totalHeight = 0;
						for (int i = 0; i < DEFAULT_LISTITEM_COUNT; i++) {
							View listItem = adapter.getView(i, null, (ListView) mView);
							listItem.measure(0, 0);
							totalHeight += listItem.getMeasuredHeight();
						}
						lp.height = totalHeight
								+ (((ListView) mView).getDividerHeight() * (DEFAULT_LISTITEM_COUNT - 1));
						custom.setLayoutParams(lp);
					}
				}
			} else {
				mView.measure(0, 0);
				int h = mView.getMeasuredHeight();
				int mMaxHeight = (int) mContext.getResources().getDimension(R.dimen.dialog_content_maxheight);
				if (mView.getMeasuredHeight() > mMaxHeight) {
					ViewGroup.LayoutParams lp = custom.getLayoutParams();
					lp.height = mMaxHeight;
					custom.setLayoutParams(lp);
				}
			}

			if (mListView != null) {
				((LinearLayout.LayoutParams) customPanel.getLayoutParams()).weight = 0;
			}
		} else {
			// Log.d("tws alertController","setupView--------2");
			mWindow.findViewById(R.id.customPanel).setVisibility(View.GONE);
		}

		/*
		 * Only display the divider if we have a title and a custom view or a
		 * message.
		 */
		if (hasTitle) {
			View divider = null;
			if (mMessage != null || mView != null || mListView != null) {
				divider = mWindow.findViewById(R.id.titleDivider);
			} else {
				// divider = mWindow.findViewById(R.id.titleDividerTop);
			}

			if (divider != null) {
				// tws-start icon divider::2014-7-22
				//2017-04-17 modify by yongchen for design
				divider.setVisibility(View.GONE);
				
//				final boolean hasTextTitle = !TextUtils.isEmpty(mTitle);
//				if (hasTextTitle) {
//					divider.setVisibility(View.VISIBLE);
//				} else {
//					divider.setVisibility(View.GONE);
//				}
				// tws-end icon divider::2014-7-22
			}

			if (mListView == null) {
				if (mView == null && TextUtils.isEmpty(mMessage)) {
					View titleSpacer = mWindow.findViewById(R.id.titleSpacer);
					if (titleSpacer != null) {
						titleSpacer.setVisibility(View.VISIBLE);
					}
				}
			} else {
				int height = divider.getBackground().getIntrinsicHeight();
				divider.setMinimumHeight(height);
			}

		} else {
			if (mView == null) {
				mMsgSpacer.setVisibility(View.VISIBLE);
			}
		}
		mBottomButtonsPanel = (LinearLayout) mWindow.findViewById(R.id.dialog_bottom_buttons_panel);
		if (mBottomDivider == null) {
			mBottomDivider = mWindow.findViewById(R.id.bottomDividerTop);
		}
		boolean hasBottomButtons = setupBottomButtons(mBottomButtonsPanel);
		if (!hasBottomButtons || !mBottomButtonsVisible) {
			mBottomButtonsPanel.setVisibility(View.GONE);
		} else {
			mBottomButtonsPanel.setVisibility(View.VISIBLE);
			mBottomDivider.setVisibility(View.VISIBLE);
		}
		if (mIsBottomDialog) {
			mWindow.setCloseOnTouchOutside(true);
		}

		setBackground(topPanel, contentPanel, customPanel, hasButtons, array, hasTitle, buttonPanel,
				mBottomButtonsPanel, hasBottomButtons);
		if (hasButtons) {
			buttonPanel.setPadding(0, 0, 0, 0);
		}
		array.recycle();
	}

	private boolean setupTitle(LinearLayout topPanel) {
		boolean hasTitle = true;

		// Hide the title template
		View titleTemplate = mWindow.findViewById(R.id.title_template);
		if (mCustomTitleView != null) {
			// Add the custom title view directly to the topPanel layout
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);

			topPanel.addView(mCustomTitleView, 0, lp);

			titleTemplate.setVisibility(View.GONE);
		} else {
			final boolean hasTextTitle = !TextUtils.isEmpty(mTitle);

			mIconView = (ImageView) mWindow.findViewById(R.id.icon);
			mTitleView = (TextView) mWindow.findViewById(R.id.alertTitle);
			mIconView.setVisibility(View.GONE);
			if (hasTextTitle) {
				/* Display the title if a title is supplied, else hide it */
				mTitleView.setText(mTitle);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.gravity = Gravity.CENTER;
				int lineCount = mTitleView.getLineCount();
				if (lineCount > 1) {
					lp.gravity = Gravity.LEFT;
				}
				mTitleView.setLayoutParams(lp);
				if (mCustomSettitleTextSize) {
					mTitleView.setTextSize(mTitleTextSize);
				}

				if (mCustomSettitleTextColor) {
					mTitleView.setTextColor(mTitleTextColor);
				}
				/*
				 * Do this last so that if the user has supplied any icons we
				 * use them instead of the default ones. If the user has
				 * specified 0 then make it disappear.
				 */
				/*
				 * if (mIconId > 0) { mIconView.setImageResource(mIconId); }
				 * else if (mIcon != null) { mIconView.setImageDrawable(mIcon);
				 * } else if (mIconId == 0) {
				 */

				/*
				 * Apply the padding from the icon to ensure the title is
				 * aligned correctly.
				 */
				// mTitleView.setPadding(mIconView.getPaddingLeft(),
				// mIconView.getPaddingTop(),
				// mIconView.getPaddingRight(),
				// mIconView.getPaddingBottom());
				// mIconView.setVisibility(View.GONE);
				// }
			} else {
				mTitleView.setVisibility(View.GONE);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.gravity = Gravity.CENTER;

				mIconView.setLayoutParams(lp);
				if (mIconId > 0) {
					mIconView.setImageResource(mIconId);
				} else if (mIcon != null) {
					mIconView.setImageDrawable(mIcon);
				} else {
					// Hide the title template
					titleTemplate.setVisibility(View.GONE);
					mWindow.findViewById(R.id.titleLinear).setVisibility(View.GONE);
					mIconView.setVisibility(View.GONE);
					topPanel.setVisibility(View.GONE);
					hasTitle = false;
				}
			}
		}
		return hasTitle;
	}

	private void setupContent(LinearLayout contentPanel) {
		mScrollView = (TwsScrollView) mWindow.findViewById(R.id.scrollView);
		mScrollView.setFocusable(false);
		mScrollView.setHeaderHeight(0);
		mScrollView.setFooterHeight(0);
		// Special case for users that only want to display a String
		mMessageView = (TextView) mWindow.findViewById(R.id.message);
		mMsgSpacer = mWindow.findViewById(R.id.msgSpacer);

		if (mMessageView == null) {
			return;
		}

		if (mMessage != null) {
			mMessageView.setText(mMessage);
			if (mIsBottomDialog) {
				mMessageView.setTextAppearance(mContext, R.style.TextAppearance_tws_Second_twsTextSmallLightBodySub);
			}
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER;
			int lineCount = mMessageView.getLineCount();
			if (lineCount > 1) {
				lp.gravity = Gravity.LEFT;
			}
			mMessageView.setLayoutParams(lp);
		} else {
			mMessageView.setVisibility(View.GONE);
			View massageLayout = mWindow.findViewById(R.id.messageLayout);
			if (massageLayout != null) {
				massageLayout.setVisibility(View.GONE);
				mScrollView.removeView(massageLayout);
			} else {
				mScrollView.removeView(mMessageView);
			}

			if (mListView != null) {
				contentPanel.removeView(mWindow.findViewById(R.id.scrollView));

				contentPanel.addView(mListView, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f);
				if (mAdapter != null && !mIsBottomDialog) {
					int num = mAdapter.getCount();
					if (num > DEFAULT_LISTITEM_COUNT) {
						int totalHeight = 0;
						for (int i = 0; i < DEFAULT_LISTITEM_COUNT; i++) {
							View listItem = mAdapter.getView(i, null, mListView);
							listItem.measure(0, 0);
							totalHeight += listItem.getMeasuredHeight();
						}
						lp.height = totalHeight + (mListView.getDividerHeight() * (DEFAULT_LISTITEM_COUNT - 1));
						contentPanel.setLayoutParams(lp);
					}
				} else {
					contentPanel.setLayoutParams(lp);
				}

			} else {
				contentPanel.setVisibility(View.GONE);
			}
		}
	}

	public void setButtonColor(Button button, ButtonColor buttonColor) {
		switch (buttonColor) {
		case BTN_RED:
			button.setTextColor(mContext.getResources().getColorStateList(R.color.tws_text_red));
			break;
		case BTN_BLUE:
			button.setTextColor(mContext.getResources().getColorStateList(R.color.tws_text_blue));
			break;
		default:
			button.setTextColor(mContext.getResources().getColorStateList(R.color.tws_dialog_button_text_light));
			break;
		}
	}

	private boolean setupButtons() {
		int BIT_BUTTON_POSITIVE = 1;
		int BIT_BUTTON_NEGATIVE = 2;
		int BIT_BUTTON_NEUTRAL = 4;
		int whichButtons = 0;
		int buttonCount = 0;
		mButtonPositive = (Button) mWindow.findViewById(R.id.button1);
		mButtonPositive.setOnClickListener(mButtonHandler);

		if (TextUtils.isEmpty(mButtonPositiveText) || mButtonPositive.getVisibility() == View.GONE) {
			mButtonPositive.setVisibility(View.GONE);
		} else {
			if (TextUtils.isEmpty(mButtonPositive.getText())) {
				mButtonPositive.setText(mButtonPositiveText);
			}
			setButtonColor(mButtonPositive, mPositiveColor);
			mButtonPositive.setVisibility(View.VISIBLE);
			whichButtons = whichButtons | BIT_BUTTON_POSITIVE;
			buttonCount++;
		}

		mButtonNegative = (Button) mWindow.findViewById(R.id.button2);
		mButtonNegative.setOnClickListener(mButtonHandler);

		if (TextUtils.isEmpty(mButtonNegativeText) || mButtonNegative.getVisibility() == View.GONE) {
			mButtonNegative.setVisibility(View.GONE);
		} else {
			if (TextUtils.isEmpty(mButtonPositive.getText())) {
				mButtonNegative.setText(mButtonNegativeText);
			}
			mButtonNegative.setText(mButtonNegativeText);
			setButtonColor(mButtonNegative, mNegativeColor);
			mButtonNegative.setVisibility(View.VISIBLE);

			whichButtons = whichButtons | BIT_BUTTON_NEGATIVE;
			buttonCount++;
		}

		mButtonNeutral = (Button) mWindow.findViewById(R.id.button3);
		mButtonNeutral.setOnClickListener(mButtonHandler);

		if (TextUtils.isEmpty(mButtonNeutralText) || mButtonNeutral.getVisibility() == View.GONE) {
			mButtonNeutral.setVisibility(View.GONE);
		} else {
			mButtonNeutral.setText(mButtonNeutralText);
			setButtonColor(mButtonNeutral, mNeutralColor);
			mButtonNeutral.setVisibility(View.VISIBLE);

			whichButtons = whichButtons | BIT_BUTTON_NEUTRAL;
			buttonCount++;
		}

		if (shouldCenterSingleButton(mContext)) {
			/*
			 * If we only have 1 button it should be centered on the layout and
			 * expand to fill 50% of the available space.
			 */
			if (whichButtons == BIT_BUTTON_POSITIVE) {
				centerButton(mButtonPositive);
			} else if (whichButtons == BIT_BUTTON_NEGATIVE) {
				centerButton(mButtonNegative);
			} else if (whichButtons == BIT_BUTTON_NEUTRAL) {
				centerButton(mButtonNeutral);
			}
		}
		// JUISTART::setup background depend on the count of the
		// button::froyohuang 2014.4.17

		if (!mButtonBGSet) {
			boolean bRipple = ThemeUtils.isShowRipple(mContext);
			switch (buttonCount) {
			case 1:
				Button onlyBtn = null;
				if (whichButtons == BIT_BUTTON_POSITIVE) {
					onlyBtn = mButtonPositive;
				} else if (whichButtons == BIT_BUTTON_NEGATIVE) {
					onlyBtn = mButtonNegative;
				} else if (whichButtons == BIT_BUTTON_NEUTRAL) {
					onlyBtn = mButtonNeutral;
				}
				if (onlyBtn != null) {
					// onlyBtn.setBackgroundResource(R.drawable.second_btn_dialog_full);

					if (bRipple) {
						if (android.os.Build.VERSION.SDK_INT > 15) {
							onlyBtn.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
						} else {
							onlyBtn.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
						}
					} else {
						onlyBtn.setBackgroundResource(R.drawable.second_btn_dialog_full);
					}
				}
				break;
			case 2:
				if (TextUtils.isEmpty(mButtonPositiveText)) {
					// mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_right);
					// mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
					if (bRipple) {
						if (android.os.Build.VERSION.SDK_INT > 15) {
							mButtonNeutral.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
							mButtonNegative.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
						} else {
							mButtonNeutral.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
							mButtonNegative.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_full));
						}
					} else {
						mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_right);
						mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
					}

				} else if (TextUtils.isEmpty(mButtonNeutralText)) {
					// mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
					// mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
					if (bRipple) {
						if (android.os.Build.VERSION.SDK_INT > 15) {
							mButtonPositive.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_right));
							mButtonNegative.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_left));
						} else {
							mButtonPositive.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_right));
							mButtonNegative.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_left));
						}
					} else {
						mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
						mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
					}

				} else if (TextUtils.isEmpty(mButtonNegativeText)) {
					// mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
					// mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_left);

					if (bRipple) {
						if (android.os.Build.VERSION.SDK_INT > 15) {
							mButtonPositive.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_right));
							mButtonNeutral.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_left));
						} else {
							mButtonPositive.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_right));
							mButtonNeutral.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
									R.drawable.second_btn_dialog_left));
						}
					} else {
						mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
						mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_left);
					}
				}
				break;
			case 3:
				// mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
				// mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_middle);
				// mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
				if (bRipple) {
					if (android.os.Build.VERSION.SDK_INT > 15) {
						mButtonPositive.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_right));
						mButtonNeutral.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_middle));
						mButtonNegative.setBackground(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_left));
					} else {
						mButtonPositive.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_right));
						mButtonNeutral.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_middle));
						mButtonNegative.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(mContext,
								R.drawable.second_btn_dialog_left));
					}
				} else {
					mButtonPositive.setBackgroundResource(R.drawable.second_btn_dialog_right);
					mButtonNeutral.setBackgroundResource(R.drawable.second_btn_dialog_middle);
					mButtonNegative.setBackgroundResource(R.drawable.second_btn_dialog_left);
				}

				break;
			default:
				break;
			}
		}

		return whichButtons != 0;
	}

	private void centerButton(Button button) {
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
		params.gravity = Gravity.CENTER_HORIZONTAL;
		params.weight = 0.5f;
		button.setLayoutParams(params);
		View leftSpacer = mWindow.findViewById(R.id.leftSpacer);
		if (leftSpacer != null) {
			leftSpacer.setVisibility(View.VISIBLE);
		}
		View rightSpacer = mWindow.findViewById(R.id.rightSpacer);
		if (rightSpacer != null) {
			rightSpacer.setVisibility(View.VISIBLE);
		}
	}

	// tws-start bottom dialog::2014-10-1
	private boolean setupBottomButtons(LinearLayout bottomButtonsPanel) {
		boolean hasButtons = false;
		if (mBottomButtonsListView != null) {
			bottomButtonsPanel.removeAllViews();
			bottomButtonsPanel.addView(mBottomButtonsListView,
					new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
			bottomButtonsPanel.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));
			if (mBottomButtonAdapter != null) {
				mBottomButtonsListView.setAdapter(mBottomButtonAdapter);
				hasButtons = true;
				if (mBottomButtonCheckedItem > -1) {
					mBottomButtonsListView.setItemChecked(mBottomButtonCheckedItem, true);
					mBottomButtonsListView.setSelection(mBottomButtonCheckedItem);
				}
			}
		}
		return hasButtons;
	}

	private void setBackground(LinearLayout topPanel, LinearLayout contentPanel, View customPanel, boolean hasButtons,
			TypedArray a, boolean hasTitle, View buttonPanel, LinearLayout bottomButtonsPanel, boolean hasBottomButtons) {

		/* Get all the different background required */
		// tws-start::added for tws theme::20121002
		int fullDark = a.getResourceId(com.android.internal.R.styleable.AlertDialog_fullDark,
				R.color.tws_dialog_dark_bg);
		int topDark = a.getResourceId(com.android.internal.R.styleable.AlertDialog_topDark,
				R.color.tws_dialog_dark_bg);
		int centerDark = a.getResourceId(com.android.internal.R.styleable.AlertDialog_centerDark,
				R.color.tws_dialog_dark_bg);
		int bottomDark = a.getResourceId(com.android.internal.R.styleable.AlertDialog_bottomDark,
				R.color.tws_dialog_dark_bg);
		int fullBright = a.getResourceId(com.android.internal.R.styleable.AlertDialog_fullBright,
				R.color.tws_dialog_holo_bg);
		int topBright = a.getResourceId(com.android.internal.R.styleable.AlertDialog_topBright,
				R.color.tws_dialog_holo_bg);
		int centerBright = a.getResourceId(com.android.internal.R.styleable.AlertDialog_centerBright,
				R.color.tws_dialog_holo_bg);
		int bottomBright = a.getResourceId(com.android.internal.R.styleable.AlertDialog_bottomBright,
				R.color.tws_dialog_holo_bg);
		int bottomMedium = a.getResourceId(com.android.internal.R.styleable.AlertDialog_bottomMedium,
				R.color.tws_dialog_holo_bg);

		if (mIsContextMenu) {
			topBright = R.drawable.context_top_holo_light;
			topDark = topBright;
		}

		// tws-end::added for tws theme::20121002
		/*
		 * We now set the background of all of the sections of the alert. First
		 * collect together each section that is being displayed along with
		 * whether it is on a light or dark background, then run through them
		 * setting their backgrounds. This is complicated because we need to
		 * correctly use the full, top, middle, and bottom graphics depending on
		 * how many views they are and where they appear.
		 */

		View[] views = new View[5];
		boolean[] light = new boolean[5];
		View lastView = null;
		boolean lastLight = false;

		int pos = 0;
		if (hasTitle) {
			views[pos] = topPanel;
			light[pos] = false;
			pos++;
		}

		/*
		 * The contentPanel displays either a custom text message or a ListView.
		 * If it's text we should use the dark background for ListView we should
		 * use the light background. If neither are there the contentPanel will
		 * be hidden so set it as null.
		 */
		views[pos] = (contentPanel.getVisibility() == View.GONE) ? null : contentPanel;
		light[pos] = mListView != null;
		pos++;
		if (customPanel != null) {
			views[pos] = customPanel;
			light[pos] = mForceInverseBackground;
			pos++;
		}
		if (hasBottomButtons) {
			views[pos] = bottomButtonsPanel;
			light[pos] = true;
			pos++;
		}
		if (hasButtons) {
			views[pos] = buttonPanel;
			light[pos] = true;
		}

		boolean setView = false;
		for (pos = 0; pos < views.length; pos++) {
			View v = views[pos];
			if (v == null) {
				continue;
			}
			if (lastView != null) {
				if (!setView) {
					lastView.setBackgroundResource(lastLight ? topBright : topDark);
				} else {
					lastView.setBackgroundResource(lastLight ? centerBright : centerDark);
				}
				setView = true;
			}
			lastView = v;
			lastLight = light[pos];
		}

		if (lastView != null) {
			if (setView) {
				/*
				 * ListViews will use the Bright background but buttons use the
				 * Medium background.
				 */
				int resId = lastLight ? (hasButtons ? bottomMedium : bottomBright) : bottomDark;
				resId = mIsContextMenu ? R.drawable.context_middle_holo_light : resId;
				lastView.setBackgroundResource(resId);

				if (hasButtons) {
					lastView.setBackgroundDrawable(null);
				}
				// lastView.setBackgroundResource(
				// lastLight ? (hasButtons ? bottomMedium : bottomBright) :
				// bottomDark);
			} else {
				lastView.setBackgroundResource(lastLight ? fullBright : fullDark);
			}
		}

		/*
		 * TODO: uncomment section below. The logic for this should be if it's a
		 * Contextual menu being displayed AND only a Cancel button is shown
		 * then do this.
		 */
		// if (hasButtons && (mListView != null)) {

		/*
		 * Yet another *special* case. If there is a ListView with buttons don't
		 * put the buttons on the bottom but instead put them in the footer of
		 * the ListView this will allow more items to be displayed.
		 */
		// buttonPanel.setBackgroundResource(R.drawable.alert_dialog_divider);
		if ((mListView != null) && (mAdapter != null)) {
			mListView.setAdapter(mAdapter);
			if (mCheckedItem > -1) {
				mListView.setItemChecked(mCheckedItem, true);
				mListView.setSelection(mCheckedItem);
			}
		}
		if (mIsBottomDialog) {
			ViewGroup parent = (ViewGroup) mWindow.findViewById(R.id.parentPanel);
			if (parent != null) {
				parent.setBackgroundColor(mContext.getResources().getColor(R.color.tws_dialog_holo_bg));
			}
		}
	}

	public static class RecycleListView extends ListView {
		boolean mRecycleOnMeasure = true;

		public RecycleListView(Context context) {
			super(context);
		}

		public RecycleListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public RecycleListView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		protected boolean recycleOnMeasure() {
			return mRecycleOnMeasure;
		}

		/*
		 * @Override public void drawDivider(Canvas canvas, Rect bounds, int
		 * childIndex) { // This widget draws the same divider for all children
		 * //Log.d("alertController",
		 * "RecycleListView drawDivider ##################################");
		 * if(childIndex==super.getChildCount()-1) return;
		 * 
		 * super.drawDivider(canvas, bounds, childIndex); }
		 */

	}

	public static class AlertParams {
		public final Context mContext;
		public final LayoutInflater mInflater;

		public int mIconId = 0;
		public Drawable mIcon;
		public CharSequence mTitle;
		public View mCustomTitleView;
		public CharSequence mMessage;
		public CharSequence mPositiveButtonText;
		public ButtonColor mPositiveColor = ButtonColor.BTN_NORMAL;
		public DialogInterface.OnClickListener mPositiveButtonListener;
		public CharSequence mNegativeButtonText;
		public ButtonColor mNegativeColor = ButtonColor.BTN_NORMAL;
		public DialogInterface.OnClickListener mNegativeButtonListener;
		public CharSequence mNeutralButtonText;
		public ButtonColor mNeutralColor = ButtonColor.BTN_NORMAL;
		public DialogInterface.OnClickListener mNeutralButtonListener;
		public boolean mCancelable;
		public boolean mCanceOutWindow = true;
		public DialogInterface.OnCancelListener mOnCancelListener;
		public DialogInterface.OnKeyListener mOnKeyListener;
		public CharSequence[] mItems;
		public ListAdapter mAdapter;
		public DialogInterface.OnClickListener mOnClickListener;
		public View mView;
		public int mViewSpacingLeft;
		public int mViewSpacingTop;
		public int mViewSpacingRight;
		public int mViewSpacingBottom;
		public boolean mViewSpacingSpecified = false;
		public boolean[] mCheckedItems;
		public boolean mIsMultiChoice;
		public boolean mIsSingleChoice;
		public int mCheckedItem = -1;
		public DialogInterface.OnMultiChoiceClickListener mOnCheckboxClickListener;
		public Cursor mCursor;
		public String mLabelColumn;
		public String mIsCheckedColumn;
		public boolean mForceInverseBackground;
		public AdapterView.OnItemSelectedListener mOnItemSelectedListener;
		public OnPrepareListViewListener mOnPrepareListViewListener;
		public boolean mRecycleOnMeasure = true;

		// tws-start bottom dialog::2014-10-1
		public CharSequence[] mBottomButtonItems;
		public Cursor mBottomButtonCursor;
		public String mBottomButtonLabelColumn;
		public DialogInterface.OnClickListener mBottomButtonOnClickListener;
		public DialogInterface.OnMultiChoiceClickListener mBottomButtonOnCheckboxClickListener;
		public ListAdapter mBottomButtonAdapter;
		public boolean[] mBottomButtonCheckedItems;
		public boolean mIsBottomButtonMultiChoice;
		public boolean mIsBottomButtonSingleChoice;
		public String mIsBottomButtonCheckedColumn;
		public int mBottomButtonCheckedItem = -1;
		public int[] mBottomButtonColorItems;
		// tws-end bottom dialog::2014-10-1

		public int mListViewHeight = -1;

		/**
		 * Interface definition for a callback to be invoked before the ListView
		 * will be bound to an adapter.
		 */
		public interface OnPrepareListViewListener {

			/**
			 * Called before the ListView is bound to an adapter.
			 * 
			 * @param listView
			 *            The ListView that will be shown in the dialog.
			 */
			void onPrepareListView(ListView listView);
		}

		public AlertParams(Context context) {
			mContext = context;
			mCancelable = true;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void apply(AlertController dialog) {
			if (mCustomTitleView != null) {
				dialog.setCustomTitle(mCustomTitleView);
			} else {
				if (mTitle != null) {
					dialog.setTitle(mTitle);
				}
				if (mIcon != null) {
					dialog.setIcon(mIcon);
				}
				if (mIconId > 0) {
					dialog.setIcon(mIconId);
				}
			}
			if (mMessage != null) {
				dialog.setMessage(mMessage);
			}
			if (mPositiveButtonText != null) {
				dialog.setButton(mPositiveColor, DialogInterface.BUTTON_POSITIVE, mPositiveButtonText,
						mPositiveButtonListener, null);
			}
			if (mNegativeButtonText != null) {
				dialog.setButton(mNegativeColor, DialogInterface.BUTTON_NEGATIVE, mNegativeButtonText,
						mNegativeButtonListener, null);
			}
			if (mNeutralButtonText != null) {
				dialog.setButton(mNeutralColor, DialogInterface.BUTTON_NEUTRAL, mNeutralButtonText,
						mNeutralButtonListener, null);
			}
			if (mForceInverseBackground) {
				dialog.setInverseBackgroundForced(true);
			}
			// For a list, the client can either supply an array of items or an
			// adapter or a cursor
			if ((mItems != null) || (mCursor != null) || (mAdapter != null)) {
				createListView(dialog);
			}
			if (mView != null) {
				if (mViewSpacingSpecified) {
					dialog.setView(mView, mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight, mViewSpacingBottom);
				} else {
					dialog.setView(mView);
				}
			}
			// tws-start bottom dialog::2014-10-1
			if ((mBottomButtonItems != null) || (mBottomButtonCursor != null) || (mBottomButtonAdapter != null)) {
				createBottomButtons(dialog);
			}
			// tws-end bottom dialog::2014-10-1

			/*
			 * dialog.setCancelable(mCancelable);
			 * dialog.setOnCancelListener(mOnCancelListener); if (mOnKeyListener
			 * != null) { dialog.setOnKeyListener(mOnKeyListener); }
			 */
		}

		private void createListView(final AlertController dialog) {
			// tws-start ListAlertDialog::2014-7-23
			final boolean hasTitle = ((mCustomTitleView != null) || (mTitle != null && !TextUtils.isEmpty(mTitle))
					|| (mIcon != null) || (mIconId > 0) || (mMessage != null && !TextUtils.isEmpty(mMessage)) || mView != null);
			final boolean hasButton = ((mPositiveButtonText != null && !TextUtils.isEmpty(mPositiveButtonText))
					|| (mNegativeButtonText != null && !TextUtils.isEmpty(mNegativeButtonText)) || (mNeutralButtonText != null && !TextUtils
					.isEmpty(mNeutralButtonText)));
			// tws-end ListAlertDialog::2014-7-23
			final RecycleListView listView = (RecycleListView) mInflater.inflate(dialog.mListLayout, null);
			boolean bRipple = ThemeUtils.isShowRipple(mContext);
			if (!bRipple) {
				listView.setSelector(mContext.getResources().getDrawable(R.drawable.dialog_list_selector_holo_light));
			}
			ListAdapter adapter;
			listView.setFooterDividersEnabled(false);
			if (mIsMultiChoice) {
				if (mCursor == null) {
					adapter = new ArrayAdapter<CharSequence>(mContext, dialog.mMultiChoiceItemLayout, R.id.text1,
							mItems) {
						// tws-start ListAlertDialog::2014-7-23
						final List<CharSequence> mList = Arrays.asList(mItems);

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							if (mCheckedItems != null) {
								boolean isItemChecked = mCheckedItems[position];
								if (isItemChecked) {
									listView.setItemChecked(position, true);
								}
							}
							setListBackground(view, position, mList.size(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				} else {
					adapter = new CursorAdapter(mContext, mCursor, false) {
						private final int mLabelIndex;
						private final int mIsCheckedIndex;

						{
							final Cursor cursor = getCursor();
							mLabelIndex = cursor.getColumnIndexOrThrow(mLabelColumn);
							mIsCheckedIndex = cursor.getColumnIndexOrThrow(mIsCheckedColumn);
						}

						@Override
						public void bindView(View view, Context context, Cursor cursor) {
							CheckedTextView text = (CheckedTextView) view.findViewById(R.id.text1);
							text.setText(cursor.getString(mLabelIndex));
							listView.setItemChecked(cursor.getPosition(), cursor.getInt(mIsCheckedIndex) == 1);
						}

						@Override
						public View newView(Context context, Cursor cursor, ViewGroup parent) {
							return mInflater.inflate(dialog.mMultiChoiceItemLayout, parent, false);
						}

						// tws-start ListAlertDialog::2014-7-23
						final int count = getCursor().getCount();

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							setListBackground(view, position, count, hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				}
			} else {
				int layout = mIsSingleChoice ? dialog.mSingleChoiceItemLayout : dialog.mListItemLayout;
				if (mCursor == null) {
					adapter = (mAdapter != null) ? mAdapter : new ArrayAdapter<CharSequence>(mContext, layout,
							R.id.text1, mItems) {
						// tws-start ListAlertDialog::2014-7-23
						final List<CharSequence> mList = Arrays.asList(mItems);

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							setListBackground(view, position, mList.size(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				} else {
					adapter = new SimpleCursorAdapter(mContext, layout, mCursor, new String[] { mLabelColumn },
							new int[] { R.id.text1 }) {
						// tws-start ListAlertDialog::2014-7-23
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							setListBackground(view, position, mLabelColumn.length(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				}
			}

			if (mOnPrepareListViewListener != null) {
				mOnPrepareListViewListener.onPrepareListView(listView);
			}

			/*
			 * Don't directly set the adapter on the ListView as we might want
			 * to add a footer to the ListView later.
			 */
			dialog.mAdapter = adapter;
			dialog.mCheckedItem = mCheckedItem;

			if (mOnClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView parent, View v, int position, long id) {
						mOnClickListener.onClick(dialog.mDialogInterface, position);
						if (!mIsSingleChoice) {
							dialog.mDialogInterface.dismiss();
						}
					}
				});
			} else if (mOnCheckboxClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView parent, View v, int position, long id) {
						if (mCheckedItems != null) {
							mCheckedItems[position] = listView.isItemChecked(position);
						}
						mOnCheckboxClickListener.onClick(dialog.mDialogInterface, position,
								listView.isItemChecked(position));
					}
				});
			}

			// Attach a given OnItemSelectedListener to the ListView
			if (mOnItemSelectedListener != null) {
				listView.setOnItemSelectedListener(mOnItemSelectedListener);
			}

			if (mIsSingleChoice) {
				listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			} else if (mIsMultiChoice) {
				listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			}
			listView.mRecycleOnMeasure = mRecycleOnMeasure;
			// tws-start ListAlertDialog::2014-7-23
			dialog.mListView = listView;
		}

		// tws-start bottom dialog::2014-10-1
		private void createBottomButtons(final AlertController dialog) {
			final boolean hasTitle = ((mCustomTitleView != null) || (mTitle != null && !TextUtils.isEmpty(mTitle))
					|| (mIcon != null) || (mIconId > 0) || (mMessage != null && !TextUtils.isEmpty(mMessage)) || mView != null);
			final boolean hasButton = false;
			final ListView listView = (ListView) mInflater.inflate(dialog.mBottomButtonLayout, null);
			boolean bRipple = ThemeUtils.isShowRipple(mContext);
			if (!bRipple) {
				listView.setSelector(mContext.getResources().getDrawable(R.drawable.dialog_list_selector_holo_light));
			}
			ListAdapter adapter;
			listView.setFooterDividersEnabled(false);
			if (mIsBottomButtonMultiChoice) {
				if (mBottomButtonCursor == null) {
					adapter = new ArrayAdapter<CharSequence>(mContext, dialog.mBottomButtonMultiChoiceItemLayout,
							R.id.text1, mBottomButtonItems) {
						// tws-start ListAlertDialog::2014-7-23
						final List<CharSequence> mList = Arrays.asList(mBottomButtonItems);

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							if (mBottomButtonCheckedItems != null) {
								boolean isItemChecked = mBottomButtonCheckedItems[position];
								if (isItemChecked) {
									listView.setItemChecked(position, true);
								}
							}
							// tws-start add bottom button
							// colors::2014-10-21
							if (mBottomButtonColorItems != null) {
								TextView tv = (TextView) view.findViewById(R.id.text1);
								setButtonColor(mContext, tv, mBottomButtonColorItems[position]);
							}
							// tws-end add bottom button
							// colors::2014-10-21
							setListBackground(view, position, mList.size(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				} else {
					adapter = new CursorAdapter(mContext, mBottomButtonCursor, false) {
						private final int mLabelIndex;
						private final int mIsCheckedIndex;

						{
							final Cursor cursor = getCursor();
							mLabelIndex = cursor.getColumnIndexOrThrow(mBottomButtonLabelColumn);
							mIsCheckedIndex = cursor.getColumnIndexOrThrow(mIsBottomButtonCheckedColumn);
						}

						@Override
						public void bindView(View view, Context context, Cursor cursor) {
							CheckedTextView text = (CheckedTextView) view.findViewById(R.id.text1);
							text.setText(cursor.getString(mLabelIndex));
							listView.setItemChecked(cursor.getPosition(), cursor.getInt(mIsCheckedIndex) == 1);
						}

						@Override
						public View newView(Context context, Cursor cursor, ViewGroup parent) {
							return mInflater.inflate(dialog.mBottomButtonMultiChoiceItemLayout, parent, false);
						}

						// tws-start ListAlertDialog::2014-7-23
						final int count = getCursor().getCount();

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							// tws-start add bottom button
							// colors::2014-10-21
							if (mBottomButtonColorItems != null) {
								TextView tv = (TextView) view.findViewById(R.id.text1);
								setButtonColor(mContext, tv, mBottomButtonColorItems[position]);
							}
							// tws-end add bottom button
							// colors::2014-10-21
							setListBackground(view, position, count, hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				}
			} else {
				int layout = mIsBottomButtonSingleChoice ? dialog.mBottomButtonSingleChoiceItemLayout
						: dialog.mBottomButtonItemLayout;
				if (mBottomButtonCursor == null) {
					adapter = (mBottomButtonAdapter != null) ? mBottomButtonAdapter : new ArrayAdapter<CharSequence>(
							mContext, layout, R.id.text1, mBottomButtonItems) {
						// tws-start ListAlertDialog::2014-7-23
						final List<CharSequence> mList = Arrays.asList(mBottomButtonItems);

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							// tws-start add bottom button
							// colors::2014-10-21
							if (mBottomButtonColorItems != null) {
								TextView tv = (TextView) view.findViewById(R.id.text1);
								setButtonColor(mContext, tv, mBottomButtonColorItems[position]);
							}
							// tws-end add bottom button
							// colors::2014-10-21
							setListBackground(view, position, mList.size(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				} else {
					adapter = new SimpleCursorAdapter(mContext, layout, mBottomButtonCursor,
							new String[] { mBottomButtonLabelColumn }, new int[] { R.id.text1 }) {
						// tws-start ListAlertDialog::2014-7-23
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							// tws-start add bottom button
							// colors::2014-10-21
							if (mBottomButtonColorItems != null) {
								TextView tv = (TextView) view.findViewById(R.id.text1);
								setButtonColor(mContext, tv, mBottomButtonColorItems[position]);
							}
							// tws-end add bottom button
							// colors::2014-10-21
							setListBackground(view, position, mBottomButtonLabelColumn.length(), hasTitle, hasButton);
							return view;
						}
						// tws-end ListAlertDialog::2014-7-23
					};
				}
			}

			/*
			 * Don't directly set the adapter on the ListView as we might want
			 * to add a footer to the ListView later.
			 */
			dialog.mBottomButtonAdapter = adapter;
			dialog.mBottomButtonCheckedItem = mBottomButtonCheckedItem;

			if (mBottomButtonOnClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView parent, View v, int position, long id) {
						mBottomButtonOnClickListener.onClick(dialog.mDialogInterface, position);
						// if (!mIsBottomButtonSingleChoice) {
						// dialog.mDialogInterface.dismiss();
						dialog.mHandler.sendMessageDelayed(dialog.mHandler.obtainMessage(
								ButtonHandler.MSG_DISMISS_DIALOG, dialog.mDialogInterface), 80);
						// }
					}
				});
			} else if (mBottomButtonOnCheckboxClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView parent, View v, int position, long id) {
						if (mBottomButtonCheckedItems != null) {
							mBottomButtonCheckedItems[position] = listView.isItemChecked(position);
						}
						mBottomButtonOnCheckboxClickListener.onClick(dialog.mDialogInterface, position,
								listView.isItemChecked(position));
					}
				});
			}

			// Attach a given OnItemSelectedListener to the ListView
			if (mOnItemSelectedListener != null) {
				listView.setOnItemSelectedListener(mOnItemSelectedListener);
			}

			if (mIsBottomButtonSingleChoice) {
				listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			} else if (mIsBottomButtonMultiChoice) {
				listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			}
			// listView.setSelector(R.color.transparent);
			setBottomButtonsStartAnimation(listView, adapter.getCount());
			dialog.mBottomButtonsListView = listView;
		}
		// tws-end bottom dialog::2014-10-1
	}

	// tws-start add bottom button colors::2014-10-21
	private static void setButtonColor(Context context, TextView tv, int color) {
		switch (color) {
		case AlertDialog.BOTTOM_BUTTON_COLOR_BLACK:
			tv.setTextColor(context.getResources().getColor(R.color.tws_bottom_dialog_list_item_light));
			break;
		case AlertDialog.BOTTOM_BUTTON_COLOR_BLUE:
			tv.setTextColor(context.getResources().getColor(R.color.tws_blue));
			break;
		case AlertDialog.BOTTOM_BUTTON_COLOR_RED:
			tv.setTextColor(context.getResources().getColor(R.color.tws_red));
			break;
		default:
			tv.setTextColor(color);
			break;
		}
	}

	// tws-end add bottom button colors::2014-10-21

	// tws-start ListAlertDialog::2014-7-23
	private static void setListBackground(View view, int index, int count, boolean hasTitle, boolean hasButton) {
		int paddingTop = 0;
		int paddingBottom = 0;
		if (hasTitle && !hasButton) {
			if (index == count - 1) {
				// view.setBackgroundResource(R.drawable.tws_preference_last_item);
				view.setMinimumHeight(mListItemHeight + mListSpace);
				view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + mListSpace);
			} else {
				// view.setBackgroundResource(R.drawable.tws_preference_item);
				view.setMinimumHeight(mListItemHeight);
				view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
			}
		} else if (!hasTitle && hasButton) {
			if (index == 0) {
				// view.setBackgroundResource(R.drawable.tws_preference_top_item);
				view.setMinimumHeight(mListItemHeight + mListSpace);
				view.setPadding(paddingLeft, paddingTop + mListSpace, paddingRight, paddingBottom);
			} else {
				// view.setBackgroundResource(R.drawable.tws_preference_item);
				view.setMinimumHeight(mListItemHeight);
				view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
			}
		} else if (!hasTitle && !hasButton) {
			if (count > 1) {
				if (index == 0) {
					// view.setBackgroundResource(R.drawable.tws_preference_top_item);
					view.setMinimumHeight(mListItemHeight + mListSpace);
					view.setPadding(paddingLeft, paddingTop + mListSpace, paddingRight, paddingBottom);
				} else if (index == count - 1) {
					// view.setBackgroundResource(R.drawable.tws_preference_last_item);
					view.setMinimumHeight(mListItemHeight + mListSpace);
					view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + mListSpace);
				} else {
					// view.setBackgroundResource(R.drawable.tws_preference_item);
					view.setMinimumHeight(mListItemHeight);
					view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
				}
			} else {
				// view.setBackgroundResource(R.drawable.tws_preference_single_item);
				view.setMinimumHeight(mListItemHeight);
				view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
			}
		} else {
			// view.setBackgroundResource(R.drawable.tws_preference_item);
			// useRippleDrawable(view, R.drawable.tws_preference_item);
			view.setMinimumHeight(mListItemHeight);
			view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
		}
	}

	// tws-end ListAlertDialog::2014-7-23

	private static void setBottomButtonsStartAnimation(ViewGroup viewGroup, int count) {
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		if (count > DEFAULT_ITEM_COUNT) {
			animation.setDuration(mBottomButtonAnimationShortDur);
		} else {
			animation.setDuration(mBottomButtonAnimationDur);
		}
		LayoutAnimationController controller = new LayoutAnimationController(animation);
		if (count > DEFAULT_ITEM_COUNT) {
			controller.setDelay((float) mBottomButtonAnimationShortDelayTime / mBottomButtonAnimationShortDur);
		} else {
			controller.setDelay((float) mBottomButtonAnimationDelayTime / mBottomButtonAnimationDur);
		}
		controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
		viewGroup.setLayoutAnimation(controller);
	}

	public void setBottomButtonsStartAnimation(boolean enable) {
		if (enable) {
			if (mBottomButtonsListView != null && mBottomButtonAdapter != null) {
				setBottomButtonsStartAnimation(mBottomButtonsListView, mBottomButtonAdapter.getCount());
			}
		}
	}

	public static void useRippleDrawable(View view, int contentResId) {
		Drawable drawable = TwsRippleUtils.getCustomDrawable(view.getContext(), R.color.default_ripple_light,
				contentResId, 0);
		boolean bRipple = ThemeUtils.isShowRipple(view.getContext());
		if (bRipple) {
			if (android.os.Build.VERSION.SDK_INT > 15) {
				view.setBackground(drawable);
			} else {
				view.setBackgroundDrawable(drawable);
			}
		}
	}
}
