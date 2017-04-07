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

package android.widget;

import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

/*
 * This is supposed to be a *very* thin veneer over TextView.
 * Do not make any changes here that do anything that a TextView
 * with a key listener and a movement method wouldn't do!
 */

/**
 * EditText is a thin veneer over TextView that configures itself to be
 * editable.
 * 
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/text.html">Text
 * Fields</a> guide.
 * </p>
 * <p>
 * <b>XML attributes</b>
 * <p>
 * See {@link android.R.styleable#EditText EditText Attributes},
 * {@link android.R.styleable#TextView TextView Attributes},
 * {@link android.R.styleable#View View Attributes}
 */
public class TwsEditText extends TextView {
	private static final String TAG = "TwsEditText";

	private Drawable[] mActionDrawables = null;
	private Drawable[] mActionDrawables_colorBg = null;
	private int mPasswordInputType = -1;
	private int mActionStatus = -1;
	private boolean isPasswordInputType = false;

	private int CLICK_AREA = 30;
	private float screenDensity = 0;
	private boolean showClearActionBtn = true;
	private boolean isWhiteBackground = true;

	public TwsEditText(Context context) {
		this(context, null);
	}

	public TwsEditText(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.twsEditTextStyle);
	}

	public TwsEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
        final TypedArray editAttributes = context.obtainStyledAttributes(attrs, R.styleable.TwsEditText, defStyle, 0);
        showClearActionBtn = editAttributes.getBoolean(R.styleable.TwsEditText_showClear, true);
        isWhiteBackground = editAttributes.getBoolean(R.styleable.TwsEditText_isWhiteBackground, true);
        editAttributes.recycle();
        
		DisplayMetrics metric = new DisplayMetrics();
		android.view.WindowManager manager = (android.view.WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getMetrics(metric);
		screenDensity = metric.density;
		twsInitDeleteStatus(context);
	}

	public void setNeedShowClearAction(boolean show) {
		if (showClearActionBtn != show) {
			showClearActionBtn = show;
			twsSetDrawable();
		}
	}

	public void setIsWhiteBackground(boolean white) {
		if (isWhiteBackground != white) {
			isWhiteBackground = white;
			if (isWhiteBackground) {
				if (mActionDrawables == null) {
					mActionDrawables = new Drawable[] { mContext.getResources().getDrawable(R.drawable.ic_password_hidden),
							mContext.getResources().getDrawable(R.drawable.ic_password_visiable),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_holo_light),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_hpsd_holo_light),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_vpsd_holo_light), };
				}
			} else {
				if (mActionDrawables_colorBg == null) {
					mActionDrawables_colorBg = new Drawable[] { mContext.getResources().getDrawable(R.drawable.ic_password_hidden_colorbg),
							mContext.getResources().getDrawable(R.drawable.ic_password_visiable_colorbg),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_holo_light_colorbg),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_hpsd_holo_light_colorbg),
							mContext.getResources().getDrawable(R.drawable.ic_clear_del_vpsd_holo_light_colorbg), };
				}
			}
			twsSetDrawable();
		}
	}

	public void twsInitDeleteStatus(Context context) {
		if (isWhiteBackground) {
			mActionDrawables = new Drawable[] { context.getResources().getDrawable(R.drawable.ic_password_hidden),
					context.getResources().getDrawable(R.drawable.ic_password_visiable),
					context.getResources().getDrawable(R.drawable.ic_clear_del_holo_light),
					context.getResources().getDrawable(R.drawable.ic_clear_del_hpsd_holo_light),
					context.getResources().getDrawable(R.drawable.ic_clear_del_vpsd_holo_light), };
		} else {
			mActionDrawables_colorBg = new Drawable[] { context.getResources().getDrawable(R.drawable.ic_password_hidden_colorbg),
					context.getResources().getDrawable(R.drawable.ic_password_visiable_colorbg),
					context.getResources().getDrawable(R.drawable.ic_clear_del_holo_light_colorbg),
					context.getResources().getDrawable(R.drawable.ic_clear_del_hpsd_holo_light_colorbg),
					context.getResources().getDrawable(R.drawable.ic_clear_del_vpsd_holo_light_colorbg), };
		}

		addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				twsSetDrawable();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {

			}
		});

		if (isPasswordInputType(getInputType())) {
			mPasswordInputType = 0;
		} else if (isVisiblePasswordInputType(getInputType())) {
			mPasswordInputType = 1;
		} else {
			mPasswordInputType = -1;
		}

		twsSetDrawable();
	}

	public void twsSetDrawable() {
		if (length() < 1) {
			mActionStatus = mPasswordInputType;
		} else {
			if (showClearActionBtn)
				mActionStatus = mPasswordInputType + 3;
			else
				mActionStatus = mPasswordInputType;
		}
		//mActionStatus = length() < 1 ? mPasswordInputType : mPasswordInputType + 3;
		if (isWhiteBackground)
			setCompoundDrawablesWithIntrinsicBounds(null, null, actionStatusIsLegal() ? mActionDrawables[mActionStatus] : null, null);
		else
			setCompoundDrawablesWithIntrinsicBounds(null, null, actionStatusIsLegal() ? mActionDrawables_colorBg[mActionStatus] : null, null);
	}

	private boolean actionStatusIsLegal() {
		if (isWhiteBackground)
			return mActionDrawables != null && 0 <= mActionStatus && mActionStatus < mActionDrawables.length;
		else
			return mActionDrawables_colorBg != null && 0 <= mActionStatus && mActionStatus < mActionDrawables_colorBg.length;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (actionStatusIsLegal() && event.getAction() == MotionEvent.ACTION_UP) {
			int eventX = (int) event.getRawX();
			int eventY = (int) event.getRawY();
			Rect rect = new Rect();
			getGlobalVisibleRect(rect);
			rect.left = rect.right - CLICK_AREA * (int) screenDensity;
			switch (mActionStatus) {
			case 0:
				if (rect.left <= eventX && eventX <= rect.right)
					setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				break;
			case 1:
				if (rect.left <= eventX && eventX <= rect.right)
					setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
				break;
			case 2:
				if (rect.left <= eventX && eventX <= rect.right) {
					setText("");

					twsSetDrawable();
				}
				break;
			case 3:
				if (rect.left <= eventX && eventX <= rect.right) {
					setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				} else {
					rect.left = rect.right - 2 * CLICK_AREA * (int) screenDensity;
					if (rect.left <= eventX && eventX <= rect.right) {
						setText("");

						twsSetDrawable();
					}
				}
				break;
			case 4:
				if (rect.left <= eventX && eventX <= rect.right) {
					setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
				} else {
					rect.left = rect.right - 2 * CLICK_AREA * (int) screenDensity;
					if (rect.left <= eventX && eventX <= rect.right) {
						setText("");

						twsSetDrawable();
					}
				}
				break;

			default:
				break;
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void setInputType(int type) {
		super.setInputType(type);
		if (isPasswordInputType(type)) {
			mPasswordInputType = 0;
		} else if (isVisiblePasswordInputType(type)) {
			mPasswordInputType = 1;
		} else {
			mPasswordInputType = -1;
		}

		twsSetDrawable();
	}

	@Override
	protected boolean getDefaultEditable() {
		return true;
	}

	@Override
	protected MovementMethod getDefaultMovementMethod() {
		return ArrowKeyMovementMethod.getInstance();
	}

	@Override
	public Editable getText() {
		return (Editable) super.getText();
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, BufferType.EDITABLE);
	}

	/**
	 * Convenience for {@link Selection#setSelection(Spannable, int, int)}.
	 */
	public void setSelection(int start, int stop) {
		Selection.setSelection(getText(), start, stop);
	}

	/**
	 * Convenience for {@link Selection#setSelection(Spannable, int)}.
	 */
	public void setSelection(int index) {
		Selection.setSelection(getText(), index);
	}

	/**
	 * Convenience for {@link Selection#selectAll}.
	 */
	public void selectAll() {
		Selection.selectAll(getText());
	}

	/**
	 * Convenience for {@link Selection#extendSelection}.
	 */
	public void extendSelection(int index) {
		Selection.extendSelection(getText(), index);
	}

	@Override
	public void setEllipsize(TextUtils.TruncateAt ellipsis) {
		if (ellipsis == TextUtils.TruncateAt.MARQUEE) {
			throw new IllegalArgumentException("EditText cannot use the ellipsize mode " + "TextUtils.TruncateAt.MARQUEE");
		}
		super.setEllipsize(ellipsis);
	}

	@Override
	public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(event);
		event.setClassName(TwsEditText.class.getName());
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		info.setClassName(TwsEditText.class.getName());
	}

	private boolean isPasswordInputType(int inputType) {
		final int variation = inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
		return variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
				|| variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
				|| variation == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
	}

	private boolean isVisiblePasswordInputType(int inputType) {
		final int variation = inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
		int var = (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		return variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
	}
}
