package com.example.plugindemo.communicationdma.download;

import qrom.component.download.QRomDownloadData;
import qrom.component.download.QRomDownloadUtils;
import tws.component.log.TwsLog;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.example.plugindemo.communicationdma.download.DownloadTaskManager.DownloadTaskListener;
import com.tencent.tws.handler.UIHandler;

/**
 * @author xinghuiquan
 * @version 创建时间：2016年11月30日 下午5:04:43
 * 
 */

public class DownloadDemoActivity extends Activity implements Callback {

    private static final String TAG = "DownloadDemoActivity";
    
    private static final String TEST_URL = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    
    private static final String TEST_FILE_NAME = "";

    private Button mButton;
    private TextView mNameTv;
    private TextView mSizeTv;
    private TextView mStatusTv;
    
    private UIHandler<Handler.Callback> mUiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication_dma_download);

        mNameTv = (TextView) findViewById(R.id.store_task_name);
        mSizeTv = (TextView) findViewById(R.id.store_task_size);
        mStatusTv = (TextView) findViewById(R.id.store_task_status);
        mButton = (Button) findViewById(R.id.download_start_bt);
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonListener(v, false);
            }
        });
        
        mButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                handleButtonListener(v, true);
                return true;
            }
        });
        
        mUiHandler = new UIHandler<Handler.Callback>(this);
    }
    
    private void handleButtonListener(View v, boolean isLongClick){
        QRomDownloadData downloadData = (QRomDownloadData) v.getTag();
        if (downloadData == null) {
            DownloadTaskManager.getInstance().addDownload(TEST_URL, TEST_FILE_NAME);
            return;
        }
        
        int status = downloadData.getStatus();
        
        switch (status) {
        case QRomDownloadData.TASK_STATUS_CREATED:
        case QRomDownloadData.TASK_STATUS_STARTED:
        case QRomDownloadData.TASK_STATUS_PROGRESS:
            DownloadTaskManager.getInstance().pauseTask(downloadData.getId());
            break;
        case QRomDownloadData.TASK_STATUS_COMPLETED:
        case QRomDownloadData.TASK_STATUS_DELETED:
            DownloadTaskManager.getInstance().addDownload(TEST_URL, TEST_FILE_NAME);
            break;
        case QRomDownloadData.TASK_STATUS_FAILED:
        case QRomDownloadData.TASK_STATUS_CANCELED:
            if (isLongClick) {
                DownloadTaskManager.getInstance().deleteTask(downloadData.getId(), true);
            } else {
                DownloadTaskManager.getInstance().resumeTask(downloadData.getId());
            }
            break;
        }
    }

    private DownloadTaskListener mDownloadTaskListener = new DownloadTaskListener() {

        @Override
        public void onTaskStateChanged(QRomDownloadData downloadData) {
            mUiHandler.obtainMessage(1, downloadData).sendToTarget();
        }
    };

    @Override
    protected void onStop() {
        DownloadTaskManager.getInstance().setDownloadTaskListener(null);
        super.onStop();
    }

    @Override
    protected void onStart() {
        DownloadTaskManager.getInstance().setDownloadTaskListener(mDownloadTaskListener);
        super.onStart();
    }

    @Override
    public boolean handleMessage(Message msg) {
        QRomDownloadData downloadData = (QRomDownloadData) msg.obj;
        handleTaskStateChanged(downloadData);
        return false;
    }
    
    private void handleTaskStateChanged(QRomDownloadData downloadData) {
        
        String fileName = downloadData.getFileName();
        mNameTv.setText(fileName);
        
        mButton.setTag(downloadData);
        
        int status = downloadData.getStatus();
        int progress = (int) (QRomDownloadUtils.getProgress(downloadData) * 100);
        
        TwsLog.i(TAG, "handleTaskStateChanged status is : " + status + ", progress = " + progress + ", fileName = " + fileName);
        updateStatusTv(downloadData);
    }
    
    
    private void updateStatusTv(QRomDownloadData downloadData){
        String size = "";
        int status = downloadData.getStatus();
        switch (status) {
        case QRomDownloadData.TASK_STATUS_CREATED:
        case QRomDownloadData.TASK_STATUS_STARTED:
        case QRomDownloadData.TASK_STATUS_PROGRESS:
            float speed = downloadData.getSpeed();
            long downloadedSize = downloadData.getDownloadedSize();
            size = Formatter.formatFileSize(this, downloadedSize) + " / ";
            mStatusTv.setText(Formatter.formatFileSize(this, (long) speed) + "/s");
            mButton.setText("Downloading");
            break;
        case QRomDownloadData.TASK_STATUS_COMPLETED:
            mStatusTv.setText("Download Complete");
            mButton.setText("Completed");
            break;
        case QRomDownloadData.TASK_STATUS_FAILED:
            mStatusTv.setText("Download failed");
            mButton.setText("failed");
            break;
        case QRomDownloadData.TASK_STATUS_CANCELED:
            mStatusTv.setText("Download pause");
            mButton.setText("Paused");
            break;
        case QRomDownloadData.TASK_STATUS_DELETED:
            mStatusTv.setText("Download deleted");
            mButton.setText("Deleted");
            break;
        }
        
        size += Formatter.formatFileSize(this, downloadData.getTotalSize());
        mSizeTv.setText(size);
    }
    

}
