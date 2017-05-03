package com.tws.plugin.bridge;

import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.tws.plugin.core.PluginIntentResolver;

final public class TwsPluginBridgeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tView = new TextView(getApplication());
		tView.setPadding(10, 80, 10, 0);
		tView.setTextSize(16);
		setContentView(tView);

		final Intent intent = getIntent();
		if (null == intent) {
			tView.setText("这是一个跳转界面，不应该出现才对哦~！！！");
			return;
		}

		String description = "【该界面是一个跳转界面，用来作为启动插件内部组件的跳板】\n请先确认对应的插件已经安装~\nintent info:";

		String pluginClassName = intent.getStringExtra(PluginIntentResolver.INTENT_EXTRA_BRIDGE_TO_PLUGIN);
		if (!TextUtils.isEmpty(pluginClassName)) {
			description += "\npluginClassName is " + pluginClassName;
		}
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action)) {
			description += "\naction is " + action;
		}

		String type = intent.getType();
		if (!TextUtils.isEmpty(type)) {
			description += "\ntype is " + type;
		}

		String scheme = intent.getScheme();
		if (!TextUtils.isEmpty(scheme)) {
			description += "\nscheme is " + scheme;
		}

		if (intent.getData() != null) {
			String data = intent.getData().toString();
			if (!TextUtils.isEmpty(data)) {
				description += "\ndata is " + data;
			}
		}

		Set<String> categories = intent.getCategories();
		if (null != categories && 0 < categories.size()) {
			description += "\ncategories is :";
			for (String string : categories) {
				description += "\n" + string;
			}
		}

		tView.setText(description);
	}

}
