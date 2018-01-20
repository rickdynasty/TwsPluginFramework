package com.rick.tws.pluginhost.main.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.rick.tws.framework.HomeUIProxy;
import com.rick.tws.framework.HostProxy;
import com.rick.tws.pluginhost.R;
import com.rick.tws.pluginhost.main.HostApplication;
import com.rick.tws.pluginhost.main.content.CellItem;
import com.rick.tws.pluginhost.main.content.HostDisplayItem;
import com.rick.tws.pluginhost.main.ui.fragment.HomeFragment;
import com.rick.tws.pluginhost.main.ui.fragment.ToastFragment;
import com.rick.tws.pluginhost.main.widget.Hotseat;
import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.InstallResult;
import com.tws.plugin.manager.PluginCallback;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import dalvik.system.BaseDexClassLoader;
import qrom.component.log.QRomLog;

public class HomeActivity extends AppCompatActivity implements HomeUIProxy {
    protected final String TAG = "rick_Print:HomeActivity";

    protected static final String FRAGMENTS_TAG = "android:support:fragments";
    //插件依赖的类型
    private final int DEPEND_ON_APPLICATION = 1;
    private final int DEPEND_ON_PLUGIN = 2;
    private HashMap<String, String> mDependOnMap = new HashMap<String, String>();

    private HomeFragment mHomeFragment = null;

    protected Hotseat mHotseat;
    private Hotseat.OnHotseatClickListener mHotseatClickCallback;
    private ArrayList<HostDisplayItem> mHotseatDisplayInfos = new ArrayList<>();
    private ArrayList<HostDisplayItem> mHomeFragementDisplayInfos = new ArrayList<>(); // myWatchfaceFragment

    protected int mNormalTextColor;
    protected int mFocusTextColor;

    // 插件更新监听
    private BroadcastReceiver mPluginChangedMonitor = null;
    // 应用安装卸载监听
    private BroadcastReceiver mAppUpdateReceiver = null;

    private HashMap<String, Fragment> mFragments = new HashMap<>();
    //需要记切换前的tagIndex,方便隐藏之前的然后显示新的
    private String mSaveClassId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mHotseat = (Hotseat) findViewById(R.id.home_bottom_tab);
        initHomeBottomTabObserver();

        mNormalTextColor = getResources().getColor(R.color.home_bottom_tab_text_default_color);
        mFocusTextColor = getResources().getColor(R.color.home_bottom_tab_text_pressed_color);

        // 监听插件更新
        initPluginChangedMonitor();
        // 监听应用的安装卸载
        initAppUpdateMonitor();

        //初始化插件的显示info
        initPluginsDisplayInfo();
        // 初始化底部Hotseat
        initHotseat();
        HostProxy.setHomeUIProxy(this);

        //检查ExternalStorage的权限
        checkWriteExternalStoragePermission();

        // 默认聚焦位置
        final int fouceIndex = mHotseat.getPosByClassId(((HostApplication) HostApplication.getInstance()).getFouceTabClassId());
        switchFragment(mHotseat.setFocusIndex(fouceIndex), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        QRomLog.i(TAG, "=========onResume=========");
    }

