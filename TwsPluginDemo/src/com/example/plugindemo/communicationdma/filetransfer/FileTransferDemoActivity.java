package com.example.plugindemo.communicationdma.filetransfer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.example.plugindemo.R;
import com.tencent.tws.filetransfermanager.FileTransferManager;
import com.tencent.tws.filetransfermanager.listener.FileTransferListener;
import com.tencent.tws.filetransfermanager.model.Contant;
import com.tencent.tws.handler.WorkHandler;
import com.tencent.tws.util.SeqGenerator;

public class FileTransferDemoActivity extends Activity implements WorkHandler.Callback {
    
    private static final String TAG = "FileTransferDemoActivity";
    
    private static final int sWHAT_LOAD_SDCARD_LIST = 1;
    
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_QUEUING = 1;
    public static final int STATUS_TRANSFERING = 2;
    public static final int STATUS_CANCEL = 3;
    public static final int STATUS_ERROR = 4;
    public static final int STATUS_COMPLETE = 5;
    public static final int STATUS_PATH_INVALID = 6;
    
    private ListView mFileList;
    
    private boolean mScrollStateIdle;
    
    private FileTransferListAdapter mAdapter;
    
    private WorkHandler<WorkHandler.Callback> mWorkHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication_dma_filetransfer);
        
        mWorkHandler = WorkHandler.build(TAG, this);
        mWorkHandler.obtainMessage(sWHAT_LOAD_SDCARD_LIST).sendToTarget();
        // start loading SDCard file list.
        
        initView();

    }
    
    private FileTransferListener mFileTransferListener = new FileTransferListener(){

        @Override
        public void onTransferCancel(long id, int reason) {
            updateListViewItem(id, STATUS_CANCEL, 0, 0);
        }

        @Override
        public void onTransferComplete(long id, String filePath) {
            updateListViewItem(id, STATUS_COMPLETE, 100, 0);
        }

        @Override
        public void onTransferError(long id, String filePath, int errorCode) {
            updateListViewItem(id, STATUS_ERROR, 0, errorCode);
        }

        @Override
        public void onTransferProgress(long id, String filePath, long progress) {
            updateListViewItem(id, STATUS_TRANSFERING, (int) progress, 0);
        }
        
    };
    
    
    private void handleItemClick(SdCardFileInfo fileInfo) {
        int status = fileInfo.getStatus();
        switch (status) {
        case STATUS_QUEUING:
        case STATUS_TRANSFERING:
            FileTransferManager.getInstance().cancel(fileInfo.getId());
            break;
        case STATUS_IDLE:
        case STATUS_CANCEL:
        case STATUS_ERROR:
        case STATUS_COMPLETE:
            sendFile(fileInfo);
            break;
        default:
            break;
        }
    }
    
    /**
     * FileTransferManager.sendFile(long taskId, String filePath, String dstPath, boolean autoTransferWhenConnected, float timesOfFileSizeRequired)
     * <li> taskId 请填一个任务id
     * <li> filePath 源路径。
     * <li> dstPath 目标路径。如果为null，则传输到默认路径（/sdcard/twsReceived/）
     * <li> autoTransferWhenConnected 正在传输的任务蓝牙断开后停止，下次蓝牙连接成功后是否自动续传。默认为true
     * <li> timesOfFileSizeRequired 发送文件，对端需要的文件大小是 要传输的文件大小的几倍。float类型，不能小于1。默认1.5
     */
    private void sendFile(SdCardFileInfo fileInfo) {
        TwsLog.v(TAG, "sendFile fileInfo: " + fileInfo.toString());
        String fileAbsolutePath = fileInfo.getFile().getAbsolutePath();
        long id = SeqGenerator.getInstance().genSeq();
        fileInfo.setId(id);
        int result = FileTransferManager.getInstance().sendFile(id, fileAbsolutePath, null, true, 1.1F);
        if (result == Contant.INVALID_FILE_PATH) {
            updateListViewItem(id, STATUS_PATH_INVALID, 0, 0);
        } else {
            updateListViewItem(id, STATUS_QUEUING, 0, 0);
        }
    }

    @Override
    protected void onStart() {
        FileTransferManager.getInstance().setFileTransferListener(mFileTransferListener);
        super.onStart();
    }
    
    @Override
    protected void onStop() {
        FileTransferManager.getInstance().unRegisterTransferListener(mFileTransferListener);
        super.onStop();
    }
    
    private void updateListViewItem(final long id, final int state, final int progress, final int errorCode) {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                mAdapter.updateListViewItem(id, state, progress, errorCode);
            }
        });
    }
    
    private void initView() {
        mFileList = (ListView) findViewById(R.id.lv_file_list);
        mFileList.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mScrollStateIdle = (scrollState == SCROLL_STATE_IDLE);
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        
        mFileList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SdCardFileInfo fileInfo = (SdCardFileInfo) ((ListView)parent).getItemAtPosition(position);
                if (fileInfo == null) {
                    TwsLog.w(TAG, "onItemClick position: " + position + ", fileInfo is NULL");
                    return;
                }
                handleItemClick(fileInfo);
            }
        });
    }

    @Override
    public void handleWorkMessage(Message msg) {
        switch (msg.what) {
        case sWHAT_LOAD_SDCARD_LIST:
            setFileListToAdapter(loadSdcardList());
            break;
        }
    }

    private List<SdCardFileInfo> loadSdcardList() {
        File sdCardFile = Environment.getExternalStorageDirectory();
        final File[] listFiles = sdCardFile.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                TwsLog.v(TAG, "loadSdcardList(): " + dir.getPath() + File.separator + filename);
                if (filename.startsWith(".")) {
                    return false;
                }
                File file = new File(dir, filename);
                return file.isFile();
            }
        });
        if (listFiles == null) {
            return null;
        }
        List<SdCardFileInfo> fileList = new ArrayList<SdCardFileInfo>();
        for (int i = 0; i < listFiles.length; i++) {
            SdCardFileInfo info = new SdCardFileInfo(listFiles[i]);
            TwsLog.i(TAG, "loadSdcardList() AbsolutePath: " + info.getFile().getAbsolutePath());
            fileList.add(info);
        }
        return fileList;
    }
    
    private void setFileListToAdapter(final List<SdCardFileInfo> fileList) {
        // stop loading SDCard file list.
        if (fileList == null || fileList.isEmpty()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter = new FileTransferListAdapter(FileTransferDemoActivity.this, fileList);
                mFileList.setAdapter(mAdapter);
            }
        });
    }
}



