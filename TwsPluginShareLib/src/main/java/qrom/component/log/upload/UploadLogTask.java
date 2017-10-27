package qrom.component.log.upload;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import qrom.component.log.QRomLog;
import qrom.component.log.impl.QRomLogUtils;

public class UploadLogTask extends AsyncTask<String, Integer, String> {
    private static final String TAG = "UploadLogTask";

    private static final int ZIP_FILE_PROCESS = 30;
    private static final int ZIP_EXTRADATA_PROCESS = 5;
    
    private static final int UPLOAD_BUSSINFO_PROCESS = 10;
    private static final int DEL_FILE_PROCESS = 10;
    /** 正式环境地址 */
    private final String LOG_DEFAULT_SERVICE = "http://wwgv.html5.qq.com/upload";
//    private final String LOG_DEFAULT_SERVICE = "http://showlog.cs0309.html5.qq.com/upload";
    /** 测试环境地址 */
    private final String LOG_TEST_SERVICE = "http://showlog.cs0309.html5.qq.com/upload";
    
    private String CONTENT_DISPOSITION_HEADER = "Content-Disposition: form-data; name=\"" ;    
    private String CONTENT_TYPE_HEADER = "Content-Type:application/octet-stream" ;
    
    private String QUOT = "\"" ;
    private String ITEM_SPERATOR = "|";
    private String PARAM_SPERATOR = ";";
    private String LINE_END = "\r\n";        
    private String LINE_START_HYPHENS = "--";
    
    private String RSP_OK ="OK";
    private String RSP_FAILS="";
    
    private int TASK_STAT_RUNNING = 1;
    private int TASK_STAT_CANCEL = -1;
    
    /** 缓冲区大小 */
    private int BUFF_SIZE = 2 * 1024;
    
    /** 请求url地址 */
    private String mReqUrl = null;
    
    /** app 相关基础信息 */
    private AppRomBaseInfo mBaseInfo;
    /** app 上报日志的业务信息 */
    private AppBussInfo mBussInfo;
    /** 日志传输状态监听*/
    private ILogTransferStatusListener mLogTransferStatusListener;
    
    private String mBoundary = null;
    
    private int mTaskStat = 0;
    
    public UploadLogTask(AppBussInfo bussInfo, AppRomBaseInfo appRomBaseInfo) {
        this(null, bussInfo, appRomBaseInfo);
    }
    
    public UploadLogTask(String reqUrl, AppBussInfo bussInfo, AppRomBaseInfo baseInfo) {
        mReqUrl = reqUrl;
        mBussInfo = bussInfo;
        mBaseInfo = baseInfo;
    }
    
    public void setLogTransferStatusListener(ILogTransferStatusListener listener) {
        mLogTransferStatusListener = listener;
        
    }
    
