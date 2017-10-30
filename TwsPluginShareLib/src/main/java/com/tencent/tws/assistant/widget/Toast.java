/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tws.assistant.widget;

import android.annotation.SuppressLint;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import qrom.component.log.QRomLog;

import com.tencent.tws.sharelib.R;

@SuppressLint("NewApi")
public class Toast extends android.widget.Toast {
	static final String TAG = "Toast";
	static final boolean localLOGV = false;

	private static boolean sAllowfloating = ("Meizu".equalsIgnoreCase(Build.BRAND) || "Xiaomi"
			.equalsIgnoreCase(Build.BRAND)) ? false : true;

	final Context mContext;
	final TNEx mTN;
	int mDuration;
	View mNextView;

	public Toast(Context context) {
		super(context);
		mContext = context;
		mTN = new TNEx(0);
		mTN.mY = context.getResources().getDimensionPixelSize(R.dimen.toast_y_offset);
		mTN.mGravity = context.getResources().getInteger(R.integer.config_toastDefaultGravity);
	}

	@Override
	public void show() {
		if (mNextView == null) {
			throw new RuntimeException("setView must have been called");
		}

		INotificationManager service = getService();
		String pkg = mContext.getPackageName();
		TNEx tn = mTN;
		tn.mNextView = mNextView;

		try {
			service.enqueueToast(pkg, tn, mDuration);
		} catch (RemoteException e) {
			// Empty
		}
	}

	@Override
	public void cancel() {
		mTN.hide();

		try {
			getService().cancelToast(mContext.getPackageName(), mTN);
		} catch (RemoteException e) {
			// Empty
		}
	}

	@Override
	public void setView(View view) {
		mNextView = view;
	}

	@Override
	public View getView() {
		return mNextView;
	}

	@Override
	public void setDuration(int duration) {
		mDuration = duration;
	}

	@Override
	public int getDuration() {
		return mDuration;
	}

	@Override
	public void setMargin(float horizontalMargin, float verticalMargin) {
		mTN.mHorizontalMargin = horizontalMargin;
		mTN.mVerticalMargin = verticalMargin;
	}

	@Override
	public float getHorizontalMargin() {
		return mTN.mHorizontalMargin;
	}

	@Override
	public float getVerticalMargin() {
		return mTN.mVerticalMargin;
	}

	@Override
	public void setGravity(int gravity, int xOffset, int yOffset) {
		mTN.mGravity = gravity;
		mTN.mX = xOffset;
		mTN.mY = yOffset;
	}

	@Override
	public int getGravity() {
		return mTN.mGravity;
	}

	@Override
	public int getXOffset() {
		return mTN.mX;
	}

	@Override
	public int getYOffset() {
		return mTN.mY;
	}

	public static android.widget.Toast makeText(Context context, CharSequence text, int duration) {
		return makeText(context, text, duration, 0);
	}

	public static android.widget.Toast makeText(Context context, CharSequence text, int duration, int onTop) {
		LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflate.inflate(R.layout.transient_notification, null);
		TextView tv = (TextView) v.findViewById(android.R.id.message);
		tv.setText(text);

		Log.d(TAG, "sAllowfloating is " + sAllowfloating);
		if (!sAllowfloating) {
			android.widget.Toast result = android.widget.Toast.makeText(context, text, duration);
			result.setView(v);
			return result;
		} else {
			Toast result = new Toast(context);
			result.mNextView = v;
			result.mDuration = duration;
			return result;
		}
	}

	public static android.widget.Toast makeText(Context context, int resId, int duration)
			throws Resources.NotFoundException {
		return makeText(context, context.getResources().getText(resId), duration);
	}

	@Override
	public void setText(int resId) {
		setText(mContext.getText(resId));
	}

	@Override
	public void setText(CharSequence s) {
		if (mNextView == null) {
			throw new RuntimeException("This Toast was not created with Toast.makeText()");
		}
		TextView tv = (TextView) mNextView.findViewById(android.R.id.message);
		if (tv == null) {
			throw new RuntimeException("This Toast was not created with Toast.makeText()");
		}
		tv.setText(s);
	}

	// =======================================================================================
	// All the gunk below is the interaction with the Notification Service,
	// which handles
	// the proper ordering of these system-wide.
	// =======================================================================================

	private static INotificationManager sService;

	static private INotificationManager getService() {
		if (sService != null) {
			return sService;
		}
		sService = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
		return sService;
	}

	private static class TNEx extends ITransientNotification.Stub {
		final Runnable mShow = new Runnable() {
			@Override
			public void run() {
				handleShow();
			}
		};

		final Runnable mHide = new Runnable() {
			@Override
			public void run() {
				handleHide();
				// Don't do this in handleHide() because it is also invoked by
				// handleShow()
				mNextView = null;
			}
		};

		private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
//		final Handler mHandler = new Handler();
		
