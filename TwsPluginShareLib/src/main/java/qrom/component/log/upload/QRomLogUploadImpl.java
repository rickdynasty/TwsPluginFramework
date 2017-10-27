package qrom.component.log.upload;

import qrom.component.log.QRomLog;
import qrom.component.log.QRomLog.LOG_REPORT_ERRCODE;
import qrom.component.log.QRomLog.REPORT_NET_TYPE;
import qrom.component.log.impl.QRomLogContants;
import qrom.component.log.impl.QRomLogImpl;
import qrom.component.log.impl.QRomLogReceiverImpl;
import qrom.component.log.impl.QRomLogUtils;
import qrom.component.log.upload.UploadLogTask.ILogTransferStatusListener;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

public class QRomLogUploadImpl implements Callback, ILogTransferStatusListener {

    private String TAG = "QRomLogUploadImpl";
    
    
    /** 上报日志相关信息 */
    public final static String ACTION_REPORT_LOG_INFO = ".qrom.intent.action.REPORT_LOG_INFO";
    /** 上报日志相关信息 */
    public final static String ACTION_WUP_LOGSDK_GETTICKET_INFO = ".qrom.intent.action.wup.logsdk.getLogTicket";
    
    private String M_ACTION_REPORT_TICKET = null;
    
    private static QRomLogUploadImpl mInstance;
    
    private static int M_RES_ID = 1000;
    
    private int M_PID = -1;
    
    private Handler mHandler = null;
    
    private Handler mainHandler = null;
    
    private AppRomBaseInfo mAppBaseInfo = null;
    
    private final int MSG_SEND_REQUEST = 1;
    private final int MSG_RECEIVE_TICKET = 2;
    private final int MSG_REPORT_LOG = 3;
    private final int MSG_TICKET_FAIL = 4;
    private final int MSG_UPLOAD_END = 5;
    private final int MSG_CHECK_UPLOAD = 6;
    private final int MSG_CANCEL_UPLOAD = 7;
    
    /** 检测日志上报的延时时间 -- 默认30s */
    private final int CHECK_LOG_TICKET_DELAY = 30 * 1000;
    
    /** 每次延时的步长 */
    private final int UPLOAD_DELAY_STEP = 3 * 60 * 1000;
    /** 最大延时时间 */
    private final int UPLOAD_DELAY_MAX = 60 * 60 * 1000;
    
    public static final String LOG_PAHT_DEFAULT = "default";
    
    /** 最大允许同时上传的任务数 */
    private final int MAX_RUN_TASK_CNT = 1;
    
    /** 日志上报类型 -- 主动上报 */
    public static final int LOG_REPORT_TYPE_APP= 1;
    /** 日志上报类型 -- 小工具上报 */
    public static final int LOG_REPORT_TYPE_TOOL = 2;
    
    /** 上传日志缓存区 */
    private SparseArray<AppBussInfo> mAppBussCache = new SparseArray<AppBussInfo>();
    /** 正在运行任务缓存区 */
    private SparseArray<UploadLogTask> mRunningTaskCache = new SparseArray<UploadLogTask>(5);
    
    private LogUploadMainCallback mMainCallback;
    
    private int mTicktErrCnt = 0;
    private long mUploadDelay = 0;
    private long mLastTicketTime = 0;
    
    private QRomLogUploadImpl() {
        M_PID = android.os.Process.myPid();
    }
    
    public static QRomLogUploadImpl getInstance() {
        
        if (mInstance == null) {
            mInstance = new QRomLogUploadImpl();
        }
        return mInstance;
    }
    
    private void init() {
        if (mHandler == null) {
            mHandler = new Handler(QRomLogImpl.getInstance().getLogThreadLooper(), this);
        }
    }
    