   public void executeTask(String...strings ) {
       if (mBussInfo != null) {
           mBussInfo.mRunState = 1;
       }
       this.execute(strings);
   }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mTaskStat = TASK_STAT_RUNNING;
        try {
            if ( mBussInfo != null && isNeedUiCallback() &&mBussInfo.mUiCallBack != null) {
                mBussInfo.mUiCallBack.onUploadStarted(mBussInfo.mResId);
            }
        } catch (Throwable e) {
            QRomLog.w(TAG, "onPreExecute -> e: " + e + ", err msg: " + e.getMessage());
        }
    }

    @Override
    protected String doInBackground(String... params) {
        String result = "";
        // 设置boundary
        mBoundary = "----uploadLogTask_" +System.currentTimeMillis();
     
        if (mLogTransferStatusListener != null) {
            mLogTransferStatusListener.onLogTransferStarted(mBussInfo);
        }
        
        // ----------------- 压缩指定log文件 -----------------
        String pkgName = params == null ? "" : params[0];
        File zipFile = zipUploadFile(pkgName);        
        // 上传zip文件
        publishProgress(ZIP_FILE_PROCESS);
        // ----------------- 文件压缩完成，开始上传-----------------
        
        HttpURLConnection connection = null;

        // 网络传输流
        DataOutputStream outputStream = null;
        // 上传文件流程
        FileInputStream fileInputStream = null;
        
        // 开始上传信息
        try {
            // 初始化连接
            connection = initConnection();
            
            if (connection == null) {
                // 删除zip文件
                if (zipFile != null) {
                    zipFile.delete();
                }

                publishProgress(100);
                return "initConnection-> fails";
            }
            
            outputStream = new DataOutputStream(connection.getOutputStream());
            
            // 上传baseInfo
            result = "上传 romInfo 失败";
            String baseInfo = getRomInfo();
            QRomLog.d(TAG, "doInBackground->baseInfo = " + baseInfo);
            uploadStr(outputStream, "rominfo", baseInfo);
            
            // 上传业务信息（错误码，错误信息等）
            result = "上传 bussinfo 失败";            
            String bussInfo = getBussInfo();
            QRomLog.d(TAG, "doInBackground->bussInfo = " + bussInfo);
            uploadStr(outputStream, "bussinfo", bussInfo);
            
            // 上传基础信息完成
            publishProgress(ZIP_FILE_PROCESS + UPLOAD_BUSSINFO_PROCESS);
            
            if (isTaskCancel()) { // 任务取消
                result = "日志上传任务取消";
            } else {                
                result = "上传 文件 失败";
                if (zipFile != null && zipFile.isFile() && zipFile.exists()) {
                    String fileName = zipFile.getAbsolutePath();
                    fileInputStream = new FileInputStream(new File(fileName));
                    result = uploadFile(outputStream, fileInputStream, "tromreport", fileName);
                }
            }
            
            // 写入传输完成结束符
            outputStream.writeBytes(LINE_START_HYPHENS);
            outputStream.writeBytes(mBoundary);
            outputStream.writeBytes(LINE_START_HYPHENS);
            outputStream.writeBytes(LINE_END);
            
            // 数据传输完成
            outputStream.flush();
            outputStream.close();
            outputStream = null;
            
            // 解析返回数据
            processResponse(connection);
        } catch (Exception e) {
            QRomLog.w(TAG, "doInBackground->上传失败, err: " + e + ", err msg: " + e.getMessage());
            if (isStrEmpty(result)) {
                result = RSP_FAILS;
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    QRomLog.w(TAG, "doInBackground->outputStream closed, err: " + e + ", err msg: " + e.getMessage());
                }
            }
            
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    QRomLog.w(TAG, "doInBackground->fileInputStream closed, err: " + e + ", err msg: " + e.getMessage());
                }
            }
        }        

        // 删除zip文件
        if (zipFile != null) {
            zipFile.delete();
        }

        publishProgress(100);

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        QRomLog.d(TAG, "onPostExecute->result = " + result);
        try {
            if ( mBussInfo != null && isNeedUiCallback() &&mBussInfo.mUiCallBack != null) {
                mBussInfo.mUiCallBack.onUploadEnd(mBussInfo.mResId, result);
            }
            
        } catch (Throwable e) {
            QRomLog.w(TAG, "onPostExecute -> e: " + e + ", err msg: " + e.getMessage());
        }
        if (mLogTransferStatusListener != null) {
            mLogTransferStatusListener.onLogTransferEnd(mBussInfo);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        QRomLog.d(TAG, "onProgressUpdate->result = " + values[0] + ", resId = " + mBussInfo.mResId);
        try {
            if (isNeedUiCallback() && mBussInfo != null && mBussInfo.mUiCallBack != null) {
                QRomLog.v(TAG, "onProgressUpdate->onUploadProgressUpdated = " + values[0]);
                mBussInfo.mUiCallBack.onUploadProgressUpdated(mBussInfo.mResId, values[0]);
            }
        } catch (Throwable e) {
            QRomLog.w(TAG, "onProgressUpdate -> e: " + e + ", err msg: " + e.getMessage());
        }
    }
    
    public AppBussInfo getAppBussInfo() {
        return mBussInfo;
    }
    
    public void cancelTask() {
        mTaskStat = TASK_STAT_CANCEL;
//        cancel(mayInterruptIfRunning)
    }
    
    private boolean isNeedUiCallback() {
        if (mLogTransferStatusListener != null) {
            return mLogTransferStatusListener.isBussInfoValid(mBussInfo);
        }
        return false;
    }
    
    private boolean isTaskCancel() {
        return mTaskStat == TASK_STAT_CANCEL;
    }
    
    private File zipUploadFile(String pkgName) {
        
        String logFilePath = mBussInfo.mFilePath;
        String extraFilePath = mBussInfo.mExtraPath;
        byte[] extraDatas = mBussInfo.mExtraDatas;
        if (isStrEmpty(logFilePath) && isStrEmpty(extraFilePath) 
                && (extraDatas == null || extraDatas.length == 0)) {  // 不上传日志信息
            QRomLog.w(TAG, "zipUploadFile -> no file or data need upload, cancel zipfile");
            return null;
        }
        
        List<String> filePaths = new ArrayList<String>();
        File uploadLogFile = null;
        String zipFilePath = null;
        if (!isStrEmpty(logFilePath)) {  // 上传log文件的信息
            uploadLogFile = new File(logFilePath);            
        }
        
        // -------  开始处理压缩文件---------
        if (uploadLogFile != null && uploadLogFile.exists()) {            
            zipFilePath = uploadLogFile.getAbsolutePath();
            if (uploadLogFile.isDirectory()) {  // 上传指定log目录：是一个文件夹
                String[] fileList = uploadLogFile.list();
                if (fileList != null && fileList.length > 0) {
                    int filesCnt = fileList.length;
                    QRomLog.d(TAG, "doInBackground-> upload files cnt = " + filesCnt);
                    for (int i = 0; i < filesCnt; i++) {
                        if (isStrEmpty(fileList[i])) {
                            continue;
                        }
                        filePaths.add(zipFilePath + File.separator +fileList[i]);
                    } // ~ 根目录文件添加完成
                }
                // 指定目录下无日志文件, 指定默认压缩文件名               
                zipFilePath = zipFilePath + File.separator + pkgName + "_UploadDefault";
            } else { // 添加指定文件                  
                filePaths.add(logFilePath);
            }
        }  // ~ end 默认日志数据信息处理完成
        
        if (isStrEmpty(zipFilePath)) {  // 无默认日志文件信息
            // 设置默认压缩文件信息
            File logFileDir = QRomLogUtils.getLogFileDirectory(pkgName, false);
            if (logFileDir == null) {
                QRomLog.w(TAG, "zipUploadFile: logFileDir is null");
                return null;
            }
            zipFilePath = logFileDir.getAbsolutePath() 
                    + File.separator + pkgName+ "_UploadDefault";
        }
        QRomLog.i(TAG, "zipUploadFile-> zipFilePath = " + zipFilePath);
        // 压缩文件信息
        File zipFile = new File(zipFilePath + ".zip");
        if (!isStrEmpty(mBussInfo.mExtraPath)) {  // 添加额外文件
            filePaths.add(mBussInfo.mExtraPath);
        }
        // 压缩所有信息
        if (!zipFiles(zipFile, filePaths, mBussInfo.mExtraDatas)) {
            QRomLog.w(TAG, "doInBackground-> zip files fails");
        }
        
        return zipFile;
    }

    /**
     * 获取业务信息
     *    -- 格式BussName=TRomSync|ErrCode=1|ErrMsg="err"
     * @return
     */
    private String getBussInfo() {
        String bussName = null;
        int errCode = 0; 
        String errMsg = null;
//        int reportType = -1;
        
        if (mBussInfo != null) {
            bussName = mBussInfo.mBussName;
            errCode = mBussInfo.mErrCode;
            errMsg = mBussInfo.mErrMsg;
//            reportType = mBussInfo.mReportType;
        }        
        
        Map<String, String> bussInfo = new HashMap<String, String>(3);
        
        if (isStrEmpty(bussName)) {
            bussName = "default";
        }
        bussInfo.put("BussName", bussName);
        bussInfo.put("ErrCode", String.valueOf(errCode));
        bussInfo.put("ErrMsg", errMsg); 
        // 改用不同业务名处理
//        bussInfo.put("ReportType", String.valueOf(reportType)); 
        
        return getItemFormatValues(bussInfo);
    }
    
    /**
     * 获取app相关基础信息
     * GUID=xxx|QUA=base64(xxx)|IMEI=xxx|LC=xx|RomId=xxx|Package=xxx
     * @return
     */
    private String getRomInfo() throws Exception {
        
        Map<String, String> infos = new HashMap<String, String>(6);
        if (mBaseInfo == null) {
            mBaseInfo = new AppRomBaseInfo();
        }
        String qua = URLEncoder.encode(mBaseInfo.mQua, "UTF-8");
        byte[] guid = mBaseInfo.mGuid;
        if (guid == null) {
            guid = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        }
        infos.put("GUID", QRomLogUtils.byteToHexString(guid));
        infos.put("QUA", qua);
        infos.put("IMEI", mBaseInfo.mImei); 
        infos.put("LC", mBaseInfo.mLc); 
        infos.put("RomId", String.valueOf(mBaseInfo.mRomId)); 
        infos.put("Package", mBaseInfo.mPkgName); 
        infos.put("Ticket", mBaseInfo.mTicket); 
        
        return getItemFormatValues(infos);
    }
    
    /**
     * 获取指定格式的字符串信息
     *    -- key=value|key=value|...
     * @param items
     * @return
     */
    private String getItemFormatValues(Map<String, String> items) {
        
        if (items == null || items.isEmpty()) {
            return "";
        }
        String key = null;
        String value = null;
        StringBuffer buffer = new StringBuffer();
        
        for (Entry<String, String> entry : items.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if (isStrEmpty(key)) {
                continue;
            }
            if (value == null) {
                value = "na";
            }
            value = value.replace("&", "_");
            value = value.replace("|", "#");
            buffer.append(key);
            buffer.append("=");
            buffer.append(value);
            buffer.append(ITEM_SPERATOR);
        }
        // 去掉最后一个分割符
        return buffer.substring(0, buffer.length() - 1);
    }
    
    private HttpURLConnection initConnection() {
        HttpURLConnection connection = null;
        URL url = null;
        try {
//            String boundary = "----uploadLogTask_" +System.currentTimeMillis();
            
            if (isStrEmpty(mReqUrl) && mBaseInfo.isTestFlg()) { // 测试环境
                mReqUrl = LOG_TEST_SERVICE;
            }
            
            if (isStrEmpty(mReqUrl)) {  // 默认正式环境
                mReqUrl = LOG_DEFAULT_SERVICE;
            }
            QRomLog.i(TAG, "initConnection-> reqUrl = " + mReqUrl +", envFlg = " + mBaseInfo.mEnvFlg + ", boundary = " + mBoundary);
            
            url = new URL(mReqUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            //connection.setChunkedStreamingMode(0);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + mBoundary);
        } catch (IOException e) {
            QRomLog.w(TAG, "initConnection-> err: " + e + ", err msg: " + e.getMessage());
            connection = null;
            url = null;
        }
        
        return connection;
    }
    
    private String uploadStr(DataOutputStream outputStream, String reqFunc, String data) throws Exception{
        // 写入开始分割连接符
        outputStream.writeBytes(LINE_START_HYPHENS);
        outputStream.writeBytes(mBoundary);
        outputStream.writeBytes(LINE_END);
        
        // 写入content disposition
        outputStream.writeBytes(CONTENT_DISPOSITION_HEADER);
        outputStream.writeBytes(reqFunc);
        outputStream.writeBytes(QUOT);
        
        outputStream.writeBytes(LINE_END);
        outputStream.writeBytes(LINE_END);
        byte[] dataBytes = data.getBytes("UTF-8");
        // 写入数据
        outputStream.write(dataBytes);
        
        
        outputStream.writeBytes(LINE_END);
        return RSP_OK;
    }
    
//    private String uploadBytes(DataOutputStream outputStream, String reqFunc, byte[] datas) throws Exception{
//                
//        // 写入开始分割连接符
//        outputStream.writeBytes(LINE_START_HYPHENS);
//        outputStream.writeBytes(mBoundary);
//        outputStream.writeBytes(LINE_END);
//        
//        // 写入content disposition
//        outputStream.writeBytes(CONTENT_DISPOSITION_HEADER);
//        outputStream.writeBytes(reqFunc);
//        outputStream.writeBytes(QUOT);
//        
//        outputStream.writeBytes(LINE_END);
//        outputStream.writeBytes(LINE_END);
//        // 写入数据
//        outputStream.write(datas);
//        
//        outputStream.writeBytes(LINE_END);
//        return RSP_OK;
//    }
    
    /**
     * 上传 压缩好了的文件
     * @param outputStream
     * @param reqFunc
     * @param fileName
     * @return
     * @throws Exception
     */
    private String uploadFile(DataOutputStream outputStream, FileInputStream fileInputStream, String reqFunc, String fileName) throws Exception{
        String response = "error";
        String uploadFileName = fileName;
        
        QRomLog.d(TAG, "uploadFile-> filename: "+fileName + ", reqUrl:"+mReqUrl);
        
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer = null;
        int maxBufferSize = BUFF_SIZE;
        
        // 写入开始分割连接符
        outputStream.writeBytes(LINE_START_HYPHENS);
        outputStream.writeBytes(mBoundary);
        outputStream.writeBytes(LINE_END);
        
        // 写入content disposition
        outputStream.writeBytes(CONTENT_DISPOSITION_HEADER);
        outputStream.writeBytes(reqFunc);
        outputStream.writeBytes(QUOT);
        outputStream.writeBytes(PARAM_SPERATOR);
        // 写入文件名参数
        outputStream.writeBytes("filename=");
        outputStream.writeBytes(QUOT);
        outputStream.writeBytes(uploadFileName);
        outputStream.writeBytes(QUOT);
        outputStream.writeBytes(LINE_END);
        
        // 写入文件类型Content-Type:
        outputStream.writeBytes(CONTENT_TYPE_HEADER);
        outputStream.writeBytes(LINE_END);
        outputStream.writeBytes(LINE_END);
        
        // 开始写文件
        bytesAvailable = fileInputStream.available();
        // 设置缓冲区大小
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        QRomLog.d(TAG, "upload file length:" + bytesAvailable);
        
        int fileLength = bytesAvailable;
        int sendSize = bufferSize;
        while (bytesRead > 0) {            
            if (isTaskCancel()) { // 任务取消
                response = "文件上传取消";
                break;
            }
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            
            // 更新上传文件进度
            int process = Math.round(((((float) sendSize / fileLength)
                            * (100 - ZIP_FILE_PROCESS - DEL_FILE_PROCESS - UPLOAD_BUSSINFO_PROCESS) / 100)) * 100);
            publishProgress(process + ZIP_FILE_PROCESS + UPLOAD_BUSSINFO_PROCESS);
            sendSize += bufferSize;
        }  // ~ end upload zip file
        
        outputStream.writeBytes(LINE_END);
        response = "上传成功";
        return response;
    }
    
    /**
     * 处理返回数据
     * @param connection
     * @return
     */
    private String processResponse(HttpURLConnection connection) {
        
        String response = null;        
        try {
            QRomLog.d(TAG, "processResponse-> start receiver rsp");
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            QRomLog.d(TAG, "processResponse-> Server Response Code:" + serverResponseCode 
                    + ", Server Response Message:" + serverResponseMessage);
            
            InputStream is = connection.getInputStream();
            int ch;
            StringBuffer strBuffer = new StringBuffer();
            
            while ((ch = is.read()) != -1) {
                strBuffer.append((char) ch);
            }
            String responseString = strBuffer.toString();
            QRomLog.d(TAG, "response string is" + responseString);
            if (serverResponseCode == 200) {//HttpStatus.SC_OK
                response = "成功";
            } else {
                response = "失败，请检查网络重试";
            }
            
        } catch (MalformedURLException ex) {
            response = "失败，请检查网络重试";
            QRomLog.w(TAG, "Upload file to server, error: " + ex + ", err msg" + ex.getMessage());
        } catch (Exception e) {
            response = "失败，请检查网络重试";
            QRomLog.w(TAG,  e);
        }
        return response;
    }


    /**
     * 仅压缩指定文件，不压缩文件夹
     * @param zipFile         压缩文件信息
     * @param filePaths     需要入的文件列表
     * @param extraDatas  压缩的额外信息
     * @return
     */
    private boolean zipFiles(File zipFile, List<String> filePaths, byte[] extraDatas) {
        
        int cnt = filePaths == null ? 0 : filePaths.size();
        if (cnt == 0 && extraDatas == null) {
            QRomLog.w(TAG, "zipFiles-> filePaths and extraData is empty");
            return false;
        }        
        // 文件个数
        QRomLog.i(TAG, "zipFiles-> fileCnt = " + cnt);
        String filePath = null;
        File fileInfo = null;
        byte buffer[] = new byte[BUFF_SIZE];
        int readFileLen;
        
        ZipOutputStream zipout = null;
        ZipEntry zipEntry = null;
        BufferedInputStream fileIos = null;
       
        try {
            //删除之前的压缩文件
            if (zipFile.exists()) {
                zipFile.delete();
            }
            if (zipFile.getParentFile() != null && !zipFile.getParentFile().exists()) {
                zipFile.mkdirs();
            }
            zipout = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(zipFile), BUFF_SIZE));
            int zipFipAllProcess = ZIP_FILE_PROCESS - ZIP_EXTRADATA_PROCESS;
            for (int i = 0; i < cnt; i++) {  // 遍历所有文件
                filePath = filePaths.get(i);
                if (isStrEmpty(filePath)) {
                    continue;
                }
                fileInfo = new File(filePath);
                if (!fileInfo.exists() || fileInfo.isDirectory()) {
                    QRomLog.i(TAG, "zipFiles-> file is not exist or is directory: " + filePath);
                    continue;
                }
                
                if (fileInfo.getAbsolutePath().equals(zipFile.getAbsolutePath())) {
                    QRomLog.i(TAG, "zipFiles->  file is zipFile, ignore !!!");
                    continue;
                }

                // 开始压缩文件
                zipEntry = new ZipEntry(fileInfo.getName());
                zipout.putNextEntry(zipEntry);
                QRomLog.i(TAG, "zipFiles-> file: " + fileInfo.getAbsolutePath());
                fileIos = new BufferedInputStream(new FileInputStream(fileInfo), BUFF_SIZE);
                while ((readFileLen = fileIos.read(buffer)) != -1) {
                    // 将文件写入zip流
                    zipout.write(buffer, 0, readFileLen);
                    
                    // 更新压缩文件进度
//                readLen += realLength;
//                int process = Math.round(((float)readLen/fileLen * ZIP_FILE_PROCESS /100) * 100);
//                publishProgress(process);
                }  // ~ end while 一个文件压缩完成
                publishProgress(Math.round((float)((i + 1)  / cnt) * zipFipAllProcess));
                // 关闭文件流
                fileIos.close();
                fileIos = null;
                        
                zipout.flush();        
                zipout.closeEntry();
            }  // ~ enf for 所有文件压缩完成
     
            // 添加额外数据
            // 开始压缩文件
            if (extraDatas != null && extraDatas.length > 0) {
                zipEntry = new ZipEntry("extraData");
                zipout.putNextEntry(zipEntry);
                zipout.write(extraDatas);
            }
            publishProgress(ZIP_FILE_PROCESS);
            zipout.close();
            zipout = null;
            return true;
        } catch (Exception e) {
            QRomLog.w(TAG, "zipFiles -> e: " + e + ", err msg: " + e.getMessage());
        } finally {
            if (fileIos != null) {
                try {
                    fileIos.close();
                } catch (Exception e) {
                    QRomLog.w(TAG, "zipFiles -> fileios close e: " + e + ", err msg: " + e.getMessage());
                }
            }            
            if (zipout != null) {
                try {
                    zipout.close();
                } catch (Exception e) {
                    QRomLog.w(TAG, "zipFiles -> zipout close e: " + e + ", err msg: " + e.getMessage());
                }
            }
        }
        
        return false;
    }
    
