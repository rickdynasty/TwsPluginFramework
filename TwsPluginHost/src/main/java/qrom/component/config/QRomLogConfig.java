package qrom.component.config;


import com.rick.tws.framework.HostProxy;

import qrom.component.log.QRomLogBaseConfig;

/**
 * Title: QRomLogConfig
 * Package: qrom.component.log
 * Author: interzhang
 * Date: 14-3-18 下午3:57
 * Version: v1.0
 */
public class QRomLogConfig extends QRomLogBaseConfig {

    @Override
    public int getLogMode() {
        // TODO Auto-generated method stub
        return QRomLogBaseConfig.LOG_BOTH;
    }

    @Override
    public String getPackageName() {
        return HostProxy.getApplication().getPackageName();
    }

//	@Override
//	public void initTraceModules() {
//		// TODO Auto-generated method stub
//		
//	}

//	@Override
//	public boolean isForceTrace() {
//		// TODO Auto-generated method stub
//		return false;
//	}
}