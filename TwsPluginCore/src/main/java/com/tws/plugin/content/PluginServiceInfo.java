package com.tws.plugin.content;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/1/21 0021.
 */

public class PluginServiceInfo implements Serializable {
    private String name;// string
    private int processIndex = -1;
    private String processName;

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessIndex(int processIndex) {
        this.processIndex = processIndex;
    }

    public int getProcessIndex() {
        return processIndex;
    }

    public void setServiceName(String name) {
        this.name = name;
    }

    public String getServiceName() {
        return name;
    }
}