    /**
     * 发送接收到ticket的消息
     * @param appbBaseInfo
     * @return
     */
    public boolean sendReceiverLogTicketInfoMsg(AppRomBaseInfo appbBaseInfo, int resId, int rspCode) {
        
        if (rspCode < 0) {  // ticket请求失败
            return sendMsg(resId, MSG_TICKET_FAIL, rspCode, appbBaseInfo);
        }
//        // 移除以前的消息，用最新的ticket
//        removeMsg(MSG_RECEIVE_TICKET);
        // 收到日志上报的ticket了，准备上报日志
        return sendMsg(MSG_RECEIVE_TICKET, MSG_RECEIVE_TICKET, resId, appbBaseInfo);
    }
    
    
    public synchronized int sendReportLogInfoMsg(AppBussInfo appBussInfo) {
        Context context = QRomLogImpl.getInstance().getContext();
        if (context == null || appBussInfo == null) {  // context 为空，未注册对应广播消息（无法获取tickt，不上报）
            QRomLog.w(TAG, "sendReportLogInfoMsg-> context is null or appBussInfo is null, cancel!");
            return LOG_REPORT_ERRCODE.ERR_NOT_REGISTE;
        }
        
        if (!QRomLogImpl.getInstance().getDebugMode()) {  // relese版本，不主动上传日志
            if (appBussInfo.mReportType != LOG_REPORT_TYPE_TOOL 
                    || !QRomLogReceiverImpl.PERMISSION_PACKAGE.equals(
                            QRomLogImpl.getInstance().getPkgName())) {  // 非小工具不允许上报上报
                QRomLog.w(TAG, "sendReportLogInfoMsg-> release app but not debugTool, not report!");
                return LOG_REPORT_ERRCODE.ERR_APP_ILLEGAL;
            }
        }
        
        if (!isNeedSendReq()) {  // 请求太频繁或其他异常，取消请求
            QRomLog.w(TAG, "sendReportLogInfoMsg-> isNeedSendReq : false, cancel!");
            return LOG_REPORT_ERRCODE.ERR_REQUEST_FREQ;
        }
        
        if (!isNetTypeOk(appBussInfo.mNetType, context)) {
            QRomLog.w(TAG, "sendReportLogInfoMsg-> isNetTypeOk : false, cancel!");
            return LOG_REPORT_ERRCODE.ERR_NETTYPE_FAILE;
        }
        
        int resId = 0;
        resId = M_RES_ID++;
        appBussInfo.mResId = resId;
        
        if (sendMsg(resId, MSG_SEND_REQUEST, 0, appBussInfo, 0)) {  // 数据发送成
            mAppBussCache.put(resId, appBussInfo);
        } else {
            resId = LOG_REPORT_ERRCODE.ERR_REQUEST_FAILE;
        }
        return resId;
    }
    
    private boolean isNetTypeOk(int netType, Context context) {
        boolean res = false;
        if (netType == REPORT_NET_TYPE.REPORT_NET_WIFI 
                && QRomLogUtils.isWifiMode(context)) {  // 当前是wifi状态
            res = true;
        } else {
            res = netType == REPORT_NET_TYPE.REPORT_NET_ALL;
        }
        QRomLog.v(TAG, "isNetTypeOk-> res = " + res + ", netType =" + netType);
        return res; 
    }
    
