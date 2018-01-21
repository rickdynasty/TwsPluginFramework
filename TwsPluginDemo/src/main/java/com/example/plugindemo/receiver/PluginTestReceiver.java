package com.example.plugindemo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.example.plugindemo.vo.ParamVO;

/**
 * 静态注册的插件receiver不能监听系统广播
 *
 * @author yongchen
 *
 */
public class PluginTestReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i("PluginTestReceiver", ((ParamVO) intent.getSerializableExtra("paramvo")) + ", action:" + intent.getAction());

		Toast.makeText(context, "PluginTestReceiver onReceive1 " + context.getResources().getText(R.string.hello_world4),
				Toast.LENGTH_SHORT).show();
	}

}
