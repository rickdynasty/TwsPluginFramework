package qrom.component.log;

import qrom.component.log.impl.QRomLogImpl;
import qrom.component.log.impl.QRomLogUtils;
import qrom.component.log.upload.AppBussInfo;
import qrom.component.log.upload.QRomLogUploadImpl;
import android.content.Context;
import android.util.Log;

/**
 * 用于输出各种Log的工具类.
 *
 * <p>
 * <b>tips:</b>
 * <p>
 *  如sdcard已经加载，日志文件优先输出到sdcard卡：/mnt/sdcard/Android/${packageName}/log；如果sdcard未加载，日志文件输出到系统存储器：/data/data/${packageName}/log
 *  <p>
 *  crash日志会单独输出到crash文件夹内（具体路径同上）
 *  <p>
 *
 *  <b>obfuscate:</b>
 *  <p>
 *  1. 在工程中引用QRomLog的jar包，不需要对这个jar再次混淆（已经是混淆后的jar文件）</br>
 *  2. 工程中需要实现<b>QRomLogConfig</b>这个类（具体说明参见<em><strong>QRomLogBaseConfig</strong></em>注释），同时工程混淆时要排除<b>QRomLogConfig</b>
 */
public class QRomLog {

    /**
     * Priority constant for the println method; use QRomLog.v.
     */
    public static final int VERBOSE = 2;

    /**
     * Priority constant for the println method; use QRomLog.d.
     */
    public static final int DEBUG = 3;

    /**
     * Priority constant for the println method; use QRomLog.i.
     */
    public static final int INFO = 4;

    /**
     * Priority constant for the println method; use QRomLog.w.
     */
    public static final int WARN = 5;

    /**
     * Priority constant for the println method; use QRomLog.e.
     */
    public static final int ERROR = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int ASSERT = 7;

    /**
     * Priority constant for the println method, use QRomLog.trace.
     */
    public static final int TRACE = 8;

