package com.example.plugindemo.communicationdma.account;

import qrom.component.log.QRomLog;
import qrom.component.wup.apiv2.RomBaseInfoBuilder;
import tws.component.log.TwsLog;
import TRom.DeviceBaseInfo;
import TRom.RomAccountInfo;
import TRom.RomBaseInfo;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.tws.framework.common.DevMgr;
import com.tencent.tws.framework.common.Device;
import com.tencent.tws.framework.common.WatchDeviceInfo;
import com.tencent.tws.phoneside.business.AccountManager;
import com.tencent.tws.phoneside.business.WeChatOAuthHelper;
import com.tencent.tws.phoneside.device.wup.DeviceInfoWupDataFactory;
import com.tencent.tws.phoneside.framework.RomBaseInfoHelper;
import com.tencent.tws.phoneside.framework.WatchDeviceInfoHelper;

/**
 * @author xinghuiquan
 * @version 创建时间：2016年12月1日 下午7:22:04
 * 
 * 
 * <li> V/DemoApplication(15616): testWeChatOAuthHelper oApi: com.tencent.mm.sdk.openapi.WXApiImplV10@3ee05963, wXAppSupportAPI: 587333634
 * 
 * <li> V/DemoApplication(15616): testAccountManager nickName: WWWWWWWWWWWWWWWWWWWWWWWW
 * <li> V/DemoApplication(15616): testAccountManager headImgPath: /storage/emulated/0/tws/accountheadimg/.nomedia/headimg.png
 * <li> V/DemoApplication(15616): testAccountManager accountInfo:   sAccount: 1021702421
 * <li> V/DemoApplication(15616):   sAccountToken: ef2dcd0846f9ec7105fe65d9dcf34310ceb7a08ba3de463ae79c3f3de3d8bafeaae9748e63e8ea5259a80584c5f6fc39af28c284a401a1369f5ad2c057efdb6613ba480e6441666c
 * <li> V/DemoApplication(15616):   eRomAccountType: 0
 * <li> V/DemoApplication(15616):   eRomTokenType: 0
 * <li> V/DemoApplication(15616):   iTokenAppId: 1600000642
 * <li> V/DemoApplication(15616):   sUnionId: 
 * <li> V/DemoApplication(15616): testAccountManager oApi: com.tencent.mm.sdk.openapi.WXApiImplV10@3ee05963, wXAppSupportAPI: 587333634
 * 
 * <li> V/DemoApplication(15616): testRomBaseInfoHelper romBaseInfo:    vGUID: 16, [
 * <li> V/DemoApplication(15616):       18
 * <li> V/DemoApplication(15616):       -57
 * <li> V/DemoApplication(15616):       -49
 * <li> V/DemoApplication(15616):       112
 * <li> V/DemoApplication(15616):       38
 * <li> V/DemoApplication(15616):       -3
 * <li> V/DemoApplication(15616):       -72
 * <li> V/DemoApplication(15616):       26
 * <li> V/DemoApplication(15616):       -1
 * <li> V/DemoApplication(15616):       30
 * <li> V/DemoApplication(15616):       -78
 * <li> V/DemoApplication(15616):       -4
 * <li> V/DemoApplication(15616):       -77
 * <li> V/DemoApplication(15616):       -93
 * <li> V/DemoApplication(15616):       -82
 * <li> V/DemoApplication(15616):       -125
 * <li> V/DemoApplication(15616):   ]
 * <li> V/DemoApplication(15616):   sQUA: SN=ADRDMTWS10_DD&VN=10151106&BN=78&VC=motorola&MO=Nexus 6&RL=1440_2392&CHID=10000&LCID=99&RV=google.2074855&OS=Android5.1.1&QV=V5
 * <li> V/DemoApplication(15616):   sIMEI: 355470062244534
 * <li> V/DemoApplication(15616):   sQIMEI: -10002
 * <li> V/DemoApplication(15616):   sLC: 35661E4122F8564
 * <li> V/DemoApplication(15616):   iRomId: 0
 * <li> V/DemoApplication(15616):   sPackName: 
 * <li> V/DemoApplication(15616):   eExtDataType: 0
 * <li> V/DemoApplication(15616):   mExtData: null
 * 
 * 
 * <li> V/DemoApplication(15616): testWatchDeviceInfoHelper lastConnectedWatchDeviceInfo: ModelId: -1, AndroidOsVer: 5.1, BuildNo: 564, CHID: 10000, DevId: 01f26846663075, LC: A1B2C3000D4E5F6, LCID: 99, ProductName: Goer_es2_160509, Qua: SN=ADRQRTWS30_DD&VN=30161121&BN=564&VC=Goer&MO=Goer_es2_160509&CHID=10000&LC=A1B2C3000D4E5F6&LCID=99&OS=5.1&QV=V5, Sn: ADRQRTWS30_DD, TosBuildType: DD, TosModel: Pacewear, TosVer: 30161121, VendorName: Goer, WatchModel: PW-O116CH
 * <li> V/DemoApplication(15616): testWatchDeviceInfoHelper connectedDev-devString: DEVICE_BLUETOOTH@F2:68:46:66:30:75
 * <li> V/DemoApplication(15616): testWatchDeviceInfoHelper info: ModelId: 0, AndroidOsVer: 5.1, BuildNo: 564, CHID: 10000, DevId: 01f26846663075, LC: A1B2C3000D4E5F6, LCID: 99, ProductName: Goer_es2_160509, Qua: SN=ADRQRTWS30_DD&VN=30161121&BN=564&VC=Goer&MO=Goer_es2_160509&CHID=10000&LC=A1B2C3000D4E5F6&LCID=99&OS=5.1&QV=V5, Sn: ADRQRTWS30_DD, TosBuildType: DD, TosModel: Pacewear, TosVer: 30161121, VendorName: Goer, WatchModel: PW-O116CH
 *
 */

