package com.tencent.tws.pluginhost.ui;

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
import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.content.HostDisplayItem;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.InstallResult;
import com.tws.plugin.manager.PluginCallback;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import dalvik.system.DexClassLoader;
import qrom.component.log.QRomLog;

/***
 * 宿主中定义的协议规则：
 * 1、位置信息：Y 指定显示的上下区域位置（当前主页分上面的actionbar、中间的fragment、底部的hotseat）；X 指定前后位置
 *    POS_Y_HOTSEAT = 0;
 *    POS_Y_HOME_FRAGEMENT = 1;
 *    POS_Y_ACTION_BAR = 2;     //x —— -1 左边；0中间的标题(默认)；1 右边；2里面的菜单
 */
public class HostHomeActivity extends TwsFragmentActivity implements HomeUIProxy {
    private final String TAG = "rick_Print:HostHomeActivity";

    private Hotseat mHotseat;
    private OnHotseatClickListener mHotseatClickCallback;

    private FragmentPagerAdapter mFragmentPagerAdapter;
    private MyWatchFragmentRevision mMyWatchFragment = null;// 这个是DM固有的，直接保留引用就不开回调进行后续同步的操作处理

    private FrameLayout mFragmentContainer;
    private ArrayList<HostDisplayItem> mHotseatDisplayItems = new ArrayList<HostDisplayItem>();
    private ArrayList<HostDisplayItem> mHomeFragementDisplayItems = new ArrayList<HostDisplayItem>(); // homeFragement

    //对于协议来说，根本不关心Y具体是什么位置，这个需要宿主和插件协商定义好
    private final int POS_Y_HOTSEAT = 0;
    private final int POS_Y_HOME_FRAGEMENT = 1;
    private final int POS_Y_ACTION_BAR = 2;
    //对于协议来说，根本不关心X具体是什么位置，这个需要宿主和插件协商定义好
    private final int POS_X_ACTION_BAR_L = -1;  //通常是默认返回按钮
    private final int POS_X_ACTION_BAR_TITLE = 0;
    private final int POS_X_ACTION_BAR_SUBTITLE = 1;
    private final int POS_X_ACTION_BAR_R = 2;
    private final int POS_X_ACTION_BAR_MENU = 3;

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
    private int mBar_r_type = DisplayItem.TYPE_ACTIVITY; // 当前默认是activity
    private String mBar_r_action_id = null;

    // 插件更新监听
    private final BroadcastReceiver mPluginChangedMonitor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int actionType = intent.getIntExtra(PluginCallback.EXTRA_TYPE, PluginCallback.TYPE_UNKNOW);
            final String packageName = intent.getStringExtra(PluginCallback.EXTRA_ID);
            final int installRlt = intent.getIntExtra(PluginCallback.EXTRA_RESULT_CODE, -1);
            if (TextUtils.isEmpty(packageName)) {// 无效的包名信息
                QRomLog.e(TAG, "Receive Invalid information package name");
                return;
            }

