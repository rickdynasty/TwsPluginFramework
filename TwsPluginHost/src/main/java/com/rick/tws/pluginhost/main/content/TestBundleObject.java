package com.rick.tws.pluginhost.main.content;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2018/3/20.
 */

public class TestBundleObject implements Parcelable {
    public static final String INTENT_EXTRA_NAME = "test_object";
    private String name;
    private int num;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public TestBundleObject(String name, int num) {
        this.name = name;
        this.num = num;
    }

    protected TestBundleObject(Parcel in) {
        name = in.readString();
        num = in.readInt();
    }

    public static final Creator<TestBundleObject> CREATOR = new Creator<TestBundleObject>() {
        @Override
        public TestBundleObject createFromParcel(Parcel in) {
            return new TestBundleObject(in.readString(), in.readInt());
        }

        @Override
        public TestBundleObject[] newArray(int size) {
            return new TestBundleObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getName());
        dest.writeInt(getNum());
    }

    @Override
    public String toString() {
        return "[name is " + name + ", num is " + num + "]";
    }
}