public class AccountTest {
    
    private static final String TAG = "AccountTest";

    
    public static void testGetAccountInfo() {
        testWeChatOAuthHelper();
        testAccountManager();
        testRomBaseInfoHelper();
        testDeviceBaseInfo();
        testWatchDeviceInfoHelper();
    }

    /**
     * WeChatOAuthHelper.getInstance().wxApi()同AccountManager.getInstance().getWXApi()
     */
    private static void testWeChatOAuthHelper() {
        IWXAPI oApi = WeChatOAuthHelper.getInstance().wxApi();
        if (oApi == null) {
            TwsLog.w(TAG, "testWeChatOAuthHelper oApi is NULL");
            return;
        }
        int wXAppSupportAPI = oApi.getWXAppSupportAPI();
        TwsLog.v(TAG, "testWeChatOAuthHelper oApi: " + oApi + ", wXAppSupportAPI: " + wXAppSupportAPI);
    }
    
    private static void testAccountManager() {
        String nickName = AccountManager.getInstance().getNickName();
        TwsLog.v(TAG, "testAccountManager nickName: " + nickName);
        
        String headImgPath = AccountManager.getInstance().getHeadImgPath();
        TwsLog.v(TAG, "testAccountManager headImgPath: " + headImgPath);
        
        RomAccountInfo accountInfo = AccountManager.getInstance().getLoginAccountIdInfo();
        if (accountInfo == null) {
            TwsLog.w(TAG, "testAccountManager accountInfo is NULL");
        } else {
            StringBuilder sb = new StringBuilder();
            accountInfo.display(sb, 1);
            TwsLog.v(TAG, "testAccountManager accountInfo: " + sb.toString());
        }
        
        IWXAPI oApi = AccountManager.getInstance().getWXApi();
        if (oApi == null) {
            TwsLog.w(TAG, "testWeChatOAuthHelper oApi is NULL");
        } else {
            int wXAppSupportAPI = oApi.getWXAppSupportAPI();
            TwsLog.v(TAG, "testAccountManager oApi: " + oApi + ", wXAppSupportAPI: " + wXAppSupportAPI);
        }
    }
    