		final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
               IBinder token = (IBinder) msg.obj;
                handleShow(token);
           }
       };

		int mGravity;
		int mX, mY;
		float mHorizontalMargin;
		float mVerticalMargin;
		// int mOnTop;

		View mView;
		View mNextView;

		WindowManager mWM;

		TNEx(int onTop) {
			final WindowManager.LayoutParams params = mParams;
			params.height = WindowManager.LayoutParams.WRAP_CONTENT;
			params.width = WindowManager.LayoutParams.WRAP_CONTENT;
			params.format = PixelFormat.TRANSLUCENT;
			params.windowAnimations = R.style.Animation_Toast_tws;
			// mOnTop = onTop;
			if (onTop == 0) {
				params.type = WindowManager.LayoutParams.TYPE_TOAST;
			} else {
				params.type = WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY;
			}

			params.setTitle("Toast");
			params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		}

		/**
		 * schedule handleShow into the right thread
		 */
		@Override
		public void show() {
			QRomLog.v(TAG, "SHOW: " + this);
			mHandler.post(mShow);
		}
		
		//android 7.1 used benylwang add
		public void show(IBinder windowToken) {
            mHandler.obtainMessage(0, windowToken).sendToTarget();
        }

		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			QRomLog.d(TAG, Build.VERSION.SDK_INT + " onTransact code = " + code);
			
			if (code == 1) {
				data.enforceInterface("android.app.ITransientNotification");
				if (Build.VERSION.SDK_INT >= 25) {
					android.os.IBinder _arg0;
					_arg0 = data.readStrongBinder();
					this.show(_arg0);
				} else {
					show();
				}
				reply.writeNoException();
				return true;
			} else {
				return super.onTransact(code, data, reply, flags);
			}
		}

		/**
		 * schedule handleHide into the right thread
		 */
		@Override
		public void hide() {
			if (localLOGV)
				Log.v(TAG, "HIDE: " + this);
			mHandler.post(mHide);
		}

		public void handleShow() {
			if (localLOGV)
				Log.v(TAG, "HANDLE SHOW: " + this + " mView=" + mView + " mNextView=" + mNextView);
			if (mView != mNextView) {
				// remove the old view if necessary
				handleHide();
				mView = mNextView;
				Context context = mView.getContext().getApplicationContext();
				if (context == null) {
					context = mView.getContext();
				}
				mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
				// We can resolve the Gravity here by using the Locale for
				// getting
				// the layout direction
				int gravity;
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
					final Configuration config = mView.getContext().getResources().getConfiguration();
					// final int gravity = Gravity.getAbsoluteGravity(mGravity,
					// config.getLayoutDirection());
					gravity = Gravity.getAbsoluteGravity(mGravity, config.getLayoutDirection());
				} else {
					gravity = mGravity;
				}
				mParams.gravity = gravity;
				if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
					mParams.horizontalWeight = 1.0f;
				}
				if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
					mParams.verticalWeight = 1.0f;
				}
				mParams.x = mX;
				mParams.y = mY;
				mParams.verticalMargin = mVerticalMargin;
				mParams.horizontalMargin = mHorizontalMargin;
				if (mView.getParent() != null) {
					if (localLOGV)
						Log.v(TAG, "REMOVE! " + mView + " in " + this);
					mWM.removeView(mView);
				}
				if (localLOGV)
					Log.v(TAG, "ADD! " + mView + " in " + this);
				mWM.addView(mView, mParams);
				trySendAccessibilityEvent();
			}
		}
		
		public void handleShow(IBinder windowToken) {
            if (localLOGV) Log.v(TAG, "HANDLE SHOW: " + this + " mView=" + mView
                    + " mNextView=" + mNextView);
            if (mView != mNextView) {
                // remove the old view if necessary
                handleHide();
                mView = mNextView;
                Context context = mView.getContext().getApplicationContext();
                String packageName = mView.getContext().getOpPackageName();
                if (context == null) {
                    context = mView.getContext();
                }
                mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                // We can resolve the Gravity here by using the Locale for getting
                // the layout direction
                final Configuration config = mView.getContext().getResources().getConfiguration();
                final int gravity = Gravity.getAbsoluteGravity(mGravity, config.getLayoutDirection());
                mParams.gravity = gravity;
                if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
                    mParams.horizontalWeight = 1.0f;
                }
                if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
                    mParams.verticalWeight = 1.0f;
                }
                mParams.x = mX;
                mParams.y = mY;
                mParams.verticalMargin = mVerticalMargin;
                mParams.horizontalMargin = mHorizontalMargin;
                mParams.packageName = packageName;
//                mParams.hideTimeoutMilliseconds = mDuration ==
//                    Toast.LENGTH_LONG ? LONG_DURATION_TIMEOUT : SHORT_DURATION_TIMEOUT;
                mParams.token = windowToken;
                if (mView.getParent() != null) {
                    if (localLOGV) Log.v(TAG, "REMOVE! " + mView + " in " + this);
                    mWM.removeView(mView);
                }
                if (localLOGV) Log.v(TAG, "ADD! " + mView + " in " + this);
                mWM.addView(mView, mParams);
                trySendAccessibilityEvent();
            }
        }


		private void trySendAccessibilityEvent() {
			AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(mView.getContext());
			if (!accessibilityManager.isEnabled()) {
				return;
			}
			// treat toasts as notifications since they are used to
			// announce a transient piece of information to the user
			AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
			event.setClassName(getClass().getName());
			event.setPackageName(mView.getContext().getPackageName());
			mView.dispatchPopulateAccessibilityEvent(event);
			accessibilityManager.sendAccessibilityEvent(event);
		}

		public void handleHide() {
			if (localLOGV)
				Log.v(TAG, "HANDLE HIDE: " + this + " mView=" + mView);
			if (mView != null) {
				// note: checking parent() just to make sure the view has
				// been added... i have seen cases where we get here when
				// the view isn't yet added, so let's try not to crash.
				if (mView.getParent() != null) {
					if (localLOGV)
						Log.v(TAG, "REMOVE! " + mView + " in " + this);
					mWM.removeView(mView);
				}

				mView = null;
			}
		}
	}
}