    private void initPluginChangedMonitor() {
        if (null == mPluginChangedMonitor) {
            mPluginChangedMonitor = new BroadcastReceiver() {
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
                                QRomLog.i(TAG, "插件：" + packageName + "安装成功了~");
                                installPlugin(packageName);
                            }
                            break;
                        case PluginCallback.TYPE_REMOVE:
                            // success ? 0 : 7
                            if (installRlt == 0) {// 卸载成功
                                QRomLog.i(TAG, "插件：" + packageName + "被卸载了哈~");
                                if (mHotseat == null) {
                                    QRomLog.i(TAG, "貌似mHotseat还没初始化哦"); // 这种情况应该基本不会出现
                                } else {
                                    removePlugin(packageName);
                                }

                                if (mHomeFragment == null) {
                                    QRomLog.i(TAG, "貌似MyWatchFragment还没构建出来"); // 这种情况有可能出现哦
                                } else {
                                    mHomeFragment.removePlugin(packageName);
                                }
                            }
                            break;
                        case PluginCallback.TYPE_REMOVE_ALL:
                            // success ? 0 : 7
                            if (installRlt == 0) {// 卸载成功
                                QRomLog.i(TAG, "~~~~(>_<)~~~~所有插件都被卸载了咯！");
                                // ~暂不处理，因为DM起来首页显示出来后，应该是不存在这个情况
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

            registerReceiver(mPluginChangedMonitor, new IntentFilter(PluginCallback.ACTION_PLUGIN_CHANGED));
        }
    }

    private void initAppUpdateMonitor() {
        if (null == mAppUpdateReceiver) {
            mAppUpdateReceiver = new BroadcastReceiver() {
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

            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addDataScheme("package");
            registerReceiver(mAppUpdateReceiver, filter);
        }
    }

    private void checkWriteExternalStoragePermission() {
        //6.0以上的有这个权限问题
        if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) {
            int REQUEST_EXTERNAL_STORAGE = 1;
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int permission = ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(HomeActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        }
    }

    private CellItem.ComponentName getComponentNameByTabIndex(int tagIndex) {
        return mHotseat.getComponentNameByTagIndex(tagIndex);
    }

    //index是不固定的，唯一固定的是classID,因此需要将tab的index转成对应的classID
    private Fragment getFragment(CellItem.ComponentName componentName) {
        QRomLog.i(TAG, "getFragment:" + componentName);
        final String classId = componentName.getClassId();
        if (TextUtils.isEmpty(classId)) {
            return null;
        }

        if (mFragments.get(componentName.getClassId()) != null) {
            return mFragments.get(componentName.getClassId());
        }

        Fragment fragment = null;
        String msg = "";
        if (classId.equals(Hotseat.HOST_HOME_FRAGMENT)) {
            fragment = mHomeFragment = new HomeFragment(mHomeFragementDisplayInfos);
        } else {
            QRomLog.i(TAG, "getFragmentByPos to get Plugin fragement:" + classId);
            Class<?> cls = null;
            if (!TextUtils.isEmpty(componentName.getPluginPackageName())) {
                PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(componentName
                        .getPluginPackageName());
                if (pluginDescriptor != null) {
                    // 插件可能尚未初始化，确保使用前已经初始化
                    LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

                    BaseDexClassLoader pluginClassLoader = plugin.pluginClassLoader;

                    String clsName = pluginDescriptor.getPluginClassNameById(classId);
                    if (clsName != null) {
                        try {
                            cls = ((ClassLoader) pluginClassLoader).loadClass(clsName);
                        } catch (ClassNotFoundException e) {
                            QRomLog.e(TAG, "loadPluginFragmentClassById:" + classId + " ClassNotFound:" + clsName + "Exception", e);
                            QRomLog.w(TAG, "没有找到：" + clsName + " 是不是被混淆了~");
                        }
                    }
                } else {
                    cls = PluginLoader.loadPluginFragmentClassById(componentName.getClassId());
                }
            } else {
                cls = PluginLoader.loadPluginFragmentClassById(componentName.getClassId());
            }

            if (cls != null) {
                try {
                    fragment = (Fragment) cls.newInstance();
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

        mFragments.put(classId, fragment);

        return fragment;// new Fragment();
    }

    private void installPlugin(String packageName) {
        PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
        if (null == pluginDescriptor || TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
            QRomLog.e(TAG, "My god !!! how can have such a situatio~!");
            Toast.makeText(getApplicationContext(), "怎会存在没packageName的插件咧？？？", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean establishedDependOn = establishedDependOns(pluginDescriptor.getPackageName(),
                pluginDescriptor.getDependOns());

        final ArrayList<DisplayItem> dis = pluginDescriptor.getDisplayItems();
        if (dis != null && 0 < dis.size()) {
            String iconDir = new File(pluginDescriptor.getInstalledPath()).getParent() + File.separator
                    + FileUtil.ICON_FOLDER;
//            for (DisplayItem di : dis) {
//                int pos = getPosByPackageName(di.pos, pluginDescriptor.getPackageName());
//                HostDisplayItem info = new HostDisplayItem(this, di, pluginDescriptor.getPackageName(), pos);
//                info.establishedDependOn = establishedDependOn;
//
//                loadPluginIcon(info, pluginDescriptor, di.pos == DisplayConfig.DISPLAY_AT_HOTSEAT, iconDir);
//
//                switch (di.pos) {
//                    case DisplayConfig.DISPLAY_AT_HOTSEAT: // 显示在Hotseat上
//                        // 当前Hotseat上暂时之放置fragment
//                        if (info.componentType != DisplayConfig.TYPE_FRAGMENT) {
//                            break;
//                        }
//                        mHotseat.addOneBottomButtonForPlugin(info, mNormalTextColor, mFocusTextColor, true);
//                        break;
//                    case DisplayConfig.DISPLAY_AT_HOME_FRAGEMENT: // 显示在my_watch_fragement
//                        // mHomeFragementDisplayInfos.add(info);
//                        mHomeFragment.addItemAndUpdateView(info);
//                        break;
//                    case DisplayConfig.DISPLAY_AT_OTHER_POS:// 显示在其他位置
//                        switch (info.componentType) {
//                            case DisplayItem.TYPE_SERVICE:
//                                Intent intent = new Intent();
//                                intent.setClassName(HomeActivity.this, info.classId);
//                                startService(intent);
//                                break;
//                            case DisplayItem.TYPE_APPLICATION:
//                                if (null == PluginLauncher.instance().startPlugin(info.classId)) {
//                                    Toast.makeText(HomeActivity.this, "startPlugin:" + info.classId + "失败!!!",
//                                            Toast.LENGTH_LONG).show();
//                                }
//                                break;
//                            default:
//                                break;
//                        }
//                        break;
//                    case DisplayItem.DISPLAY_AT_MENU: // 这个当前没有，暂不处理
//                    default:
//                        break;
//                }
//            }
        } else {
            Toast.makeText(getApplicationContext(), "插件：" + pluginDescriptor.getApplicationName() + "没配置显示协议",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void removePlugin(String packageName) {
        QRomLog.i(TAG, "removePlugin:" + packageName);
        int removeTagIndex = mHotseat.removePlugin(packageName);
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        String name = makeFragmentName(mFragmentContainer.getId(), mFragmentPagerAdapter.getItemId(removeTagIndex));
//        Fragment fragment = fragmentManager.findFragmentByTag(name);
//        if (fragment != null) {
//            QRomLog.i(TAG, "removePlugin removeTagIndex=" + removeTagIndex);
//            transaction.remove(fragment);
//            // transaction.commit();
//
//            // rick_Note:这里被删除的fragment需要做清理操作哈~
//        }
    }

    private void establishedDependOnForPlugin(String pid) {
        mHotseat.establishedDependOnForPlugin(pid);
        mHomeFragment.establishedDependOnForPlugin(pid);
    }

    private void unEstablishedDependOnForPlugin(String pid) {
        mHotseat.unEstablishedDependOnForPlugin(pid);
        mHomeFragment.unEstablishedDependOnForPlugin(pid);
    }

    /**
     * 收集内容： String classId int componentType CharSequence text int
     * bkNormalResId, int bkFocusResId
     */
    private void initPluginsDisplayInfo() {
        // 底部Hotseat 以及 首页homeFragment的内容是有顺序的
        mHotseatDisplayInfos.clear();
        mHomeFragementDisplayInfos.clear();
        // mOtherPosDisplayInfos.clear();

        Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
        Iterator<PluginDescriptor> itr = plugins.iterator();
        boolean hasGetHotSeatPos = false;
        while (itr.hasNext()) {
            final PluginDescriptor pluginDescriptor = itr.next();
            // 没有PackageName的插件得过滤掉
            if (TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
                QRomLog.e(TAG, "My god !!! how can have such a situatio~!");
                continue;
            }

            // 依赖检测
            boolean establishedDependOn = establishedDependOns(pluginDescriptor.getPackageName(), pluginDescriptor.getDependOns());

            final ArrayList<DisplayItem> dis = pluginDescriptor.getDisplayItems();
            if (dis != null && 0 < dis.size()) {
                hasGetHotSeatPos = false;
                String iconDir = new File(pluginDescriptor.getInstalledPath()).getParent() + File.separator + FileUtil.ICON_FOLDER;
                ArrayList<DisplayItem> gemelItems = null;
                for (DisplayItem di : dis) {
                    if (DisplayItem.INVALID_POS == di.gemel_x && DisplayItem.INVALID_POS == di.gemel_y) {
                        //需要对插件模块通过协议配置的显示项进行一次简单的处理，这种处理是结合项目的需求更方便灵活的操作
                        HostDisplayItem info = new HostDisplayItem(di, pluginDescriptor.getPackageName(), establishedDependOn);
                        loadPluginIcon(info, pluginDescriptor, di.y == HostDisplayItem.DISPLAY_AT_HOTSEAT, iconDir);

                        switch (di.y) {
                            case HostDisplayItem.DISPLAY_AT_HOTSEAT: // 显示在Hotseat上
                                if (!hasGetHotSeatPos) {
                                    mHotseatDisplayInfos.add(info);
                                    hasGetHotSeatPos = true;
                                } else {
                                    QRomLog.e(TAG, "哦~——~哦，插件：" + pluginDescriptor.getPackageName()
                                            + " 竟然有两个在Hotseat的Pos位，需要框架做一点点的扩展兼容哈。");
                                }
                                break;
                            case HostDisplayItem.DISPLAY_AT_HOME_FRAGEMENT: // 显示在my_watch_fragement
                                mHomeFragementDisplayInfos.add(info);
                                break;
                            case HostDisplayItem.DISPLAY_AT_OTHER_POS:// 显示在其他位置
                                // mOtherPosDisplayInfos.add(info);
                                // 这个时机已经调整到application的onCreate了
                                break;
                            case HostDisplayItem.DISPLAY_AT_MENU: // 这个当前没有，暂不处理
                            default:
                                break;
                        }
                    } else {
                        //gemel Item项不需要独立构建一个DisplayInfo
                        if (null == gemelItems) {
                            gemelItems = new ArrayList<>();
                        }
                        gemelItems.add(di);
                    }
                }

                //处理gemel的Item项
                if (null != gemelItems) {

                }
            } else {
                QRomLog.e(TAG, "插件：" + pluginDescriptor.getPackageName() + "没有配置plugin-display协议~");
            }
        }
    }

    private void initHotseat() {
        if (mHotseat == null) {
            mHotseat = (Hotseat) findViewById(R.id.home_bottom_tab);
        }

        if (mHotseat == null) {
            QRomLog.e(TAG, "initBottomTabView() mHotseat is null");
            return;
        }

        // 添加home fragment
        mHotseat.addOneBottomButton(Hotseat.HOST_HOME_FRAGMENT, null, Hotseat.FRAGMENT_COMPONENT, getResources().getString(R.string.hotseat_home_title),
                getResources().getDrawable(R.mipmap.hotseat_icon_home_default),
                getResources().getDrawable(R.mipmap.hotseat_icon_home_pressed), mNormalTextColor,
                mFocusTextColor, 0);


        // 其他的根据插件配置来
        for (HostDisplayItem item : mHotseatDisplayInfos) {
            // 当前Hotseat上暂时之放置fragment
            if (item.action_type != HostDisplayItem.TYPE_FRAGMENT) {
                continue;
            }

            mHotseat.addOneBottomButtonForPlugin(item, mNormalTextColor, mFocusTextColor, false);
        }
    }

    private void initHomeBottomTabObserver() {
        mHotseatClickCallback = new Hotseat.OnHotseatClickListener() {

            @Override
            public void onItemClick(int index) {
                QRomLog.i(TAG, "onItemClick:" + index);
                switchFragment(index);
            }

            @Override
            public void updateActionBar(CellItem.ActionBarInfo actionBarInfo) {
                // Title
//                if (actionBarInfo.ab_titlerestype == 1) {
//                    mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//                    View view = mActionBar.getCustomView();
//                    ImageView imageView;
//                    if (view != null && view instanceof ImageView) {
//                        imageView = (ImageView) view;
//                    } else {
//                        imageView = (ImageView) getLayoutInflater().inflate(R.layout.action_bar_home, null);
//                        mActionBar.setCustomView(imageView);
//                    }
//                    QRomLog.i(TAG, "image=" + imageView + " ab_title=" + actionBarInfo.ab_title);
//                    Drawable drawable = PluginManagerHelper.getPluginIcon(actionBarInfo.ab_title);
//                    if (drawable == null) {
//                        int resId = getResources().getIdentifier(actionBarInfo.ab_title, "drawable", getPackageName());
//                        if (resId > 0) {
//                            drawable = getResources().getDrawable(resId);
//                        } else {
//                            String module = DeviceModelHelper.getInstance().getDeviceModel(GoerHomeActivity.this);
//                            if (DeviceModelHelper.DEVICE_MODEL_HENGSHAN.equals(module)) {
//                                drawable = getResources().getDrawable(R.drawable.ic_home_title_hengshan_black);
//                            }
//                            else {
//                                drawable = getResources().getDrawable(R.drawable.ic_home_title_hype_black);
//                            }
//                        }
//                    }
//                    imageView.setImageDrawable(drawable);
//                } else {
//                    mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
//                    mActionBar.setTitle(actionBarInfo.ab_title);
//                }
//                QRomLog.i(TAG, "updateActionBar:" + actionBarInfo.toString());
//
//                // 左侧按钮
//                if (actionBarInfo.ab_lbtnrestype == DisplayConfig.ACTIONBAR_BTN_TYPE_ICON) {
//                    mActionBar.getActionBarHome().setVisibility(View.VISIBLE);
//                    mActionBar.getActionBarHome().setClickable(true);
//
//                    final String resName = actionBarInfo.ab_lbtnres_normal + "_" + actionBarInfo.ab_lbtnres_focus;
//                    dillActionBarIcon(actionBarInfo, mActionBar.getActionBarHome(), false);
//
//                    mBar_lBtnActionContent = actionBarInfo.ab_rbtncontent;
//                    mBar_lBtnActionType = actionBarInfo.ab_rbtnctype;
//                    if (actionBarInfo.ab_lbtnctype != DisplayConfig.TYPE_PLUGIN_CUSTOM_CLICK) {
//                        mActionBar.getActionBarHome().setOnClickListener(mLeftButtonClick);
//                    }
//                } else if (TextUtils.isEmpty(actionBarInfo.ab_lbtncontent)) {// 不需要左侧按钮
//                    mActionBar.getActionBarHome().setVisibility(View.INVISIBLE);
//                    mActionBar.getActionBarHome().setClickable(false);
//                }
//
//                // 右侧按钮
//                if (actionBarInfo.ab_rbtnrestype == DisplayConfig.ACTIONBAR_BTN_TYPE_ICON) {
//                    mActionBar.getRightButtonView().setVisibility(View.VISIBLE);
//                    mActionBar.getRightButtonView().setClickable(true);
//
//                    dillActionBarIcon(actionBarInfo, mActionBar.getRightButtonView(), true);
//
//                    mBar_rBtnActionContent = actionBarInfo.ab_rbtncontent;
//                    mBar_rBtnActionType = actionBarInfo.ab_rbtnctype;
//                    if (actionBarInfo.ab_rbtnctype != DisplayConfig.TYPE_PLUGIN_CUSTOM_CLICK) {
//                        mActionBar.getRightButtonView().setOnClickListener(mRightButtonClick);
//                    }
//                } else if (TextUtils.isEmpty(actionBarInfo.ab_rbtncontent)) {// 不需要右侧按钮
//                    mActionBar.getRightButtonView().setImageResource(R.color.transparent);
//                    mActionBar.getRightButtonView().setClickable(false);
//                }
            }
        };

        mHotseat.addHotseatClickObserver(mHotseatClickCallback);
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
        QRomLog.i(TAG, "establishedDependOn:" + dependOnDes);
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
                    QRomLog.i(TAG, des);
                }

                mDependOnMap.put(packageName, pid);
                QRomLog.i(TAG, "mDependOnMap put:" + packageName + " " + pid);

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
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }

        if (packageInfo == null) {
            return false;
        } else {
            return true;
        }
    }

    private void loadPluginIcon(final HostDisplayItem item, final PluginDescriptor pluginDescriptor, boolean isHotseat, String iconDir) {
        if (TextUtils.isEmpty(item.normalResName)) {
            QRomLog.e(TAG, "loadPluginIcon:" + item.normalResName + " failed for Illegal resources name~");
            return;
        }

        String iconPath;
        Bitmap normalIcon = null;
        if (null == PluginManagerHelper.getPluginIcon(item.normalResName)) {
            iconPath = iconDir + File.separator + item.normalResName + FileUtil.FIX_ICON_NAME;
            normalIcon = BitmapFactory.decodeFile(iconPath);
            if (normalIcon != null) {
                PluginManagerHelper.addPluginIcon(item.normalResName, new BitmapDrawable(getResources(), normalIcon));
            }
        }

        if (null == PluginManagerHelper.getPluginIcon(item.focusResName)
                && !item.normalResName.equals(item.focusResName)) {
            iconPath = iconDir + File.separator + item.focusResName + FileUtil.FIX_ICON_NAME;
            Bitmap focusIcon = BitmapFactory.decodeFile(iconPath);
            if (focusIcon != null) {
                PluginManagerHelper.addPluginIcon(item.focusResName, new BitmapDrawable(getResources(), focusIcon));
            }
        }

        if (!isHotseat)
            return;

//        if (hostDI.ab_titlerestype == 1 && !TextUtils.isEmpty(hostDI.ab_title)
//                && null == PluginManagerHelper.getPluginIcon(hostDI.ab_title)) {
//            iconPath = iconDir + File.separator + info.ab_title + FileUtil.FIX_ICON_NAME;
//            Bitmap icon = BitmapFactory.decodeFile(iconPath);
//            if (icon != null) {
//                PluginManagerHelper.addPluginIcon(hostDI.ab_title, new BitmapDrawable(getResources(), icon));
//            }
//        }
//
//        if (info.ab_rbtnrestype == DisplayItem.RES_TYPE_DRAWABLE && !TextUtils.isEmpty(info.ab_rbtnres_normal)
//                && null == PluginManagerHelper.getPluginIcon(info.ab_rbtnres_normal)) {
//
//            iconPath = iconDir + File.separator + info.ab_rbtnres_normal + FileUtil.FIX_ICON_NAME;
//            Bitmap abr_normalIcon = BitmapFactory.decodeFile(iconPath);
//            if (abr_normalIcon != null) {
//                PluginManagerHelper.addPluginIcon(info.ab_rbtnres_normal, new BitmapDrawable(getResources(),
//                        abr_normalIcon));
//            }
//
//            if (null == PluginManagerHelper.getPluginIcon(info.ab_rbtnres_focus)
//                    && !info.ab_rbtnres_normal.equals(info.ab_rbtnres_focus)) {
//                iconPath = iconDir + File.separator + info.ab_rbtnres_focus + FileUtil.FIX_ICON_NAME;
//                Bitmap abr_focusIcon = BitmapFactory.decodeFile(iconPath);
//                if (abr_focusIcon != null) {
//                    PluginManagerHelper.addPluginIcon(info.ab_rbtnres_focus, new BitmapDrawable(getResources(),
//                            abr_focusIcon));
//                }
//            }
//        }
//
//        if (info.ab_lbtnrestype == DisplayItem.RES_TYPE_DRAWABLE && !TextUtils.isEmpty(info.ab_lbtnres_normal)
//                && null == PluginManagerHelper.getPluginIcon(info.ab_lbtnres_normal)) {
//
//            iconPath = iconDir + File.separator + info.ab_lbtnres_normal + FileUtil.FIX_ICON_NAME;
//            Bitmap abr_normalIcon = BitmapFactory.decodeFile(iconPath);
//            if (abr_normalIcon != null) {
//                PluginManagerHelper.addPluginIcon(info.ab_lbtnres_normal, new BitmapDrawable(getResources(), abr_normalIcon));
//            }
//
//            if (null == PluginManagerHelper.getPluginIcon(info.ab_lbtnres_focus)
//                    && !info.ab_lbtnres_normal.equals(info.ab_lbtnres_focus)) {
//                iconPath = iconDir + File.separator + info.ab_lbtnres_focus + FileUtil.FIX_ICON_NAME;
//                Bitmap abr_focusIcon = BitmapFactory.decodeFile(iconPath);
//                if (abr_focusIcon != null) {
//                    PluginManagerHelper.addPluginIcon(info.ab_lbtnres_focus, new BitmapDrawable(getResources(), abr_focusIcon));
//                }
//            }
//        }
//
//        if (!TextUtils.isEmpty(info.giconres_normal) && PluginManagerHelper.getPluginIcon(info.giconres_normal) == null) {
//            iconPath = iconDir + File.separator + info.giconres_normal + FileUtil.FIX_ICON_NAME;
//            Bitmap giconres_normal = BitmapFactory.decodeFile(iconPath);
//            if (giconres_normal != null) {
//                PluginManagerHelper.addPluginIcon(info.giconres_normal, new BitmapDrawable(getResources(), giconres_normal));
//            }
//        }
//        if (!TextUtils.isEmpty(info.giconres_focus) && PluginManagerHelper.getPluginIcon(info.giconres_focus) == null) {
//            iconPath = iconDir + File.separator + info.giconres_focus + FileUtil.FIX_ICON_NAME;
//            Bitmap giconres_normal = BitmapFactory.decodeFile(iconPath);
//            if (giconres_normal != null) {
//                PluginManagerHelper.addPluginIcon(info.giconres_focus, new BitmapDrawable(getResources(), giconres_normal));
//            }
//        }
    }

    // 这个函数同步FragmentPagerAdapter，建议直接用FragmentPagerAdapter里面的接口
    private String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    private void switchFragment(int tagIndex) {
        QRomLog.i(TAG, "switchFragment:" + tagIndex);
        switchFragment(tagIndex, true);
    }

    private void switchFragment(int tagIndex, boolean anim) {
        if (tagIndex < 0) {
            QRomLog.e(TAG, "我的乖乖，怎么会有位置是：" + tagIndex + " 的内容可以切换咧，得check一下是否Hotseat没有内容？");
            return;
        }

        final CellItem.ComponentName componentName = getComponentNameByTabIndex(tagIndex);
        final String classId = componentName.getClassId();
        if (mSaveClassId.endsWith(classId)) {
            QRomLog.e(TAG, "同一个fragment不需要切换，直接return ~");
            return;
        }

        QRomLog.i(TAG, "switchFragment:" + tagIndex);
        Fragment fragment = getFragment(componentName);
        if (null == fragment) {
            Toast.makeText(this, "switchFragment:" + tagIndex + " getFragment return null classId", Toast.LENGTH_LONG).show();
            return;
        }

        //注意这里不能缓存FragmentTransaction 需要每次都去get
        FragmentTransaction ft = anim ? getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right) :
                getSupportFragmentManager().beginTransaction();
        if (null != mFragments.get(mSaveClassId)) {
            ft.hide(mFragments.get(mSaveClassId));
        }

        if (!fragment.isAdded()) {
            ft.add(R.id.home_fragment_container, fragment);
        }

        ft.show(fragment).commit();
        mSaveClassId = classId;
    }

    @Override
    public void switchToFragment(String classId, int extras) {

    }

    @Override
    public void setHighlightCellItem(String classId, boolean needHighlight) {

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 当前不对mFragments进行状态保存[恢复的时候状态错乱了]，在这里通过mCurrentPage来保存并进行恢复
        if (outState != null) {
            outState.remove(FRAGMENTS_TAG);
        }
    }
}
