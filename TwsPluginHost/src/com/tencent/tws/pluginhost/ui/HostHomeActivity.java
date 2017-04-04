package com.tencent.tws.pluginhost.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import tws.component.log.TwsLog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.assistant.support.v4.app.FragmentPagerAdapter;
import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.framework.utils.HostProxy;
import com.tencent.tws.pluginhost.HostApplication;
import com.tencent.tws.pluginhost.R;
import com.tencent.tws.pluginhost.content.CellItem.ComponentName;
import com.tencent.tws.pluginhost.ui.view.Hotseat;
import com.tencent.tws.pluginhost.ui.view.MyWatchFragmentRevision;
import com.tws.plugin.content.DisplayConfig;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.PluginManagerHelper;

import dalvik.system.DexClassLoader;

public class HostHomeActivity extends TwsFragmentActivity {
	private final String TAG = "rick_Print:HostHomeActivity";

	private Hotseat mHotseat;

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

	private int mNormalTextColor;
	private int mFocusTextColor;
	// private HomeBar mHomeBar;
	private ActionBar mActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_home);
		mHotseat = (Hotseat) findViewById(R.id.home_bottom_tab);
		mFragmentContainer = (FrameLayout) findViewById(R.id.home_fragment_container);

		mNormalTextColor = getResources().getColor(R.color.home_bottom_tab_text_default_color);
		mFocusTextColor = getResources().getColor(R.color.home_bottom_tab_text_pressed_color);

		initPluginPosFromConfig(POS_HOTSEAT);
		initPluginPosFromConfig(POS_HOME_FRAGEMENT);

		initPluginsDisplayInfo();
		// 初始化底部Hotseat
		initHotseat();

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
		final int fouceIndex = mHotseat.getPosByClassId(((HostApplication) HostApplication.getInstance()).getFouceTabClassId());
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
		mHotseat.addOneBottomButton(Hotseat.MY_WATCH_FRAGMENT, null, Hotseat.FRAGMENT_COMPONENT, getResources()
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

	private void initActionBar() {
		mActionBar = getTwsActionBar();
		mActionBar.getActionBarHome().setVisibility(View.GONE);
		mActionBar.getRightButtonView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// handleActionBarRightButtonClick();
			}
		});
		mActionBar.getRightButtonView().setClickable(false);
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

	private void initPluginsDisplayInfo() {
		// TODO Auto-generated method stub

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
		} else if (classId.equals(Hotseat.MY_WATCH_FRAGMENT)) {
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
				this.title_zh = titles[0];
				if (1 < titles.length) {
					this.title_en = titles[1];
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
					+ this.componentType + " title_zh=" + this.title_zh + " normalResName=" + this.normalResName
					+ " focusResName=" + this.focusResName + " packageName=" + this.packageName);

			if (dc.pos == DisplayConfig.DISPLAY_AT_HOTSEAT) {
				// actionbar的标题
				if (!TextUtils.isEmpty(dc.ab_title)) {
					final String[] titles = dc.ab_title.split(DisplayConfig.SEPARATOR_VALUE);
					this.ab_title_zh = titles[0];
					if (1 < titles.length) {
						this.ab_title_en = titles[1];
					}
				} else {
					this.ab_title_zh = this.ab_title_en = getResources().getString(R.string.app_name);
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

				TwsLog.d(TAG, "DisplayInfo ab_title_zh=" + this.ab_title_zh + " ab_title_en=" + ab_title_en
						+ " ab_rbtncontent=" + this.ab_rbtncontent + " ab_rbtnctype=" + this.ab_rbtnctype
						+ " ab_rbtnres_normal=" + this.ab_rbtnres_normal + " ab_rbtnres_focus=" + this.ab_rbtnres_focus
						+ " ab_rbtnrestype=" + this.ab_rbtnrestype);
			}
		}

		public String classId = null;
		public int componentType;
		public CharSequence title_zh = null;
		public CharSequence title_en = null;
		public String normalResName = null;
		public String focusResName = null;
		public String statKey = "";
		public String packageName = "";
		public int location = 0; // 用于显示的索引位置

		// ActionBar
		public String ab_title_zh = null;
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
}
