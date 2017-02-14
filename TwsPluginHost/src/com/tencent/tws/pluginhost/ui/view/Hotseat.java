package com.tencent.tws.pluginhost.ui.view;

import java.util.ArrayList;
import java.util.Iterator;

import tws.component.log.TwsLog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.tencent.tws.pluginhost.content.CellItem;
import com.tencent.tws.pluginhost.content.CellItem.ActionBarInfo;
import com.tencent.tws.pluginhost.content.CellItem.ComponentName;
import com.tencent.tws.pluginhost.ui.HostHomeActivity.DisplayInfo;
import com.tws.plugin.content.DisplayConfig;
import com.tws.plugin.manager.PluginManagerHelper;

public class Hotseat extends LinearLayout implements OnClickListener {
	private final String TAG = "rick_Print_dm:Hotseat";

	public static final String MY_WATCH_FRAGMENT = "my_watch_fragment";

	public static final int FRAGMENT_COMPONENT = DisplayConfig.TYPE_FRAGMENT;
	public static final int ACTIVITY_COMPONENT = DisplayConfig.TYPE_ACTIVITY;
	public static final int SERVICE_COMPONENT = DisplayConfig.TYPE_SERVICE;
	public static final int VIEW_COMPONENT = DisplayConfig.TYPE_VIEW;

	private ArrayList<OnHotseatClickListener> onListeners = new ArrayList<OnHotseatClickListener>();
	private ArrayList<CellItem> mHomeBottomButtons = new ArrayList<CellItem>();
	private CellItem mFoucsButton = null;
	private static int mAddChildIndex = 0;

	public Hotseat(Context context) {
		this(context, null);
	}

