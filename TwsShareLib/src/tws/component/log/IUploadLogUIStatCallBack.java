package tws.component.log;

public interface IUploadLogUIStatCallBack {
    
    void onUploadStarted(int resId);
    
    void onUploadProgressUpdated(int resId, int progress);
    
    void onUploadEnd(int resId, String result);
}
