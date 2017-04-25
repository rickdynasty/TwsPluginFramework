package com.tencent.tws.pluginhost.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import tws.component.log.TwsLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.assistant.support.v4.app.FragmentManager;
import com.tencent.tws.assistant.support.v4.app.FragmentPagerAdapter;
import com.tencent.tws.assistant.support.v4.app.FragmentTransaction;
import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.framework.HomeUIProxy;
import com.tencent.tws.framework.HostProxy;
import com.tencent.tws.pluginhost.HostApplication;
import com.tencent.tws.pluginhost.R;
import com.tencent.tws.pluginhost.ui.view.CellItem.ActionBarInfo;
import com.tencent.tws.pluginhost.ui.view.CellItem.ComponentName;
import com.tencent.tws.pluginhost.ui.view.Hotseat;
import com.tencent.tws.pluginhost.ui.view.Hotseat.OnHotseatClickListener;
import com.tencent.tws.pluginhost.ui.view.MyWatchFragmentRevision;
import com.tws.plugin.content.DisplayConfig;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.InstallResult;
import com.tws.plugin.manager.PluginCallback;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;

import dalvik.system.DexClassLoader;

public class HostHomeActivity extends TwsFragmentActivity implements HomeUIProxy {
	private final String TAG = "rick_Print:HostHomeActivity";

	private Hotseat mHotseat;
	private OnHotseatClickListener mHotseatClickCallback;

	private FragmentPagerAdapter mFragmentPagerAdapter;
	private MyWatchFragmentRevision mMyWatchFragment = null;// 这个是DM固有的，直接保留引用就不开回调进行后续同步的操作处理

	private FrameLayout mFragmentContainer;
	private ArrayList<DisplayInfo> mHotseatDisplayInfos = new ArrayList<DisplayInfo>();
	private ArrayList<DisplayInfo> mHomeFragementDisplayInfos = new ArrayList<DisplayInfo>(); // myWatchfaceFragment

	private final int POS_HOTSEAT = DisplayConfig.DISPLAY_AT_HOTSEAT;
	private final int POS_HOME_FRAGEMENT = DisplayConfig.DISPLAY_AT_HOME_FRAGEMENT;
	private final String POS_HOTSEAT_CONFIG_NAME = "plugin_hotseat_pos.ini";
	private final String POS_MYWATCH_CONFIG_NAME = "plugin_mywatch_pos.ini";
	private HashMap<String, Integer> mPluginHotsetPos = new HashMap<String, Integer>();
	private HashMap<String, Integer> mPluginMywatchPos = new HashMap<String, Integer>();
	private final int POS_WEIGHT = 5; // 位置信息的权重值

	private final int DEPEND_ON_APPLICATION = 1;
	private final int DEPEND_ON_PLUGIN = 2;

	private HashMap<String, String> mDependOnMap = new HashMap<String, String>();

	private int mNormalTextColor;
	private int mFocusTextColor;

	// private HomeBar mHomeBar;
	private ActionBar mActionBar;
	private int mBar_rBtnActionType = DisplayConfig.TYPE_ACTIVITY; // 当前默认是activity
	private String mBar_rBtnActionContent = null;

	// 插件更新监听
	private final BroadcastReceiver mPluginChangedMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int actionType = intent.getIntExtra(PluginCallback.EXTRA_TYPE, PluginCallback.TYPE_UNKNOW);
			final String packageName = intent.getStringExtra(PluginCallback.EXTRA_ID);
			final int installRlt = intent.getIntExtra(PluginCallback.EXTRA_RESULT_CODE, -1);
			if (TextUtils.isEmpty(packageName)) {// 无效的包名信息
				TwsLog.e(TAG, "Receive Invalid information package name");
				return;
			}

