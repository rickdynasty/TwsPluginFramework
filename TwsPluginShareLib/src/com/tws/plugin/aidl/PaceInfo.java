package com.tws.plugin.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * <一句话功能简述> Parcelable是Android特有的功能，效率比实现Serializable接口高
 * 
 * @author Administrator
 * @version [版本号, 2012-12-10]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class PaceInfo implements Parcelable {
    private int iBatteryLv;

    private String sMacAddr;

    private String sDevName;

    private boolean bConnect;

    public boolean isConnect() {
        return bConnect;
    }

    public void setConnect(boolean connect) {
        bConnect = connect;
    }

    public String getMacAddr() {
        return sMacAddr;
    }

    public void setMacAddr(String mac) {
        sMacAddr = mac;
    }

    public String getDevName() {
        return sDevName;
    }

    public void setDevName(String name) {
        sDevName = name;
    }

    public void setBattery(int val) {
        iBatteryLv = val;
    }

    public int getBattery() {
        return iBatteryLv;
    }

	/**
	 * <默认构造函数>
	 */
	public PaceInfo() {

	}

	/**
	 * <默认构造函数>
	 */
	public PaceInfo(Parcel in) {
		// 注意顺序
        iBatteryLv = in.readInt();
        sMacAddr = in.readString();
        sDevName = in.readString();
        bConnect = (in.readByte() == 1);
	}

	/**
	 * seems meaningless return 0;
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * 将对象序列化为一个Parcel对象 可以将Parcel看成是一个流，通过writeToParcel把对象写到流里面,
	 * 再通过createFromParcel从流里读取对象 注意:写的顺序和读的顺序必须一致。
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(iBatteryLv);
        dest.writeString(sMacAddr);
        dest.writeString(sDevName);
        dest.writeByte((byte) (bConnect ? 1 : 0));
	}

	/**
	 * 实例化静态内部对象CREATOR实现接口Parcelable.Creator public static
	 * final一个都不能少，内部对象CREATOR的名称也不能改变，必须全部大写
	 */
	public static final Parcelable.Creator<PaceInfo> CREATOR = new Creator<PaceInfo>() {

		// 将Parcel对象反序列化为HarlanInfo
		@Override
		public PaceInfo createFromParcel(Parcel source) {
			PaceInfo hlInfo = new PaceInfo(source);
			return hlInfo;
		}

		@Override
		public PaceInfo[] newArray(int size) {
			return new PaceInfo[size];
		}

	};

    public void readFromParcel(Parcel _reply) {
        iBatteryLv = _reply.readInt();
        sMacAddr = _reply.readString();
        sDevName =_reply.readString();
        bConnect = (_reply.readByte() == 1);
    }
}
