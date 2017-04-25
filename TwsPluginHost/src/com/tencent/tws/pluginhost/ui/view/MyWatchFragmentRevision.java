package com.tencent.tws.pluginhost.ui.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import tws.component.log.TwsLog;
import android.app.TwsActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.pluginhost.HostApplication;
import com.tencent.tws.pluginhost.R;
import com.tencent.tws.pluginhost.ui.HostHomeActivity.DisplayInfo;
import com.tencent.tws.pluginhost.ui.MessageManagerActivity;
import com.tencent.tws.pluginhost.ui.SettingsActivity;
import com.tws.plugin.content.DisplayConfig;
import com.tws.plugin.manager.PluginManagerHelper;

public class MyWatchFragmentRevision extends Fragment implements OnClickListener {
	private static final String TAG = "rick_Print:MyWatchFragmentRevision";

	private static final int FIX_LOCATION_BEGIN = 99;

	private Resources mResources;

	private LinearLayout mFragmentContainer;
	private RelativeLayout mWatchInfoLayout;
	private ImageView mWatchImg;
	private TextView mConnectText, mWatchNameText;
	private ImageView mNotiRedpointImg, mOtaRedpoint;// mUpgradeRedPointImageView

	// 只做初始化构造存储用，后面不可在用
	private ArrayList<DisplayInfo> mDisplayInfos = new ArrayList<DisplayInfo>();

	// 这个列表在安装和卸载插件的时候都需要维护
	private ArrayList<WatchFragmentContentItem> mContentItems = new ArrayList<WatchFragmentContentItem>();

	// 通知管理 和 设置是DM 固有的两项
	private WatchFragmentContentItem mMessageMgrItem = null, mSettingsItem = null;

	private TextView mNotificationDescTextView;

	private int item_layout_height;
	private int item_paddingLeft;

	public MyWatchFragmentRevision(ArrayList<DisplayInfo> displayInfos) {
		super();
		mDisplayInfos.addAll(displayInfos);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_my_watch_revision, container, false);

		mResources = getResources();
		item_layout_height = (int) mResources.getDimension(R.dimen.HOST_HOME_FRAGMENT_revision_item_height);
		item_paddingLeft = (int) mResources.getDimension(R.dimen.HOST_HOME_FRAGMENT_revision_item_margin_left);

		initView(rootView);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		TwsLog.d(TAG, "=========onResume=========");
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	/**
	 * 注意这里的index是指fragment列表PaceWear这一列的后面开始计算
	 */
	public void addContentItem(DisplayInfo info) {
		// 当前显示在首页My_Watch的内容暂只接收activity
		if (info.componentType != DisplayConfig.TYPE_ACTIVITY) {
			return;
		}

		if (!isEnabledDisplayInfo(info)) {
			TwsLog.e(TAG, "info is illegal(already exists), This will be ignored!");
			return;
		}

		WatchFragmentContentItem item = new WatchFragmentContentItem(mFragmentContainer.getContext(), true);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, item_layout_height);
		item.setPadding(item_paddingLeft, 0, 0, 0);
		item.setLayoutParams(lp);
		item.setBackgroundResource(R.drawable.list_selector_background);// R.drawable.dm_common_single_item_selector
		item.setImageViewImageDrawable(PluginManagerHelper.getPluginIcon(info.normalResName));

		final ContextThemeWrapper context = getActivity();
		final Resources res = context == null ? HostApplication.getInstance().getResources() : context.getResources();
		final Locale locale = res.getConfiguration().locale;
		if ("zh".equals(locale.getLanguage())) {
			if ("HK".equals(locale.getCountry())) {
				item.setText(info.title_zh_HK);
			} else if ("TW".equals(locale.getCountry())) {
				item.setText(info.title_zh_TW);
			} else {
				item.setText(info.title_zh_CN);
			}
		} else {
			item.setText(info.title_en);
		}

		item.setOnClickListener(this);
		item.mStatKey = info.statKey;
		item.setActionClass(info.classId, info.componentType);
		item.setPluginPackageName(info.packageName);
		item.setLocation(info.location < 0 ? FIX_LOCATION_BEGIN - 1 : info.location);
		item.setVisibility(info.establishedDependOn ? View.VISIBLE : View.GONE);