			switch (actionType) {
			case PluginCallback.TYPE_INSTALL:
				if (installRlt == InstallResult.SUCCESS) {
					TwsLog.d(TAG, "插件：" + packageName + "安装成功了~");
					installPlugin(packageName);
				}
				break;
			case PluginCallback.TYPE_REMOVE:
				// success ? 0 : 7
				if (installRlt == 0) {// 卸载成功
					TwsLog.d(TAG, "插件：" + packageName + "被卸载了哈~");
					if (mHotseat == null) {
						TwsLog.d(TAG, "貌似mHotseat还没初始化哦"); // 这种情况应该基本不会出现
					} else {
						removePlugin(packageName);
					}

					if (mMyWatchFragment == null) {
						TwsLog.d(TAG, "貌似MyWatchFragment还没构建出来"); // 这种情况有可能出现哦
					} else {
						mMyWatchFragment.removePlugin(packageName);
					}
				}
				break;
			case PluginCallback.TYPE_REMOVE_ALL:
				// success ? 0 : 7
				if (installRlt == 0) {// 卸载成功
					TwsLog.d(TAG, "~~~~(>_<)~~~~所有插件都被卸载了咯！");
					// ~暂不处理，因为DM起来首页显示出来后，应该是不存在这个情况
				}
				break;
			default:
				break;
			}
		}
	};

	// 应用安装卸载监听
	private final BroadcastReceiver mAppUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_ADDED)) {
				String packageName = intent.getData().getSchemeSpecificPart();
				if (mDependOnMap.containsKey(packageName)) {
					String pid = mDependOnMap.get(packageName);
					establishedDependOnForPlugin(pid);
				}
			} else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REMOVED)) {
				String packageName = intent.getData().getSchemeSpecificPart();
				if (mDependOnMap.containsKey(packageName)) {
					String pid = mDependOnMap.get(packageName);
					unEstablishedDependOnForPlugin(pid);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		// 监听插件更新
		registerReceiver(mPluginChangedMonitor, new IntentFilter(PluginCallback.ACTION_PLUGIN_CHANGED));

		// 监听应用的安装卸载
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PACKAGE_ADDED");
		filter.addAction("android.intent.action.PACKAGE_REMOVED");
		filter.addDataScheme("package");
		registerReceiver(mAppUpdateReceiver, filter);

		setContentView(R.layout.activity_home);
		mHotseat = (Hotseat) findViewById(R.id.home_bottom_tab);
		mFragmentContainer = (FrameLayout) findViewById(R.id.home_fragment_container);
		initHomeBottomTabObserver();

		mNormalTextColor = getResources().getColor(R.color.home_bottom_tab_text_default_color);
		mFocusTextColor = getResources().getColor(R.color.home_bottom_tab_text_pressed_color);

		initPluginPosFromConfig(POS_HOTSEAT);
		initPluginPosFromConfig(POS_HOME_FRAGEMENT);

		initPluginsDisplayInfo();
		// 初始化底部Hotseat
		initHotseat();
		HostProxy.setHomeUIProxy(this);

		mFragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

			@Override
			public Fragment getItem(int position) {
				return getFragmentByTagIndex(position);
			}

			@Override
			public int getCount() {
				return mHotseat.childCount();
			}

		};

		initActionBar();

		// 默认聚焦位置
		final int fouceIndex = mHotseat.getPosByClassId(((HostApplication) HostApplication.getInstance())
				.getFouceTabClassId());
		switchFragment(mHotseat.setFocusIndex(fouceIndex));
	}

	private void initHotseat() {

		if (mHotseat == null) {
			mHotseat = (Hotseat) findViewById(R.id.home_bottom_tab);
		}

		if (mHotseat == null) {
			TwsLog.e(TAG, "initBottomTabView() mHotseat is null");
			return;
		}
		// 添加my watch fragment
		mHotseat.addOneBottomButton(Hotseat.HOST_HOME_FRAGMENT, null, Hotseat.FRAGMENT_COMPONENT, getResources()
				.getString(R.string.home_my_watch),
				getResources().getDrawable(R.drawable.home_bottom_tab_my_watch_default),
				getResources().getDrawable(R.drawable.home_bottom_tab_my_watch_pressed), mNormalTextColor,
				mFocusTextColor, 0);

		// 其他的根据插件配置来
		for (DisplayInfo info : mHotseatDisplayInfos) {
			// 当前Hotseat上暂时之放置fragment
			if (info.componentType != DisplayConfig.TYPE_FRAGMENT) {
				continue;
			}

			mHotseat.addOneBottomButtonForPlugin(info, mNormalTextColor, mFocusTextColor, false);
		}
	}

	private void initHomeBottomTabObserver() {
		mHotseatClickCallback = new OnHotseatClickListener() {

			@Override
			public void onItemClick(int index) {
				TwsLog.d(TAG, "onItemClick:" + index);
				switchFragment(index);
			}

			@Override
			public void updateActionBar(ActionBarInfo actionBarInfo) {
				mActionBar.setTitle(actionBarInfo.ab_title);
				TwsLog.d(TAG, "updateActionBar:" + actionBarInfo.toString());
				if (TextUtils.isEmpty(actionBarInfo.ab_rbtncontent)) {// 不需要右侧按钮
					mActionBar.getRightButtonView().setImageResource(R.color.transparent);
					mActionBar.getRightButtonView().setClickable(false);
				} else if (actionBarInfo.ab_rbtnrestype == DisplayConfig.ACTIONBAR_RBTN_TYPE_ICON) {
					mActionBar.getRightButtonView().setVisibility(View.VISIBLE);
					mActionBar.getRightButtonView().setClickable(true);

					final String resName = actionBarInfo.ab_rbtnres_normal + "_" + actionBarInfo.ab_rbtnres_focus;
					Drawable drawable = PluginManagerHelper.getPluginIcon(resName);
					if (drawable != null) {
						mActionBar.getRightButtonView().setImageDrawable(drawable);
					} else {
						final Drawable normal = PluginManagerHelper.getPluginIcon(actionBarInfo.ab_rbtnres_normal);
						if (TextUtils.isEmpty(actionBarInfo.ab_rbtnres_normal) || normal == null) {
							mActionBar.getRightButtonView().setImageResource(R.drawable.ic_launcher);
						} else if (actionBarInfo.ab_rbtnres_normal.equals(actionBarInfo.ab_rbtnres_focus)) {
							mActionBar.getRightButtonView().setImageDrawable(normal);
						} else {
							final Drawable focus = PluginManagerHelper.getPluginIcon(actionBarInfo.ab_rbtnres_normal);
							if (focus == null) {
								mActionBar.getRightButtonView().setImageDrawable(normal);
							} else {
								StateListDrawable stateListDrawable = new StateListDrawable();
								stateListDrawable.addState(new int[] { android.R.attr.state_pressed },
										PluginManagerHelper.getPluginIcon(actionBarInfo.ab_rbtnres_focus));
								stateListDrawable.addState(new int[] {}, normal);

								PluginManagerHelper.addPluginIcon(resName, stateListDrawable);
								mActionBar.getRightButtonView().setImageDrawable(stateListDrawable);
							}
						}
					}

					mBar_rBtnActionContent = actionBarInfo.ab_rbtncontent;
					mBar_rBtnActionType = actionBarInfo.ab_rbtnctype;
				} else {
					// 略
				}
			}

			@Override
			public void onItemClick(int tagIndex, int extras) {
				TwsLog.d(TAG, "onItemClick:" + tagIndex);
				switchFragment(tagIndex, extras);
			}
		};

		mHotseat.addHotseatClickObserver(mHotseatClickCallback);
	}

	private void initActionBar() {
		mActionBar = getTwsActionBar();
		mActionBar.getActionBarHome().setVisibility(View.GONE);
		mActionBar.getRightButtonView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				handleActionBarRightButtonClick();
			}
		});
		mActionBar.getRightButtonView().setClickable(false);
	}

	private void handleActionBarRightButtonClick() {
		Intent intent = new Intent();
		intent.setClassName(this, mBar_rBtnActionContent);
		switch (mBar_rBtnActionType) {
		case DisplayConfig.TYPE_ACTIVITY:
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	private void installPlugin(String packageName) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
		if (null == pluginDescriptor || TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
			Toast.makeText(getApplicationContext(), "怎会存在没packageName的插件咧？？？", Toast.LENGTH_SHORT).show();
			Exception here = new Exception();
			here.fillInStackTrace();
			TwsLog.e(TAG, "My god !!! how can have such a situatio~!:" + packageName, here);
			return;
		}

		if (PluginApplication.getInstance().getEliminatePlugins().contains(pluginDescriptor.getPackageName())) {
			TwsLog.w(TAG, "当前插件" + pluginDescriptor.getPackageName() + "已经被列入黑名单了");
			return;
		}

		boolean establishedDependOn = establishedDependOns(pluginDescriptor.getPackageName(),
				pluginDescriptor.getDependOns());

		final ArrayList<DisplayConfig> dcs = pluginDescriptor.getDisplayConfigs();
		if (dcs != null && 0 < dcs.size()) {
			String iconDir = new File(pluginDescriptor.getInstalledPath()).getParent() + File.separator
					+ FileUtil.ICON_FOLDER;
			for (DisplayConfig dc : dcs) {
				DisplayInfo info = new DisplayInfo(dc, pluginDescriptor.getPackageName());
				info.establishedDependOn = establishedDependOn;

				loadPluginIcon(info, pluginDescriptor, dc.pos == DisplayConfig.DISPLAY_AT_HOTSEAT, iconDir);

				switch (dc.pos) {
				case DisplayConfig.DISPLAY_AT_HOTSEAT: // 显示在Hotseat上
					// 当前Hotseat上暂时之放置fragment
					if (info.componentType != DisplayConfig.TYPE_FRAGMENT) {
						break;
					}
					mHotseat.addOneBottomButtonForPlugin(info, mNormalTextColor, mFocusTextColor, true);
					break;
				case DisplayConfig.DISPLAY_AT_HOME_FRAGEMENT: // 显示在host_home_fragement
					// mHomeFragementDisplayInfos.add(info);
					mMyWatchFragment.addContentItem(info);
					break;
				case DisplayConfig.DISPLAY_AT_OTHER_POS:// 显示在其他位置
					switch (info.componentType) {
					case DisplayConfig.TYPE_SERVICE:
						Intent intent = new Intent();
						intent.setClassName(HostHomeActivity.this, info.classId);
						startService(intent);
						break;
					case DisplayConfig.TYPE_PACKAGENAEM:
						if (null == PluginLauncher.instance().startPlugin(info.classId)) {
							Toast.makeText(HostHomeActivity.this, "startPlugin:" + info.classId + "失败!!!",
									Toast.LENGTH_LONG).show();
						}
						break;
					default:
						break;
					}
					break;
				case DisplayConfig.DISPLAY_AT_MENU: // 这个当前没有，暂不处理
				default:
					break;
				}
			}
		} else {
			Toast.makeText(getApplicationContext(), "插件：" + pluginDescriptor.getApplicationName() + "没配置显示协议",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void removePlugin(String packageName) {
		TwsLog.d(TAG, "removePlugin:" + packageName);
		int removeTagIndex = mHotseat.removePlugin(packageName);
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		String name = makeFragmentName(mFragmentContainer.getId(), mFragmentPagerAdapter.getItemId(removeTagIndex));
		Fragment fragment = fragmentManager.findFragmentByTag(name);
		if (fragment != null) {
			TwsLog.d(TAG, "removePlugin removeTagIndex=" + removeTagIndex);
			transaction.remove(fragment);
			// transaction.commit();

			// rick_Note:这里被删除的fragment需要做清理操作哈~
		}
	}

	private void establishedDependOnForPlugin(String pid) {
		mHotseat.establishedDependOnForPlugin(pid);
		mMyWatchFragment.establishedDependOnForPlugin(pid);
	}

	private void unEstablishedDependOnForPlugin(String pid) {
		mHotseat.unEstablishedDependOnForPlugin(pid);
		mMyWatchFragment.unEstablishedDependOnForPlugin(pid);
	}

	// 这个函数同步FragmentPagerAdapter，建议直接用FragmentPagerAdapter里面的接口
	private String makeFragmentName(int viewId, long id) {
		return "android:switcher:" + viewId + ":" + id;
	}

	private void switchFragment(int tagIndex) {
		if (tagIndex < 0) {
			TwsLog.e(TAG, "我的乖乖，怎么会有位置是：" + tagIndex + " 的内容可以切换咧，得check一下是否Hotseat没有内容？");
			return;
		}

		TwsLog.d(TAG, "switchFragment:" + tagIndex);
		Fragment fragment = (Fragment) mFragmentPagerAdapter.instantiateItem(mFragmentContainer, tagIndex);
		mFragmentPagerAdapter.setPrimaryItem(mFragmentContainer, tagIndex, fragment);
		mFragmentPagerAdapter.finishUpdate(mFragmentContainer);
	}

	private void switchFragment(int tagIndex, int extras) {
		if (tagIndex < 0) {
			TwsLog.e(TAG, "我的乖乖，怎么会有位置是：" + tagIndex + " 的内容可以切换咧，得check一下是否Hotseat没有内容？");
			return;
		}

		TwsLog.d(TAG, "switchFragment:" + tagIndex);
		Fragment fragment = (Fragment) mFragmentPagerAdapter.instantiateItem(mFragmentContainer, tagIndex);
		fragment.acceptExtras(extras);
		mFragmentPagerAdapter.setPrimaryItem(mFragmentContainer, tagIndex, fragment);
		mFragmentPagerAdapter.finishUpdate(mFragmentContainer);
	}

	/**
	 * 收集内容： String classId int componentType CharSequence text int
	 * bkNormalResId, int bkFocusResId
	 */
	private void initPluginsDisplayInfo() {
		// 底部Hotseat 以及 首页watchFragment的内容是有顺序的
		mHotseatDisplayInfos.clear();
		mHomeFragementDisplayInfos.clear();
		// mOtherPosDisplayInfos.clear();

		Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
		Iterator<PluginDescriptor> itr = plugins.iterator();
		boolean hasGetHotSeatPos = false;
		while (itr.hasNext()) {
			final PluginDescriptor pluginDescriptor = itr.next();
			if (TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
				Log.e(TAG, "My god !!! how can have such a situatio~!");
				continue;
			}
			if (PluginApplication.getInstance().getEliminatePlugins().contains(pluginDescriptor.getPackageName())) {
				TwsLog.w(TAG, "当前插件" + pluginDescriptor.getPackageName() + "已经被列入黑名单了");
				continue;
			}

			boolean establishedDependOn = establishedDependOns(pluginDescriptor.getPackageName(),
					pluginDescriptor.getDependOns());

			final ArrayList<DisplayConfig> dcs = pluginDescriptor.getDisplayConfigs();
			if (dcs != null && 0 < dcs.size()) {
				hasGetHotSeatPos = false;
				String iconDir = new File(pluginDescriptor.getInstalledPath()).getParent() + File.separator
						+ FileUtil.ICON_FOLDER;
				for (DisplayConfig dc : dcs) {
					DisplayInfo info = new DisplayInfo(dc, pluginDescriptor.getPackageName());
					info.establishedDependOn = establishedDependOn;

					loadPluginIcon(info, pluginDescriptor, dc.pos == DisplayConfig.DISPLAY_AT_HOTSEAT, iconDir);

					switch (dc.pos) {
					case DisplayConfig.DISPLAY_AT_HOTSEAT: // 显示在Hotseat上
						if (!hasGetHotSeatPos) {
							mHotseatDisplayInfos.add(info);
							hasGetHotSeatPos = true;
						} else {
							TwsLog.e(TAG, "哦~——~哦，插件：" + pluginDescriptor.getPackageName()
									+ " 竟然有两个在Hotseat的Pos位，需要框架做一点点的扩展兼容哈。");
						}
						break;
					case DisplayConfig.DISPLAY_AT_HOME_FRAGEMENT: // 显示在host_home_fragement
						mHomeFragementDisplayInfos.add(info);
						break;
					case DisplayConfig.DISPLAY_AT_OTHER_POS:// 显示在其他位置
						// mOtherPosDisplayInfos.add(info);
						// 这个时机已经调整到application的onCreate了
						break;
					case DisplayConfig.DISPLAY_AT_MENU: // 这个当前没有，暂不处理
					default:
						break;
					}
				}
			} else {
				Log.e(TAG, "插件：" + pluginDescriptor.getPackageName() + "没有配置plugin-display协议~");
			}
		}
	}

	private boolean establishedDependOns(String pid, ArrayList<String> dependOns) {
		// 依赖检测
		if (dependOns == null || dependOns.size() <= 0) {
			return true;
		}

		// 当前这种依赖只处理一个
		String dependOnDes = dependOns.get(0);
		if (TextUtils.isEmpty(dependOnDes))
			return true;

		final String[] values = dependOnDes.split(DisplayConfig.SEPARATOR_DEPEND);
		TwsLog.d(TAG, "establishedDependOn:" + dependOnDes);
		String packageName = null;
		int type = DEPEND_ON_APPLICATION;
		if (values.length == 1) {
			packageName = values[0];
			type = DEPEND_ON_APPLICATION;
		} else if (values.length == 2) {
			type = Integer.parseInt(values[0]);
			packageName = values[1];
		} else {
			Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
		}

		switch (type) {
		case DEPEND_ON_APPLICATION:
			if (mDependOnMap.containsKey(packageName)) {
				String des = "插件：" + pid + "所依赖的：" + packageName + "条件，还有插件：" + mDependOnMap.get(packageName)
						+ "也依赖这个条件";
				Toast.makeText(this, des, Toast.LENGTH_LONG).show();
				TwsLog.d(TAG, des);
			}

			mDependOnMap.put(packageName, pid);
			TwsLog.d(TAG, "mDependOnMap put:" + packageName + " " + pid);

			if (!dependOnInstalledApp(packageName)) {
				return false;
			}
			break;
		case DEPEND_ON_PLUGIN:
			break;
		default:
			break;
		}

		return true;
	}

	private boolean dependOnInstalledApp(String packagename) {
		// String packagename = "com.qding.community";
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(packagename, 0);
		} catch (NameNotFoundException e) {
			packageInfo = null;
			e.printStackTrace();
		}

		if (packageInfo == null) {
			return false;
		} else {
			return true;
		}
	}

	private void loadPluginIcon(final DisplayInfo info, final PluginDescriptor pluginDescriptor, boolean isHotseat,
			String iconDir) {
		if (TextUtils.isEmpty(info.normalResName)) {
			TwsLog.e(TAG, "loadPluginIcon:" + info.normalResName + " failed for Illegal resources name~");
			return;
		}

		String iconPath;
		Bitmap normalIcon = null;
		if (null == PluginManagerHelper.getPluginIcon(info.normalResName)) {
			iconPath = iconDir + File.separator + info.normalResName + FileUtil.FIX_ICON_NAME;
			normalIcon = BitmapFactory.decodeFile(iconPath);
			if (normalIcon != null) {
				PluginManagerHelper.addPluginIcon(info.normalResName, new BitmapDrawable(getResources(), normalIcon));
			}
		}

		if (null == PluginManagerHelper.getPluginIcon(info.focusResName)
				&& !info.normalResName.equals(info.focusResName)) {
			iconPath = iconDir + File.separator + info.focusResName + FileUtil.FIX_ICON_NAME;
			Bitmap focusIcon = BitmapFactory.decodeFile(iconPath);
			if (focusIcon != null) {
				PluginManagerHelper.addPluginIcon(info.focusResName, new BitmapDrawable(getResources(), focusIcon));
			}
		}

		if (!isHotseat)
			return;

		if (info.ab_rbtnrestype == DisplayConfig.ACTIONBAR_RBTN_TYPE_ICON && !TextUtils.isEmpty(info.ab_rbtnres_normal)
				&& null == PluginManagerHelper.getPluginIcon(info.ab_rbtnres_normal)) {

			iconPath = iconDir + File.separator + info.ab_rbtnres_normal + FileUtil.FIX_ICON_NAME;
			Bitmap abr_normalIcon = BitmapFactory.decodeFile(iconPath);
			if (abr_normalIcon != null) {
				PluginManagerHelper.addPluginIcon(info.ab_rbtnres_normal, new BitmapDrawable(getResources(),
						abr_normalIcon));
			}

			if (null == PluginManagerHelper.getPluginIcon(info.ab_rbtnres_focus)
					&& !info.ab_rbtnres_normal.equals(info.ab_rbtnres_focus)) {
				iconPath = iconDir + File.separator + info.ab_rbtnres_focus + FileUtil.FIX_ICON_NAME;
				Bitmap abr_focusIcon = BitmapFactory.decodeFile(iconPath);
				if (abr_focusIcon != null) {
					PluginManagerHelper.addPluginIcon(info.ab_rbtnres_focus, new BitmapDrawable(getResources(),
							abr_focusIcon));
				}
			}
		}
	}

	private Fragment getFragmentByTagIndex(int tagIndex) {
		final ComponentName componentName = mHotseat.getComponentNameByTagIndex(tagIndex);
		final String classId = componentName.getClassId();
		TwsLog.d(TAG, "getFragmentByTagIndex:" + tagIndex + " classId is " + classId + " will create it(Fragment)");

		Fragment fragment = null;
		String msg = "";
		if (TextUtils.isEmpty(classId)) {
			msg = "invalid classId：" + classId + "，请先check这个无效的标识符来源~~~~";
			Toast.makeText(this, "getFragmentByTagIndex:" + tagIndex + " return null classId", Toast.LENGTH_LONG)
					.show();
		} else if (classId.equals(Hotseat.HOST_HOME_FRAGMENT)) {
			fragment = mMyWatchFragment = new MyWatchFragmentRevision(mHomeFragementDisplayInfos);
		} else {
			TwsLog.d(TAG, "getFragmentByPos to get Plugin fragement:" + classId);
			Class<?> clazz = null;
			if (!TextUtils.isEmpty(componentName.getPluginPackageName())) {
				PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(componentName
						.getPluginPackageName());
				if (pluginDescriptor != null) {
					// 插件可能尚未初始化，确保使用前已经初始化
					LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

					DexClassLoader pluginClassLoader = plugin.pluginClassLoader;

					String clazzName = pluginDescriptor.getPluginClassNameById(classId);
					if (clazzName != null) {
						try {
							clazz = ((ClassLoader) pluginClassLoader).loadClass(clazzName);
						} catch (ClassNotFoundException e) {
							TwsLog.e(TAG, "loadPluginFragmentClassById:" + classId + " ClassNotFound:" + clazzName
									+ "Exception", e);
							TwsLog.w(TAG, "没有找到：" + clazzName + " 是不是被混淆了~");
						}
					}
				} else {
					clazz = PluginLoader.loadPluginFragmentClassById(componentName.getClassId());
				}
			} else {
				clazz = PluginLoader.loadPluginFragmentClassById(componentName.getClassId());
			}

			if (clazz != null) {
				try {
					fragment = (Fragment) clazz.newInstance();
				} catch (InstantiationException e) {
					TwsLog.e(TAG, "InstantiationException", e);
				} catch (IllegalAccessException e) {
					TwsLog.e(TAG, "IllegalAccessException", e);
				}
			}

			msg = "Not found classId：" + classId + "，请先check提供这个Fragment的插件是否有安装哈(⊙o⊙)~";
		}

		if (fragment == null) {
			fragment = new ToastFragment();
			((ToastFragment) fragment).setToastMsg(msg);
		}

		return fragment;// new Fragment();
	}

	// 二次解析DisplayConfig
	public class DisplayInfo {
		public static final int DEFAULT_DISPLAY_LOCATION = 99;

		public DisplayInfo(final DisplayConfig dc, final String packageName) {
			this.classId = dc.content;
			this.componentType = dc.contentType;
			// 注意协议对二级pos没配置的默认赋值是-1，这种需要随波逐流，也就是谁先安装谁放前面，谁叫没给配置咧
			final int pos = getPosByPackageName(dc.pos, packageName);
			if (-1 < pos) {
				if (dc.secondPos < 0) {
					dc.secondPos = 0;
				}

				if (POS_WEIGHT <= dc.secondPos) {
					dc.secondPos = dc.secondPos % POS_WEIGHT;
				}

				this.location = pos * POS_WEIGHT + dc.secondPos;
			} else {
				this.location = DEFAULT_DISPLAY_LOCATION;
			}
			TwsLog.d(TAG, "==packageName:" + packageName + " title=" + dc.title + " dc.pos=" + dc.pos + " location is "
					+ this.location);

			if (!TextUtils.isEmpty(dc.title)) {
				final String[] titles = dc.title.split(DisplayConfig.SEPARATOR_VALUE);
				this.title_en = this.title_zh_CN = this.title_zh_TW = this.title_zh_HK = titles[0];
				if (1 < titles.length) {
					this.title_en = titles[1];
				}

				if (2 < titles.length) {
					this.title_zh_TW = this.title_zh_HK = titles[2];
				}

				if (3 < titles.length) {
					this.title_zh_TW = titles[3];
				}
			}

			this.statKey = dc.statKey;
			if (!TextUtils.isEmpty(dc.iconResName)) {
				final String[] resNames = dc.iconResName.split(DisplayConfig.SEPARATOR_VALUE);
				this.normalResName = resNames[0];
				if (1 < resNames.length) {
					this.focusResName = resNames[1];
				}
			}
			this.packageName = packageName;

			TwsLog.d(TAG, "DisplayInfo classId=" + this.classId + " location=" + location + " componentType="
					+ this.componentType + " title_zh=" + this.title_zh_CN + "/" + this.ab_title_zh_HK + "/"
					+ this.ab_title_zh_TW + " normalResName=" + this.normalResName + " focusResName="
					+ this.focusResName + " packageName=" + this.packageName);

			if (dc.pos == DisplayConfig.DISPLAY_AT_HOTSEAT) {
				// actionbar的标题
				if (!TextUtils.isEmpty(dc.ab_title)) {
					final String[] titles = dc.ab_title.split(DisplayConfig.SEPARATOR_VALUE);
					this.ab_title_en = this.ab_title_zh_CN = this.ab_title_zh_TW = this.ab_title_zh_HK = titles[0];
					if (1 < titles.length) {
						this.ab_title_en = titles[1];
					}

					if (2 < titles.length) {
						this.ab_title_zh_TW = this.ab_title_zh_HK = titles[2];
					}

					if (3 < titles.length) {
						this.ab_title_zh_TW = titles[3];
					}
				} else {
					this.ab_title_en = this.ab_title_zh_CN = this.ab_title_zh_TW = this.ab_title_zh_HK = getResources()
							.getString(R.string.app_name);
				}

				if (!TextUtils.isEmpty(dc.ab_rbtncontent)) {
					this.ab_rbtncontent = dc.ab_rbtncontent;
					this.ab_rbtnctype = dc.ab_rbtnctype;

					if (!TextUtils.isEmpty(dc.ab_rbtnres)) {
						final String[] resNames = dc.ab_rbtnres.split(DisplayConfig.SEPARATOR_VALUE);
						this.ab_rbtnres_normal = resNames[0];// 是显示文本还是图标由ab_rbtnrestype来决定
						if (1 < resNames.length) {
							this.ab_rbtnres_focus = resNames[1];
						}
					}

					this.ab_rbtnrestype = dc.ab_rbtnrestype;
				} else {
					// action 行为都没有 就不处理了
				}

				TwsLog.d(TAG, "DisplayInfo ab_title_zh=" + this.ab_title_zh_CN + "/" + this.ab_title_zh_HK + "/"
						+ this.ab_title_zh_TW + " ab_title_en=" + ab_title_en + " ab_rbtncontent="
						+ this.ab_rbtncontent + " ab_rbtnctype=" + this.ab_rbtnctype + " ab_rbtnres_normal="
						+ this.ab_rbtnres_normal + " ab_rbtnres_focus=" + this.ab_rbtnres_focus + " ab_rbtnrestype="
						+ this.ab_rbtnrestype);
			}
		}

		public String classId = null;
		public int componentType;
		public CharSequence title_zh_CN = null;
		public CharSequence title_zh_TW = null;
		public CharSequence title_zh_HK = null;
		public CharSequence title_en = null;
		public String normalResName = null;
		public String focusResName = null;
		public String statKey = "";
		public String packageName = "";
		public int location = 0; // 用于显示的索引位置

		public boolean establishedDependOn = true;

		// ActionBar
		public String ab_title_zh_CN = null;
		public String ab_title_zh_TW = null;
		public String ab_title_zh_HK = null;
		public String ab_title_en = null;
		// ActionBar右侧按钮上触发点击后的行为内容
		public String ab_rbtncontent = null;
		// ActionBar右侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
		public int ab_rbtnctype = 2;// 默认是activity
		// 显示在ActionBar右侧按钮上的内容类型 1、String文本资源 2、图标资源
		public int ab_rbtnrestype = 1;
		// 显示在ActionBar右侧按钮上的内容，根据类型进行配置
		public String ab_rbtnres_normal = null;
		public String ab_rbtnres_focus = null;
	}

	public void initPluginPosFromConfig(int posType) {
		String configFile = "";
		HashMap<String, Integer> pluginPos;
		switch (posType) {
		case POS_HOTSEAT:
			configFile = POS_HOTSEAT_CONFIG_NAME;
			pluginPos = mPluginHotsetPos;
			break;
		case POS_HOME_FRAGEMENT:
			configFile = POS_MYWATCH_CONFIG_NAME;
			pluginPos = mPluginMywatchPos;
			break;
		default:
			return;
		}

		pluginPos.clear();
		boolean sucess = false;
		Integer pos = 0;
		try {
			InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open(configFile));
			BufferedReader bufReader = new BufferedReader(inputReader);
			String line = "";
			while ((line = bufReader.readLine()) != null) {
				line = line.trim();
				if (TextUtils.isEmpty(line))
					continue;

				pluginPos.put(line, pos);
				++pos;
			}
			sucess = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!sucess) {
			switch (posType) {
			case POS_HOTSEAT:
				mPluginHotsetPos.clear();
				String[] defaultPos_Hotset = getResources().getStringArray(R.array.default_hotset_pos);
				pos = 0;
				for (String packageName : defaultPos_Hotset) {
					packageName = packageName.trim();
					if (TextUtils.isEmpty(packageName))
						continue;

					mPluginHotsetPos.put(packageName, pos);
					++pos;
				}
				break;
			case POS_HOME_FRAGEMENT:
				mPluginMywatchPos.clear();
				String[] defaultPos_Mywatch = getResources().getStringArray(R.array.default_mywatch_pos);
				pos = 0;
				for (String packageName : defaultPos_Mywatch) {
					packageName = packageName.trim();
					if (TextUtils.isEmpty(packageName))
						continue;

					mPluginMywatchPos.put(packageName, pos);
					++pos;
				}
				break;
			default:
				break;
			}
		}
	}

	private int getPosByPackageName(int posType, String packageName) {
		switch (posType) {
		case POS_HOTSEAT:
			if (mPluginHotsetPos.containsKey(packageName)) {
				return mPluginHotsetPos.get(packageName);
			}
			return -1;
		case POS_HOME_FRAGEMENT:
			if (mPluginMywatchPos.containsKey(packageName)) {
				return mPluginMywatchPos.get(packageName);
			}
			return -1;
		default:
			return -1;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// 当前不对mFragments进行状态保存[恢复的时候状态错乱了]，在这里通过mCurrentPage来保存并进行恢复
		if (outState != null) {
			outState.remove(FRAGMENTS_TAG);
		}
	}

	@Override
	public void switchToFragment(String classId, int extras) {
		if (mHotseat == null) {
			TwsLog.e(TAG, "switchToFragment:" + classId + " Failed for mHotseat is null");
			return;
		}

		mHotseat.switchToFragment(classId, extras);
	}

	@Override
	public void setHighlightCellItem(String classId, boolean needHighlight) {
		if (mHotseat == null) {
			TwsLog.e(TAG, "setHighlightCellItem:" + classId + " Failed for mHotseat is null");
			return;
		}

		mHotseat.setHighlightCellItem(classId, needHighlight);
	}
}
