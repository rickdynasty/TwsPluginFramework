package com.tencent.tws.pluginhost.plugindebug;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import tws.component.log.TwsLog;
import android.app.TwsActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.tencent.tws.pluginhost.R;
import com.tencent.tws.pluginhost.plugindebug.widget.StatusButton;
import com.tencent.tws.sharelib.SharePOJO;
import com.tws.plugin.bridge.TwsPluginBridgeActivity;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.InstallResult;
import com.tws.plugin.manager.PluginCallback;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.ResourceUtil;

public class DebugPluginActivity extends TwsActivity {

	private static final String TAG = "rick_Print:MainActivity";
	private ViewGroup mList;
	private ViewGroup mBuiltinPlugList;
	private ViewGroup mSdcardPluginList;
	boolean isInstalled = false;
	private HashMap<String, String> mBuildinMap = new HashMap<String, String>();
	private String mInnerSDCardPath = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug_plugin);

		setTitle("Host-插件调试界面");

		// SD卡存放路径
		mInnerSDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugins";
		File sdPlgusFile = new File(mInnerSDCardPath);
		if (!sdPlgusFile.isDirectory() || !sdPlgusFile.exists()) {
			sdPlgusFile.mkdirs();
		}
		initView();

		// 监听插件安装 安装新插件后刷新当前页面
		registerReceiver(pluginInstallEvent, new IntentFilter(PluginCallback.ACTION_PLUGIN_CHANGED));
	}

	private static final String ASSETS_PLUGS_DIR = "plugins";

	private void initView() {
		mList = (ViewGroup) findViewById(R.id.list);
		mBuiltinPlugList = (ViewGroup) findViewById(R.id.builtin_plug_list);
		mSdcardPluginList = (ViewGroup) findViewById(R.id.sdcard_plugin_list);

		showInstalledAll();
		showBuildinPluginList();
		showSdcardPluginList();
	}

	private String mTmpFileName = "";

	private void showSdcardPluginList() {
		mSdcardPluginList.removeAllViews();
		File file = new File(mInnerSDCardPath);
		File[] subFile = file.listFiles();
		if (subFile == null || subFile.length < 1) {
			Toast.makeText(this, "Inner SDCard Plugins Path empty~!", Toast.LENGTH_SHORT).show();
			findViewById(R.id.sdcard_plugin_text).setVisibility(View.GONE);
			return;
		} else {
			findViewById(R.id.sdcard_plugin_text).setVisibility(View.VISIBLE);
		}

		for (File plugin : subFile) {
			mTmpFileName = plugin.getName();
			if (!mTmpFileName.endsWith(".apk"))
				continue;

			final StatusButton button = new StatusButton(this);
			button.setPadding(15, 3, 3, 3);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			mSdcardPluginList.addView(button, layoutParam);

			String fileNameWithoutFix = getApkFileName(mTmpFileName);
			int iStatus = StatusButton.UNINSTALL_PLUGIN;
			if (mBuildinMap.containsKey(fileNameWithoutFix)) {
				iStatus = StatusButton.INSTALLED_PLUGIN;
			}

			button.setStatus(iStatus);
			// pluginFile位插件的文件全名，便于点击获取插件的文件名
			button.setPluginLabel(mTmpFileName);

			// 登录和配对的属于DM依赖插件暂时不让卸载
			if (mTmpFileName.startsWith("TwsPluginLogin") || mTmpFileName.startsWith("TwsPluginPair")) {
				if (iStatus == StatusButton.INSTALLED_PLUGIN) {
					button.setEnabled(false);
				}
			}

			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (view instanceof StatusButton) {
						final StatusButton buttonEx = (StatusButton) view;
						if (buttonEx.getStatus() == StatusButton.INSTALLED_PLUGIN) {
							String pluginLabel = (String) buttonEx.getPluginLabel();
							String pluginId = mBuildinMap.get(getApkFileName(pluginLabel));

							if (!TextUtils.isEmpty(pluginId)) {
								PluginManagerHelper.remove(pluginId);
								buttonEx.setStatus(StatusButton.UNINSTALL_PLUGIN);
							}
						} else {
							String apkName = (String) buttonEx.getPluginLabel();
							PluginManagerHelper.installPlugin(mInnerSDCardPath + "//" + apkName);
							buttonEx.setStatus(StatusButton.INSTALLED_PLUGIN);
						}
					}
				}
			});
		}
	}

	private void showBuildinPluginList() {
		mBuiltinPlugList.removeAllViews();
		String[] mBuildInPlugins = null;
		try {
			mBuildInPlugins = getAssets().list(ASSETS_PLUGS_DIR);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String pluginFile : mBuildInPlugins) {
			if (!pluginFile.endsWith(".apk"))
				continue;

			final StatusButton button = new StatusButton(this);
			button.setPadding(15, 3, 3, 3);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			mBuiltinPlugList.addView(button, layoutParam);

			String fileNameWithoutFix = getApkFileName(pluginFile);
			int iStatus = StatusButton.UNINSTALL_PLUGIN;
			if (mBuildinMap.containsKey(fileNameWithoutFix)) {
				iStatus = StatusButton.INSTALLED_PLUGIN;
			}

			button.setStatus(iStatus);
			// pluginFile位插件的文件全名，便于点击获取插件的文件名
			button.setPluginLabel(pluginFile);
			// 登录和配对的属于DM依赖插件暂时不让卸载
			if (pluginFile.startsWith("TwsPluginLogin") || pluginFile.startsWith("TwsPluginPair")) {
				if (iStatus == StatusButton.INSTALLED_PLUGIN) {
					button.setEnabled(false);
				}
			}

			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					TwsLog.d(TAG, "onClick:" + view);
					if (view instanceof StatusButton) {
						final StatusButton buttonEx = (StatusButton) view;
						if (buttonEx.getStatus() == StatusButton.INSTALLED_PLUGIN) {
							String pluginLabel = (String) buttonEx.getPluginLabel();
							String pluginId = mBuildinMap.get(getApkFileName(pluginLabel));

							if (!TextUtils.isEmpty(pluginId)) {
								PluginManagerHelper.remove(pluginId);
								buttonEx.setStatus(StatusButton.UNINSTALL_PLUGIN);
							}
						} else {
							PluginLoader.copyAndInstall(ASSETS_PLUGS_DIR + "/" + (String) buttonEx.getPluginLabel());
							buttonEx.setStatus(StatusButton.INSTALLED_PLUGIN);
						}
					}
				}
			});

		}
	}

	private String getApkFileName(String apkFile) {
		if (!apkFile.endsWith(".apk"))
			return null;

		return apkFile.substring(0, apkFile.length() - 4);
	}

	private void showInstalledAll() {
		mBuildinMap.clear();
		ViewGroup root = mList;
		root.removeAllViews();
		// 列出所有已经安装的插件
		Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
		Iterator<PluginDescriptor> itr = plugins.iterator();
		while (itr.hasNext()) {
			final PluginDescriptor pluginDescriptor = itr.next();
			Button button = new Button(this);
			button.setPadding(9, 0, 9, 0);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			root.addView(button, layoutParam);

			TwsLog.d(TAG, "插件id：" + pluginDescriptor.getPackageName());
			String pluginLabel = ResourceUtil.getLabel(pluginDescriptor);
			mBuildinMap.put(pluginLabel, pluginDescriptor.getPackageName());
			button.setText("打开插件：" + pluginLabel + ", V" + pluginDescriptor.getVersion());
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
							pluginDescriptor.getPackageName());
					if (launchIntent == null) {
						Toast.makeText(DebugPluginActivity.this,
								"插件" + pluginDescriptor.getPackageName() + "没有配置Launcher", Toast.LENGTH_SHORT).show();
						// 没有找到Launcher，打开插件详情
						Intent intent = new Intent(DebugPluginActivity.this, PluginDetailActivity.class);
						intent.putExtra("plugin_id", pluginDescriptor.getPackageName());
						startActivity(intent);
					} else {
						// 打开插件的Launcher界面
						if (!pluginDescriptor.isStandalone()) {
							// 测试向非独立插件传宿主中定义的VO对象
							launchIntent.putExtra("paramVO", new SharePOJO("宿主传过来的测试VO"));
						}
						startActivity(launchIntent);
					}
				}
			});
		}

		Button button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("手动打开BridgeActivity");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(DebugPluginActivity.this, TwsPluginBridgeActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(pluginInstallEvent);

		if (scn != null) {
			unbindService(scn);
			scn = null;
		}
	};

	private ServiceConnection scn;

	private final BroadcastReceiver pluginInstallEvent = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int actionType = intent.getIntExtra(PluginCallback.EXTRA_TYPE, PluginCallback.TYPE_UNKNOW);
			int code = intent.getIntExtra(PluginCallback.EXTRA_RESULT_CODE, -1);
			String packageName = intent.getStringExtra(PluginCallback.EXTRA_ID);
			String des = getActionDes(actionType)
					+ (actionType == PluginCallback.TYPE_REMOVE_ALL ? "" : (" 插件:" + packageName)) + " "
					+ getErrMsg(code);
			Toast.makeText(DebugPluginActivity.this, des, Toast.LENGTH_SHORT).show();
			TwsLog.d(TAG, "");

			showInstalledAll();
			showBuildinPluginList();
			showSdcardPluginList();
		};
	};

	private static String getActionDes(int actionType) {
		switch (actionType) {
		case PluginCallback.TYPE_INSTALL:
			return "安装";
		case PluginCallback.TYPE_REMOVE:
			return "卸载";
		case PluginCallback.TYPE_REMOVE_ALL:
			return "卸载所有";
		case PluginCallback.TYPE_START:
			return "启动";
		case PluginCallback.TYPE_STOP:
			return "停止";
		default:
			return "未知操作";
		}
	}

	private static String getErrMsg(int code) {
		String msg = "";
		switch (code) {
		case InstallResult.SUCCESS:
			msg = "成功";
			break;
		case InstallResult.SRC_FILE_NOT_FOUND:
			msg = "失败: 安装文件未找到";
			break;
		case InstallResult.COPY_FILE_FAIL:
			msg = "失败: 复制安装文件到安装目录失败";
			break;
		case InstallResult.SIGNATURES_INVALIDATE:
			msg = "失败: 安装文件验证失败";
			break;
		case InstallResult.VERIFY_SIGNATURES_FAIL:
			msg = "失败: 插件和宿主签名串不匹配";
			break;
		case InstallResult.PARSE_MANIFEST_FAIL:
			msg = "失败: 插件Manifest文件解析出错";
			break;
		case InstallResult.FAIL_BECAUSE_HAS_LOADED:
			msg = "失败: 同版本插件已加载,无需安装";
			break;
		case InstallResult.MIN_API_NOT_SUPPORTED:
			msg = "失败: 当前系统版本过低,不支持此插件";
			break;
		default:
			msg = "失败: 其他 code=" + code;
		}

		return msg;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// 打印一下目录结构
		FileUtil.printAll(new File(getApplicationInfo().dataDir));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyDown keyCode=" + keyCode);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyUp keyCode=" + keyCode);
		return super.onKeyUp(keyCode, event);
	}
}