    protected QRomLog(){
    }


    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        QRomLogImpl.getInstance().log('v', tag, msg, null);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('v', tag, msg, tr);
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        QRomLogImpl.getInstance().log('d', tag,getLineMethod()+ msg, null);
    }

    public static String getLineMethod()
    {
        StackTraceElement localStackTraceElement = java.lang.Thread.currentThread().getStackTrace()[4];
        return "[ (" + localStackTraceElement.getFileName() + ":" + localStackTraceElement.getLineNumber() + ")#" + localStackTraceElement.getMethodName() + " ] ";
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('d', tag, msg, tr);
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        QRomLogImpl.getInstance().log('i', tag, msg, null);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('i', tag, msg, tr);
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        QRomLogImpl.getInstance().log('w', tag, msg, null);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static void w(String tag, Throwable tr) {
        QRomLogImpl.getInstance().log('w', tag, null, tr);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('w', tag, msg, tr);
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        QRomLogImpl.getInstance().log('e', tag, msg, null);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('e', tag, msg, tr);
    }
    
    /**
     * 输出error等级以上的日志
     *
     * @param tag		日志TAG
     * @param throwable	异常对象
     */
    public static void e(String tag, Throwable throwable) {
        QRomLogImpl.getInstance().log('e', tag, null, throwable);
    }


    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     */
    public static void wtf(String tag, String msg) {
        QRomLogImpl.getInstance().log('t', tag, msg, null);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, String)}, with an exception to log.
     * @param tag Used to identify the source of a log message.
     * @param tr An exception to log.
     */
    public static void wtf(String tag, Throwable tr) {
        QRomLogImpl.getInstance().log('t', tag, null, tr);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, Throwable)}, with a message as well.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr An exception to log.  May be null.
     */
    public static void wtf(String tag, String msg, Throwable tr) {
        QRomLogImpl.getInstance().log('t', tag, msg, tr);
    }

    /**
     * 输出trace日志
     *
     * @param module	trace模块的ID
     * @param tag		日志TAG
     * @param msg		日志内容
     */
    @Deprecated
    public static void trace(int module, String tag, String msg) {
        QRomLogImpl.getInstance().trace(module, tag, msg, null);
    }

    /**
     * 输出trace异常
     *
     * @param module	    trace模块的ID
     * @param tag		    日志TAG
     * @param throwable		异常对象
     */
    @Deprecated
    public static void trace(int module, String tag, Throwable throwable) {
        QRomLogImpl.getInstance().trace(module, tag, "", throwable);
    }

    /**
     * 输出trace日志
     * (trace是用来回溯程序关键流程的重要Log信息，在release版本发布中，可由特定的机制开启日志、上报日志 ——
     *  因为日志文件会较大，所以会用trace和常用的debug、warning、error等日志区别开，上报时只上报trace日志
     *
     *  tips: 上报功能还在完善中......
     * )
     *
     * @param tag		日志TAG
     * @param msg		日志内容
     */
    public static void trace(String tag, String msg) {
        QRomLogImpl.getInstance().trace(0, tag, msg, null);
    }

    /**
     * 输出trace异常
     *
     * @param tag		    日志TAG
     * @param throwable		异常对象
     */
    public static void trace(String tag, Throwable throwable) {
        QRomLogImpl.getInstance().trace(0, tag, "", throwable);
    }

    /**
     * 输出导致crash的异常信息
     *
     * @param tag        日志TAG
     * @param throwable  异常对象
     */
    public static void crash(String tag, Throwable throwable) {
        QRomLogImpl.getInstance().crash(tag, throwable);
    }
    
    
    /**
     * 输出导致crash的异常信息
     *
     * @param tag        日志TAG
     * @param msg        日志内容
     */
    public static void crash(String tag, String msg) {
        QRomLogImpl.getInstance().crash(tag, msg);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    /**
     * Low-level logging call.
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
        return Log.println(priority, tag, msg);
    }

    /**
     * 注册Log相关广播的Receiver（在Application的onCreate中注册）
     * （tips：在Application的onCreate中注册，如果不注册广播，不影响日志集成功能使用，但是不能开启release调试模式）
     *
     * @param context 上下文
     */
    public static void registerLogReceiver(Context context) {
        QRomLogImpl.getInstance().registerLogReceiver(context);
    }

    // ************************ 2015-02-09 添加日志上传功能接口***********************
    

    /**
     * 小工具仅上传app对应的日志文件
     * @param pkgName     上传日志的app
     * @param logFilePath    指定的日志文件（null: 上传默认日志路径下的所有文件）
     * @param uiCallBack     ui进度回调
     * @return  resId
     */
    public static int reportTraceLogFilesByTools(String pkgName, String logFilePath, int netType,
            IUploadLogUIStatCallBack uiCallBack) {
        String logPath = logFilePath;
        if (logFilePath == null || logFilePath.equals("")) {  // 未指定具体日志文件，上传默认路径下所有日志文件
            logPath = QRomLogUtils.getLogFileDirectory(pkgName, false).getAbsolutePath();
        }
        return reportTraceInfoAndLogFiles("debugTool", 0, "debugTool_" + pkgName, null, null, 
                QRomLogUploadImpl.LOG_REPORT_TYPE_TOOL, logPath, pkgName, netType, uiCallBack);
    }
    
    /**
     * 取消对应的resID的上传操作
     * @param resId   对应请求的reportxxx接口的返回值， resId < 0：取消所有任务
     */
    public static void cancelReportLog(int resId) {
        QRomLogUploadImpl.getInstance().cancelReportLogRequest(resId);
    }
    
    
    /**
     * app仅主动上传对应的日志文件
     * @return
     */
    public static int reportTraceLogFiles() {
        return reportTraceInfoAndLogFiles("reportTraceLogFiles", 0, null, null, null, 
                QRomLogUploadImpl.LOG_PAHT_DEFAULT);
    }
    
    /**
     * app仅上传错误信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @return
     */
    public static int reportTraceInfoOnly(String bussName, int errCode, String errMsg) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, null, null, null);
    }
    
    /**
     * 仅上传附加文件
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
	 * @param extraFilePath  附加文件路径（绝对路径）
     * @return
     */
    public static int reportExtraFileOnly(String bussName, int errCode, String errMsg, String extraFilePath) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, extraFilePath, null, null);
    }

    /**
     * 上传默认错误信息和qromlog默认路径的日志
     * @param bussName
     * @param errCode
     * @param errMsg
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, String errMsg) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, null, null, 
                QRomLogUploadImpl.LOG_PAHT_DEFAULT);
    }
    
    /**
     *  app上传错误信息和app对应的日志文件以及指定附加文件信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraFilePath  附加文件路径（绝对路径）
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, String errMsg, String extraFilePath) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, extraFilePath, null, 
                QRomLogUploadImpl.LOG_PAHT_DEFAULT);
    }
    
    /**
     *  app上传错误信息和app对应的日志文件以及指定附加文件信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraFilePath  附加文件路径（绝对路径）
     * @param uiCallbBack   ui回调监听
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, String errMsg,
            String extraFilePath, IUploadLogUIStatCallBack uiCallbBack) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, extraFilePath, null, 
                QRomLogUploadImpl.LOG_REPORT_TYPE_APP, QRomLogUploadImpl.LOG_PAHT_DEFAULT, null, 
                REPORT_NET_TYPE.REPORT_NET_WIFI, uiCallbBack);
    }
    
    /**
     * 
     *  app上传错误信息和app对应的日志文件以及指定附加数据信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraData       byte[] 附加数据信息
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, String errMsg, byte[] extraData) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, null, extraData, 
                QRomLogUploadImpl.LOG_PAHT_DEFAULT);
    }
    
    /**
     * 
     *  app上传错误信息和app对应的日志文件以及指定附加文件信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraFilePath  附加文件路径（绝对路径）
     * @param extraData       byte[] 附加数据信息
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, 
            String errMsg, String extraFilePath, byte[] extraData) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, extraFilePath, extraData, 
                QRomLogUploadImpl.LOG_PAHT_DEFAULT);
    }

    /**
     * 
     *  app上传错误信息和app对应的日志文件以及指定附加文件信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraFilePath  附加文件路径（绝对路径）
     * @param extraData       byte[] 附加数据信息
     * @param logPath          log日志路径  （若不想上传日志文件，则设置为null）
     * @return
     */
    public static int reportTraceInfoAndLogFiles(String bussName, int errCode, 
            String errMsg, String extraFilePath, byte[] extraData, String logPath) {
        return reportTraceInfoAndLogFiles(bussName, errCode, errMsg, extraFilePath, extraData, 
                QRomLogUploadImpl.LOG_REPORT_TYPE_APP, logPath, null, 
                REPORT_NET_TYPE.REPORT_NET_WIFI, null);
    }

    /**
     *  上传错误信息和app对应的日志文件以及指定附加文件信息
     * @param bussName      业务名
     * @param errCode          错误码
     * @param errMsg           错误信息
     * @param extraFilePath  附加文件路径（绝对路径）
     * @param extraData       byte[] 附加数据信息
     * @param reportType     上报类型； 1： app主动上传日志，2：小工具上传日志信息
     * @param logPath          log路径
     * @param pkgName      app日志的包名
     * @param netType        上报的网络状态
     * @param uiCallbBack   ui回调监听
     * @return
     */
    private static int reportTraceInfoAndLogFiles(String bussName, int errCode, String errMsg, 
            String extraFilePath, byte[] extraData, int reportType, String logPath, String pkgName,
            int netType, IUploadLogUIStatCallBack uiCallbBack) {
        
        AppBussInfo appBussInfo = new AppBussInfo(bussName, errCode, errMsg, reportType);
        if (QRomLogUploadImpl.LOG_PAHT_DEFAULT.equals(logPath)) {
            logPath = QRomLogImpl.getInstance().getLogStoragePathStr();
        }
        // 设置上传的log相关信息
        appBussInfo.mFilePath = logPath;
        appBussInfo.mExtraPath = extraFilePath;
        appBussInfo.mExtraDatas = extraData;
        appBussInfo.mUploadPkg = pkgName;                
        appBussInfo.mUiCallBack = uiCallbBack;
        appBussInfo.mNetType = netType;
        return QRomLogUploadImpl.getInstance().sendReportLogInfoMsg(appBussInfo);
    }

    
    /**
     * 上报网络类型
     * @author sukeyli
     *
     */
    public static class REPORT_NET_TYPE {
        /** 上报日志网络类型 -- 全网上报 */
        public static final int REPORT_NET_WIFI = 0;
        /** 上报日志网络类型 -- 仅wifi上报 */
        public static final int REPORT_NET_ALL = 1;
    }
    
    public static class LOG_REPORT_ERRCODE {
        /** 未注册日志上报功能 */
        public static final int ERR_NOT_REGISTE = -1;
        /** 请求发送失败 */
        public static final int ERR_REQUEST_FAILE = -2;
        /** 上报频繁 -- 后台返回码错误，间隔最小周期后才能请求 */
        public static final int ERR_REQUEST_FREQ = -3;
        /** 调用app非法，非debug版本（ 非小工具release版本不允许上报上报）*/
        public static final int ERR_APP_ILLEGAL = -4;
        /** 上报网络类型不匹配 */
        public static final int ERR_NETTYPE_FAILE = -5;
    }

