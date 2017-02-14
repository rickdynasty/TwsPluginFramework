package com.example.plugindemo.communicationdma.cmd;

import android.content.Context;
import android.util.SparseArray;

import com.example.plugindemo.PluginTestApplication;
import com.tencent.tws.framework.common.CommandHandler;
import com.tencent.tws.framework.common.MsgCmdDefine;
import com.tencent.tws.framework.common.MsgDispatcher;
import com.tencent.tws.framework.proxy.PluginCommandHandler;

/**
 * @author xinghuiquan
 * @version 创建时间：2016年12月1日 下午7:16:23
 * 
 */

public class CommandRegister {
    
    /**
     * 此方法必须要在Application的onCreate的时候注册好。
     * 
     * 追加到Host内存中的com.tencent.tws.framework.common.MsgDispatcher.m_oSparseArrayOfMsgReceiver中
     */
    public static void registerCommandHandler() {
        MsgDispatcher.getInstance().appendPluginRecvMsg(build()); 
    }
    
    
    /**
     * array.put(MsgCmdDefine定义的CMD值, new PluginCommandHandler(Application的context, ICommandHandler的实现类.class.getName()));
     * 
     * 用下面的“计步器目标变更”的CMD举例（手表计步器应用，切换目标即可触发CMD）
     */
    private static SparseArray<CommandHandler> build() {
        SparseArray<CommandHandler> array = new SparseArray<CommandHandler>();
        array.put(MsgCmdDefine.CMD_SEND_STEP_GOAL_INFO, new PluginCommandHandler(getContext(), DemoCommandHandler.class.getName()));
        return array;
    }
    
    
    private static Context getContext() {
        return PluginTestApplication.sContext;
    }
    
}


