package com.example.plugindemo.communicationdma.cmd;

import tws.component.log.TwsLog;

import com.qq.taf.jce.JceInputStream;
import com.tencent.tws.framework.common.Device;
import com.tencent.tws.framework.common.ICommandHandler;
import com.tencent.tws.framework.common.TwsMsg;
import com.tencent.tws.proto.StepGoalInfo;

/**
 * @author xinghuiquan
 * @version 创建时间：2016年11月29日 下午3:02:52
 * 
 */

public class DemoCommandHandler implements ICommandHandler {
    
    private static final String TAG = "DemoCommandHandler";
    
    private static class SingletonHolder {
        private static DemoCommandHandler sInstance = new DemoCommandHandler();
    }

    /**
     * 如果ICommandHandler实现类*需要是同一个对象*，就实现getInstance方法。
     * 
     * com.tencent.tws.framework.common.HandlerBase.getHandlerSingleInstance(Class<?>) 用到
     */
    public static DemoCommandHandler getInstance() {
        return SingletonHolder.sInstance;
    }
    
    private DemoCommandHandler() {
        
    }
    
    
    @Override
    public boolean doCommand(TwsMsg twsMsg, Device device) {
        TwsLog.v(TAG, "this: " + this); // 查看ICommandHandler是否是同一个对象

        final StepGoalInfo stepGaolInfo = new StepGoalInfo();
        stepGaolInfo.readFrom(new JceInputStream(twsMsg.msgByte(), twsMsg.startPosOfContent()));

        StringBuilder builder = new StringBuilder();
        stepGaolInfo.display(builder, 1);
        TwsLog.d(TAG, "health_history : stepGaolInfo=" + builder.toString());

        return false;
    }

}


