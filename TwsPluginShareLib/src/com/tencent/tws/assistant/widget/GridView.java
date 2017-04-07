package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.lang.reflect.Field;

import android.widget.ScrollBarDrawable;

import com.tencent.tws.assistant.drawable.TwsScrollBarDrawable;
import com.tencent.tws.assistant.gaussblur.JNIBlur;
import com.tencent.tws.assistant.gaussblur.NativeBlurProcess;
import com.tencent.tws.assistant.utils.ReflectUtils;
import com.tencent.tws.sharelib.R;

public class GridView extends android.widget.GridView {

    /**
     * A class that represents a fixed view in a list, for example a header at the top
     * or a footer at the bottom.
     */
    private static class FixedViewInfo {
        /**
         * The view to add to the grid
         */
        public View view;
        public ViewGroup viewContainer;
        /**
         * The data backing the view. This is returned from {@link ListAdapter#getItem(int)}.
         */
        public Object data;
        /**
         * <code>true</code> if the fixed view should be selectable in the grid
         */
        public boolean isSelectable;
    }

    private ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<FixedViewInfo>();

    private ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<FixedViewInfo>();

    private int mRequestedNumColumns;

    private int mNumColmuns = 1;
    
  	private int mBlurBottomHeight, mBlurTopHeight;
  	private Paint mBlurPaint;
  	private Rect mBlurForBottomRect, mBlurForTopRect, mForContentRect;
  	private Bitmap mForBottomBitmap, mForTopBitmap;
  	private Canvas mForBottomCanvas, mForTopCanvas;
  	private View mTopBlurView, mBottomBlurView;
  	private JNIBlur mBlur;
  	private Drawable mBlurTopDrawable, mBlurBottomDrawable;
  	
    private void initHeaderGridView() {
        super.setClipChildren(false);
    }

    public GridView(Context context) {
        super(context);
        initHeaderGridView();
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeaderGridView();
    }

