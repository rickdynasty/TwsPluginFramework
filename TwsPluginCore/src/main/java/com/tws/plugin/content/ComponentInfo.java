package com.tws.plugin.content;

public class ComponentInfo {
    public ComponentInfo(String name, int type, String packageName, String processName, int processIndex) {
        this.type = type;
        this.name = name;
        this.packageName = packageName;
        this.processName = processName;
        this.processIndex = processIndex;
    }

    public int type;
    public String name;
    public String packageName;
    public String processName;
    public int processIndex = -1;
}