//    public void zipFiles(File resFile, File zipFile) throws IOException {
//        ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), 1024));
//
//        zipFile(resFile, zipout);
//
//        zipout.close();
//    }
//
//    private void zipFile(File resFile, ZipOutputStream zipout) throws FileNotFoundException, IOException {
//
//        byte buffer[] = new byte[BUFF_SIZE];
//
//        BufferedInputStream in = new BufferedInputStream(new FileInputStream(resFile), BUFF_SIZE);
//
//        ZipEntry zipEntry = new ZipEntry(resFile.getName());
//        zipout.putNextEntry(zipEntry);
//
//        QRomLog.d(TAG, "zipFile :" + resFile.getName() + ", size:" + resFile.length());
//
//        int readLen = 0;
//        long fileLen = resFile.length();
//        
//        int realLength;
//        while ((realLength = in.read(buffer)) != -1) {
//            zipout.write(buffer, 0, realLength);
//            
//            // 更新压缩文件进度
//            readLen += realLength;
//            int process = Math.round(((float)readLen/fileLen * ZIP_FILE_PROCESS /100) * 100);
//            publishProgress(process);
//        }
//
//        in.close();
//        zipout.flush();
//        zipout.closeEntry();
//    }
    
    private boolean isStrEmpty(String str) {
        return str == null || "".equals(str);
    }
    
    /**
     * 上传状态回调
     * @author sukeyli
     *
     */
    public static interface ILogTransferStatusListener {
        /**
         * 开始传输日志
         */
        void onLogTransferStarted(AppBussInfo appBussInfo);
        /**
         * 日志传输完成 
         */
        void onLogTransferEnd(AppBussInfo appBussInfo);
        
        boolean isBussInfoValid(AppBussInfo appBussInfo);
    }
}
