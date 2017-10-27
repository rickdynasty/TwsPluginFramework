package com.tws.plugin.aidl;
import com.tws.plugin.aidl.PaceInfo;
interface IPaceCallBack{
	/*
	*Wallet begin
	*/
	void onConnectResult(boolean isSuc,String mac);
	void onScanResult(in PaceInfo[] infos);
		/*
	*Wallet end
	*/
}