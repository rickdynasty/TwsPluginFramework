package tws.component.log.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import tws.component.log.TwsLog;
import tws.component.log.TwsLogBaseConfig;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

/**
 * Title: TwsLogHandler
 * Package: tws.component.log.impl
 * Author: interzhang
 * Date: 14-3-17 下午3:58
 * Version: v1.0
 */
public class TwsLogHandler extends Handler {

    protected static final int LOG_MSG_LOG_WRITE = 1;
    protected static final int LOG_MSG_LOG_RESET = 2;

    protected static final int LOG_MSG_TRACE_WRITE = 3;
    protected static final int LOG_MSG_TRACE_RESET = 4;
    protected static final int LOG_MSG_TRACE_CLOSE = 5;

    protected static final int LOG_MSG_FILE_CLEAN = 6;


    private static final int FILE_TRACE_MAX_LENGTH = 1024 * 1024;
    private static final int FILE_LOG_MAX_LENGTH = 8 * 1024 * 1024;
    private static final long LOG_DAYMILLISECOND = 24 * 60 * 60 * 1000; // 一天的毫秒数

    private Calendar mCalendar = null;

    private boolean mTraceWriterFlag = false;
    private FileWriter mTraceWriter = null;
    private File mTraceFile = null;
    private long mTraceLength = 0;
    private int mTraceFileDate = -1;

    private boolean mLogWriterFlag = false;
    private FileWriter mLogWriter = null;
    private File mLogFile = null;
    private long mLogLength = 0;
    private int mLogFileDate = -1;

    private String mProcessName = null;
    private TwsLogBaseConfig mConfig = null;

    public TwsLogHandler(Looper looper, TwsLogBaseConfig config) {
        super(looper);
        mConfig = config;
        mCalendar = Calendar.getInstance();
    }

    private String getLogFileSubName(boolean isTrace) {
        if (mProcessName == null) {
            mProcessName = TwsLogUtils.getProcessName();
            if (!TextUtils.isEmpty(mProcessName)) {
                mProcessName = mProcessName.replaceAll(":", "_");
            }
        }

        if (TextUtils.isEmpty(mProcessName)) {
            mProcessName = "pid-" + android.os.Process.myPid();
        }

        if (!isTrace) {
            Calendar cal = Calendar.getInstance();
            return String.format("%s_%d%02d%02d", mProcessName, cal.get(Calendar.YEAR),
                    (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH));
        } else {
            return mProcessName;
        }
    }

    /**
     * 公共的关闭TraceWriter的关闭方法，清零状态
     */
    private void closeTraceWriter() {
        if (mTraceWriter != null) {
            try {
                mTraceWriter.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            mTraceWriter = null;
        }
        mTraceWriterFlag = false;
        mTraceLength = 0;
        mTraceFile = null;
        mTraceFileDate = -1;
    }

    private void closeLogWriter() {
        if (mLogWriter != null) {
            try {
                mLogWriter.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            mLogWriter = null;
        }
        mLogWriterFlag = false;
        mLogLength = 0;
        mLogFile = null;
        mLogFileDate = -1;
    }

    /**
     * 保存当前的trace log到重命名的文件中
     * （在文件超过大小，或者准备开始上传前需要restore）
     */
    private void restoreCurTraceLog() {
        if (mTraceWriter != null) {
            try {
                mTraceWriter.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            mTraceWriter = null;
            mTraceLength = 0;
        }

        if (mTraceFile != null) {
            renameTraceLog(mTraceFile);
        }

        mTraceWriterFlag = false;
        mTraceFile = null;
        mTraceFileDate = -1;
    }

    public ArrayList<File> prepareUploadTraceLog() {
        ArrayList<File> list = new ArrayList<File>();

        File dir1 = TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), true);
        File dir2 = TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
        File[] files = dir1.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                File out = renameTraceLog(f);
                if (out != null) {
                    list.add(out);
                }
            }
        }

