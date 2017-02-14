package com.example.plugindemo.service;

import tws.component.log.TwsLog;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.example.plugindemo.vo.ParamVO;
import com.tencent.tws.sharelib.IMyAidlInterface;
import com.tencent.tws.sharelib.util.HostProxy;

/**
 * @author yongchen
 * 
 */
public class PluginTestService extends Service {
	private static String TAG = "PluginTestService";

	@Override
	public void onCreate() {
		super.onCreate();
		TwsLog.d(TAG, "PluginTestService onCreate" + getApplication() + " " + getApplicationContext() + " "
				+ getResources().getText(R.string.hello_world3));
		Toast.makeText(HostProxy.getApplication(),
				"PluginTestService 01 onCreate " + getResources().getText(R.string.hello_world3), Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			TwsLog.d(TAG, ((ParamVO) intent.getSerializableExtra("paramvo")) + ", action:" + intent.getAction());
		}

		TwsLog.d("PluginTestService",
				"PluginTestService onStartCommand " + " " + getResources().getText(R.string.hello_world3));

		Toast.makeText(HostProxy.getApplication(),
				"PluginTestService 02 " + getResources().getText(R.string.hello_world3), Toast.LENGTH_SHORT).show();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TwsLog.d(TAG, "PluginTestService onDestroy");
		Toast.makeText(HostProxy.getApplication(), "停止PluginTestService", Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new IMyAidlInterface.Stub() {

			@Override
			public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString)
					throws RemoteException {
				TwsLog.d(TAG, "aString is " + aString + " anInt:" + anInt + " aLong:" + aLong);
			}
		};
	}

}