    private static void testRomBaseInfoHelper() {
        RomBaseInfo romBaseInfo = RomBaseInfoHelper.getRomBaseInfo();
        if (romBaseInfo == null) {
            TwsLog.w(TAG, "testRomBaseInfoHelper romBaseInfo is NULL");
        } else {
            StringBuilder sb = new StringBuilder();
            romBaseInfo.display(sb, 1);
            TwsLog.v(TAG, "testRomBaseInfoHelper romBaseInfo: " + sb.toString());
        }
    }
    
    public static DeviceBaseInfo testDeviceBaseInfo() {
        RomBaseInfo stPhoneBaseInfo = new RomBaseInfoBuilder().build();
        RomBaseInfo stWatchBaseInfo = DeviceInfoWupDataFactory.getInstance().getWatchRomBaseInfo();

        if (stPhoneBaseInfo == null) {
             QRomLog.d(TAG,"testDeviceBaseInfo stPhoneBaseInfo is NULL");
             return null;
        }

        if (stWatchBaseInfo == null) {
             QRomLog.d(TAG,"testDeviceBaseInfo stWatchBaseInfo is NULL");
             return null;
        }

        DeviceBaseInfo stBaseInfo = new DeviceBaseInfo(stPhoneBaseInfo, stWatchBaseInfo);
        QRomLog.d(TAG, "phone QUA: " + stPhoneBaseInfo.getSQUA());
        QRomLog.i(TAG, "watch QUA: " + stWatchBaseInfo.getSQUA());
        
        QRomLog.d(TAG, "phone IMEI: " + stPhoneBaseInfo.getSIMEI());
        QRomLog.i(TAG, "watch IMEI: " + stWatchBaseInfo.getSIMEI());
        return stBaseInfo;
    }
    
    private static void testWatchDeviceInfoHelper() {
        WatchDeviceInfo lastConnectedWatchDeviceInfo = WatchDeviceInfoHelper.getInstance().getLastConnectedWatchDeviceInfo();
        if (lastConnectedWatchDeviceInfo == null) {
            TwsLog.w(TAG, "testWatchDeviceInfoHelper lastConnectedWatchDeviceInfo is NULL");
        } else {
            TwsLog.v(TAG, "testWatchDeviceInfoHelper lastConnectedWatchDeviceInfo: " + getWatchDeviceInfoLog(lastConnectedWatchDeviceInfo));
        }
        
        Device connectedDev = DevMgr.getInstance().connectedDev();
        if (connectedDev == null) {
            TwsLog.w(TAG, "testWatchDeviceInfoHelper connectedDev is NULL");
        } else {
            TwsLog.v(TAG, "testWatchDeviceInfoHelper connectedDev-devString: " + connectedDev.devString());
            WatchDeviceInfo info = WatchDeviceInfoHelper.getInstance().getWatchDeviceInfo(connectedDev);
            
            if (info == null) {
                TwsLog.w(TAG, "testWatchDeviceInfoHelper info is NULL");
            } else {
                TwsLog.v(TAG, "testWatchDeviceInfoHelper info: " + getWatchDeviceInfoLog(info));
            }
        }
    }
    
    private static String getWatchDeviceInfoLog(WatchDeviceInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("ModelId: " + info.m_nModelId);
        sb.append(", AndroidOsVer: " + info.m_strAndroidOsVer);
        sb.append(", BuildNo: " + info.m_strBuildNo);
        sb.append(", CHID: " + info.m_strCHID);
        sb.append(", DevId: " + info.m_strDevId);
        sb.append(", LC: " + info.m_strLC);
        sb.append(", LCID: " + info.m_strLCID);
        sb.append(", ProductName: " + info.m_strProductName);
        sb.append(", Qua: " + info.m_strQua);
        sb.append(", Sn: " + info.m_strSn);
        sb.append(", TosBuildType: " + info.m_strTosBuildType);
        sb.append(", TosModel: " + info.m_strTosModel);
        sb.append(", TosVer: " + info.m_strTosVer);
        sb.append(", VendorName: " + info.m_strVendorName);
        sb.append(", WatchModel: " + info.m_strWatchModel);
        return sb.toString();
    }
}


