package com.example.plugindemo.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.rick.tws.framework.HostProxy;

@SuppressWarnings("ALL")
public class PluginWebViewActivity extends Activity implements OnClickListener {
	WebView web;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_webview);

		Button bn = (Button) findViewById(R.id.load);
		bn.setOnClickListener(this);

		web = (WebView) findViewById(R.id.webview);
		setUpWebViewSetting();
		setClient();

		// ILoginService login = (ILoginService)
		// getSystemService("login_service");
		// if (login != null) {
		// LoginVO vo = login.login("admin", "123456");
		// Toast.makeText(this, vo.getUsername() + ":" + vo.getPassword(),
		// Toast.LENGTH_SHORT).show();
		// } else {
		// Toast.makeText(this, "ILoginService == null",
		// Toast.LENGTH_SHORT).show();
		// }

		try {
			String currentPackageName = getPackageManager().getActivityInfo(
					new ComponentName(this.getPackageName(), this.getClass().getName()), 0).packageName;
			Toast.makeText(this, "测试PackageManager查询插件信息" + currentPackageName, Toast.LENGTH_SHORT).show();
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		// 插件在加载本地页面之前需要将WebView里面进程唯一的上下文改成自己的，否则是加载不了本地页面的
		{
			HostProxy.switchWebViewContext(this);
		}
		web.loadUrl("file:///android_asset/local_web_test.html");
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.load) {
			web.loadUrl("http://www.baidu.com/");
		}
	}

	private void setUpWebViewSetting() {
		WebSettings webSettings = web.getSettings();

		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);// 根据cache-control决定是否从网络上取数据
		webSettings.setSupportZoom(true);
		webSettings.setBuiltInZoomControls(true);// 显示放大缩小
		webSettings.setJavaScriptEnabled(true);
		// webSettings.setPluginsEnabled(true);
		webSettings.setPluginState(PluginState.ON);
		webSettings.setUserAgentString(webSettings.getUserAgentString());
		webSettings.setDomStorageEnabled(true);
		webSettings.setAppCacheEnabled(true);
		webSettings.setAppCachePath(getCacheDir().getPath());
		webSettings.setUseWideViewPort(true);// 影响默认满屏和双击缩放
		webSettings.setLoadWithOverviewMode(true);// 影响默认满屏和手势缩放

	}

	private void setClient() {
		web.setWebChromeClient(new WebChromeClient() {
		});

		// 如果要自动唤起自定义的scheme，不能设置WebViewClient，
		// 否则，需要在shouldOverrideUrlLoading中自行处理自定义scheme
		// webView.setWebViewClient();
		web.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}

		});
	}
}
