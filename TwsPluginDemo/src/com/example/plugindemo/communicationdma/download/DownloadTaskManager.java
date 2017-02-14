package com.example.plugindemo.communicationdma.download;

import java.io.File;

import qrom.component.download.QRomDownloadData;
import qrom.component.download.QRomDownloadTaskObserver;
import qrom.component.log.QRomLog;
import android.content.Context;
import android.text.TextUtils;

import com.example.plugindemo.PluginTestApplication;
import com.tencent.tws.phoneside.store.download.OptUtil;
import com.tencent.tws.phoneside.store.download.QRomDownloadManager;
import com.tencent.tws.phoneside.utils.MD5Util;
import com.tencent.tws.util.FileUtils;

/**
 * @author xinghuiquan
 * @version 创建时间：2016年12月1日 下午4:39:07
 * 
 */

public class DownloadTaskManager {
    
    private static final String TAG = "DownloadTaskManager";

    private static class SingletonHolder {
        private static DownloadTaskManager sInstance = new DownloadTaskManager();
    }

    public static DownloadTaskManager getInstance() {
        return SingletonHolder.sInstance;
    }
    
    private Context mContext;
    
    private DownloadTaskListener mDownloadTaskListener;
    
    private DownloadTaskManager() { 
        mContext = PluginTestApplication.sContext;
    }
    
    public void setDownloadTaskListener(DownloadTaskListener downloadTaskListener) {
        mDownloadTaskListener = downloadTaskListener;
    }
    
    public int addDownload(String url, String downloadFileName) {
        QRomDownloadData downloadData = createAndSetDownloadData(url, downloadFileName);
        int taskId = QRomDownloadManager.getInstance(mContext).addNewTask(downloadData, mQRomDownloadTaskObserver);
        return taskId;
    }
    
    public void resumeTask(int taskId) {
        QRomDownloadManager.getInstance(mContext).resumeTask(taskId);
    }
    
    public void pauseTask(int taskId) {
        QRomDownloadManager.getInstance(mContext).cancelTask(taskId);
    }
    
    public void deleteTask(int taskId, boolean isDelFile) {
        QRomDownloadManager.getInstance(mContext).deleteTask(taskId, isDelFile);
    }
    
    private QRomDownloadTaskObserver mQRomDownloadTaskObserver = new QRomDownloadTaskObserver() {
        @Override
        public void onTaskStateChanged(QRomDownloadData downloadData) {
            if (downloadData == null) {
                QRomLog.w(TAG,"onTaskStateChanged() downloadData is NULL");
                return;
            }
            if (mDownloadTaskListener == null) {
                return;
            }
            mDownloadTaskListener.onTaskStateChanged(downloadData);
        }
        
    };
    
    
    private QRomDownloadData createAndSetDownloadData(String url, String downloadFileName) {
        QRomDownloadData downloadData = createDownloadData(url, downloadFileName);
        if (downloadData == null) {
            throw new NullPointerException("createAndSetDownloadData() return value can not be null");
        }
        
        String fileFolder = downloadData.getFileFolder();
        String fileName = downloadData.getFileName();
        
        
        QRomLog.v(TAG, "createStoreTask() fileFolder = " + fileFolder + ", fileName = " + fileName);
        if (TextUtils.isEmpty(fileFolder) || TextUtils.isEmpty(fileName)) {
            throw new NullPointerException("createAndSetDownloadData() fileFolder or fileName can not be EMPTY");
        }
        
        return downloadData;
    }
    
    private QRomDownloadData createDownloadData(String url, String downloadFileName) {
        
        QRomLog.v(TAG, "start createDownloadData() downloadFileName = " + downloadFileName + ", url = " + url);
        
        if (!checkDownloadPreCondition(mContext, url)) {
            throw new NullPointerException("createDownloadData() has NUll param");
        }
        
        QRomDownloadData taskData = checkHasDownloaded(mContext, url);
        if (taskData != null) {
            return taskData;
        }

        taskData = initQRomDownloadData(url, downloadFileName);

        File file = new File(taskData.getFileFolder(), taskData.getFileName());
        if (file.exists()) {
            QRomLog.e(TAG, "createDownloadData() file exists, delete this file, file is " + file.getPath() + ", url is " + url);
            file.delete();
        }
        
        QRomLog.v(TAG, "createDownloadData() downloadFileName = " + downloadFileName + ", url = " + url);
        return taskData;
    }
    
    private static boolean checkDownloadPreCondition(Context context, String url) {
        
        if (context == null) {
            QRomLog.e(TAG, "checkDownloadPreCondition() context is NULL");
            return false;
        }
        
        if (TextUtils.isEmpty(url)) {
            QRomLog.e(TAG, "checkDownloadPreCondition() url is NULL");
            return false;
        }
        return true;
    }
    
    private QRomDownloadData checkHasDownloaded(Context context, String url) {
        
        // 相同url的只能有一个下载任务
        QRomDownloadData taskData = QRomDownloadManager.getInstance(context).getTaskDataByUrl(url);
        QRomLog.i(TAG,"checkHasDownloaded() taskData = " + taskData);
        
        if (taskData == null) {
            return null;
        }
        
        long totalSize = taskData.getTotalSize();
        
        if (totalSize == 0) {
            return null;
        }
        
        if (isNeedReDownload(taskData)) {
            QRomDownloadManager.getInstance(context).deleteTask(taskData.getId(), true);
            return null;
        }
        
        return taskData;
    }
    
    private boolean isNeedReDownload(QRomDownloadData taskData) {
//        String md5 = taskData.getMd5();
        // MD5相同等，看业务自己做判断。
        return true;
    }
    
    private QRomDownloadData initQRomDownloadData(String url, String downloadFileName) {
        QRomDownloadData taskData = new QRomDownloadData();
        if (TextUtils.isEmpty(taskData.getFileFolder())) {
            taskData.setFileFolder(OptUtil.getDownloadDir().getAbsolutePath());
        }
        
        if (TextUtils.isEmpty(downloadFileName)) {
            downloadFileName = getFileNameFromUrl(url);
        }
        
        if (TextUtils.isEmpty(downloadFileName)) {
            downloadFileName = MD5Util.getMD5String(url);
        }
        
        QRomLog.v(TAG, "initQRomDownloadData() downloadFileName: " + downloadFileName + ", url: " + url + ", fileFolder: " + taskData.getFileFolder());
        
        taskData.setUrl(url);
        taskData.setTitle(downloadFileName);
        taskData.setFileName(downloadFileName);
        taskData.setIsAutoRename(false);
        taskData.setTaskType(QRomDownloadManager.TASK_TYPE_DM_PLUG_IN);
        taskData.setIsPatch(true);
        taskData.setNetWorkRule(QRomDownloadData.TASK_NET_WORK_RULE_MOBILE_REMINDER);
        return taskData;
    }
    
    protected String getFileNameFromUrl(String url) {
        String fileName = FileUtils.getFileName(url);
        return fileName;
    }
    
    
    public interface DownloadTaskListener {
        void onTaskStateChanged(QRomDownloadData downloadData);
    }
    
}
