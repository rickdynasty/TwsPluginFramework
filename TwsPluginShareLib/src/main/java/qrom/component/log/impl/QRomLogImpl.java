package qrom.component.log.impl;

import android.content.Context;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import qrom.component.log.QRomLog;
import qrom.component.log.QRomLogBaseConfig;
import qrom.component.log.QRomLogReceiver;
import qrom.component.log.upload.QRomLogUploadImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * Title: QRomLogImpl
 * Package: qrom.component.log.impl
 * Author: interzhang
 * Date: 14-3-17 下午3:44
 * Version: v1.0
 */
public class QRomLogImpl {

    private QRomLogBaseConfig mConfig = null;

    private HandlerThread mLogThread = null;
    private QRomLogHandler mLogHandler = null;

//    private QRomTraceParams mTraceParams = null;
//    private boolean mTraceInit = false;

    private QRomLogReceiver mLogReceiver;

    private WeakReference<Context> mContextRef = null;

    private static QRomLogImpl me = null;

    private static StringBuilder mStrBuilder = new StringBuilder(1024);

    private boolean mDebugable = false;

    private boolean mForceLog = false;
    
    private int mPid = -1;

    private QRomLogImpl() {
        mPid = android.os.Process.myPid();

        try {
            Class<QRomLogBaseConfig> clz = (Class<QRomLogBaseConfig>) Class.forName(
                    "qrom.component.config.QRomLogConfig");
            mConfig = clz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Missing class QRomLogConfig(must implement the abstract class QRomLogBaseConfig) " +
                    "in package which is 'qrom.component.config'");
        }

        try {
            Class<?> clz = Class.forName(
                    mConfig.getPackageName() + ".BuildConfig");
            Field f = clz.getField("DEBUG");
            mDebugable = f.getBoolean(null);
        } catch (Exception e) {
            mDebugable = false;
        }

        //for android os
        if ("android".equals(mConfig.getPackageName()) && checkIfQromDebugVer()) {
        	mDebugable = true;
        }

        mForceLog = QRomLogUtils.getLogCfgSwitch(mConfig.getPackageName());