    /**
     * 发送上报日志消息
     * @param appBussInfo
     * @return
     */
    private synchronized int onProcessLogTicketInfoMsg(int resId, AppBussInfo appBussInfo) {
        Context context = QRomLogImpl.getInstance().getContext();
        if (context == null || appBussInfo == null) {  // context 为空
            QRomLog.w(TAG, "onProcessLogTicketInfoMsg-> context or appBussInfo is null, cancel! resId = " + resId);
            return -11;
        }
        
        try {
            
            if (!isNeedSendReq()) {
                QRomLog.w(TAG, "onProcessLogTicketInfoMsg-> isNeedSendReq : false, cancel! resId = " + resId);
                return -13;
            }
            
            if (!isNeedStartUpload()) {  // 不能启动新的任务了
                QRomLog.w(TAG, "onProcessLogTicketInfoMsg-> MAX_RUN_TASK_CNT, delay resId = " + resId);
                mAppBussCache.put(resId, appBussInfo);
                return -14;                
            }
            
            if (!isNetTypeOk(appBussInfo.mNetType, QRomLogImpl.getInstance().getContext())) {
                QRomLog.w(TAG, "onProcessLogTicketInfoMsg-> isNetTypeOk : false, cancel! resId = " + resId);
                return -15;
            }
            
            String pkgName = QRomLogImpl.getInstance().getPkgName();
            if (pkgName == null || "".equals(pkgName)) {
                pkgName = context.getPackageName();
            }
            if (resId <= 0) {
                resId = M_RES_ID++;
            }
            // 发送获取wup ticket的请求
            Intent intent = new Intent(pkgName + ACTION_WUP_LOGSDK_GETTICKET_INFO);
            // 必填数据
            intent.putExtra(QRomLogContants.PARAM_KEY_APP_PKGNAME, pkgName);
            intent.putExtra(QRomLogContants.PARAM_KEY_TICKET_TIMEOUT, CHECK_LOG_TICKET_DELAY);
            intent.putExtra(QRomLogContants.PARAM_KEY_REPORT_RESID, resId);
            intent.putExtra(QRomLogContants.PARAM_KEY_REPORT_PID, M_PID);
//            // 若有其他额外附加数据，通过bundle 传递, wup sdk则将该数据返回
//            intent.putExtra(QRomLogContants.PARAM_KEY_REPORT_EXTRA_DATA, null);
            
            // 发送ticket请求
            context.sendBroadcast(intent);
            appBussInfo.mResId = resId;
            // 延时上报日志信息            
            if (sendMsg(resId, MSG_REPORT_LOG, 0, appBussInfo, CHECK_LOG_TICKET_DELAY + 1000)) {  // 数据发送成功
                mAppBussCache.put(resId, appBussInfo);
            } else {   // 数据发送失败
                resId = -12;
            }
        } catch (Exception e) {
            QRomLog.w(TAG, "onProcessLogTicketInfoMsg-> e: " + e + ", err msg = " + e.getMessage());
        }
        QRomLog.d(TAG, "onProcessLogTicketInfoMsg-> resId = " + resId);
        return resId;
    }
    
    /**
     * 取消对应的上传信息
     * @param resId
     */
    public void cancelReportLogRequest(int resId) {
        sendMsg(MSG_CANCEL_UPLOAD, MSG_CANCEL_UPLOAD, resId, null);
    }
    
    public String getReportTicktAction(Context context) {
        
        if (M_ACTION_REPORT_TICKET == null) {
            
            String pkgName = QRomLogImpl.getInstance().getPkgName();
            if (pkgName == null || "".equals(pkgName)) {
                pkgName = context.getPackageName();
            }
            M_ACTION_REPORT_TICKET = pkgName + QRomLogUploadImpl.ACTION_REPORT_LOG_INFO;
        }
        return M_ACTION_REPORT_TICKET;
    }
    
    private boolean isNeedStartUpload() {
        if (mRunningTaskCache.size() >= MAX_RUN_TASK_CNT) {
            QRomLog.w(TAG, "isNeedStartUpload-> MAX_RUN_TASK_CNT! ");
            return false;
        }
        return true;
    }
    
    /**
     * 上报日志信息
     * @param resId
     * @param appBussInfo
     */
    private void reportLogInfo(int resId, final AppBussInfo appBussInfo) {

        if (appBussInfo == null || resId <= 0) {
            QRomLog.w(TAG, "reportLogInfo-> param is err, resid = " + resId);
            return;
        }
        
        if (mAppBaseInfo == null || mAppBaseInfo.mTicket == null) {
            QRomLog.w(TAG, "reportLogInfo-> mAppBaseInfo is err!");
            return;
        }
        
        if (!isNeedSendReq()) {
            QRomLog.w(TAG, "reportLogInfo-> isNeedSendReq : false, cancel! resid = " + resId);
            return;
        }
        
        if (!isNetTypeOk(appBussInfo.mNetType, QRomLogImpl.getInstance().getContext())) {
            QRomLog.w(TAG, "reportLogInfo-> isNetTypeOk : false, cancel! resid = " + resId);
            return;
        }
        
        if (mainHandler == null) {
            mMainCallback = new LogUploadMainCallback();
            mainHandler = new Handler(Looper.getMainLooper(), mMainCallback);
        }
        if (!isNeedStartUpload()) {  // 不能启动新的上传任务，已打到最大限制
            QRomLog.w(TAG, "reportLogInfo-> MAX_RUN_TASK_CNT! delay resId = " + resId);
            mAppBussCache.put(resId, appBussInfo);
            return;
        }
        UploadLogTask uploadLogTask = new UploadLogTask(appBussInfo, mAppBaseInfo);
        uploadLogTask.setLogTransferStatusListener(mInstance);
        QRomLog.v(TAG, "reportLogInfo-> mRunningTaskCache.put task resId = " + resId + ", buss resId = " + appBussInfo.mResId);
        mRunningTaskCache.put(resId, uploadLogTask);

        Message msg = mainHandler.obtainMessage(0, uploadLogTask);
        msg.arg1 = resId;
        mainHandler.sendMessage(msg);
    }
    