	public Hotseat(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Hotseat(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOrientation(LinearLayout.HORIZONTAL);
	}

	public interface OnHotseatClickListener {
		public void onItemClick(int tagIndex);

		public void updateActionBar(final ActionBarInfo actionBarInfo);
	}

	public void addHotseatClickObserver(OnHotseatClickListener onClickListener) {
		if (onListeners == null) {
			TwsLog.e(TAG, "removeHomeBottomTabObserver, mHomeBottomTabCallbacksObserver is null, ignore");
			return;
		}

		if (onListeners.contains(onClickListener)) {
			TwsLog.d(TAG, "mHomeBottomTabCallbacksObserver, mHomeBottomTabCallbacksObserver had it, ignore");
			return;
		}

		onListeners.add(onClickListener);
	}

	public void removeHotseatClickObserver(OnHotseatClickListener onClickListener) {
		if (onListeners == null) {
			TwsLog.e(TAG, "removeHomeBottomTabObserver, mHomeBottomTabCallbacksObserver is null, ignore");
			return;
		}

		if (onListeners.contains(onClickListener)) {
			onListeners.remove(onClickListener);
		}
	}

	// type 1:fragment 2:activity 3:service
	// 专给我的手表等DM固有的Pos位提供的
	public void addOneBottomButton(String classId, String packageName, int componentType, CharSequence text,
			Drawable normalBackground, Drawable focusBackground, int textNormalColor, int textFocusColor, int location) {
		CellItem button = new CellItem(getContext());
		button.setActionClass(classId, packageName, componentType);
		button.setText(text);
		button.setNormalBackground(normalBackground);
		button.setFocusBackground(focusBackground);
		button.setTextColorNormal(textNormalColor);
		button.setTextColorFocus(textFocusColor);
		LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
		button.setLayoutParams(params);
		button.setOnClickListener(this);
		button.setLocation(location);
		button.setTagIndex(mAddChildIndex);
		++mAddChildIndex;
		addView(button);
		// 这里需要入队列跟My_fragment有些差异，因为fragment的切换需要依赖这个队列
		mHomeBottomButtons.add(button);
	}

	public int addOneBottomButtonForPlugin(final DisplayInfo info, int textNormalColor, int textFocusColor,
			boolean newInstallPlugin) {
		// 当前显示在Hotseat的内容暂只接收fragment
		if (info.componentType != DisplayConfig.TYPE_FRAGMENT) {
			return -1;
		}

		if (!isEnabledDisplayInfo(info)) {
			TwsLog.e(TAG, "info is illegal(already exists), This will be ignored!");
			return -1;
		}

		CellItem button = new CellItem(getContext());
		button.setActionClass(info.classId, info.packageName, info.componentType);
		button.setText(info.title_zh);
		button.setNormalBackground(PluginManagerHelper.getPluginIcon(info.normalResName));
		button.setFocusBackground(PluginManagerHelper.getPluginIcon(info.focusResName));
		button.setTextColorNormal(textNormalColor);
		button.setTextColorFocus(textFocusColor);
		LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
		button.setLayoutParams(params);
		button.setOnClickListener(this);
		button.setLocation(info.location);
		button.setTagIndex(mAddChildIndex);
		++mAddChildIndex;

		button.mActionBarInfo.ab_title = info.ab_title_zh;
		button.mActionBarInfo.ab_rbtnctype = info.ab_rbtnctype;
		button.mActionBarInfo.ab_rbtncontent = info.ab_rbtncontent;
		button.mActionBarInfo.ab_rbtnrestype = info.ab_rbtnrestype;
		button.mActionBarInfo.ab_rbtnres_normal = info.ab_rbtnres_normal;
		button.mActionBarInfo.ab_rbtnres_focus = info.ab_rbtnres_focus;

		boolean insertRlt = false;
		int index = 1;
		// 注意这里第一个是我的手表 这个位置是不动的
		if (0 < button.getLocation()) {
			for (index = 1; index < mHomeBottomButtons.size(); index++) {
				if (button.getLocation() < mHomeBottomButtons.get(index).getLocation()) {
					insertRlt = true;
					mHomeBottomButtons.add(index, button);
					break;
				}
			}
		}
		if (!insertRlt) {
			mHomeBottomButtons.add(button);
		}

		TwsLog.d(TAG, "addOneBottomButton:" + info.title_zh + " add index " + index);
		addView(button, index);

		return index;
	}

	public boolean isEnabledDisplayInfo(final DisplayInfo info) {
		if (info == null || TextUtils.isEmpty(info.packageName) || TextUtils.isEmpty(info.classId))
			return false;

		for (CellItem item : mHomeBottomButtons) {
			if (info.packageName.equals(item.getPluginPackageName()) && info.classId.equals(item.getClassId())) {
				return false;
			}
		}

		return true;
	}

	public int childCount() {
		return mHomeBottomButtons.size();
	}

	// 当前暂时只处理一个，底部Hotseat一个packageName只让放置一个
	public int removePlugin(String packageName) {
		if (TextUtils.isEmpty(packageName))
			return -1;

		Iterator<CellItem> iter = mHomeBottomButtons.iterator();
		boolean foucsChanged = false;
		CellItem removeItem = null, preItem = null;
		while (iter.hasNext()) {
			CellItem button = iter.next();
			if (packageName.equals(button.getPluginPackageName())) {
				if (mFoucsButton == button) {
					foucsChanged = true;
					mFoucsButton = preItem;
				}

				removeItem = button;
				// 从Hotseat界面移除
				removeView(button);
				// 从缓存数组里面移除
				mHomeBottomButtons.remove(button);

				if (foucsChanged) {
					break; // 需要退出遍历执行切换焦点流程
				} else {
					return button.getTagIndex();
				}
			}
			preItem = button;
		}

		if (foucsChanged && onListeners != null) {
			for (OnHotseatClickListener listener : onListeners) {
				listener.updateActionBar(mFoucsButton.mActionBarInfo);
				listener.onItemClick(mFoucsButton == null ? 0 : mFoucsButton.getTagIndex());
			}
		}

		return removeItem == null ? -1 : removeItem.getTagIndex();
	}

	@Override
	public void onClick(View view) {
		if (view instanceof CellItem) {
			if (view == mFoucsButton)
				return;

			setFocus((CellItem) view);

			if (onListeners != null) {
				for (OnHotseatClickListener listener : onListeners) {
					listener.updateActionBar(mFoucsButton.mActionBarInfo);
					listener.onItemClick(mFoucsButton.getTagIndex());
				}
			}
		} else {
			TwsLog.d(TAG, "onClick view=" + view);
		}
	}

	public String getFouceTabClassId() {
		return mFoucsButton == null ? "" : mFoucsButton.getClassId();
	}

	public int getFouceTagIndex() {
		return mFoucsButton == null ? 0 : mFoucsButton.getTagIndex();
	}

	private void setFocus(final CellItem button) {
		TwsLog.d(TAG, "call setFocus HomeBottomButton");
		if (null == button) {
			Exception here = new Exception("call setFouce on null object");
			here.fillInStackTrace();
			TwsLog.d(TAG, "call setFouce on null object", here);
			return;
		}

		boolean matched = false;
		for (CellItem btn : mHomeBottomButtons) {
			if (btn.equals(button)) {
				button.setFocus(true);
				mFoucsButton = button;
				matched = true;
			} else {
				btn.setFocus(false);
			}
		}

		if (!matched) {
			mFoucsButton = null;
			TwsLog.d(TAG, "setFouce:HomeBottomButton - classId is " + button.getClassId()
					+ " no matching to the right!!!");
			printHomeBottomButtonsInfo();
		}
	}

	/**
	 * @return FoucsButton TagIndex
	 */
	public int setFocusIndex(int arrayIndex) {
		if (mHomeBottomButtons.size() < 1)
			return -1;

		int focusTagIndex = -1;
		mFoucsButton = null;

		if (arrayIndex < 0) {
			arrayIndex = 0;
		} else if (mHomeBottomButtons.size() <= arrayIndex) {
			arrayIndex = mHomeBottomButtons.size() - 1;
		}

		for (int index = 0; index < mHomeBottomButtons.size(); index++) {
			if (index == arrayIndex) {
				mFoucsButton = mHomeBottomButtons.get(index);
				mFoucsButton.setFocus(true);
				focusTagIndex = mFoucsButton.getTagIndex();
			} else {
				mHomeBottomButtons.get(index).setFocus(false);
			}
		}

		if (mFoucsButton != null && onListeners != null) {
			for (OnHotseatClickListener listener : onListeners) {
				listener.updateActionBar(mFoucsButton.mActionBarInfo);
			}
		}

		return focusTagIndex;
	}

	private void printHomeBottomButtonsInfo() {
		TwsLog.d(TAG, "===========================printHomeBottomButtonsInfo begin===========================");
		int index = 0;
		for (CellItem button : mHomeBottomButtons) {
			TwsLog.d(TAG, "[" + index + "] classId:" + button.getClassId() + " type is " + button.getComponentType());
		}
		TwsLog.d(TAG, "===========================printHomeBottomButtonsInfo end===========================");
	}

	public ComponentName getComponentNameByTagIndex(int tagIndex) {
		if (mFoucsButton == null) {
			setFocusIndex(0);
		}

		if (mFoucsButton != null && mFoucsButton.getTagIndex() == tagIndex) {
			return mFoucsButton.getComponentName();
		}

		for (CellItem item : mHomeBottomButtons) {
			if (item.getTagIndex() == tagIndex) {
				return mFoucsButton.getComponentName();
			}
		}

		return null;
	}

	public int getPosByClassId(String classId) {
		if (TextUtils.isEmpty(classId))
			return 0;

		for (int index = 0; index < mHomeBottomButtons.size(); index++) {
			if (classId.equals(mHomeBottomButtons.get(index).getClassId()))
				return index;
		}

		return 0;
	}
}
