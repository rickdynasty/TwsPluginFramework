package com.tws.plugin.aidl;
import com.tws.plugin.aidl.PaceInfo;
import com.tws.plugin.aidl.IPaceCallBack;
interface PaceServiceAIDL
{
	/**
	 *get info from server and 
	 *Transfer a callback methods handle;
	 *if occur error ,will be return null
	 *对于非基本数据类型和String和CharSequence类型,要加上方向指示
	 *包括in、out和inout，in表示由客户端设置，out表示由服务端设置，inout是两者均可设置。
     */

	/**
	*wallet begin
	*/
	int create(IPaceCallBack callback);
	int destory(IPaceCallBack callback);
	int connect(String macId);
	int disconnect();
	int scan();
	int getDeviceInfo(out PaceInfo info);
	int selectAid(String aid);
	byte[] transmit(in byte[] apdus);
	int close();
	/**
	*wallet end
	*/
}