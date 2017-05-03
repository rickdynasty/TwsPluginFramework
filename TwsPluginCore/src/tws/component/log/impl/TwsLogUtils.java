package tws.component.log.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import tws.component.log.TwsLog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * Title: TwsLogUtils
 * Package: tws.component.log.impl
 * Author: interzhang
 * Date: 14-3-17 下午4:23
 * Version: v1.0
 */
public class TwsLogUtils {
    public final static String KEY_LOG_SWITCH ="log_switch";
    private static final String LOG_CFG_DIR = "/log_cfg";
    private static final String LOG_CFG_FILENAME = "log_switch.ini";
    
    private static String PHONE_IMEI = null;
    
    public static final int TYPE_UNKNOWN = 0x000;
    public static final int TYPE_NET = 0x001;
    public static final int TYPE_WAP = 0x002;    
    public static final int TYPE_WIFI = 0x004;
    public static final int TYPE_2G = 0x008;
    public static final int TYPE_3G = 0x010;
    public static final int TYPE_4G = 0x011;

    public static String getProcessName() {
        int pid = android.os.Process.myPid();
        Object out = execCommand(new String[]{"/system/bin/ps", String.valueOf(pid)}, new ProcessHandler() {
            @Override
            public Object handleProcessInputStream(InputStream is) {
                if (is != null) {
                    BufferedReader br = null;
                    try {
                        String str;
                        int index = 0;
                        br = new BufferedReader(new InputStreamReader(is));
                        while ( (str = br.readLine()) != null ) {
                            if (index == 1) {
                                int pos = str.lastIndexOf(" ");
                                if (pos >= 0) {
                                    return str.substring(pos + 1);
                                }
                                break;
                            }
                            index++;
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    } finally {
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }

                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
                return null;
            }
        });

        if (out != null && String.class.isInstance(out)) {
            return (String)out;
        } else {
            return "";
        }
    }

    /**
     * 检查SD卡是否可用
     *
     * @return boolean
     */
    public static boolean isExternalStorageAvailable() {
        try {
            /* 华为的手机会脑残，Environment.getExternalStorageState()会NullPointerException */
            if (TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState())){
                return true;
            }
            return false;
        } catch(Exception ex) {
            return false;
        }
    }

    public static File getLogFileDirectory(String packageName, boolean dataStorage) {
        String logPtah;
        if ("android".equals(packageName)) {
        	logPtah = "data/system/tws" + "/log/";
        } else if (!isExternalStorageAvailable() || dataStorage) {
            logPtah = "data/data/" + packageName + "/log/";
        } else {
            logPtah = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/Android/data/" + packageName + "/log/";
        }

        File logDir = new File(logPtah);
        if (logDir.exists()) {
            return logDir;
        } else {
            boolean res = logDir.mkdirs();
            if (res) {
               return logDir;
            } else {
                return null;
            }
        }
    }

    public static File getCrashFileDirectory(String packageName) {
        String path;
        if ("android".equals(packageName)) {
        	path = "data/system/tws" + "/crash/";
        } else if (!isExternalStorageAvailable()) {
            path = "data/data/" + packageName + "/crash/";
        } else {
            path = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/Android/data/" + packageName + "/crash/";
        }

        File dir = new File(path);
        if (dir.exists()) {
            return dir;
        } else {
            boolean res = dir.mkdirs();
            if (res) {
                return dir;
            } else {
                return null;
            }
        }
    }

    public static File createNewFile(String fileDir, String fileName) {
        if (TextUtils.isEmpty(fileDir) || TextUtils.isEmpty(fileName)) {
            return null;
        }
        // 文件夹路径
        File dir = new File(fileDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
        }
        // 创建文件
        File file = new File(fileDir, fileName);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return file;
    }


    private static Object execCommand(String[] args, ProcessHandler handler) {
        if(args == null || args.length == 0) {
            return null;
        }

        final ProcessWorker processWorker = new ProcessWorker(args);
        Process process = null;
        processWorker.start();
        try {
            processWorker.join(3000l); // 3s超时
            if (processWorker.getExitCode() != null) {
                process = processWorker.getProcess();
                if (process != null) {
                    return handler.handleProcessInputStream(process.getInputStream());
                }
            }
        } catch(Exception e) {
            processWorker.interrupt();
        } finally {
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch(Exception e) {}
        }
        return null;
    }

    private static class ProcessWorker extends Thread {
        private String[] mArgs;
        private Integer mExitCode;
        private Process mProcess;

        public ProcessWorker(String[] args) {
            mArgs = args;
        }

        public Integer getExitCode() {
            return mExitCode;
        }

        public Process getProcess() {
            return mProcess;
        }

        @Override
        public void run() {
            try {
                final ProcessBuilder cmd = new ProcessBuilder(mArgs);
                mProcess = cmd.start();
                mExitCode = mProcess.waitFor();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static interface ProcessHandler {
        public Object handleProcessInputStream(InputStream in);
    }

    /**
     * 获取TwsLog日志配置开关
     * @param 
     * @return
     */
    public static boolean getLogCfgSwitch(String packageName) {
        int logSwitch = 0;
        InputStream inputStream = null;

        File logCfg = new File(getLogCfgPath(packageName));
        if (logCfg == null || !logCfg.exists() || !logCfg.isFile()) {
            return false;
        }

        try {
            inputStream = new FileInputStream(logCfg);
            Properties property = new Properties();
            property.load(inputStream);
            String value = property.getProperty(KEY_LOG_SWITCH);
            logSwitch = Integer.valueOf(value);
        } catch (Exception e) {
            e.printStackTrace();
            logSwitch = 0;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return logSwitch == 1 ? true : false;
    }
    
    /**
     * 获取TwsLog配置文件路径
     * @param 
     * @return
     */
    private static String getLogCfgPath(String packageName) {
        String logFile = "";
        if ("android".equals(packageName)) {
        	logFile = "data/system/tws" + LOG_CFG_DIR + File.separator + LOG_CFG_FILENAME;
        } else if(isExternalStorageAvailable()) {
            String logCfgDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/tws" + LOG_CFG_DIR;
            logFile = logCfgDir + File.separator + LOG_CFG_FILENAME;
        }
        return logFile;
    }
    
    /**
     * 获取手机imei
     * @param context
     * @return
     */
    public static String getImei(Context context){
        
        if (PHONE_IMEI != null && !"".equals(PHONE_IMEI)) {
            return PHONE_IMEI;
        }
        
        try {
            TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            if (mTelephonyManager != null) {
                PHONE_IMEI = mTelephonyManager.getDeviceId();
            }
            
            if (PHONE_IMEI != null) {
                PHONE_IMEI = PHONE_IMEI.toLowerCase();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return PHONE_IMEI;
    }

    /**
     * 将字节型数据转化为16进制字符串
     */
    public static String byteToHexString(byte[] bytes)  {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }
    
    /**
     * 是否是wifi网络模式 (通过contex获取系统当前状态)
     * @param context
     * @return
     */
    public static boolean isWifiMode(Context context) {
        return getNetType(context) == TYPE_WIFI;
    }
    
    /**
     * 或当前网络类型
     *    -- 返回2G/3G/wifi类型
     */
    private static int getNetType(Context context) {
        
        if (context == null) {
            return TYPE_UNKNOWN;
        }
        
        try {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return TYPE_UNKNOWN;
            }
            // getActiveNetworkInfo方法在部分机型上调用crash，这里catch下异常
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo == null) {
                return TYPE_UNKNOWN;
            }

            int type = networkInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return TYPE_WIFI;
            }

            if (type == ConnectivityManager.TYPE_MOBILE) {
                int subType = networkInfo.getSubtype();
                switch (subType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return TYPE_2G;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return TYPE_4G;
                default:
                    return TYPE_3G;
                }
            }

        } catch (Exception e) {
            TwsLog.w("TwsLogUtils", "getNetType-> e:" + e + ", err msg: " + e.getMessage());
        }
        return TYPE_UNKNOWN;
    }
}