    /**
     * check是否需要继续上传数据
     */
    private void onCheckRestartUploadInfo() {
        if (mAppBussCache.size() == 0) {
            QRomLog.d(TAG, "checkUploadInfo-> no data need upload, cancel");
            return;
        }
        int cnt = mAppBussCache.size();
        int key = 0;
        AppBussInfo appBussInfo = null;
        for (int i = 0; i < cnt; i++) {
            key = mAppBussCache.keyAt(i);
            appBussInfo = mAppBussCache.get(key);
            if (appBussInfo != null) {
                sendMsg(key, MSG_SEND_REQUEST, 0, appBussInfo);
                return;
            }
        }
    }
    
    private void onCancelRequest(int resId) {
        QRomLog.d(TAG, "onCancelRequest-> resId: " + resId);
        AppBussInfo appBussInfo = null;
                
        UploadLogTask uploadLogTask = null;
        if (resId < 0) {  // 取消所有上传信息
            QRomLog.d(TAG, "onCancelRequest-> cancel all request!");
            mAppBussCache.clear();
            int runCnt = mRunningTaskCache.size();
            for (int i = 0; i < runCnt; i++) {
                uploadLogTask = mRunningTaskCache.valueAt(i);
                if (uploadLogTask == null) {
                    continue;
                }
                uploadLogTask.cancelTask();
            }
            return;
        }
        
        // 取消指定任务
        // 取消队列中的任务信息
        appBussInfo = mAppBussCache.get(resId);
        if (appBussInfo != null) {
            appBussInfo.mRunState = -1;
        }
        mAppBussCache.remove(resId);
        // 取消正在运行的任务
        uploadLogTask = mRunningTaskCache.get(resId);
        if (uploadLogTask != null) {          
            uploadLogTask.cancelTask(); 
        }
        
    }
    
    /**
     * 获取ticket失败
     */
    private void onProcessTicketFailed(int resId, int rspCode) {
        
        mLastTicketTime = System.currentTimeMillis();
        mTicktErrCnt++;
        mUploadDelay = mTicktErrCnt * UPLOAD_DELAY_STEP;
        if (mUploadDelay > UPLOAD_DELAY_MAX) {
            mTicktErrCnt = 0;
            mUploadDelay = UPLOAD_DELAY_MAX;
        }
        QRomLog.w(TAG, "handleMessage->MSG_TICKET_FAIL: resId = " + resId + ", rspCode: " + rspCode
                + ", errCnt = " + mTicktErrCnt + ", delay = " + mUploadDelay + ", cur: " + mLastTicketTime);
        // 移除对应消息
        removeMsg(resId);
        mAppBussCache.clear();
        // 清除默认信息
        mAppBaseInfo = null;
    }
    
    private boolean sendMsg(int what, int arg1, int arg2, Object obj, long delay) {
        init();
        // 收到日志上报的ticket了，准备上报日志
        Message msg = mHandler.obtainMessage(what);
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        return mHandler.sendMessageDelayed(msg, delay);
    }
    
    private boolean sendMsg(int what, int arg1, int arg2, Object obj) {
        return sendMsg(what, arg1, arg2, obj, 0);
    }
    
    private void removeMsg(int what) {
        mHandler.removeMessages(what);
    }
    
    private boolean isNeedSendReq() {
        long subTime = System.currentTimeMillis() - mLastTicketTime;
        if ((subTime > 0 && subTime < mUploadDelay) 
                || (subTime < 0 && subTime > -mUploadDelay)) {  // 当前时间据上次间隔时间小于指定延时范围
            return false;
        }
        return true;
    }
    