        files = dir2.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                File out = renameTraceLog(f);
                if (out != null) {
                    list.add(out);
                }
            }
        }

        return list;
    }

    private File renameTraceLog(File src) {
        if (src.getName().startsWith("trace_") && src.getName().endsWith(".txt")) {
            Calendar cal = Calendar.getInstance();
            String subName = String.format("%d%02d%02d%02d%02d%02d", cal.get(Calendar.YEAR),
                    (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            String newPath = src.getAbsolutePath().replace(".txt", "_" + subName + ".log");
            File newFile = new File(newPath);
            try {
                newFile.createNewFile();
                src.renameTo(newFile);
                return newFile;
            } catch (Exception e) {
                src.delete();
                if (newFile.exists()) {
                    newFile.delete();
                }
                return null;
            }
        }
        return null;
    }

    /**
     * 清空File的内容，重头开始写
     */
    private void resetCurFileLog() {
        if (mLogWriter != null) {
            try {
                mLogWriter.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            mLogWriter = null;
        }

        if (mLogFile != null) {
            final String name = mLogFile.getName().replace(".log", "");
            File dir = TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
            String[] files = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.contains(name);
                }
            });
            int index = (files == null) ? 1 : files.length;
            mLogFile.renameTo(new File(mLogFile.getParent(), name+"(" + index +").log"));
        }

        mLogWriterFlag = false;
        mLogFile = null;
    }

    private void delExpiredLogFile(File dir, long expiredTime) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if ((file.getName().startsWith("log") || file.getName().startsWith("crash")) &&
                            (System.currentTimeMillis() - file.lastModified() > expiredTime)) {
                        file.delete();
                    }
                }
            }
        }
    }

    private void closeTraceLog(boolean quit) {
        removeMessages(LOG_MSG_TRACE_WRITE);
        removeMessages(LOG_MSG_TRACE_RESET);
        restoreCurTraceLog();
        Thread thread = Thread.currentThread();
        if (HandlerThread.class.isInstance(thread)) {
            ((HandlerThread) thread).quit();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case LOG_MSG_LOG_WRITE:  // debug的文件日志
                mCalendar.setTimeInMillis(System.currentTimeMillis());
                try {
                    if (!mLogWriterFlag || mLogFileDate != mCalendar.get(Calendar.DAY_OF_MONTH)) {
                        File dir = TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
                        if (dir != null) {
                            mLogFile = TwsLogUtils.createNewFile(dir.getAbsolutePath(), "log_" + getLogFileSubName(false) + ".log");
                            mLogFileDate = mCalendar.get(Calendar.DAY_OF_MONTH);
                            if (mLogFile != null) {
                                try {
                                    mLogWriter = new FileWriter(mLogFile, true);
                                    mLogWriterFlag = true;
                                    mLogLength = mLogFile.length();
                                } catch (IOException ex) {
                                    mLogWriter = null;
                                    mLogLength = 0;
                                }
                            }
                        }

                        if (mLogWriter == null) {
                            // mLogWriter创建失败，延时再创建
                            sendEmptyMessageDelayed(LOG_MSG_LOG_RESET, 3 * 60000);
                            return;
                        }
                    }

                    if (mLogWriter != null) {
                        try {
                            mLogWriter.write((String) msg.obj);
                            mLogWriter.flush();
                            mLogLength += ((String) msg.obj).length();
                        } catch (IOException e) {
                            // mLogWriter发生IOException，重新创建
                            closeLogWriter();
                        }
                    }

                    if (mLogLength > FILE_LOG_MAX_LENGTH) {
                        resetCurFileLog();
                        return;
                    }

                } catch (Exception ex) {
                	if	(ex.getMessage() != null)
                		TwsLog.w("TraceLog", ex.getMessage());
                	else
                		TwsLog.w("TraceLog", "Write log catch a null exception!!!");
                }
                break;

            case LOG_MSG_TRACE_WRITE:  // 用户行为的trace日志
                mCalendar.setTimeInMillis(System.currentTimeMillis());
                if (!mTraceWriterFlag || mTraceFileDate != mCalendar.get(Calendar.DAY_OF_MONTH)) {
                    File dir = TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), false);
                    if (dir != null) {
                        mTraceFile = TwsLogUtils.createNewFile(dir.getAbsolutePath(), "trace_" + getLogFileSubName(true) + ".txt");
                        if (mTraceFile != null) {
                            mTraceFileDate = mCalendar.get(Calendar.DAY_OF_MONTH);
                            try {
                                mTraceWriter = new FileWriter(mTraceFile, true);
                                mTraceWriterFlag = true;
                                mTraceLength = mTraceFile.length();
                            } catch (IOException ex) {
                                closeTraceWriter();
                            }
                        }
                    }

                    if (mTraceWriter == null) {
                        // mLogWriter创建失败，延时再创建
                        sendEmptyMessageDelayed(LOG_MSG_TRACE_RESET, 3 * 60000);
                        return;
                    }
                }

                if (mTraceWriter != null) {
                    try {
                        mTraceWriter.write((String) msg.obj);
                        mTraceWriter.flush();
                        mTraceLength += ((String) msg.obj).length();
                    } catch (IOException e) {
                        // mLogWriter发生IOException，重新创建
                        closeTraceWriter();
                    }
                }

                // 检查文件是否超过大小
                if (mTraceLength > FILE_TRACE_MAX_LENGTH) {
                    restoreCurTraceLog();
                }
                break;

            case LOG_MSG_LOG_RESET:  // 重新初始化mLogWriter
                closeLogWriter();
                break;

            case LOG_MSG_TRACE_RESET:  // 重新初始化mTraceWriter
                closeTraceWriter();
                break;

            case LOG_MSG_TRACE_CLOSE:
                if (msg.obj == null) {
                    closeTraceLog(false);
                } else {
                    closeTraceLog(true);
                }
                break;

            case LOG_MSG_FILE_CLEAN:  // 清理过期日志
                delExpiredLogFile(TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), true), 3 * LOG_DAYMILLISECOND);
                delExpiredLogFile(TwsLogUtils.getLogFileDirectory(mConfig.getPackageName(), false), 3 * LOG_DAYMILLISECOND);
                delExpiredLogFile(TwsLogUtils.getCrashFileDirectory(mConfig.getPackageName()), 3 * LOG_DAYMILLISECOND);
                break;

            default:
        }
    }
}
