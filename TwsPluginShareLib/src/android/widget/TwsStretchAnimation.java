package android.widget;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class TwsStretchAnimation {

	private final static String TAG = "SizeChange";
	// interpolator
	private Interpolator mInterpolator;
	// need flexible view
	private View mView; 
	
	private int mFlexibleType;
	// current size
	private int mCurrSize; 
	// rae size
	private int mRawSize;
	// min size
	private int mMinSize;
	// max size
	private int mMaxSize;
	// animator finished flag
	private boolean isFinished = true;
	private TYPE mType = TYPE.vertical;
	// single frame time ms
	private final static int FRAMTIME = 20;
	// change view horizontal or vertical size
	public static enum TYPE {
		horizontal,
		vertical
	}
	// animator running time
	private int mDuration;
	// animator starting time
	private long mStartTime;
	private float mDurationReciprocal;
	// need change view increment
	private int mDSize;

	public TwsStretchAnimation(int maxSize, int minSize, TYPE type, int duration) {
		if (minSize >= maxSize) {
			throw new RuntimeException("view is maxsize not small minsize");
		}
		mMinSize = minSize;
		mMaxSize = maxSize;
		mType = type;
		mDuration = duration;
	}

	public void setInterpolator(Interpolator interpolator) {
		mInterpolator = interpolator;
	}

	public TYPE getmType() {
		return mType;
	}

	public boolean isFinished() {
		return isFinished;
	}

	public void setDuration(int duration) {
		mDuration = duration;
	}

	private void changeViewSize() {
		if (mView != null && mView.getVisibility() != View.GONE) {
			LayoutParams params = mView.getLayoutParams();
			if (mType == TYPE.vertical) {
				params.height = mCurrSize;
			} else if (mType == TYPE.horizontal) {
				params.width = mCurrSize;
			}
			mView.setLayoutParams(params);
//			Log.i(TAG, "CurrSize = " + mCurrSize + " Max=" + mMaxSize + " min="
//					+ mMinSize);
		}
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
//			Log.i(TAG, "handleMessage what = " + msg.what);
			switch (msg.what) {
			case 1:
				if(!computeViewSize()) {
					mHandler.sendEmptyMessageDelayed(1, FRAMTIME);
				}else {
					mHandler.sendEmptyMessageDelayed(2, FRAMTIME);
				}
				break;
			case 2:
				if (animationlistener != null) {
					animationlistener.animationEnd(mView, mFlexibleType);
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}

	};

	/**
	 * @return :true,show animator finished
	 */
	private boolean computeViewSize() {

		if (isFinished) {
			return isFinished;
		}
		int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);

		if (timePassed <= mDuration) {
			float x = timePassed * mDurationReciprocal;
			if (mInterpolator != null) {
				x = mInterpolator.getInterpolation(x);
			}
			mCurrSize = mRawSize + Math.round(x * mDSize);
//			Log.i(TAG, "x = " + x + "; mCurrSize = " + mCurrSize);
			changeViewSize();
		} else {
			mCurrSize = mRawSize + mDSize;
			changeViewSize();
			isFinished = true;
//			Log.i(TAG, "isFinished = " + isFinished + "; mCurrSize = " + mCurrSize);
		}
		return isFinished;
	}

	public void startAnimation(View view, int type) {
//		Log.i(TAG, "isFinished = " + isFinished + "; type = " + type);
		if (isFinished) {
			mFlexibleType = type;
			if (view != null) {
				mView = view;
			} else {
				Log.e(TAG, "view is empty");
				return;
			}
			isFinished = false;
			mDurationReciprocal = 1.0f / (float) mDuration;
			if (mType == TYPE.vertical) {
				mRawSize = mCurrSize = mView.getHeight();
			} else if (mType == TYPE.horizontal) {
				mRawSize = mCurrSize = mView.getWidth();
			}
//			Log.i(TAG, "mRawSize=" + mRawSize + "; mCurrSize = " + mCurrSize 
//					+ "; mMaxSize = " + mMaxSize
//					+ "; mMinSize = " + mMinSize);
			if (mCurrSize > mMaxSize || mCurrSize < mMinSize) {
				throw new RuntimeException(
						"View size dissatisfy currentViewSize > mMaxSize || currentViewSize < mMinSize");
			}
			// starting animator time
			mStartTime = AnimationUtils.currentAnimationTimeMillis();
			if (mCurrSize < mMaxSize) {
				mDSize = mMaxSize - mCurrSize;
			} else {
				mDSize = mMinSize - mMaxSize;
			}
//			Log.i(TAG, "mDSize=" + mDSize);
			mHandler.sendEmptyMessage(1);
		}
	}

	private AnimationListener animationlistener;

	interface AnimationListener {
		public void animationEnd(View v, int type);
	}

	public void setOnAnimationListener(AnimationListener listener) {
		animationlistener = listener;
	}
}