            switch (actionType) {
                case PluginCallback.TYPE_INSTALL:
                    if (installRlt == InstallResult.SUCCESS) {
                        QRomLog.d(TAG, "插件：" + packageName + "安装成功了~");
                        dillPluginDisplayItemItems(PluginManagerHelper.getPluginDescriptorByPluginId(packageName), true);
                    }
                    break;
                case PluginCallback.TYPE_REMOVE:
                    // success ? 0 : 7
                    if (installRlt == 0) {// 卸载成功
                        QRomLog.d(TAG, "插件：" + packageName + "被卸载了哈~");
                        if (mHotseat == null) {
                            QRomLog.d(TAG, "貌似mHotseat还没初始化哦"); // 这种情况应该基本不会出现
                        } else {
                            removePlugin(packageName);
                        }

                        if (mMyWatchFragment == null) {
                            QRomLog.d(TAG, "貌似MyWatchFragment还没构建出来"); // 这种情况有可能出现哦
                        } else {
                            mMyWatchFragment.removePlugin(packageName);
                        }
                    }
                    break;
                case PluginCallback.TYPE_REMOVE_ALL:
                    // success ? 0 : 7
                    if (installRlt == 0) {// 卸载成功
                        QRomLog.d(TAG, "~~~~(>_<)~~~~所有插件都被卸载了咯！");
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

        //通过配置文件来初始化插件的指定位置信息
        initPluginDisplayPosFromConfig(POS_Y_HOTSEAT);
        initPluginDisplayPosFromConfig(POS_Y_HOME_FRAGEMENT);

        //初始化插件的显示信息
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
            QRomLog.e(TAG, "initBottomTabView() mHotseat is null");
            return;
        }
        // 添加my watch fragment
        mHotseat.addOneBottomButton(Hotseat.HOST_HOME_FRAGMENT, null, Hotseat.FRAGMENT_COMPONENT, getResources()
                        .getString(R.string.home_my_watch),
                getResources().getDrawable(R.drawable.home_bottom_tab_my_watch_default),
                getResources().getDrawable(R.drawable.home_bottom_tab_my_watch_pressed), mNormalTextColor,
                mFocusTextColor, 0);

        // 其他的根据插件配置来
        for (HostDisplayItem item : mHotseatDisplayItems) {
            // 当前Hotseat上暂时之放置fragment
            if (item.type != DisplayItem.TYPE_FRAGMENT) {
                continue;
            }

            mHotseat.addOneBottomButtonForPlugin(item, mNormalTextColor, mFocusTextColor, false);
        }
    }