        if (mLogHandler == null) {
            initLogThread();
            if (mLogThread.getLooper() != null) {
                mLogHandler = new QRomLogHandler(mLogThread.getLooper(), mConfig);
                mLogHandler.sendEmptyMessage(QRomLogHandler.LOG_MSG_FILE_CLEAN);  // 清理过期日志
            }
        }
    }

    public static QRomLogImpl getInstance() {
        if (me == null) {
            me = new QRomLogImpl();
        }
        return me;
    }

    public boolean isQRomLogOpen() {
        if (!mForceLog && !mDebugable) {
            return false;
        }

        if (mConfig.getLogMode() == QRomLogBaseConfig.LOG_NONE) {
            return false;
        }

        return true;
    }

    public void log(char level, String tag, String msg, Throwable tr) {
        if (!mForceLog && !mDebugable) {
            return;
        }

        if (mConfig.getLogMode() == QRomLogBaseConfig.LOG_NONE) {
            return;
        }

        if (tag == null) {
            return;
        }

        if (msg != null) {
            msg += '\n';
        }

        String throwableStacks = null;
        // 是否需要输出到IDE控制台上
        if (mConfig.getLogMode() == QRomLogBaseConfig.LOG_CONSOLE ||
                mConfig.getLogMode() == QRomLogBaseConfig.LOG_BOTH) {
            if (tr != null) {
                throwableStacks = QRomLog.getStackTraceString(tr);
            }
            switch (level) {
                case 'i':
                    Log.i(tag, tr == null ? msg : msg + throwableStacks);
                    break;

                case 'v':
                    Log.v(tag, tr == null ? msg : msg + throwableStacks);
                    break;

                case 'd':
                    Log.d(tag, tr == null ? msg : msg + throwableStacks);
                    break;

                case 'w':
                    if (msg == null) {
                        Log.w(tag, throwableStacks);
                    } else {
                        Log.w(tag, tr == null ? msg : throwableStacks);
                    }
                    break;

                case 'e':
                	if (msg == null) {
                        Log.e(tag, throwableStacks);
                    } else {
                        Log.e(tag, tr == null ? msg : msg + throwableStacks);
                    }
                    break;

                case 't':
                    if (msg == null) {
                        Log.wtf(tag, tr.getMessage(), tr);
                    } else {
                        Log.wtf(tag, msg, tr);
                    }
                    break;

                default:
                    Log.d(tag, msg);
            }
        }

        // 是否需要在file中输出日志
        if (mForceLog || mConfig.getLogMode() == QRomLogBaseConfig.LOG_FILE ||
                mConfig.getLogMode() == QRomLogBaseConfig.LOG_BOTH) {
            if (throwableStacks == null && tr != null) {
                throwableStacks = QRomLog.getStackTraceString(tr);
            }

            switch (level) {
                case 'i':
                    sendHandlerMsg("Info", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
                    break;

                case 'v':
                    sendHandlerMsg("Verbose", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
                    break;

                case 'd':
                    sendHandlerMsg("Debug", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
                    break;

                case 'w':
                    if (msg == null) {
                        sendHandlerMsg("Warn", tag, throwableStacks, false, mPid);
                    } else {
                        sendHandlerMsg("Warn", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
                    }
                    break;

                case 'e':
                    if (msg == null) {
                        sendHandlerMsg("Error", tag, throwableStacks, false, mPid);
                    } else {
                        sendHandlerMsg("Error", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
                    }
                    break;

                case 't':
                    TerribleFailure what = new TerribleFailure(msg, tr);
                    sendHandlerMsg("Assert", tag, QRomLog.getStackTraceString(what), false, mPid);
                    break;

                default:
                    sendHandlerMsg("Debug", tag, tr == null ? msg : msg + throwableStacks, false, mPid);
            }
        }
    }

    public void trace(int module, String tag, String msg, Throwable tr) {
        if (!mForceLog && !mDebugable) {
            return;
        }

        if (msg != null) {
            msg += '\n';
        }

        if (tag != null) {
            if (mForceLog || mConfig.getLogMode() == QRomLogBaseConfig.LOG_FILE ||
                    mConfig.getLogMode() == QRomLogBaseConfig.LOG_BOTH) {
                sendHandlerMsg("Trace", tag, tr == null ? msg : msg + QRomLog.getStackTraceString(tr), false, mPid);
            }
        }
    }


    public void crash(String tag, Throwable throwable) {
        if (throwable == null || mConfig.getLogMode() == QRomLogBaseConfig.LOG_NONE) {
            return;
        }

        File dir = QRomLogUtils.getCrashFileDirectory(mConfig.getPackageName());
        File crashFile;
        if (dir != null) {
            Calendar cal = Calendar.getInstance();
            crashFile = QRomLogUtils.createNewFile(dir.getAbsolutePath(), "crash_" + formatDate(cal) + ".log");
            if (crashFile != null) {
                FileWriter writer = null;
                try {
                    writer = new FileWriter(crashFile, true);
                    writer.write(formatLogMessage("Crash", tag, QRomLog.getStackTraceString(throwable), mPid));
                    writer.write("\r\n");
                    writer.flush();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void crash(String tag, String msg) {
        if (tag == null || msg == null || mConfig.getLogMode() == QRomLogBaseConfig.LOG_NONE) {
            return;
        }

        File dir = QRomLogUtils.getCrashFileDirectory(mConfig.getPackageName());
        File crashFile;
        if (dir != null) {
            Calendar cal = Calendar.getInstance();
            crashFile = QRomLogUtils.createNewFile(dir.getAbsolutePath(), "crash_" + formatDate(cal) + ".log");
            if (crashFile != null) {
                FileWriter writer = null;
                try {
                    writer = new FileWriter(crashFile, true);
                    writer.write(formatLogMessage("Crash", tag, msg, mPid));
                    writer.write("\r\n");
                    writer.flush();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void setForceLog(boolean isForce) {
        mForceLog = isForce;
    }

    protected void sendHandlerMsg(String level, String tag, String msg, boolean isTrace, int pid) {
        if (mLogHandler != null) {
            Message message;
            if (isTrace) {
                message = mLogHandler.obtainMessage(QRomLogHandler.LOG_MSG_TRACE_WRITE, formatLogMessage(level, tag, msg, pid));
            } else {
                message = mLogHandler.obtainMessage(QRomLogHandler.LOG_MSG_LOG_WRITE, formatLogMessage(level, tag, msg, pid));
            }
            mLogHandler.sendMessage(message);
        }
    }

    public synchronized void registerLogReceiver(Context context) {
        if (context != null) {
            setContext(context);
        } else {
            return;
        }

        if (mLogReceiver == null) {
            mLogReceiver = new QRomLogReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(QRomLogReceiver.ACTION_FORCE_LOG);
            filter.addAction(QRomLogReceiver.ACTION_TRACE_LOG);
            // 添加接收上报日志所需的相关信息数据
            String pkgName = getPkgName();
            if (pkgName == null || "".equals(pkgName)) {
                pkgName = context.getPackageName();
            }
            filter.addAction(pkgName+QRomLogUploadImpl.ACTION_REPORT_LOG_INFO);
//            QRomLog.w("=====", "registerReceiver: " + pkgName+QRomLogUploadImpl.ACTION_REPORT_LOG_INFO);
            //aidanzhang, 临时解决方法，system server中拿到的context, context.getApplicationContext()为null
            if(context.getApplicationContext() != null)
            	context.getApplicationContext().registerReceiver(mLogReceiver, filter);
            else
            	context.registerReceiver(mLogReceiver, filter);
        }
    }


//    public void trace(int module, String tag, String msg, Throwable tr) {
//        if (!mForceLog && !mDebugable) {
//            return;
//        }
//
//        if (!mTraceInit) {
//            mTraceInit = true;
//            readTraceLogParams();
//        }
//
//        if (mTraceParams == null) {
//            return;
//        }
//
//        if (tag != null) {
//            if (mForceLog || mConfig.getLogMode() == QRomLogBaseConfig.LOG_FILE ||
//                    mConfig.getLogMode() == QRomLogBaseConfig.LOG_BOTH) {
//                sendHandlerMsg("Trace", tag, tr == null ? msg : msg + '\n' + QRomLog.getStackTraceString(tr), false);
//            }
//
//            if (mTraceParams.traceModules.size() > 0) {
//                if (mTraceParams.traceExpires < System.currentTimeMillis()) {
//                    closeTraceLog();
//                } else {
//                    if (mTraceParams.traceModules.contains(module) || mTraceParams.traceModules.contains(QRomLogBaseConfig.TRACE_MODULE_ALL)) {
//                        String moduleStr = mConfig.getTraceModules().get(module);
//                        sendHandlerMsg(moduleStr == null ? "UNKNOWN" : moduleStr, tag, msg, true);
//                    }
//                }
//            }
//        }
//    }

//    /**
//     * 打开TraceLog日志
//     *
//     * @param expireTime 多少毫秒后Trace失效
//     */
//    public void openTraceLog(ArrayList<Integer> modules, long expireTime) {
//        if (modules != null && modules.size() > 0 && expireTime > 0) {
//            if (mTraceParams == null) {
//                mTraceParams = new QRomTraceParams();
//            } else {
//                mTraceParams.traceModules.clear();
//            }
//
//            mTraceParams.traceModules.addAll(modules);
//            mTraceParams.traceExpires = System.currentTimeMillis() + expireTime;
//        }
//    }
//
//    public void closeTraceLog() {
//        if (mLogHandler == null || mTraceParams == null) {
//            return;
//        }
//
//        Message msg = Message.obtain(mLogHandler, QRomLogHandler.LOG_MSG_TRACE_CLOSE);
//        if (mConfig.getLogMode() ==  QRomLogBaseConfig.LOG_NONE) {
//            msg.obj = true;
//            mLogHandler.sendMessageAtFrontOfQueue(msg);
//            mLogThread = null;
//            mLogHandler = null;
//            mCalendar = null;
//        } else{
//            mLogHandler.sendMessageAtFrontOfQueue(msg);
//        }
//
//        /* 清空Trace Params */
//        mTraceParams = null;
//    }
//
//    public void registerTraceLogReceiver(Context context) {
//        if (context != null) {
//             setContext(context);
//        } else {
//            return;
//        }
//
//        if (mTraceLogReceiver == null) {
//            String action = context.getPackageName() + ".ACTION_TRACELOG";
//            mTraceLogReceiver = new QRomLogReceiver();
//            context.getApplicationContext().registerReceiver(mTraceLogReceiver,
//                    new IntentFilter(action));
//        }
//    }
//
//    public void unregisterTraceLogReceiver(Context context) {
//        if (context != null) {
//            setContext(context);
//        } else {
//            return;
//        }
//
//        if (mTraceLogReceiver != null) {
//            context.getApplicationContext().unregisterReceiver(mTraceLogReceiver);
//            mTraceLogReceiver = null;
//        }
//    }
//
//    public ArrayList<File> prepareUploadTraceLog() {
//        if (mLogHandler != null) {
//            return mLogHandler.prepareUploadTraceLog();
//        } else {
//            return null;
//        }
//    }
//
//    /**
//     * 发广播的方式打开TraceLog日志
//     *
//     * @param expireTime 多少毫秒后Trace失效
//     */
//    public void notifyTraceLogOpened(Context context, ArrayList<Integer> modules, long expireTime) {
//        if (context != null) {
//            setContext(context);
//        } else {
//            return;
//        }
//
//        writeTraceLogParams(modules, 1);
//        Intent intent = new Intent(context.getPackageName() + ".ACTION_TRACELOG");
//        intent.putIntegerArrayListExtra("TRACE_MODULES", modules);
//        if (expireTime > 0) {
//            intent.putExtra("TRACE_EXPIRES", expireTime);
//        }
//        intent.putExtra("TRACE_FLAG", true);
//        context.sendBroadcast(intent);
//    }
//
//    public void notifyTraceLogClosed(Context context) {
//        if (context != null) {
//            setContext(context);
//        } else {
//            return;
//        }
//
//        writeTraceLogParams(null, 0);
//        Intent intent = new Intent(context.getPackageName() + ".ACTION_TRACELOG");
//        intent.putExtra("TRACE_FLAG", false);
//        context.sendBroadcast(intent);
//    }
//
//
//    private void readTraceLogParams() {
//        File ini = new File(QRomLogUtils.getLogFileDirectory(mConfig.getPackageName(), true), "trace.ini");
//        if (ini != null && ini.exists()) {
//            BufferedReader br = null;
//            String modules = null;
//            String expires = null;
//            try {
//                br = new BufferedReader(new FileReader(ini));
//                String line;
//                while((line = br.readLine()) != null) {
//                    if (line.startsWith("TraceModules=")) {
//                        modules = line.substring("TraceModules=".length());
//                    } else if (line.startsWith("TraceExpires=")){
//                        expires = line.substring("TraceExpires=".length());
//                    }
//                }
//            } catch (Exception ex) {
//                System.out.println(ex.getMessage());
//            } finally {
//                if (br != null) {
//                    try {
//                        br.close();
//                    } catch (IOException e) {
//                        System.out.println(e.getMessage());
//                    }
//                }
//            }
//
//            if (!TextUtils.isEmpty(modules) && !TextUtils.isEmpty(expires)) {
//                long time = Long.parseLong(expires);
//                if (time > System.currentTimeMillis()) {
//                    String[] array = modules.split(";");
//                    if (array != null && array.length > 0) {
//                        ArrayList<Integer> list = new ArrayList<Integer>();
//                        for (String s : array) {
//                            list.add(Integer.parseInt(s));
//                        }
//
//                        if (mTraceParams == null) {
//                            mTraceParams = new QRomTraceParams();
//                        } else {
//                            mTraceParams.traceModules.clear();
//                        }
//                        mTraceParams.traceModules.addAll(list);
//                        mTraceParams.traceExpires = time;
//                    } else {
//                        ini.delete();
//                    }
//                } else {
//                    ini.delete();
//                }
//            } else {
//                ini.delete();
//            }
//        }
//    }
//
//    private void writeTraceLogParams(ArrayList<Integer> modules, int timeOutHours) {
//        if (modules != null && modules.size() > 0) {
//            File dir = QRomLogUtils.getLogFileDirectory(mConfig.getPackageName(), true);
//            if (dir == null) {
//               return;
//            }
//
//            // 创建ini文件
//            File ini = new File(dir, "trace.ini");
//            if (!ini.exists()) {
//                try {
//                    if (!ini.createNewFile()) {
//                        return;
//                    }
//                } catch (IOException ex) {
//                    return;
//                }
//            }
//
//            // 解析参数
//            StringBuilder builder = new StringBuilder();
//            int size = modules.size();
//            for (int i = 0; i < size; i++) {
//                if (i != 0) {
//                    builder.append(";");
//                }
//                builder.append(modules.get(i));
//            }
//            long expires = System.currentTimeMillis() + timeOutHours * 60 * 60 * 1000;
//            // 写入参数
//            BufferedWriter bw = null;
//            try {
//                bw = new BufferedWriter(new FileWriter(ini));
//                bw.write("TraceModules=" + builder.toString() + "\r\n");
//                bw.write("TraceExpires=" + String.valueOf(expires) + "\r\n");
//                bw.flush();
//            } catch (Exception ex) {
//                ini.delete();
//            } finally {
//                if (bw != null) {
//                    try {
//                        bw.close();
//                    } catch (IOException e) {
//                        System.out.println(e.getMessage());
//                    }
//                }
//            }
//        } else {
//            File ini = new File(QRomLogUtils.getLogFileDirectory(mConfig.getPackageName(), true), "trace.ini");
//            if (ini.exists()) {
//                ini.delete();
//            }
//        }
//    }

    private void setContext(Context context) {
        if (mContextRef == null) {
        	if (context.getApplicationContext() != null) {
        		mContextRef = new WeakReference<Context>(context.getApplicationContext());
        	} else {
        		mContextRef = new WeakReference<Context>(context);
        	}
        } else {
            Context ctx = mContextRef.get();
            if (ctx == null) {
            	if (context.getApplicationContext() != null) {
            		mContextRef = new WeakReference<Context>(context.getApplicationContext());
            	} else {
            		mContextRef = new WeakReference<Context>(context);
            	}
            }
        }
    }

    /**
     * 获取context
     * @return
     */
    public Context getContext() {
        if (mContextRef != null) {
            return  mContextRef.get();
        }
        return null;
    }
   

    public boolean getDebugMode() {
        return mDebugable;
    }
    
    private synchronized static String formatLogMessage(String level, String tag, String content, int pid) {

        // %s/thread-%d/%s/%s: %s
        String time = formatTime();
        mStrBuilder.setLength(0);
        mStrBuilder.append(time);
        try {
        	mStrBuilder.append("/thread-").append(Thread.currentThread().getId()).append("/");
        	mStrBuilder.append(level).append("/").append(tag)
                    .append("(")
                    .append(pid)
                    .append(")").append(": ").append(content.toString());
        } catch (ArrayIndexOutOfBoundsException e) {
        	//do nothing
        }
        return mStrBuilder.toString();
    }

    private static String formatTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        try {
            mStrBuilder.setLength(0);
            int num = calendar.get(Calendar.MONTH) + 1;
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append("-");
            num = calendar.get(Calendar.DAY_OF_MONTH);
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append(" ");
            num = calendar.get(Calendar.HOUR_OF_DAY);
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append(":");
            num = calendar.get(Calendar.MINUTE);
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append(":");
            num = calendar.get(Calendar.SECOND);
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append(":");
            num = calendar.get(Calendar.MILLISECOND);
            if (num < 100) {
                mStrBuilder.append("0");
                if (num < 10) {
                    mStrBuilder.append("0");
                }
            }
            mStrBuilder.append(num);
            return mStrBuilder.toString();
        } finally {
            mStrBuilder.setLength(0);
        }
    }

    private synchronized static String formatDate(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }

        try {
            mStrBuilder.setLength(0);
            int num = calendar.get(Calendar.YEAR);
            mStrBuilder.append(num);

            num = calendar.get(Calendar.MONTH) + 1;
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);

            mStrBuilder.append("-");
            num = calendar.get(Calendar.DAY_OF_MONTH);
            if (num < 10) {
                mStrBuilder.append("0");
            }
            mStrBuilder.append(num);
            return mStrBuilder.toString();
        } finally {
            mStrBuilder.setLength(0);
        }
    }

//    private static class QRomTraceParams {
//        public Set<Integer> traceModules = null;
//        public long traceExpires = 0;
//        public QRomTraceParams() {
//            traceModules = new HashSet<Integer>();
//        }
//    }

    private static class TerribleFailure extends Exception {
        TerribleFailure(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
    
    public static boolean checkIfQromDebugVer() {
    	boolean result = false;
    	try {
    		String ret = getSysProp("getprop ro.qrom.build.version.type");
    		if ("DD".equals(ret.trim())) {
    			result = true;
    		}
		} catch (Exception e) {

		}
    	return result;
    }
    
    private static String getSysProp(String cmd) throws Exception{
        
        java.lang.Process process   = Runtime.getRuntime().exec(cmd);
        InputStreamReader inputStreamReader = new   InputStreamReader(process.getInputStream()); 
        char[] buf = new char[15];
        int readLen = 0;
        StringBuilder strb = new StringBuilder();;
        while ((readLen = inputStreamReader.read(buf)) != -1) {
            strb.append(buf, 0, readLen);
        }
      
       return strb.toString();
    }
    
    private void initLogThread() {
        if (mLogThread == null) {
            mLogThread = new HandlerThread("QRomLogThread");
            mLogThread.start();
        }
    }
    
    public Looper getLogThreadLooper() {
        
        initLogThread();
        return mLogThread.getLooper();
    }

    public File getLogStoragePath() {
        return QRomLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
    }
    
    public String getLogStoragePathStr() {
        File logFileDir = QRomLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
        if (logFileDir == null) {
            return null;
        }
        return logFileDir.getAbsolutePath();
    }
    
    public String getPkgName() {
        if (mConfig != null) {
            return mConfig.getPackageName();
        }
        return null;
    }
}
