package qrom.component.log.upload;

public class AppRomBaseInfo {

    public static final int EVN_FLG_RELEASE = 0;
    public static final int EVN_FLG_TEST = 1;
    
    public byte[] mGuid = null;
    public String mQua = "";
    public String mLc = "";
    public String mImei = "";
    public long mRomId = 0;
    public String mPkgName = "";

    /** 请求的票据 */
    public String mTicket="";
    /** 日志上传对应环境标识：0： 正式环境；1: 测试环境
     * ticket是测试环境下发，则日志上传到测试环境server上，
     * 正式环境ticket上传到正式环境server*/
    public int mEnvFlg = EVN_FLG_RELEASE;
    
    public boolean isTestFlg() {
        return mEnvFlg == EVN_FLG_TEST;
    }
}
