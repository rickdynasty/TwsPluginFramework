package com.example.plugindemo;

import com.rick.tws.share.SharePOJO;
import com.rick.tws.share.ShareService;

import qrom.component.log.QRomLog;

/**
 * @author yongchen
 */
public class PluginSharedService implements ShareService {

    private static final String TAG = "rick_Print:PluginSharedService";

    @Override
    public SharePOJO doSomething(String condition) {
        QRomLog.i(TAG, condition);
        return new SharePOJO(condition + " : 插件追加的文字");
    }
}
