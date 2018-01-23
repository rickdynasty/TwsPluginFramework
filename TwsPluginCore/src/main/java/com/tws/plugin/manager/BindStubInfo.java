package com.tws.plugin.manager;

/**
 * Created by Administrator on 2018/1/22.
 */

public class BindStubInfo {
    public BindStubInfo() {
    }

    public BindStubInfo(String stubName, int pIndex) {
        this.stubName = stubName;
        this.pIndex = pIndex;
    }

    public String stubName = "";
    public int pIndex = -1;

    @Override
    public String toString() {
        return "stubName is " + stubName + " pIndex is " + pIndex;
    }
}

class ActivityStubInfo extends BindStubInfo {
    public int launchMode;

    public ActivityStubInfo(String stubName, int pIndex, int launchMode) {
        this.stubName = stubName;
        this.pIndex = pIndex;
        this.launchMode = launchMode;
    }

    @Override
    public String toString() {
        return "stubName is " + stubName + " pIndex is " + pIndex + " launchMode is " + launchMode;
    }
}
