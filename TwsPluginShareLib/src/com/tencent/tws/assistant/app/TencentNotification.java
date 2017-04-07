package com.tencent.tws.assistant.app;

import android.app.Notification;
import android.os.Parcel;
import android.os.Parcelable;

public class TencentNotification extends Notification{
	
	public static final int TENCENT_NOTI_TYPE_ADD = 0;
	public static final int TENCENT_NOTI_TYPE_UPDATE = 1;
	public static final int TENCENT_NOTI_TYPE_DELETE = 2;
	
	public int mNotificationGroupId = 0;
	public Notification mNotification;
	public int mNotificationType = 0;
	
	private static boolean M_IS_IN_FRAMEWORK = true; 
	
	public static boolean isInFramework() {
		return M_IS_IN_FRAMEWORK;
	}
	
	public TencentNotification(Notification n, int notificationId, int type) {
		mNotification = n;
		mNotificationGroupId = notificationId;
		mNotificationType = type;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub
		super.writeToParcel(arg0, arg1);
		if (mNotification == null) {
			mNotification = new Notification();
		}
		mNotification.writeToParcel(arg0, arg1);
		arg0.writeInt(mNotificationGroupId);
		arg0.writeInt(mNotificationType);
	}
	
	public static final Parcelable.Creator<TencentNotification> CREATOR = new Creator<TencentNotification>()  {      
		@Override    
		public TencentNotification createFromParcel(Parcel source) {    
			return new TencentNotification(source);    
		} 
		@Override    
		public TencentNotification[] newArray(int size) {    
			return new TencentNotification[size];    
		}    
	};  
	private TencentNotification(Parcel dest) {    
		super(dest);
		mNotification = new Notification(dest);
		mNotificationGroupId = dest.readInt();
		mNotificationType = dest.readInt();
	}    


}