    public GridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initHeaderGridView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mRequestedNumColumns != AUTO_FIT) {
            mNumColmuns = mRequestedNumColumns;
        }
        if (mNumColmuns <= 0) {
            mNumColmuns = 1;
        }

        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderFooterViewGridAdapter) {
            ((HeaderFooterViewGridAdapter) adapter).setNumColumns(getNumColumns());
        }
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        // Ignore, since the header rows depend on not being clipped
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so GridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v            The view to add.
     * @param data         Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();

        if (adapter != null && !(adapter instanceof HeaderFooterViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add header view to grid -- setAdapter has already been called.");
        }

        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);

        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderFooterViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so GridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     */
    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    /**
     * Add a fixed view to appear at the bottom of the grid. If addFooterView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so GridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v            The view to add.
     * @param data         Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addFooterView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();

        if (adapter != null && !(adapter instanceof HeaderFooterViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add footer view to grid -- setAdapter has already been called.");
        }

        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);

        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderFooterViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    /**
     * Add a fixed view to appear at the bottom of the grid. If addFooterView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so GridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     */
    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    public int getHeaderViewCount() {
        return mHeaderViewInfos.size();
    }

    public int getFooterViewCount() {
        return mFooterViewInfos.size();
    }

    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header view
     */
    public boolean removeHeaderView(View v) {
        if (mHeaderViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderFooterViewGridAdapter) adapter).removeHeader(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mHeaderViewInfos);
            return result;
        }
        return false;
    }

    /**
     * Removes a previously-added footer view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a footer view
     */
    public boolean removeFooterView(View v) {
        if (mFooterViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderFooterViewGridAdapter) adapter).removeFooter(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mFooterViewInfos);
            return result;
        }
        return false;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {

    	blurSetup();
    	
        if (mHeaderViewInfos.size() > 0 || mFooterViewInfos.size() > 0) {
            HeaderFooterViewGridAdapter hadapter = new HeaderFooterViewGridAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
            int numColumns = getNumColumns();
            if (numColumns > 1) {
                hadapter.setNumColumns(numColumns);
            }
            super.setAdapter(hadapter);
        } else {
            super.setAdapter(adapter);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	super.onSizeChanged(w, h, oldw, oldh);
    	if (w != oldw || h != oldh) {
			blurInit();
		}
    }

    private class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int targetWidth = GridView.this.getMeasuredWidth()
                    - GridView.this.getPaddingLeft()
                    - GridView.this.getPaddingRight();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
                    MeasureSpec.getMode(widthMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setNumColumns(int numColumns) {
        super.setNumColumns(numColumns);
        // Store specified value for less than Honeycomb.
        mRequestedNumColumns = numColumns;
    }

    @Override
    public int getNumColumns() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return super.getNumColumns();
        }

        // Return value for less than Honeycomb.
        return mNumColmuns;
    }

    /**
     * ListAdapter used when a GridView has header views. This ListAdapter
     * wraps another one and also keeps track of the header views and their
     * associated data objects.
     * <p>This is intended as a base class; you will probably not need to
     * use this class directly in your own code.
     */
    private static class HeaderFooterViewGridAdapter implements WrapperListAdapter, Filterable {

        // This is used to notify the container of updates relating to number of columns
        // or headers changing, which changes the number of placeholders needed
        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        private final ListAdapter mAdapter;
        private int mNumColumns = 1;

        // This ArrayList is assumed to NOT be null.
        ArrayList<FixedViewInfo> mHeaderViewInfos;

        ArrayList<FixedViewInfo> mFooterViewInfos;

        boolean mAreAllFixedViewsSelectable;

        private final boolean mIsFilterable;

        public HeaderFooterViewGridAdapter(ArrayList<FixedViewInfo> headerViewInfos, ArrayList<FixedViewInfo> footerViewInfos, ListAdapter adapter) {
            mAdapter = adapter;
            mIsFilterable = adapter instanceof Filterable;

            if (headerViewInfos == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            if (footerViewInfos == null) {
                throw new IllegalArgumentException("footerViewInfos cannot be null");
            }
            mHeaderViewInfos = headerViewInfos;
            mFooterViewInfos = footerViewInfos;

            mAreAllFixedViewsSelectable = (areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos));
        }

        public int getHeadersCount() {
            return mHeaderViewInfos.size();
        }

        public int getFootersCount() {
            return mFooterViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (mAdapter == null || mAdapter.isEmpty()) && getHeadersCount() == 0 && getFootersCount() == 0;
        }

        public void setNumColumns(int numColumns) {
            if (numColumns < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (mNumColumns != numColumns) {
                mNumColumns = numColumns;
                notifyDataSetChanged();
            }
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> infos) {
            if (infos != null) {
                for (FixedViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean removeHeader(View v) {
            for (int i = 0; i < mHeaderViewInfos.size(); i++) {
                FixedViewInfo info = mHeaderViewInfos.get(i);
                if (info.view == v) {
                    mHeaderViewInfos.remove(i);

                    mAreAllFixedViewsSelectable = (areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos));

                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }

            return false;
        }

        public boolean removeFooter(View v) {
            for (int i = 0; i < mFooterViewInfos.size(); i++) {
                FixedViewInfo info = mFooterViewInfos.get(i);
                if (info.view == v) {
                    mFooterViewInfos.remove(i);

                    mAreAllFixedViewsSelectable = (areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos));

                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }

            return false;
        }

        @Override
        public int getCount() {
            if (mAdapter != null) {
                return (getHeadersCount() * mNumColumns) + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns) + (getFootersCount() * mNumColumns);
            } else {
                return (getHeadersCount() * mNumColumns) + (getFootersCount() * mNumColumns);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (mAdapter != null) {
                return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                return (position % mNumColumns == 0)
                        && mHeaderViewInfos.get(position / mNumColumns).isSelectable;
            }

            // Adapter
            if (position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                final int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = 0;
                if (mAdapter != null) {
                    adapterCount = mAdapter.getCount();
                    if (adjPosition < adapterCount) {
                        return mAdapter.isEnabled(adjPosition);
                    }
                }
            }

            // Empty item
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                return false;
            }

            // Footer
            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns) + numFootersAndPlaceholders) {
                return (position % mNumColumns == 0)
                        && mFooterViewInfos.get((position - numHeadersAndPlaceholders - mAdapter.getCount() - (mAdapter.getCount() % mNumColumns)) / mNumColumns).isSelectable;
            }

            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % mNumColumns == 0) {
                    return mHeaderViewInfos.get(position / mNumColumns).data;
                }
                return null;
            }

            // Adapter
            if (position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                final int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = 0;
                if (mAdapter != null) {
                    adapterCount = mAdapter.getCount();
                    if (adjPosition < adapterCount) {
                        return mAdapter.getItem(adjPosition);
                    }
                }
            }

            // Empty item
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                return null;
            }

            // Footer
            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns) + numFootersAndPlaceholders) {
                if (position % mNumColumns == 0) {
                    return mFooterViewInfos.get((position - numHeadersAndPlaceholders - mAdapter.getCount() - (mAdapter.getCount() % mNumColumns)) / mNumColumns).data;
                }
            }

            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public long getItemId(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (mAdapter != null) {
                if (position >= numHeadersAndPlaceholders && position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                    int adjPosition = position - numHeadersAndPlaceholders;
                    int adapterCount = mAdapter.getCount();
                    if (adjPosition < adapterCount) {
                        return mAdapter.getItemId(adjPosition);
                    }
                }
            }
            return -1;
        }

        @Override
        public boolean hasStableIds() {
            if (mAdapter != null) {
                return mAdapter.hasStableIds();
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = mHeaderViewInfos
                        .get(position / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return headerViewContainer;
                } else {
                    convertView = new View(parent.getContext());
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }

            // Adapter
            if (position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                final int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = 0;
                if (mAdapter != null) {
                    adapterCount = mAdapter.getCount();
                    if (adjPosition < adapterCount) {
                        return mAdapter.getView(adjPosition, convertView, parent);
                    }
                }
            }

            // Empty item
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                // We need to do this because GridView uses the height of the last item
                // in a row to determine the height for the entire row.
                // TODO Current implementation may not be enough in the case of 3 or more column. May need to be careful on the INVISIBLE View height.
                convertView = mAdapter.getView(mAdapter.getCount() - 1, convertView, parent);
                convertView.setVisibility(View.INVISIBLE);
                return convertView;
            }

            // Footer
            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders + mAdapter.getCount()  + (mAdapter.getCount() % mNumColumns) + numFootersAndPlaceholders) {
                View footerViewContainer = mFooterViewInfos
                        .get((position - numHeadersAndPlaceholders - mAdapter.getCount() - (mAdapter.getCount() % mNumColumns)) / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return footerViewContainer;
                } else {
                    convertView = new View(parent.getContext());
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(footerViewContainer.getHeight());
                    return convertView;
                }
            }

            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public int getItemViewType(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders && (position % mNumColumns != 0)) {
                // Placeholders get the last view type number
                return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
            }
            if (mAdapter != null && position >= numHeadersAndPlaceholders && position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemViewType(adjPosition);
                }
            }
            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (mAdapter != null && position < numHeadersAndPlaceholders + mAdapter.getCount() + numFootersAndPlaceholders) {
                return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
            }

            return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        }

        @Override
        public int getViewTypeCount() {
            if (mAdapter != null) {
                return mAdapter.getViewTypeCount() + 1;
            }
            return 2;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        @Override
        public Filter getFilter() {
            if (mIsFilterable) {
                return ((Filterable) mAdapter).getFilter();
            }
            return null;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mAdapter;
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }
    }
    
    private Bitmap topBitmap, bottomBitmap;
    
    @Override
    public void draw(Canvas canvas) {
    	
    	if (mBlurBottomHeight > 0 || mBlurTopHeight > 0) {
	    	if (mBlurBottomHeight > 0 && mForBottomCanvas != null) {
	    		mForBottomCanvas.save();
	    		mForBottomCanvas.translate(0, -mBlurForBottomRect.top);
	    		mForBottomCanvas.drawColor(mContext.getResources().getColor(R.color.tws_windowbackground_color));
	    		super.draw(mForBottomCanvas);
	    		mForBottomCanvas.restore();
	    	}
	    			
	    	if (mBlurTopHeight > 0 && mForTopCanvas != null) {
	    		mForTopCanvas.save();
	    		mForTopCanvas.drawColor(mContext.getResources().getColor(R.color.tws_windowbackground_color));
	    		super.draw(mForTopCanvas);
	    		mForTopCanvas.restore();
	    	}
	
	    	canvas.save();
	    	if (mForContentRect != null) {
	    		canvas.clipRect(mForContentRect);
	    	}
	    	super.draw(canvas);
	    	canvas.restore();
    			
	    	if (mBlurBottomHeight > 0 && mForBottomBitmap != null) {
	    		if (NativeBlurProcess.noBlurSo) {
	    			bottomBitmap = mForBottomBitmap;
	    		}
	    		else {
	    			bottomBitmap = mBlur.blur(mForBottomBitmap, true);
	    		}
	    		if (bottomBitmap != null && !bottomBitmap.isRecycled())
	    			canvas.drawBitmap(bottomBitmap, null, mBlurForBottomRect, mBlurPaint);
			}
			if (mBlurTopHeight > 0 && mForTopBitmap != null) {
				if (NativeBlurProcess.noBlurSo) {
					topBitmap = mForTopBitmap;
				}
				else {
					topBitmap = mBlur.blur(mForTopBitmap, true);
				}
				if (topBitmap != null && !topBitmap.isRecycled())
					canvas.drawBitmap(topBitmap, null, mBlurForTopRect, mBlurPaint);
			}
    	} 
    	else {
    		super.draw(canvas);
    	}
    }
    
    @Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		blurRecycle();
	}
    
    private void blurSetup() {
		
		if (mBlurBottomHeight > 0) {
			mBottomBlurView = new View(getContext());
			LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mBlurBottomHeight);
			mBottomBlurView.setLayoutParams(params);
			if (mBlurBottomDrawable != null) {
				if (android.os.Build.VERSION.SDK_INT > 15) {
					mBottomBlurView.setBackground(mBlurBottomDrawable);
				}
				else {
					mBottomBlurView.setBackgroundDrawable(mBlurBottomDrawable);
				}
			}
			addFooterView(mBottomBlurView, null, false);
		}
		
		if (mBlurTopHeight > 0) {
			mTopBlurView = new View(getContext());
			LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mBlurTopHeight);
			mTopBlurView.setLayoutParams(params);
			if (mBlurTopDrawable != null) {
				if (android.os.Build.VERSION.SDK_INT > 15) {
					mTopBlurView.setBackground(mBlurTopDrawable);
				}
				else {
					mTopBlurView.setBackgroundDrawable(mBlurTopDrawable);
				}
			}
			addHeaderView(mTopBlurView, null, false);
		}
    }
    
    private void blurInit() {
		if (mBlurBottomHeight > 0 || mBlurTopHeight > 0) {
			
			if (mBlurPaint == null) {
				mBlurPaint = new Paint();
				mBlurPaint.setAntiAlias(true);
			}
			
			if (mBlurForTopRect == null) {
				mBlurForTopRect = new Rect();
			}
			mBlurForTopRect.left = 0;
			mBlurForTopRect.right = getWidth();
			mBlurForTopRect.top = 0;
			mBlurForTopRect.bottom = mBlurTopHeight;
			
			if (mBlurForBottomRect == null) {
				mBlurForBottomRect = new Rect();
			}
			mBlurForBottomRect.left = 0;
			mBlurForBottomRect.right = getWidth();
			mBlurForBottomRect.bottom = getHeight();
			mBlurForBottomRect.top = mBlurForBottomRect.bottom - mBlurBottomHeight;
			
			if (mForContentRect == null) {
				mForContentRect = new Rect();
			}
			mForContentRect.left = 0;
			mForContentRect.right = getWidth();
			mForContentRect.bottom = mBlurForBottomRect.top;
			mForContentRect.top = mBlurForTopRect.bottom;
			
			blurRecycle();
			
			if (mBlurBottomHeight > 0) {
				if (NativeBlurProcess.noBlurSo) {
					mForBottomBitmap = Bitmap.createBitmap((mBlurForBottomRect.right - mBlurForBottomRect.left),
							mBlurBottomHeight, Config.ARGB_8888);
					mForBottomCanvas = new Canvas(mForBottomBitmap);
				}
				else {
					mForBottomBitmap = Bitmap.createBitmap((mBlurForBottomRect.right - mBlurForBottomRect.left) / 10,
							mBlurBottomHeight / 10, Config.ARGB_8888);
					mForBottomCanvas = new Canvas(mForBottomBitmap);
					mForBottomCanvas.scale(0.1f, 0.1f);
				}
			}
			
			if (mBlurTopHeight > 0) {
				if (NativeBlurProcess.noBlurSo) {
					mForTopBitmap = Bitmap.createBitmap((mBlurForTopRect.right - mBlurForTopRect.left),
							mBlurTopHeight, Config.ARGB_8888);
					mForTopCanvas = new Canvas(mForTopBitmap);
				}
				else {
					mForTopBitmap = Bitmap.createBitmap((mBlurForTopRect.right - mBlurForTopRect.left) / 10,
							mBlurTopHeight / 10, Config.ARGB_8888);
					mForTopCanvas = new Canvas(mForTopBitmap);
					mForTopCanvas.scale(0.1f, 0.1f);
				}
			}
			
			if (mBlur == null) {
				mBlur = new JNIBlur(mContext);
			}
			
//			try {
//				Class clazz = ReflectUtils.forClassName("android.view.View");
//				Method method = ReflectUtils.getDeclaredMethod(clazz, "twsSetParameters", int.class, int.class);
//				ReflectUtils.invoke(method, this, mBlurTopHeight, mBlurBottomHeight);
//			} catch (Exception e) {
//				Log.v("scrollbar", "twsSetParameters not found");
//			}
			twsSetParameters(mBlurTopHeight, mBlurBottomHeight);
		}
		else {
//			try {
//				Class clazz = ReflectUtils.forClassName("android.view.View");
//				Method method = ReflectUtils.getDeclaredMethod(clazz, "twsSetParameters", int.class, int.class);
//				ReflectUtils.invoke(method, this, 0, 0);
//			} catch (Exception e) {
//				Log.v("scrollbar", "twsSetParameters not found");
//			}
			twsSetParameters(0, 0);
		}
	}
    
    private void blurRecycle() {

		if (mForBottomBitmap != null && !mForBottomBitmap.isRecycled()) {
			mForBottomBitmap.recycle();
		}
		
		if (mForTopBitmap != null && !mForTopBitmap.isRecycled()) {
			mForTopBitmap.recycle();
		}
	}
    
    private Bitmap blurScale(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.3f, 0.3f);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }
    
	public void setFooterBlank(boolean flag) {
		if (flag)
			this.mBlurBottomHeight = (int) getContext().getResources().getDimension(R.dimen.tws_actionbar_split_height);
		else
			this.mBlurBottomHeight = 0;
		
	}
	
	public void setHeaderBlank(boolean flag) {
		if (flag)
			this.mBlurTopHeight = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_height);
		else
			this.mBlurTopHeight = 0;
	}
	
	public void setHeaderBlankWithStatusbar(boolean flag) {
		if (flag)
			this.mBlurTopHeight = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_height);
		else
			this.mBlurTopHeight = 0;
	}
	
	public boolean enableTopBlur(boolean flag) {
		if (flag) {
			setHeaderBlankWithStatusbar(false);
			if (mBlurTopHeight > 0) {
				mTopBlurView = new View(mContext);
				LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mBlurTopHeight);
				mTopBlurView.setLayoutParams(params);
				if (mBlurTopDrawable != null) {
					if (android.os.Build.VERSION.SDK_INT > 15) {
						mTopBlurView.setBackground(mBlurTopDrawable);
					}
					else {
						mTopBlurView.setBackgroundDrawable(mBlurTopDrawable);
					}
				}
				addHeaderView(mTopBlurView, null, false);
			}
			blurInit();
		}
		else {
			setHeaderBlankWithStatusbar(false);
            blurInit();
            if (mTopBlurView != null) {
            	removeHeaderView(mTopBlurView);
            }
		}
		return flag;
	}
	
	public boolean enableBottomBlur(boolean flag) {
		if (flag) {
			setFooterBlank(false);
			if (mBlurBottomHeight > 0) {
				mBottomBlurView = new View(mContext);
				LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mBlurBottomHeight);
				mBottomBlurView.setLayoutParams(params);
				if (mBlurBottomDrawable != null) {
					if (android.os.Build.VERSION.SDK_INT > 15) {
						mBottomBlurView.setBackground(mBlurBottomDrawable);
					}
					else {
						mBottomBlurView.setBackgroundDrawable(mBlurBottomDrawable);
					}
					
				}
				addFooterView(mBottomBlurView, null, false);
			}
			blurInit();
		}
		else {
			setFooterBlank(false);
            blurInit();
            if (mBottomBlurView != null) {
            	removeFooterView(mBottomBlurView);
            }
		}
		return flag;
	}
	
	public void changeTopBlurDrawable(Drawable drawable) {
		this.mBlurTopDrawable = drawable;
	}
	
	public void changeBottomBlurDrawable(Drawable drawable) {
		this.mBlurBottomDrawable = drawable;
	}
	
	//tws-start for framework xposed rebuild::2015-05-22
    private void twsInitTwsScrollBarDrawable(boolean initialize) {
		try {
			Class<?> viewClz = Class.forName("android.view.View");
			Field scrollCacheField = viewClz.getDeclaredField("mScrollCache");
			scrollCacheField.setAccessible(true);
			Object scrollCache = scrollCacheField.get(this);
			Class<?> scrollCacheClz= scrollCacheField.getType();
			
			if (initialize && scrollCache == null) {
				String methodName = "initScrollCache";
				Method method = viewClz.getDeclaredMethod(methodName);
				method.setAccessible(true);
				method.invoke(this);
				
				scrollCache = scrollCacheField.get(this);
			}
			
			if (scrollCache != null) {
				Field scrollBarField = scrollCacheClz.getDeclaredField("scrollBar");
				scrollBarField.setAccessible(true);
				Object scrollBar = scrollBarField.get(scrollCache);
				if (scrollBar == null) {
					scrollBarField.set(scrollCache, new TwsScrollBarDrawable());
				}
			}
		} catch (Exception e) {
			Log.e("tws.widget.ListView", "twsInitTwsScrollBarDrawable|exp:"+e.getMessage());
		}
    }
    
    @Override
    protected void initializeScrollbars(TypedArray a) {
    	Log.d("tws.widget.ListView", "initializeScrollbars");
    	
    	twsInitTwsScrollBarDrawable(true);
    	
    	super.initializeScrollbars(a);
    }
    
    @Override
    protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
    	twsInitTwsScrollBarDrawable(false);
    	
        return super.awakenScrollBars(startDelay, invalidate);
    }
    
    private void twsSetParameters(int start, int end) {
		try {
			Log.d("tws.widget.ListView", "twsSetParameters|start="+start+",end="+end);
			
			Class<?> viewClz = Class.forName("android.view.View");
			Field scrollCacheField = viewClz.getDeclaredField("mScrollCache");
			scrollCacheField.setAccessible(true);
			Object scrollCache = scrollCacheField.get(this);
			Class<?> scrollCacheClz= scrollCacheField.getType();
			
			if (scrollCache != null) {
				Field scrollBarField = scrollCacheClz.getDeclaredField("scrollBar");
				scrollBarField.setAccessible(true);
				Object scrollBar = scrollBarField.get(scrollCache);
				if (scrollBar != null) {
					if (scrollBar instanceof TwsScrollBarDrawable) {
						TwsScrollBarDrawable drawable = (TwsScrollBarDrawable) scrollBar;
						drawable.twsSetParameters(getHeight(), start, end);
					}
				}
			}
		} catch (Exception e) {
			Log.e("tws.widget.ListView", "twsSetParameters exp:"+e.getMessage());
		}
	}
    //tws-end for framework xposed rebuild::2015-05-22
}
