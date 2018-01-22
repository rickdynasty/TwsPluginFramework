package com.tws.plugin.manager;

/**
 * Created by Administrator on 2018/1/22.
 */

public class BindStubInfo {
    public BindStubInfo(String stubName, int pIndex, int launchMode) {
        this.stubName = stubName;
        this.pIndex = pIndex;
        this.launchMode = launchMode;
    }

    public String stubName = "";
    public int pIndex = -1;
    public int launchMode;

    @Override
    public String toString() {
        return "stubName is " + stubName + " pIndex is " + pIndex;
    }
}