    @Override
    public boolean handleMessage(Message msg) {
        int what = msg.arg1;
        QRomLog.i(TAG, "handleMessage-> what = " + what);
        int resId = 0;
        AppBussInfo appBussInfo = null;
        try {
            switch (what) {
            case MSG_SEND_REQUEST:  // 收到log上报的请求
                resId = msg.what;
                appBussInfo =(AppBussInfo) msg.obj;
                mAppBussCache.remove(resId);
                int result = onProcessLogTicketInfoMsg(resId, appBussInfo);
                QRomLog.d(TAG, "handleMessage-> MSG_SEND_REQUEST, rspId = " + resId 
                        + ", result = " + result);                
                break;
                
            case MSG_RECEIVE_TICKET: // 收到log上传的ticket
                // 更新baseinfo
                mAppBaseInfo = (AppRomBaseInfo) msg.obj;
                resId = msg.arg2;
                // 移除对应消息
                removeMsg(resId);
                // 立刻重新上报的消息（发送之前移除相同的延时的消息）
                appBussInfo = mAppBussCache.get(resId);
                mAppBussCache.remove(resId);
                sendMsg(resId, MSG_REPORT_LOG, 0, appBussInfo);
                break;
                
            case MSG_REPORT_LOG:  // 上报日志
                resId = msg.what;
                // 移除缓存数据
                mAppBussCache.remove(resId);
                reportLogInfo(resId, (AppBussInfo)msg.obj);
                break;
                
            case MSG_UPLOAD_END:  // 文件上传完成
                appBussInfo = (AppBussInfo) msg.obj;                
                // 移除已执行的task
                mRunningTaskCache.remove(appBussInfo.mResId);       
                // 是否启动新的上报任务
                sendMsg(MSG_CHECK_UPLOAD, MSG_CHECK_UPLOAD, 0, null);
                break;
                
            case MSG_CHECK_UPLOAD: // 检测是否需要重新上报
                onCheckRestartUploadInfo();
                break;
                
            case MSG_CANCEL_UPLOAD:  // 取消上传
                onCancelRequest(msg.arg2);
                break;
            case MSG_TICKET_FAIL: // ticket 获取失败
                int rspCode = msg.arg2;
                resId = msg.what;
                onProcessTicketFailed(resId, rspCode);
                break;

            default:
                break;
            }
        } catch (Exception e) {
            QRomLog.w(TAG, "handleMessage-> e:" + e + ", err msg: " + e.getMessage());
        }
        
        return false;
    }

    @Override
    public void onLogTransferStarted(AppBussInfo appBussInfo) {
        QRomLog.i(TAG, "onLogTransferStarted -> resId = " + appBussInfo.mResId);
    }

    @Override
    public void onLogTransferEnd(AppBussInfo appBussInfo) {
        QRomLog.i(TAG, "onLogTransferEnd -> resId = " + appBussInfo.mResId);
        sendMsg(MSG_UPLOAD_END, MSG_UPLOAD_END, 0, appBussInfo);
    }
    
    @Override
    public boolean isBussInfoValid(AppBussInfo appBussInfo) {
        int resId = appBussInfo.mResId;
        AppBussInfo tempInfo = mAppBussCache.get(resId);
        UploadLogTask uploadLogTask = mRunningTaskCache.get(resId);
        if ((tempInfo != null && tempInfo.mRunState < 0)
                ||  uploadLogTask == null) {  // 没有找到对应的信息，可能被取消了
            QRomLog.w(TAG, "isBussInfoValid-> resId = " + resId + ", task: " + uploadLogTask);
            return false;
        }
        return true;
    }
    
    class LogUploadMainCallback implements Callback {
        
        public boolean handleMessage(Message msg) {
            UploadLogTask uploadLogTask = null;
            switch (msg.what) {
            case 0:
                uploadLogTask = (UploadLogTask) msg.obj;
                String pkgName = uploadLogTask.getAppBussInfo().mUploadPkg;
                if (pkgName == null || "".equals(pkgName)) {
                    pkgName = QRomLogImpl.getInstance().getPkgName();
                }
                QRomLog.d(TAG, "reportLogInfo-> uploadLogTask : start resId = " + msg.arg1 
                        + ", pkg name = " + pkgName);      
                uploadLogTask.executeTask(pkgName);
                break;

            default:
                break;
            }
            return false;
        }
        
    }
}