    private void initHomeBottomTabObserver() {
        mHotseatClickCallback = new OnHotseatClickListener() {

            @Override
            public void onItemClick(int index) {
                QRomLog.d(TAG, "onItemClick:" + index);
                switchFragment(index);
            }

            @Override
            public void updateActionBar(ActionBarInfo actionBarInfo) {
                mActionBar.setTitle(actionBarInfo.title);
                QRomLog.d(TAG, "updateActionBar:" + actionBarInfo.toString());
                if (TextUtils.isEmpty(actionBarInfo.r_action_id)) {// 不需要右侧按钮
                    mActionBar.getRightButtonView().setImageResource(R.color.transparent);
                    mActionBar.getRightButtonView().setClickable(false);
                } else if (actionBarInfo.r_res_type == DisplayItem.RES_TYPE_DRAWABLE) {
                    mActionBar.getRightButtonView().setVisibility(View.VISIBLE);
                    mActionBar.getRightButtonView().setClickable(true);

                    final String resName = actionBarInfo.r_res_normal + "_" + actionBarInfo.r_res_focus;
                    Drawable drawable = PluginManagerHelper.getPluginIcon(resName);
                    if (drawable != null) {
                        mActionBar.getRightButtonView().setImageDrawable(drawable);
                    } else {
                        final Drawable normal = PluginManagerHelper.getPluginIcon(actionBarInfo.r_res_normal);
                        if (TextUtils.isEmpty(actionBarInfo.r_res_normal) || normal == null) {
                            mActionBar.getRightButtonView().setImageResource(R.drawable.ic_launcher);
                        } else if (actionBarInfo.r_res_normal.equals(actionBarInfo.r_res_focus)) {
                            mActionBar.getRightButtonView().setImageDrawable(normal);
                        } else {
                            final Drawable focus = PluginManagerHelper.getPluginIcon(actionBarInfo.r_res_normal);
                            if (focus == null) {
                                mActionBar.getRightButtonView().setImageDrawable(normal);
                            } else {
                                StateListDrawable stateListDrawable = new StateListDrawable();
                                stateListDrawable.addState(new int[]{android.R.attr.state_pressed},
                                        PluginManagerHelper.getPluginIcon(actionBarInfo.r_res_focus));
                                stateListDrawable.addState(new int[]{}, normal);

                                PluginManagerHelper.addPluginIcon(resName, stateListDrawable);
                                mActionBar.getRightButtonView().setImageDrawable(stateListDrawable);
                            }
                        }
                    }

                    mBar_r_type = actionBarInfo.r_type;
                    mBar_r_action_id = actionBarInfo.r_action_id;
                } else {
                    mBar_r_type = DisplayItem.TYPE_UNKOWN;
                    mBar_r_action_id = null;
                }
            }

            @Override
            public void onItemClick(int tagIndex, int extras) {
                QRomLog.d(TAG, "onItemClick:" + tagIndex);
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
        switch (mBar_r_type) {
            case DisplayItem.TYPE_ACTIVITY:
                intent.setClassName(this, mBar_r_action_id);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private void removePlugin(String packageName) {
        QRomLog.d(TAG, "removePlugin:" + packageName);
        int removeTagIndex = mHotseat.removePlugin(packageName);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        String name = makeFragmentName(mFragmentContainer.getId(), mFragmentPagerAdapter.getItemId(removeTagIndex));
        Fragment fragment = fragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            QRomLog.d(TAG, "removePlugin removeTagIndex=" + removeTagIndex);
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
            QRomLog.e(TAG, "我的乖乖，怎么会有位置是：" + tagIndex + " 的内容可以切换咧，得check一下是否Hotseat没有内容？");
            return;
        }

        QRomLog.d(TAG, "switchFragment:" + tagIndex);
        Fragment fragment = (Fragment) mFragmentPagerAdapter.instantiateItem(mFragmentContainer, tagIndex);
        mFragmentPagerAdapter.setPrimaryItem(mFragmentContainer, tagIndex, fragment);
        mFragmentPagerAdapter.finishUpdate(mFragmentContainer);
    }

    private void switchFragment(int tagIndex, int extras) {
        if (tagIndex < 0) {
            QRomLog.e(TAG, "我的乖乖，怎么会有位置是：" + tagIndex + " 的内容可以切换咧，得check一下是否Hotseat没有内容？");
            return;
        }

        QRomLog.d(TAG, "switchFragment:" + tagIndex);
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
        mHotseatDisplayItems.clear();
        mHomeFragementDisplayItems.clear();
        // mOtherPosDisplayInfos.clear();

        ArrayList<DisplayItem> gemelDisplayItems = new ArrayList<DisplayItem>();

        Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
        Iterator<PluginDescriptor> itr = plugins.iterator();
        boolean hasGetHotSeatPos = false;
        while (itr.hasNext()) {
            dillPluginDisplayItemItems(itr.next(), false);
        }

        //当前demo只在Hotseat上面的item才会绑定双生性质的item
        dillGemelItem(gemelDisplayItems, mHotseatDisplayItems);
    }

    private void dillPluginDisplayItemItems(final PluginDescriptor pluginDescriptor, boolean isInstall) {
        if (null == pluginDescriptor || TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
            Exception here = new Exception();
            here.fillInStackTrace();
            QRomLog.e(TAG, "My god !!! how can have such a situatio~!", here);
            return;
        }

        if (PluginApplication.getInstance().getEliminatePlugins().contains(pluginDescriptor.getPackageName())) {
            QRomLog.w(TAG, "当前插件" + pluginDescriptor.getPackageName() + "已经被列入黑名单了");
            return;
        }

        boolean establishedDependOn = establishedDependOns(pluginDescriptor.getPackageName(),
                pluginDescriptor.getDependOns());

        ArrayList<DisplayItem> gemelDisplayItems = new ArrayList<DisplayItem>();
        final ArrayList<DisplayItem> dis = pluginDescriptor.getDisplayItems();
        if (dis != null && 0 < dis.size()) {
            boolean hasGetHotSeatPos = false;   //这个标识符是规定一个插件最多只能放一个图标在Hotseat上，仅仅是这个demo的规定
            String iconDir = new File(pluginDescriptor.getInstalledPath()).getParent() + File.separator + FileUtil.ICON_FOLDER;
            for (DisplayItem di : dis) {
                //延后处理双生性质的Item
                if (DisplayItem.INVALID_POS < di.gemel_x && DisplayItem.INVALID_POS < di.gemel_y) {
                    gemelDisplayItems.add(di);
                    continue;
                }

                HostDisplayItem hostDisplayItem = new HostDisplayItem(di, pluginDescriptor.getPackageName());
                hostDisplayItem.establishedDependOn = establishedDependOn;

                loadPluginIcon(hostDisplayItem, pluginDescriptor, di.y == POS_Y_HOTSEAT, iconDir);

                switch (di.y) {
                    case POS_Y_HOTSEAT: // 显示在Hotseat上
                        // 当前Hotseat上暂时之放置fragment
                        if (hostDisplayItem.type != DisplayItem.TYPE_FRAGMENT) {
                            QRomLog.w(TAG, "发现不是fragment内容类型的Item要放到Hotseat上，Error for 不符合当前的规定...");
                            break;
                        }

                        if (!hasGetHotSeatPos) {
                            hasGetHotSeatPos = true;

                            if (isInstall) {
                                mHotseat.addOneBottomButtonForPlugin(hostDisplayItem, mNormalTextColor, mFocusTextColor, true);
                            } else {
                                mHotseatDisplayItems.add(hostDisplayItem);
                            }
                        } else {
                            QRomLog.e(TAG, "哦~——~哦，插件：" + pluginDescriptor.getPackageName()
                                    + " 竟然有两个在Hotseat的Pos位，需要框架做一点点的扩展兼容哈。");
                        }
                        break;
                    case POS_Y_HOME_FRAGEMENT: // 显示在host_home_fragement
                        // mHomeFragementDisplayInfos.add(info);
                        if (isInstall) {
                            mMyWatchFragment.addContentItem(hostDisplayItem);
                        } else {
                            mHomeFragementDisplayItems.add(hostDisplayItem);
                        }
                        break;
                    case POS_Y_ACTION_BAR:// 显示在标题栏
                        //这种当前Demo暂定是双生属性的Item
                        break;
                    default:// 显示在其他位置
                        if (isInstall) {    //接收到安装广播的 需要处理随宿主启动的内容
                            switch (hostDisplayItem.type) {
                                case DisplayItem.TYPE_SERVICE:
                                    Intent intent = new Intent();
                                    intent.setClassName(HostHomeActivity.this, hostDisplayItem.action_id);
                                    startService(intent);
                                    break;
                                case DisplayItem.TYPE_APPLICATION:
                                    if (null == PluginLauncher.instance().startPlugin(hostDisplayItem.action_id)) {
                                        Toast.makeText(HostHomeActivity.this, "startPlugin:" + hostDisplayItem.action_id + "(action_id) 失败!!!",
                                                Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                }
            }

            //当前demo只在Hotseat上面的item才会绑定双生性质的item
            dillGemelItem(gemelDisplayItems, mHotseatDisplayItems);
        } else {
            Toast.makeText(getApplicationContext(), "插件：" + pluginDescriptor.getApplicationName() + "没配置显示协议",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void dillGemelItem(final ArrayList<DisplayItem> gemelDisplayItems, final ArrayList<HostDisplayItem> hostDisplayItems) {
        boolean compared = false;
        for (DisplayItem gemelItem : gemelDisplayItems) {
            compared = false;
            for (HostDisplayItem hostDisplayItem : hostDisplayItems) {
                if (gemelItem.gemel_x == hostDisplayItem.x && gemelItem.gemel_y == hostDisplayItem.y) {
                    compared = true;
                    if (gemelItem.y != POS_Y_ACTION_BAR) {
                        QRomLog.e(TAG, "gemelItem：" + gemelItem.toString() + " Error for：不是放在ACTION_BAR上的");
                        break;
                    }

                    switch (gemelItem.x) {
                        case POS_X_ACTION_BAR_L:
                            break;
                        case POS_X_ACTION_BAR_TITLE:
                            hostDisplayItem.applyActionBarTitleItem(gemelItem);
                            break;
                        case POS_X_ACTION_BAR_SUBTITLE:
                            hostDisplayItem.applyActionBarSubtitleItem(gemelItem);
                            break;
                        case POS_X_ACTION_BAR_R:
                            if (!TextUtils.isEmpty(gemelItem.icon)) {
                                hostDisplayItem.applyActionBarRighButtonItem(gemelItem, DisplayItem.RES_TYPE_DRAWABLE);
                            } else if (!TextUtils.isEmpty(gemelItem.title)) {
                                hostDisplayItem.applyActionBarRighButtonItem(gemelItem, DisplayItem.RES_TYPE_STRING);
                            } else {
                                QRomLog.e(TAG, "gemelItem：" + gemelItem.toString() + " Error for：不明确的BAR_R资源类型");
                            }
                            break;
                        case POS_X_ACTION_BAR_MENU:
                            hostDisplayItem.applyActionBarMenuItem(gemelItem);
                            break;
                        default:
                            QRomLog.e(TAG, "gemelItem：" + gemelItem.toString() + " Error for：不明确的ACTION_BAR位置");
                            break;
                    }
                    break;
                }
            }

            if (!compared) {
                QRomLog.e(TAG, "gemelItem：" + gemelItem.toString() + " Error for：没有配对上HostDisplayItem");
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

        final String[] values = dependOnDes.split(DisplayItem.SEPARATOR_VALUE);
        QRomLog.d(TAG, "establishedDependOn:" + dependOnDes);
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
                    QRomLog.d(TAG, des);
                }

                mDependOnMap.put(packageName, pid);
                QRomLog.d(TAG, "mDependOnMap put:" + packageName + " " + pid);

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

    private void loadPluginIcon(final HostDisplayItem hostDisplayItem, final PluginDescriptor pluginDescriptor, boolean isHotseat,
                                String iconDir) {
        if (TextUtils.isEmpty(hostDisplayItem.normalResName)) {
            QRomLog.e(TAG, "loadPluginIcon:" + hostDisplayItem.normalResName + " failed for Illegal resources name~");
            return;
        }

        String iconPath;
        Bitmap normalIcon = null;
        if (null == PluginManagerHelper.getPluginIcon(hostDisplayItem.normalResName)) {
            iconPath = iconDir + File.separator + hostDisplayItem.normalResName + FileUtil.FIX_ICON_NAME;
            normalIcon = BitmapFactory.decodeFile(iconPath);
            if (normalIcon != null) {
                PluginManagerHelper.addPluginIcon(hostDisplayItem.normalResName, new BitmapDrawable(getResources(), normalIcon));
            }
        }

        if (null == PluginManagerHelper.getPluginIcon(hostDisplayItem.focusResName)
                && !hostDisplayItem.normalResName.equals(hostDisplayItem.focusResName)) {
            iconPath = iconDir + File.separator + hostDisplayItem.focusResName + FileUtil.FIX_ICON_NAME;
            Bitmap focusIcon = BitmapFactory.decodeFile(iconPath);
            if (focusIcon != null) {
                PluginManagerHelper.addPluginIcon(hostDisplayItem.focusResName, new BitmapDrawable(getResources(), focusIcon));
            }
        }

        if (!isHotseat || null == hostDisplayItem.actionBarDisplayItem)
            return;

        if (hostDisplayItem.actionBarDisplayItem.r_res_type == DisplayItem.RES_TYPE_DRAWABLE && !TextUtils.isEmpty(hostDisplayItem.actionBarDisplayItem.r_res_normal)
                && null == PluginManagerHelper.getPluginIcon(hostDisplayItem.actionBarDisplayItem.r_res_normal)) {

            iconPath = iconDir + File.separator + hostDisplayItem.actionBarDisplayItem.r_res_normal + FileUtil.FIX_ICON_NAME;
            Bitmap abr_normalIcon = BitmapFactory.decodeFile(iconPath);
            if (abr_normalIcon != null) {
                PluginManagerHelper.addPluginIcon(hostDisplayItem.actionBarDisplayItem.r_res_normal, new BitmapDrawable(getResources(),
                        abr_normalIcon));
            }

            if (null == PluginManagerHelper.getPluginIcon(hostDisplayItem.actionBarDisplayItem.r_res_focus)
                    && !hostDisplayItem.actionBarDisplayItem.r_res_normal.equals(hostDisplayItem.actionBarDisplayItem.r_res_focus)) {
                iconPath = iconDir + File.separator + hostDisplayItem.actionBarDisplayItem.r_res_focus + FileUtil.FIX_ICON_NAME;
                Bitmap abr_focusIcon = BitmapFactory.decodeFile(iconPath);
                if (abr_focusIcon != null) {
                    PluginManagerHelper.addPluginIcon(hostDisplayItem.actionBarDisplayItem.r_res_focus, new BitmapDrawable(getResources(),
                            abr_focusIcon));
                }
            }
        }
    }

    private Fragment getFragmentByTagIndex(int tagIndex) {
        final ComponentName componentName = mHotseat.getComponentNameByTagIndex(tagIndex);
        final String classId = componentName.getClassId();
        QRomLog.d(TAG, "getFragmentByTagIndex:" + tagIndex + " classId is " + classId + " will create it(Fragment)");

        Fragment fragment = null;
        String msg = "";
        if (TextUtils.isEmpty(classId)) {
            msg = "invalid classId：" + classId + "，请先check这个无效的标识符来源~~~~";
            Toast.makeText(this, "getFragmentByTagIndex:" + tagIndex + " return null classId", Toast.LENGTH_LONG)
                    .show();
        } else if (classId.equals(Hotseat.HOST_HOME_FRAGMENT)) {
            fragment = mMyWatchFragment = new MyWatchFragmentRevision(mHomeFragementDisplayItems);
        } else {
            QRomLog.d(TAG, "getFragmentByPos to get Plugin fragement:" + classId);
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
                            QRomLog.e(TAG, "loadPluginFragmentClassById:" + classId + " ClassNotFound:" + clazzName
                                    + "Exception", e);
                            QRomLog.w(TAG, "没有找到：" + clazzName + " 是不是被混淆了~");
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
                    QRomLog.e(TAG, "InstantiationException", e);
                } catch (IllegalAccessException e) {
                    QRomLog.e(TAG, "IllegalAccessException", e);
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

    public void initPluginDisplayPosFromConfig(int posType) {
        String configFile = "";
        HashMap<String, Integer> pluginPos;
        switch (posType) {
            case POS_Y_HOTSEAT:
                configFile = POS_HOTSEAT_CONFIG_NAME;
                pluginPos = mPluginHotsetPos;
                break;
            case POS_Y_HOME_FRAGEMENT:
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
                case POS_Y_HOTSEAT:
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
                case POS_Y_HOME_FRAGEMENT:
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

    private int getPosByPackageName(int pos_y, String packageName) {
        switch (pos_y) {
            case POS_Y_HOTSEAT:
                if (mPluginHotsetPos.containsKey(packageName)) {
                    return mPluginHotsetPos.get(packageName);
                }
                return -1;
            case POS_Y_HOME_FRAGEMENT:
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
            QRomLog.e(TAG, "switchToFragment:" + classId + " Failed for mHotseat is null");
            return;
        }

        mHotseat.switchToFragment(classId, extras);
    }

    @Override
    public void setHighlightCellItem(String classId, boolean needHighlight) {
        if (mHotseat == null) {
            QRomLog.e(TAG, "setHighlightCellItem:" + classId + " Failed for mHotseat is null");
            return;
        }

        mHotseat.setHighlightCellItem(classId, needHighlight);
    }

    @Override
    public Context getHostFitContext() {
        return this;
    }
}