		boolean insertRlt = false;
		int index = 0;
		for (index = 0; index < mContentItems.size(); index++) {
			if (item.getLocation() < mContentItems.get(index).getLocation()) {
				insertRlt = true;
				mContentItems.add(index, item);
				break;
			}
		}
		if (!insertRlt) {
			mContentItems.add(item);
		}

		mFragmentContainer.addView(item, index + 1);// +1是fragment顶部有一个固定的linelayout
	}

	public boolean isEnabledDisplayInfo(final DisplayInfo info) {
		if (info == null || TextUtils.isEmpty(info.packageName) || TextUtils.isEmpty(info.classId))
			return false;

		for (WatchFragmentContentItem item : mContentItems) {
			if (info.packageName.equals(item.getPluginPackageName()) && info.classId.equals(item.getClassId())) {
				return false;
			}
		}

		return true;
	}

	public void printContentItemsInfo() {
		TwsLog.d(TAG, "============== begin printContentItemsInfo ==============");
		for (int index = 0; index < mContentItems.size(); index++) {
			final WatchFragmentContentItem item = mContentItems.get(index);
			TwsLog.d(
					TAG,
					"mContentItems[" + index + "] text is " + item.getTextViewText() + " Location is "
							+ item.getLocation());
		}
		TwsLog.d(TAG, "============== end printContentItemsInfo ==============");
	}

	private void initView(View rootView) {
		mFragmentContainer = (LinearLayout) rootView.findViewById(R.id.my_watch_revision_item_layout);
		mWatchInfoLayout = (RelativeLayout) rootView.findViewById(R.id.my_watch_revision_watch_info_layout);
		boolean hasOverlayActionbar = getActivity().getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		if (hasOverlayActionbar) {
			int top = (int) getResources().getDimension(R.dimen.tws_action_bar_height);
			if (getActivity() instanceof TwsActivity) {
				top += TwsActivity.getStatusBarHeight();
			}
			mWatchInfoLayout.setPadding(0, top, 0, 0);
		}

		mWatchInfoLayout.setOnClickListener(this);

		mWatchImg = (ImageView) rootView.findViewById(R.id.my_watch_revision_watch_img);
		mWatchImg.setImageResource(R.drawable.twatch_dm_png_default);

		mConnectText = (TextView) rootView.findViewById(R.id.my_watch_revision_connect_text);
		mConnectText.setText("未连接");

		mWatchNameText = (TextView) rootView.findViewById(R.id.my_watch_revision_watch_name);
		mWatchNameText.setText(R.string.watch_name);

		mOtaRedpoint = (ImageView) rootView.findViewById(R.id.my_watch_revision_redpoint_img);
		mOtaRedpoint.setImageResource(R.drawable.red_point);

		final Resources res = rootView.getContext().getResources();

		final int thickSplitHeight = (int) res.getDimension(R.dimen.HOST_HOME_FRAGMENT_revision_item_big_divider);
		final Drawable thickSplitBackground = res.getDrawable(R.color.tws_stipple);

		// 注意：插件提供的Item 索引值应该是从1开始的，上面有一个mWatchInfoLayout
		if (mDisplayInfos != null) {
			for (DisplayInfo info : mDisplayInfos) {
				addContentItem(info);
			}

			printContentItemsInfo();
		}

		// add 通知管理
		mMessageMgrItem = new WatchFragmentContentItem(rootView.getContext());
		mMessageMgrItem.setToNotify();
		LayoutParams lp_notify = new LayoutParams(LayoutParams.MATCH_PARENT, item_layout_height);
		mMessageMgrItem.setPadding(item_paddingLeft, 0, 0, 0);
		mMessageMgrItem.setLayoutParams(lp_notify);
		mMessageMgrItem.setBackgroundResource(R.drawable.list_selector_background);// R.drawable.dm_common_single_item_selector
		mMessageMgrItem.setImageViewImageDrawable(R.drawable.home_item_notification_normal);
		mMessageMgrItem.setText(res.getString(R.string.message_mgr));
		mMessageMgrItem.setOnClickListener(this);
		mMessageMgrItem.mSpecialFlg = WatchFragmentContentItem.ITEM_MESSAGE;
		mMessageMgrItem.setActionClass(MessageManagerActivity.class.getName(), DisplayConfig.TYPE_ACTIVITY);
		mMessageMgrItem.setLocation(FIX_LOCATION_BEGIN);
		mFragmentContainer.addView(mMessageMgrItem);
		// mContentItems.add(notifyMgr); //notifyMgr 作为DM固有的item可以不参与管理

		mNotiRedpointImg = mMessageMgrItem.getNotifyImageView();
		mNotificationDescTextView = mMessageMgrItem.getNotifyTextView();

		// 添加分割线 - 粗的
		insertSplit(mFragmentContainer, thickSplitHeight, 0, thickSplitBackground);
		// 最后添加Settings
		mSettingsItem = new WatchFragmentContentItem(rootView.getContext(), true);
		LayoutParams lp_settings = new LayoutParams(LayoutParams.MATCH_PARENT, item_layout_height);
		mSettingsItem.setPadding(item_paddingLeft, 0, 0, 0);
		mSettingsItem.setLayoutParams(lp_settings);
		mSettingsItem.setBackgroundResource(R.drawable.list_selector_background);// R.drawable.dm_common_single_item_selector
		mSettingsItem.setText(res.getString(R.string.settings));
		mSettingsItem.setImageViewImageDrawable(R.drawable.home_item_my_settings);
		mSettingsItem.setOnClickListener(this);
		mSettingsItem.mSpecialFlg = WatchFragmentContentItem.ITEM_SETTINGS;
		mSettingsItem.setActionClass(SettingsActivity.class.getName(), DisplayConfig.TYPE_ACTIVITY);
		mSettingsItem.setLocation(FIX_LOCATION_BEGIN + 1);
		mFragmentContainer.addView(mSettingsItem);
		// mContentItems.add(settings); //Settings 作为DM固有的item可以不参与管理
	}

	private void insertSplit(LinearLayout root, int height, int marginLeft, Drawable background) {
		TextView tv = new TextView(root.getContext());
		tv.setBackground(background);
		LinearLayout.LayoutParams lp_split = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
		lp_split.leftMargin = marginLeft;
		root.addView(tv, lp_split);
	}

	@Override
	public void onClick(View view) {
		if (view instanceof WatchFragmentContentItem) {
			final WatchFragmentContentItem item = (WatchFragmentContentItem) view;

			Intent intent = new Intent();
			if (TextUtils.isEmpty(item.getPluginPackageName())) {
				intent.setClassName(getActivity(), item.getClassId());
			} else {
				intent.setClassName(item.getPluginPackageName(), item.getClassId());
			}

			switch (item.getComponentType()) {
			case DisplayConfig.TYPE_ACTIVITY:
				startActivity(intent);
				break;
			default:
				break;
			}

			if (item.mSpecialFlg == WatchFragmentContentItem.ITEM_MESSAGE && item.isNotify()
					&& mNotiRedpointImg.getVisibility() == View.VISIBLE) {
				mNotiRedpointImg.setVisibility(View.INVISIBLE);
			}
		} else {
			switch (view.getId()) {
			case R.id.my_watch_revision_watch_info_layout:
				mOtaRedpoint.setVisibility(View.INVISIBLE);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		TwsLog.d(TAG, "=========onDestroyView=========");
	}

	public void removePlugin(String packageName) {
		if (TextUtils.isEmpty(packageName))
			return;

		ArrayList<WatchFragmentContentItem> removeItems = new ArrayList<WatchFragmentContentItem>();
		Iterator<WatchFragmentContentItem> iter = mContentItems.iterator();
		removeItems.clear();
		while (iter.hasNext()) {
			WatchFragmentContentItem item = iter.next();
			if (packageName.equals(item.getPluginPackageName())) {
				mFragmentContainer.removeView(item);
				removeItems.add(item);
			}
		}

		if (0 < removeItems.size()) {
			mContentItems.removeAll(removeItems);
		}
	}

	public void unEstablishedDependOnForPlugin(String pid) {
		if (TextUtils.isEmpty(pid))
			return;

		for (WatchFragmentContentItem item : mContentItems) {
			if (pid.equals(item.getPluginPackageName())) {
				item.setVisibility(View.GONE);
			}
		}
	}

	public void establishedDependOnForPlugin(String pid) {
		if (TextUtils.isEmpty(pid))
			return;

		for (WatchFragmentContentItem item : mContentItems) {
			if (pid.equals(item.getPluginPackageName())) {
				item.setVisibility(View.VISIBLE);
			}
		}
	}
}