//**********************   2014-12-24  先屏蔽trace上报的相关功能 ******************
//
//    /**
//     * 通知所有监听器开启trace log
//     *
//     * @param context 	Context
//     * @param modules 	开启trace log的trace模块ID列表
//     * @param expireTime 开启trace log的时间长度(毫秒)
//     */
//    public static void notifyTraceLogOpened(Context context, ArrayList<Integer> modules, long expireTime) {
//        QRomLogImpl.getInstance().notifyTraceLogOpened(context, modules, expireTime);
//    }
//
//    /**
//     * 通知所有trace模块关闭trace log
//     *
//     * @param context	Context
//     */
//    public static void notifyTraceLogClosed(Context context) {
//        QRomLogImpl.getInstance().notifyTraceLogClosed(context);
//    }
//
//    /**
//     * 注册trace log监听器
//     *
//     * @param context 上下文
//     */
//    public static void registerTraceLogReceiver(Context context) {
//        QRomLogImpl.getInstance().registerTraceLogReceiver(context);
//    }
//
//    /**
//     * 解注册trace log监听器
//     *
//     * @param context 上下文
//     */
//    public static void unregisterTraceLogReceiver(Context context) {
//        QRomLogImpl.getInstance().unregisterTraceLogReceiver(context);
//    }
//
//    /**
//     * 准备上报trace log
//     *
//     * @return 用于上报的trace Log的文件列表
//     */
//    public static ArrayList<File> prepareUploadTraceLog() {
//        return QRomLogImpl.getInstance().prepareUploadTraceLog();
//    }
//
//
//**********************   2014-12-24  先屏蔽trace上报的相关功能 ******************


    //benylwang add
    public static boolean isQRomLogOpen() {
        return QRomLogImpl.getInstance().isQRomLogOpen();
    }
}
