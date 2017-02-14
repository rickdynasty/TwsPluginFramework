package tws.component.log.upload;

import tws.component.log.IUploadLogUIStatCallBack;
import tws.component.log.TwsLog.REPORT_NET_TYPE;

public class AppBussInfo {
    
    /** 上传日志-- 业务名 */
    public String mBussName = null;
    /** 上传日志-- 错误码 */
    public int mErrCode = 0;
    /** 上传日志-- 错误信息 */
    public String mErrMsg = null;
    /** 上传日志-- 上传类型*/
    public int mReportType = -1;
    /** 业务数据标识 */
    public int mResId = -1;
    
    
    /** 上传日志 -- app日志包名: 用于默认zip包包名*/
    public String mUploadPkg = null;    
    /** 上传日志 -- 日志文件路径(null : 不上传日志文件; "default" : 上传默认路径下的日志文件) */
    public String mFilePath = null;
    /** 上传日志 -- 额外数据文件路径 */ 
    public String mExtraPath = null;
    /** 上传日志 -- 附加信息 */
    public byte[] mExtraDatas = null;
    
    /** 当前状态：0 ： 默认状态； 1： 正在执行； -1：取消*/
    public int mRunState = 0; 
    /** 上报网络状态*/
    public int mNetType = REPORT_NET_TYPE.REPORT_NET_WIFI;
    
    public IUploadLogUIStatCallBack mUiCallBack = null;
    
    /**无法从WUP获取RomId，让app自己传递 */
    public long mRomId = 0L;
    
    public AppBussInfo(String bussName, int errCode, String errMsg, int reportType) {
        this(bussName, errCode, errMsg, reportType, 0L);
    }
    
    public AppBussInfo(String bussName, int errCode, String errMsg, int reportType, long romId) {
        mBussName = bussName;
        mErrCode = errCode;
        mErrMsg = errMsg;
        mReportType = reportType;
        mRomId = romId;
    }

    
}
