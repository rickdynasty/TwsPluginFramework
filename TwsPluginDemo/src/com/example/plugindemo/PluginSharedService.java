package com.example.plugindemo;

import tws.component.log.TwsLog;

import com.tencent.tws.sharelib.SharePOJO;
import com.tencent.tws.sharelib.ShareService;

/**
 * @author yongchen
 */
public class PluginSharedService implements ShareService {

	private static final String TAG = "rick_Pring:PluginSharedService";

	@Override
	public SharePOJO doSomething(String condition) {
		TwsLog.d(TAG, condition);
        return new SharePOJO(condition + " : 插件追加的文字");
    }
}
