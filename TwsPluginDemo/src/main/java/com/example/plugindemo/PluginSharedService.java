package com.example.plugindemo;

import qrom.component.log.QRomLog;

import com.tencent.tws.sharelib.SharePOJO;
import com.tencent.tws.sharelib.ShareService;

/**
 * @author yongchen
 */
public class PluginSharedService implements ShareService {

	private static final String TAG = "rick_Print:PluginSharedService";

	@Override
	public SharePOJO doSomething(String condition) {
		QRomLog.d(TAG, condition);
		return new SharePOJO(condition + " : 插件追加的文字");
	}
}
