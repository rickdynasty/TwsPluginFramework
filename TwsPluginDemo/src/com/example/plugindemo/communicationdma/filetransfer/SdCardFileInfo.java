package com.example.plugindemo.communicationdma.filetransfer;

import java.io.File;

public class SdCardFileInfo {

    private long mId;

    private File mFile;

    private int mStatus;

    private int mErrorCode;

    private float mProgress;
    
    public SdCardFileInfo(File file) {
        mFile = file;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int staus) {
        mStatus = staus;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = progress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mId: " + mId);
        sb.append(", mFile: " + mFile);
        sb.append(", mStatus: " + mStatus);
        sb.append(", mErrorCode: " + mErrorCode);
        sb.append(", mProgress: " + mProgress);
        return sb.toString();
    }

}
