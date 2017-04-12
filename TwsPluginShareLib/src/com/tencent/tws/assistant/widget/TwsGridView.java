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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.GridLayoutAnimationController;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.RemoteViews.RemoteView;
import android.widget.WrapperListAdapter;

import com.tencent.tws.assistant.drawable.TwsScrollBarDrawable;
import com.tencent.tws.assistant.gaussblur.JNIBlur;
import com.tencent.tws.assistant.gaussblur.NativeBlurProcess;
import com.tencent.tws.sharelib.R;

/**
 * A view that shows items in two-dimensional scrolling grid. The items in the
 * grid come from the {@link ListAdapter} associated with this view.
 *
 * <p>See the <a href="{@docRoot}guide/topics/ui/layout/gridview.html">Grid
 * View</a> guide.</p>
 * 
 * @attr ref android.R.styleable#GridView_horizontalSpacing
 * @attr ref android.R.styleable#GridView_verticalSpacing
 * @attr ref android.R.styleable#GridView_stretchMode
 * @attr ref android.R.styleable#GridView_columnWidth
 * @attr ref android.R.styleable#GridView_numColumns
 * @attr ref android.R.styleable#GridView_gravity
 */
@RemoteView
public class TwsGridView extends AbsListView {
    /**
     * Disables stretching.
     * 
     * @see #setStretchMode(int) 
     */
    public static final int NO_STRETCH = 0;
    /**
     * Stretches the spacing between columns.
     * 
     * @see #setStretchMode(int) 
     */
    public static final int STRETCH_SPACING = 1;
    /**
     * Stretches columns.
     * 
     * @see #setStretchMode(int) 
     */
    public static final int STRETCH_COLUMN_WIDTH = 2;
    /**
     * Stretches the spacing between columns. The spacing is uniform.
     * 
     * @see #setStretchMode(int) 
     */
    public static final int STRETCH_SPACING_UNIFORM = 3;

    /**
     * Creates as many columns as can fit on screen.
     * 
     * @see #setNumColumns(int) 
     */
    public static final int AUTO_FIT = -1;

    private int mNumColumns = AUTO_FIT;

    private int mHorizontalSpacing = 0;
    private int mRequestedHorizontalSpacing;
    private int mVerticalSpacing = 0;
    private int mStretchMode = STRETCH_COLUMN_WIDTH;
    private int mColumnWidth;
    private int mRequestedColumnWidth;
    private int mRequestedNumColumns;

    private View mReferenceView = null;
    private View mReferenceViewInSelectedRow = null;

    private int mGravity = Gravity.START;

    private final Rect mTempRect = new Rect();

    public TwsGridView(Context context) {
        this(context, null);
    }

    public TwsGridView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.gridViewStyle);
    }

    public TwsGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.GridView, defStyle, 0);

        int hSpacing = a.getDimensionPixelOffset(
                com.android.internal.R.styleable.GridView_horizontalSpacing, 0);
        setHorizontalSpacing(hSpacing);

        int vSpacing = a.getDimensionPixelOffset(
                com.android.internal.R.styleable.GridView_verticalSpacing, 0);
        setVerticalSpacing(vSpacing);

        int index = a.getInt(com.android.internal.R.styleable.GridView_stretchMode, STRETCH_COLUMN_WIDTH);
        if (index >= 0) {
            setStretchMode(index);
        }

        int columnWidth = a.getDimensionPixelOffset(com.android.internal.R.styleable.GridView_columnWidth, -1);
        if (columnWidth > 0) {
            setColumnWidth(columnWidth);
        }

        int numColumns = a.getInt(com.android.internal.R.styleable.GridView_numColumns, 1);
        setNumColumns(numColumns);

        index = a.getInt(com.android.internal.R.styleable.GridView_gravity, -1);
        if (index >= 0) {
            setGravity(index);
        }
        
        a.recycle();
        
        initHeaderGridView();
    }

    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets up this AbsListView to use a remote views adapter which connects to a RemoteViewsService
     * through the specified intent.
     * @param intent the intent used to identify the RemoteViewsService for the adapter to connect to.
     */
    @android.view.RemotableViewMethod
    public void setRemoteViewsAdapter(Intent intent) {
        super.setRemoteViewsAdapter(intent);
    }

    /**
     * Sets the data behind this GridView.
     *
     * @param adapter the adapter providing the grid's data
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
    	
    	blurSetup();
    	
        if (mHeaderViewInfos.size() > 0 || mFooterViewInfos.size() > 0) {
            HeaderFooterViewGridAdapter hadapter = new HeaderFooterViewGridAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
            int numColumns = getNumColumns();
            if (numColumns > 1) {
                hadapter.setNumColumns(numColumns);
            }
            if (mAdapter != null && mDataSetObserver != null) {
                mAdapter.unregisterDataSetObserver(mDataSetObserver);
            }

            resetList();
            mRecycler.clear();        
            mAdapter = hadapter;

            mOldSelectedPosition = INVALID_POSITION;
            mOldSelectedRowId = INVALID_ROW_ID;

            // AbsListView#setAdapter will update choice mode states.
            super.setAdapter(hadapter);

            if (mAdapter != null) {
                mOldItemCount = mItemCount;
                mItemCount = mAdapter.getCount();
                mDataChanged = true;
                checkFocus();

                mDataSetObserver = new AdapterDataSetObserver();
                mAdapter.registerDataSetObserver(mDataSetObserver);

                mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

                int position;
                if (mStackFromBottom) {
                    position = lookForSelectablePosition(mItemCount - 1, false);
                } else {
                    position = lookForSelectablePosition(0, true);
                }
                setSelectedPositionInt(position);
                setNextSelectedPositionInt(position);
                checkSelectionChanged();
            } else {
                checkFocus();            
                // Nothing selected
                checkSelectionChanged();
            }

            requestLayout();
            
            
            
            
        } else {
        	if (mAdapter != null && mDataSetObserver != null) {
                mAdapter.unregisterDataSetObserver(mDataSetObserver);
            }

            resetList();
            mRecycler.clear();        
            mAdapter = adapter;

            mOldSelectedPosition = INVALID_POSITION;
            mOldSelectedRowId = INVALID_ROW_ID;

            // AbsListView#setAdapter will update choice mode states.
            super.setAdapter(adapter);

            if (mAdapter != null) {
                mOldItemCount = mItemCount;
                mItemCount = mAdapter.getCount();
                mDataChanged = true;
                checkFocus();

                mDataSetObserver = new AdapterDataSetObserver();
                mAdapter.registerDataSetObserver(mDataSetObserver);

                mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

                int position;
                if (mStackFromBottom) {
                    position = lookForSelectablePosition(mItemCount - 1, false);
                } else {
                    position = lookForSelectablePosition(0, true);
                }
                setSelectedPositionInt(position);
                setNextSelectedPositionInt(position);
                checkSelectionChanged();
            } else {
                checkFocus();            
                // Nothing selected
                checkSelectionChanged();
            }

            requestLayout();
        }
    }

    @Override
    int lookForSelectablePosition(int position, boolean lookDown) {
        final ListAdapter adapter = mAdapter;
        if (adapter == null || isInTouchMode()) {
            return INVALID_POSITION;
        }

        if (position < 0 || position >= mItemCount) {
            return INVALID_POSITION;
        }
        return position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void fillGap(boolean down) {
        final int numColumns = mNumColumns;
        final int verticalSpacing = mVerticalSpacing;

        final int count = getChildCount();

        if (down) {
            int paddingTop = 0;
            if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
                paddingTop = getListPaddingTop();
            }
            final int startOffset = count > 0 ?
                    getChildAt(count - 1).getBottom() + verticalSpacing : paddingTop;
            int position = mFirstPosition + count;
            if (mStackFromBottom) {
                position += numColumns - 1;
            }
            fillDown(position, startOffset);
            if (!isNeedBounce()) {
                correctTooHigh(numColumns, verticalSpacing, getChildCount());
            }
        } else {
            int paddingBottom = 0;
            if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
                paddingBottom = getListPaddingBottom();
            }
            final int startOffset = count > 0 ?
                    getChildAt(0).getTop() - verticalSpacing : getHeight() - paddingBottom;
            int position = mFirstPosition;
            if (!mStackFromBottom) {
                position -= numColumns;
            } else {
                position--;
            }
            fillUp(position, startOffset);
            if (!isNeedBounce()) {
                correctTooLow(numColumns, verticalSpacing, getChildCount());
            }
        }
    }

    /**
     * Fills the list from pos down to the end of the list view.
     *
     * @param pos The first position to put in the list
     *
     * @param nextTop The location where the top of the item associated with pos
     *        should be drawn
     *
     * @return The view that is currently selected, if it happens to be in the
     *         range that we draw.
     */
    private View fillDown(int pos, int nextTop) {
        View selectedView = null;

        int end = (mBottom - mTop);
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end -= mListPadding.bottom;
        }

        while (nextTop < end && pos < mItemCount) {
            View temp = makeRow(pos, nextTop, true);
            if (temp != null) {
                selectedView = temp;
            }

            // mReferenceView will change with each call to makeRow()
            // do not cache in a local variable outside of this loop
            nextTop = mReferenceView.getBottom() + mVerticalSpacing;

            pos += mNumColumns;
        }

        setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        return selectedView;
    }

    private View makeRow(int startPos, int y, boolean flow) {
        final int columnWidth = mColumnWidth;
        final int horizontalSpacing = mHorizontalSpacing;

        final boolean isLayoutRtl = isLayoutRtl();

        int last;
        int nextLeft;

        if (isLayoutRtl) {
            nextLeft = getWidth() - mListPadding.right - columnWidth -
                    ((mStretchMode == STRETCH_SPACING_UNIFORM) ? horizontalSpacing : 0);
        } else {
            nextLeft = mListPadding.left +
                    ((mStretchMode == STRETCH_SPACING_UNIFORM) ? horizontalSpacing : 0);
        }

        if (!mStackFromBottom) {
            last = Math.min(startPos + mNumColumns, mItemCount);
        } else {
            last = startPos + 1;
            startPos = Math.max(0, startPos - mNumColumns + 1);

            if (last - startPos < mNumColumns) {
                final int deltaLeft = (mNumColumns - (last - startPos)) * (columnWidth + horizontalSpacing);
                nextLeft += (isLayoutRtl ? -1 : +1) * deltaLeft;
            }
        }

        View selectedView = null;

        final boolean hasFocus = shouldShowSelector();
        final boolean inClick = touchModeDrawsInPressedState();
        final int selectedPosition = mSelectedPosition;

        View child = null;
        for (int pos = startPos; pos < last; pos++) {
            // is this the selected item?
            boolean selected = pos == selectedPosition;
            // does the list view have focus or contain focus

            final int where = flow ? -1 : pos - startPos;
            child = makeAndAddView(pos, y, flow, nextLeft, selected, where);

            nextLeft += (isLayoutRtl ? -1 : +1) * columnWidth;
            if (pos < last - 1) {
                nextLeft += horizontalSpacing;
            }

            if (selected && (hasFocus || inClick)) {
                selectedView = child;
            }
        }

        mReferenceView = child;
        
        if (selectedView != null) {
            mReferenceViewInSelectedRow = mReferenceView;
        }

        return selectedView;
    }

    /**
     * Fills the list from pos up to the top of the list view.
     *
     * @param pos The first position to put in the list
     *
     * @param nextBottom The location where the bottom of the item associated
     *        with pos should be drawn
     *
     * @return The view that is currently selected
     */
    private View fillUp(int pos, int nextBottom) {
        View selectedView = null;

        int end = 0;
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end = mListPadding.top;
        }

        while (nextBottom > end && pos >= 0) {

            View temp = makeRow(pos, nextBottom, false);
            if (temp != null) {
                selectedView = temp;
            }

            nextBottom = mReferenceView.getTop() - mVerticalSpacing;

            mFirstPosition = pos;

            pos -= mNumColumns;
        }

        if (mStackFromBottom) {
            mFirstPosition = Math.max(0, pos + 1);
        }

        setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        return selectedView;
    }

    /**
     * Fills the list from top to bottom, starting with mFirstPosition
     *
     * @param nextTop The location where the top of the first item should be
     *        drawn
     *
     * @return The view that is currently selected
     */
    private View fillFromTop(int nextTop) {
        mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
        mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
        if (mFirstPosition < 0) {
            mFirstPosition = 0;
        }
        mFirstPosition -= mFirstPosition % mNumColumns;
        return fillDown(mFirstPosition, nextTop);
    }

    private View fillFromBottom(int lastPosition, int nextBottom) {
        lastPosition = Math.max(lastPosition, mSelectedPosition);
        lastPosition = Math.min(lastPosition, mItemCount - 1);

        final int invertedPosition = mItemCount - 1 - lastPosition;
        lastPosition = mItemCount - 1 - (invertedPosition - (invertedPosition % mNumColumns));

        return fillUp(lastPosition, nextBottom);
    }

    private View fillSelection(int childrenTop, int childrenBottom) {
        final int selectedPosition = reconcileSelectedPosition();
        final int numColumns = mNumColumns;
        final int verticalSpacing = mVerticalSpacing;

        int rowStart;
        int rowEnd = -1;

        if (!mStackFromBottom) {
            rowStart = selectedPosition - (selectedPosition % numColumns);
        } else {
            final int invertedSelection = mItemCount - 1 - selectedPosition;

            rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
            rowStart = Math.max(0, rowEnd - numColumns + 1);
        }

        final int fadingEdgeLength = getVerticalFadingEdgeLength();
        final int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);

        final View sel = makeRow(mStackFromBottom ? rowEnd : rowStart, topSelectionPixel, true);
        mFirstPosition = rowStart;

        final View referenceView = mReferenceView;

        if (!mStackFromBottom) {
            fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);
            pinToBottom(childrenBottom);
            fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
        } else {
            final int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom,
                    fadingEdgeLength, numColumns, rowStart);
            final int offset = bottomSelectionPixel - referenceView.getBottom();
            offsetChildrenTopAndBottom(offset);
            fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);
            pinToTop(childrenTop);
            fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
        }

        return sel;
    }

    private void pinToTop(int childrenTop) {
        if (mFirstPosition == 0) {
            final int top = getChildAt(0).getTop();
            final int offset = childrenTop - top;
            if (offset < 0) {
                offsetChildrenTopAndBottom(offset);
            }
        }
    }

    private void pinToBottom(int childrenBottom) {
        final int count = getChildCount();
        if (mFirstPosition + count == mItemCount) {
            final int bottom = getChildAt(count - 1).getBottom();
            final int offset = childrenBottom - bottom;
            if (offset > 0) {
                offsetChildrenTopAndBottom(offset);
            }
        }
    }    

    @Override
    int findMotionRow(int y) {
        final int childCount = getChildCount();
        if (childCount > 0) {

            final int numColumns = mNumColumns;
            if (!mStackFromBottom) {
                for (int i = 0; i < childCount; i += numColumns) {
                    if (y <= getChildAt(i).getBottom()) {
                        return mFirstPosition + i;
                    }
                }
            } else {
                for (int i = childCount - 1; i >= 0; i -= numColumns) {
                    if (y >= getChildAt(i).getTop()) {
                        return mFirstPosition + i;
                    }
                }
            }
        }
        return INVALID_POSITION;
    }

    /**
     * Layout during a scroll that results from tracking motion events. Places
     * the mMotionPosition view at the offset specified by mMotionViewTop, and
     * then build surrounding views from there.
     *
     * @param position the position at which to start filling
     * @param top the top of the view at that position
     * @return The selected view, or null if the selected view is outside the
     *         visible area.
     */
    private View fillSpecific(int position, int top) {
        final int numColumns = mNumColumns;

        int motionRowStart;
        int motionRowEnd = -1;

        if (!mStackFromBottom) {
            motionRowStart = position - (position % numColumns);
        } else {
            final int invertedSelection = mItemCount - 1 - position;

            motionRowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
            motionRowStart = Math.max(0, motionRowEnd - numColumns + 1);
        }

        final View temp = makeRow(mStackFromBottom ? motionRowEnd : motionRowStart, top, true);

        // Possibly changed again in fillUp if we add rows above this one.
        mFirstPosition = motionRowStart;

        final View referenceView = mReferenceView;
        // We didn't have anything to layout, bail out
        if (referenceView == null) {
            return null;
        }

        final int verticalSpacing = mVerticalSpacing;

        View above;
        View below;

        if (!mStackFromBottom) {
            above = fillUp(motionRowStart - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            below = fillDown(motionRowStart + numColumns, referenceView.getBottom() + verticalSpacing);
            // Check if we have dragged the bottom of the grid too high
            final int childCount = getChildCount();
            if (childCount > 0) {
                if (!isNeedBounce()) {
                    correctTooHigh(numColumns, verticalSpacing, childCount);
                }
            }
        } else {
            below = fillDown(motionRowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            above = fillUp(motionRowStart - 1, referenceView.getTop() - verticalSpacing);
            // Check if we have dragged the bottom of the grid too high
            final int childCount = getChildCount();
            if (childCount > 0) {
                if (!isNeedBounce()) {
                    correctTooLow(numColumns, verticalSpacing, childCount);
                }
            }
        }

        if (temp != null) {
            return temp;
        } else if (above != null) {
            return above;
        } else {
            return below;
        }
    }

    private void correctTooHigh(int numColumns, int verticalSpacing, int childCount) {
        // First see if the last item is visible
        final int lastPosition = mFirstPosition + childCount - 1;
        if (lastPosition == mItemCount - 1 && childCount > 0) {
            // Get the last child ...
            final View lastChild = getChildAt(childCount - 1);

            // ... and its bottom edge
            final int lastBottom = lastChild.getBottom();
            // This is bottom of our drawable area
            final int end = (mBottom - mTop) - mListPadding.bottom;

            // This is how far the bottom edge of the last view is from the bottom of the
            // drawable area
            int bottomOffset = end - lastBottom;        

            final View firstChild = getChildAt(0);
            final int firstTop = firstChild.getTop();

            // Make sure we are 1) Too high, and 2) Either there are more rows above the
            // first row or the first row is scrolled off the top of the drawable area
            if (bottomOffset > 0 && (mFirstPosition > 0 || firstTop < mListPadding.top))  {
                if (mFirstPosition == 0) {
                    // Don't pull the top too far down
                    bottomOffset = Math.min(bottomOffset, mListPadding.top - firstTop);
                }
                // Move everything down
                offsetChildrenTopAndBottom(bottomOffset);
                if (mFirstPosition > 0) {
                    // Fill the gap that was opened above mFirstPosition with more rows, if
                    // possible
                    fillUp(mFirstPosition - (mStackFromBottom ? 1 : numColumns),
                            firstChild.getTop() - verticalSpacing);
                    // Close up the remaining gap
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    private void correctTooLow(int numColumns, int verticalSpacing, int childCount) {
        if (mFirstPosition == 0 && childCount > 0) {
            // Get the first child ...
            final View firstChild = getChildAt(0);

            // ... and its top edge
            final int firstTop = firstChild.getTop();

            // This is top of our drawable area
            final int start = mListPadding.top;

            // This is bottom of our drawable area
            final int end = (mBottom - mTop) - mListPadding.bottom;

            // This is how far the top edge of the first view is from the top of the
            // drawable area
            int topOffset = firstTop - start;
            final View lastChild = getChildAt(childCount - 1);
            final int lastBottom = lastChild.getBottom();
            final int lastPosition = mFirstPosition + childCount - 1;

            // Make sure we are 1) Too low, and 2) Either there are more rows below the
            // last row or the last row is scrolled off the bottom of the drawable area
            if (topOffset > 0 && (lastPosition < mItemCount - 1 || lastBottom > end))  {
                if (lastPosition == mItemCount - 1 ) {
                    // Don't pull the bottom too far up
                    topOffset = Math.min(topOffset, lastBottom - end);
                }
                
                // Move everything up
                offsetChildrenTopAndBottom(-topOffset);
                if (lastPosition < mItemCount - 1) {
                    // Fill the gap that was opened below the last position with more rows, if
                    // possible
                    fillDown(lastPosition + (!mStackFromBottom ? 1 : numColumns),
                            lastChild.getBottom() + verticalSpacing);
                    // Close up the remaining gap
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    /**
     * Fills the grid based on positioning the new selection at a specific
     * location. The selection may be moved so that it does not intersect the
     * faded edges. The grid is then filled upwards and downwards from there.
     *
     * @param selectedTop Where the selected item should be
     * @param childrenTop Where to start drawing children
     * @param childrenBottom Last pixel where children can be drawn
     * @return The view that currently has selection
     */
    private View fillFromSelection(int selectedTop, int childrenTop, int childrenBottom) {
        final int fadingEdgeLength = getVerticalFadingEdgeLength();
        final int selectedPosition = mSelectedPosition;
        final int numColumns = mNumColumns;
        final int verticalSpacing = mVerticalSpacing;

        int rowStart;
        int rowEnd = -1;

        if (!mStackFromBottom) {
            rowStart = selectedPosition - (selectedPosition % numColumns);
        } else {
            int invertedSelection = mItemCount - 1 - selectedPosition;

            rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
            rowStart = Math.max(0, rowEnd - numColumns + 1);
        }

        View sel;
        View referenceView;

        int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);
        int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength,
                numColumns, rowStart);

        sel = makeRow(mStackFromBottom ? rowEnd : rowStart, selectedTop, true);
        // Possibly changed again in fillUp if we add rows above this one.
        mFirstPosition = rowStart;

        referenceView = mReferenceView;
        adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
        adjustForBottomFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);

        if (!mStackFromBottom) {
            fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);
        } else {
            fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);
        }


        return sel;
    }

    /**
     * Calculate the bottom-most pixel we can draw the selection into
     *
     * @param childrenBottom Bottom pixel were children can be drawn
     * @param fadingEdgeLength Length of the fading edge in pixels, if present
     * @param numColumns Number of columns in the grid
     * @param rowStart The start of the row that will contain the selection
     * @return The bottom-most pixel we can draw the selection into
     */
    private int getBottomSelectionPixel(int childrenBottom, int fadingEdgeLength,
            int numColumns, int rowStart) {
        // Last pixel we can draw the selection into
        int bottomSelectionPixel = childrenBottom;
        if (rowStart + numColumns - 1 < mItemCount - 1) {
            bottomSelectionPixel -= fadingEdgeLength;
        }
        return bottomSelectionPixel;
    }

    /**
     * Calculate the top-most pixel we can draw the selection into
     *
     * @param childrenTop Top pixel were children can be drawn
     * @param fadingEdgeLength Length of the fading edge in pixels, if present
     * @param rowStart The start of the row that will contain the selection
     * @return The top-most pixel we can draw the selection into
     */
    private int getTopSelectionPixel(int childrenTop, int fadingEdgeLength, int rowStart) {
        // first pixel we can draw the selection into
        int topSelectionPixel = childrenTop;
        if (rowStart > 0) {
            topSelectionPixel += fadingEdgeLength;
        }
        return topSelectionPixel;
    }

    /**
     * Move all views upwards so the selected row does not interesect the bottom
     * fading edge (if necessary).
     *
     * @param childInSelectedRow A child in the row that contains the selection
     * @param topSelectionPixel The topmost pixel we can draw the selection into
     * @param bottomSelectionPixel The bottommost pixel we can draw the
     *        selection into
     */
    private void adjustForBottomFadingEdge(View childInSelectedRow,
            int topSelectionPixel, int bottomSelectionPixel) {
        // Some of the newly selected item extends below the bottom of the
        // list
        if (childInSelectedRow.getBottom() > bottomSelectionPixel) {

            // Find space available above the selection into which we can
            // scroll upwards
            int spaceAbove = childInSelectedRow.getTop() - topSelectionPixel;

            // Find space required to bring the bottom of the selected item
            // fully into view
            int spaceBelow = childInSelectedRow.getBottom() - bottomSelectionPixel;
            int offset = Math.min(spaceAbove, spaceBelow);

            // Now offset the selected item to get it into view
            offsetChildrenTopAndBottom(-offset);
        }
    }

    /**
     * Move all views upwards so the selected row does not interesect the top
     * fading edge (if necessary).
     *
     * @param childInSelectedRow A child in the row that contains the selection
     * @param topSelectionPixel The topmost pixel we can draw the selection into
     * @param bottomSelectionPixel The bottommost pixel we can draw the
     *        selection into
     */
    private void adjustForTopFadingEdge(View childInSelectedRow,
            int topSelectionPixel, int bottomSelectionPixel) {
        // Some of the newly selected item extends above the top of the list
        if (childInSelectedRow.getTop() < topSelectionPixel) {
            // Find space required to bring the top of the selected item
            // fully into view
            int spaceAbove = topSelectionPixel - childInSelectedRow.getTop();

            // Find space available below the selection into which we can
            // scroll downwards
            int spaceBelow = bottomSelectionPixel - childInSelectedRow.getBottom();
            int offset = Math.min(spaceAbove, spaceBelow);

            // Now offset the selected item to get it into view
            offsetChildrenTopAndBottom(offset);
        }
    }

    /**
     * Smoothly scroll to the specified adapter position. The view will
     * scroll such that the indicated position is displayed.
     * @param position Scroll to this adapter position.
     */
    @android.view.RemotableViewMethod
    public void smoothScrollToPosition(int position) {
        super.smoothScrollToPosition(position);
    }

    /**
     * Smoothly scroll to the specified adapter position offset. The view will
     * scroll such that the indicated position is displayed.
     * @param offset The amount to offset from the adapter position to scroll to.
     */
    @android.view.RemotableViewMethod
    public void smoothScrollByOffset(int offset) {
        super.smoothScrollByOffset(offset);
    }

    /**
     * Fills the grid based on positioning the new selection relative to the old
     * selection. The new selection will be placed at, above, or below the
     * location of the new selection depending on how the selection is moving.
     * The selection will then be pinned to the visible part of the screen,
     * excluding the edges that are faded. The grid is then filled upwards and
     * downwards from there.
     *
     * @param delta Which way we are moving
     * @param childrenTop Where to start drawing children
     * @param childrenBottom Last pixel where children can be drawn
     * @return The view that currently has selection
     */
    private View moveSelection(int delta, int childrenTop, int childrenBottom) {
        final int fadingEdgeLength = getVerticalFadingEdgeLength();
        final int selectedPosition = mSelectedPosition;
        final int numColumns = mNumColumns;
        final int verticalSpacing = mVerticalSpacing;

        int oldRowStart;
        int rowStart;
        int rowEnd = -1;

        if (!mStackFromBottom) {
            oldRowStart = (selectedPosition - delta) - ((selectedPosition - delta) % numColumns);

            rowStart = selectedPosition - (selectedPosition % numColumns);
        } else {
            int invertedSelection = mItemCount - 1 - selectedPosition;

            rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
            rowStart = Math.max(0, rowEnd - numColumns + 1);

            invertedSelection = mItemCount - 1 - (selectedPosition - delta);
            oldRowStart = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
            oldRowStart = Math.max(0, oldRowStart - numColumns + 1);
        }

        final int rowDelta = rowStart - oldRowStart;

        final int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);
        final int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength,
                numColumns, rowStart);

        // Possibly changed again in fillUp if we add rows above this one.
        mFirstPosition = rowStart;

        View sel;
        View referenceView;

        if (rowDelta > 0) {
            /*
             * Case 1: Scrolling down.
             */

            final int oldBottom = mReferenceViewInSelectedRow == null ? 0 :
                    mReferenceViewInSelectedRow.getBottom();

            sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldBottom + verticalSpacing, true);
            referenceView = mReferenceView;

            adjustForBottomFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
        } else if (rowDelta < 0) {
            /*
             * Case 2: Scrolling up.
             */
            final int oldTop = mReferenceViewInSelectedRow == null ?
                    0 : mReferenceViewInSelectedRow .getTop();

            sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldTop - verticalSpacing, false);
            referenceView = mReferenceView;

            adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
        } else {
            /*
             * Keep selection where it was
             */
            final int oldTop = mReferenceViewInSelectedRow == null ?
                    0 : mReferenceViewInSelectedRow .getTop();

            sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldTop, true);
            referenceView = mReferenceView;
        }

        if (!mStackFromBottom) {
            fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);
        } else {
            fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);
        }

        return sel;
    }

    private boolean determineColumns(int availableSpace) {
        final int requestedHorizontalSpacing = mRequestedHorizontalSpacing;
        final int stretchMode = mStretchMode;
        final int requestedColumnWidth = mRequestedColumnWidth;
        boolean didNotInitiallyFit = false;
        
        if (mRequestedNumColumns == AUTO_FIT) {
            if (requestedColumnWidth > 0) {
                // Client told us to pick the number of columns
                mNumColumns = (availableSpace + requestedHorizontalSpacing) /
                        (requestedColumnWidth + requestedHorizontalSpacing);
            } else {
                // Just make up a number if we don't have enough info
                mNumColumns = 2;
            }
        } else {
            // We picked the columns
            mNumColumns = mRequestedNumColumns;
        }
        
        if (mNumColumns <= 0) {
            mNumColumns = 1;
        }

        switch (stretchMode) {
        case NO_STRETCH:
            // Nobody stretches
            mColumnWidth = requestedColumnWidth;
            mHorizontalSpacing = requestedHorizontalSpacing;
            break;

        default:
            int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) -
                    ((mNumColumns - 1) * requestedHorizontalSpacing);

            if (spaceLeftOver < 0) {
                didNotInitiallyFit = true;
            }

            switch (stretchMode) {
            case STRETCH_COLUMN_WIDTH:
                // Stretch the columns
                mColumnWidth = requestedColumnWidth + spaceLeftOver / mNumColumns;
                mHorizontalSpacing = requestedHorizontalSpacing;
                break;

            case STRETCH_SPACING:
                // Stretch the spacing between columns
                mColumnWidth = requestedColumnWidth;
                if (mNumColumns > 1) {
                    mHorizontalSpacing = requestedHorizontalSpacing + 
                        spaceLeftOver / (mNumColumns - 1);
                } else {
                    mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
                }
                break;

            case STRETCH_SPACING_UNIFORM:
                // Stretch the spacing between columns
                mColumnWidth = requestedColumnWidth;
                if (mNumColumns > 1) {
                    mHorizontalSpacing = requestedHorizontalSpacing + 
                        spaceLeftOver / (mNumColumns + 1);
                } else {
                    mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
                }
                break;
            }

            break;
        }
        return didNotInitiallyFit;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Sets up mListPadding
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            if (mColumnWidth > 0) {
                widthSize = mColumnWidth + mListPadding.left + mListPadding.right;
            } else {
                widthSize = mListPadding.left + mListPadding.right;
            }
            widthSize += getVerticalScrollbarWidth();
        }
        
        int childWidth = widthSize - mListPadding.left - mListPadding.right;
        boolean didNotInitiallyFit = determineColumns(childWidth);

        int childHeight = 0;
        int childState = 0;

        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        final int count = mItemCount;
        if (count > 0) {
            final View child = obtainView(0, mIsScrap);

            AbsListView.LayoutParams p = (AbsListView.LayoutParams) child.getLayoutParams();
            if (p == null) {
                p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
                child.setLayoutParams(p);
            }
            p.viewType = mAdapter.getItemViewType(0);
            p.forceAdd = true;

            int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
            int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
            child.measure(childWidthSpec, childHeightSpec);

            childHeight = child.getMeasuredHeight();
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            if (mRecycler.shouldRecycleViewType(p.viewType)) {
                mRecycler.addScrapView(child, -1);
            }
        }
        
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = mListPadding.top + mListPadding.bottom + childHeight +
                    getVerticalFadingEdgeLength() * 2;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            int ourSize =  mListPadding.top + mListPadding.bottom;
           
            final int numColumns = mNumColumns;
            for (int i = 0; i < count; i += numColumns) {
                ourSize += childHeight;
                if (i + numColumns < count) {
                    ourSize += mVerticalSpacing;
                }
                if (ourSize >= heightSize) {
                    ourSize = heightSize;
                    break;
                }
            }
            heightSize = ourSize;
        }

        if (widthMode == MeasureSpec.AT_MOST && mRequestedNumColumns != AUTO_FIT) {
            int ourSize = (mRequestedNumColumns*mColumnWidth)
                    + ((mRequestedNumColumns-1)*mHorizontalSpacing)
                    + mListPadding.left + mListPadding.right;
            if (ourSize > widthSize || didNotInitiallyFit) {
                widthSize |= MEASURED_STATE_TOO_SMALL;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
        mWidthMeasureSpec = widthMeasureSpec;
        
        if (mRequestedNumColumns != AUTO_FIT) {
        	mNumColumns = mRequestedNumColumns;
        }
        if (mNumColumns <= 0) {
        	mNumColumns = 1;
        }

        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderFooterViewGridAdapter) {
            ((HeaderFooterViewGridAdapter) adapter).setNumColumns(getNumColumns());
        }
    }

    @Override
    protected void attachLayoutAnimationParameters(View child,
            ViewGroup.LayoutParams params, int index, int count) {

        GridLayoutAnimationController.AnimationParameters animationParams =
                (GridLayoutAnimationController.AnimationParameters) params.layoutAnimationParameters;

        if (animationParams == null) {
            animationParams = new GridLayoutAnimationController.AnimationParameters();
            params.layoutAnimationParameters = animationParams;
        }

        animationParams.count = count;
        animationParams.index = index;
        animationParams.columnsCount = mNumColumns;
        animationParams.rowsCount = count / mNumColumns;

        if (!mStackFromBottom) {
            animationParams.column = index % mNumColumns;
            animationParams.row = index / mNumColumns;
        } else {
            final int invertedIndex = count - 1 - index;

            animationParams.column = mNumColumns - 1 - (invertedIndex % mNumColumns);
            animationParams.row = animationParams.rowsCount - 1 - invertedIndex / mNumColumns;
        }
    }

    @Override
    protected void layoutChildren() {
        final boolean blockLayoutRequests = mBlockLayoutRequests;
        if (!blockLayoutRequests) {
            mBlockLayoutRequests = true;
        }

        try {
            super.layoutChildren();

            invalidate();

            if (mAdapter == null) {
                resetList();
                invokeOnItemScrollListener();
                return;
            }

            final int childrenTop = mListPadding.top;
            final int childrenBottom = mBottom - mTop - mListPadding.bottom;

            int childCount = getChildCount();
            int index;
            int delta = 0;

            View sel;
            View oldSel = null;
            View oldFirst = null;
            View newSel = null;

            // Remember stuff we will need down below
            switch (mLayoutMode) {
            case LAYOUT_SET_SELECTION:
                index = mNextSelectedPosition - mFirstPosition;
                if (index >= 0 && index < childCount) {
                    newSel = getChildAt(index);
                }
                break;
            case LAYOUT_FORCE_TOP:
            case LAYOUT_FORCE_BOTTOM:
            case LAYOUT_SPECIFIC:
            case LAYOUT_SYNC:
                break;
            case LAYOUT_MOVE_SELECTION:
                if (mNextSelectedPosition >= 0) {
                    delta = mNextSelectedPosition - mSelectedPosition;
                }
                break;
            default:
                // Remember the previously selected view
                index = mSelectedPosition - mFirstPosition;
                if (index >= 0 && index < childCount) {
                    oldSel = getChildAt(index);
                }

                // Remember the previous first child
                oldFirst = getChildAt(0);
            }

            boolean dataChanged = mDataChanged;
            if (dataChanged) {
                handleDataChanged();
            }

            // Handle the empty set by removing all views that are visible
            // and calling it a day
            if (mItemCount == 0) {
                resetList();
                invokeOnItemScrollListener();
                return;
            }

            setSelectedPositionInt(mNextSelectedPosition);

            // Pull all children into the RecycleBin.
            // These views will be reused if possible
            final int firstPosition = mFirstPosition;
            final RecycleBin recycleBin = mRecycler;

            if (dataChanged) {
                for (int i = 0; i < childCount; i++) {
                    recycleBin.addScrapView(getChildAt(i), firstPosition+i);
                }
            } else {
                recycleBin.fillActiveViews(childCount, firstPosition);
            }

            // Clear out old views
            //removeAllViewsInLayout();
            detachAllViewsFromParent();
            //recycleBin.removeSkippedScrap();

            switch (mLayoutMode) {
            case LAYOUT_SET_SELECTION:
                if (newSel != null) {
                    sel = fillFromSelection(newSel.getTop(), childrenTop, childrenBottom);
                } else {
                    sel = fillSelection(childrenTop, childrenBottom);
                }
                break;
            case LAYOUT_FORCE_TOP:
                mFirstPosition = 0;
                sel = fillFromTop(childrenTop);
                adjustViewsUpOrDown();
                break;
            case LAYOUT_FORCE_BOTTOM:
                sel = fillUp(mItemCount - 1, childrenBottom);
                adjustViewsUpOrDown();
                break;
            case LAYOUT_SPECIFIC:
                sel = fillSpecific(mSelectedPosition, mSpecificTop);
                break;
            case LAYOUT_SYNC:
                sel = fillSpecific(mSyncPosition, mSpecificTop);
                break;
            case LAYOUT_MOVE_SELECTION:
                // Move the selection relative to its old position
                sel = moveSelection(delta, childrenTop, childrenBottom);
                break;
            default:
                if (childCount == 0) {
                    if (!mStackFromBottom) {
                        setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
                                INVALID_POSITION : 0);
                        sel = fillFromTop(childrenTop);
                    } else {
                        final int last = mItemCount - 1;
                        setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
                                INVALID_POSITION : last);
                        sel = fillFromBottom(last, childrenBottom);
                    }
                } else {
                    if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
                        sel = fillSpecific(mSelectedPosition, oldSel == null ?
                                childrenTop : oldSel.getTop());
                    } else if (mFirstPosition < mItemCount)  {
                        sel = fillSpecific(mFirstPosition, oldFirst == null ?
                                childrenTop : oldFirst.getTop());
                    } else {
                        sel = fillSpecific(0, childrenTop);
                    }
                }
                break;
            }

            // Flush any cached views that did not get reused above
            recycleBin.scrapActiveViews();

            if (sel != null) {
               positionSelector(INVALID_POSITION, sel);
               mSelectedTop = sel.getTop();
            } else if (mTouchMode > TOUCH_MODE_DOWN && mTouchMode < TOUCH_MODE_SCROLL) {
                View child = getChildAt(mMotionPosition - mFirstPosition);
                if (child != null) positionSelector(mMotionPosition, child);
            } else {
                mSelectedTop = 0;
                mSelectorRect.setEmpty();
            }

            mLayoutMode = LAYOUT_NORMAL;
            mDataChanged = false;
            if (mPositionScrollAfterLayout != null) {
                post(mPositionScrollAfterLayout);
                mPositionScrollAfterLayout = null;
            }
            mNeedSync = false;
            setNextSelectedPositionInt(mSelectedPosition);

            updateScrollIndicators();

            if (mItemCount > 0) {
                checkSelectionChanged();
            }

            invokeOnItemScrollListener();
        } finally {
            if (!blockLayoutRequests) {
                mBlockLayoutRequests = false;
            }
        }
    }


    /**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param position Logical position in the list
     * @param y Top or bottom edge of the view to add
     * @param flow if true, align top edge to y. If false, align bottom edge to
     *        y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @param where to add new item in the list
     * @return View that was added
     */
    private View makeAndAddView(int position, int y, boolean flow, int childrenLeft,
            boolean selected, int where) {
        View child;

        if (!mDataChanged) {
            // Try to use an existing view for this position
            child = mRecycler.getActiveView(position);
            if (child != null) {
                // Found it -- we're using an existing child
                // This just needs to be positioned
                setupChild(child, position, y, flow, childrenLeft, selected, true, where);
                return child;
            }
        }

        // Make a new view for this position, or convert an unused view if
        // possible
        child = obtainView(position, mIsScrap);

        // This needs to be positioned and measured
        setupChild(child, position, y, flow, childrenLeft, selected, mIsScrap[0], where);

        return child;
    }

    /**
     * Add a view as a child and make sure it is measured (if necessary) and
     * positioned properly.
     *
     * @param child The view to add
     * @param position The position of the view
     * @param y The y position relative to which this view will be positioned
     * @param flow if true, align top edge to y. If false, align bottom edge
     *        to y.
     * @param childrenLeft Left edge where children should be positioned
     * @param selected Is this position selected?
     * @param recycled Has this view been pulled from the recycle bin? If so it
     *        does not need to be remeasured.
     * @param where Where to add the item in the list
     *
     */
    private void setupChild(View child, int position, int y, boolean flow, int childrenLeft,
            boolean selected, boolean recycled, int where) {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "setupGridItem");

        boolean isSelected = selected && shouldShowSelector();
        final boolean updateChildSelected = isSelected != child.isSelected();
        final int mode = mTouchMode;
        final boolean isPressed = mode > TOUCH_MODE_DOWN && mode < TOUCH_MODE_SCROLL &&
                mMotionPosition == position;
        final boolean updateChildPressed = isPressed != child.isPressed();
        
        boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();

        // Respect layout params that are already in the view. Otherwise make
        // some up...
        AbsListView.LayoutParams p = (AbsListView.LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
        }
        p.viewType = mAdapter.getItemViewType(position);

        if (recycled && !p.forceAdd) {
            attachViewToParent(child, where, p);
        } else {
            p.forceAdd = false;
            addViewInLayout(child, where, p, true);
        }

        if (updateChildSelected) {
            child.setSelected(isSelected);
            if (isSelected) {
                requestFocus();
            }
        }

        if (updateChildPressed) {
            child.setPressed(isPressed);
        }

        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mCheckStates.get(position));
            } else if (getContext().getApplicationInfo().targetSdkVersion
                    >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                child.setActivated(mCheckStates.get(position));
            }
        }

        if (needToMeasure) {
            int childHeightSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);

            int childWidthSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }

        final int w = child.getMeasuredWidth();
        final int h = child.getMeasuredHeight();

        int childLeft;
        final int childTop = flow ? y : y - h;

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                childLeft = childrenLeft;
                break;
            case Gravity.CENTER_HORIZONTAL:
                childLeft = childrenLeft + ((mColumnWidth - w) / 2);
                break;
            case Gravity.RIGHT:
                childLeft = childrenLeft + mColumnWidth - w;
                break;
            default:
                childLeft = childrenLeft;
                break;
        }

        if (needToMeasure) {
            final int childRight = childLeft + w;
            final int childBottom = childTop + h;
            child.layout(childLeft, childTop, childRight, childBottom);
        } else {
            child.offsetLeftAndRight(childLeft - child.getLeft());
            child.offsetTopAndBottom(childTop - child.getTop());
        }

        if (mCachingStarted) {
            child.setDrawingCacheEnabled(true);
        }

        if (recycled && (((AbsListView.LayoutParams)child.getLayoutParams()).scrappedFromPosition)
                != position) {
            child.jumpDrawablesToCurrentState();
        }

        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }

    /**
     * Sets the currently selected item
     * 
     * @param position Index (starting at 0) of the data item to be selected.
     * 
     * If in touch mode, the item will not be selected but it will still be positioned
     * appropriately.
     */
    @Override
    public void setSelection(int position) {
        if (!isInTouchMode()) {
            setNextSelectedPositionInt(position);
        } else {
            mResurrectToPosition = position;
        }
        mLayoutMode = LAYOUT_SET_SELECTION;
        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }
        requestLayout();
    }

    /**
     * Makes the item at the supplied position selected.
     *
     * @param position the position of the new selection
     */
    @Override
    void setSelectionInt(int position) {
        int previousSelectedPosition = mNextSelectedPosition;

        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }

        setNextSelectedPositionInt(position);
        layoutChildren();
        
        final int next = mStackFromBottom ? mItemCount - 1  - mNextSelectedPosition : 
            mNextSelectedPosition;
        final int previous = mStackFromBottom ? mItemCount - 1
                - previousSelectedPosition : previousSelectedPosition;

        final int nextRow = next / mNumColumns;
        final int previousRow = previous / mNumColumns;

        if (nextRow != previousRow) {
            awakenScrollBars();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return commonKey(keyCode, 1, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return commonKey(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return commonKey(keyCode, 1, event);
    }

    private boolean commonKey(int keyCode, int count, KeyEvent event) {
        if (mAdapter == null) {
            return false;
        }

        if (mDataChanged) {
            layoutChildren();
        }

        boolean handled = false;
        int action = event.getAction();

        if (action != KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || arrowScroll(FOCUS_LEFT);
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || arrowScroll(FOCUS_RIGHT);
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || arrowScroll(FOCUS_UP);
                    } else if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_UP);
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || arrowScroll(FOCUS_DOWN);
                    } else if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_DOWN);
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded();
                        if (!handled
                                && event.getRepeatCount() == 0 && getChildCount() > 0) {
                            keyPressed();
                            handled = true;
                        }
                    }
                    break;

                case KeyEvent.KEYCODE_SPACE:
                    if (mPopup == null || !mPopup.isShowing()) {
                        if (event.hasNoModifiers()) {
                            handled = resurrectSelectionIfNeeded() || pageScroll(FOCUS_DOWN);
                        } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                            handled = resurrectSelectionIfNeeded() || pageScroll(FOCUS_UP);
                        }
                    }
                    break;

                case KeyEvent.KEYCODE_PAGE_UP:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || pageScroll(FOCUS_UP);
                    } else if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_UP);
                    }
                    break;

                case KeyEvent.KEYCODE_PAGE_DOWN:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || pageScroll(FOCUS_DOWN);
                    } else if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_DOWN);
                    }
                    break;

                case KeyEvent.KEYCODE_MOVE_HOME:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_UP);
                    }
                    break;

                case KeyEvent.KEYCODE_MOVE_END:
                    if (event.hasNoModifiers()) {
                        handled = resurrectSelectionIfNeeded() || fullScroll(FOCUS_DOWN);
                    }
                    break;

                case KeyEvent.KEYCODE_TAB:
                    // XXX Sometimes it is useful to be able to TAB through the items in
                    //     a GridView sequentially.  Unfortunately this can create an
                    //     asymmetry in TAB navigation order unless the list selection
                    //     always reverts to the top or bottom when receiving TAB focus from
                    //     another widget.  Leaving this behavior disabled for now but
                    //     perhaps it should be configurable (and more comprehensive).
                    if (false) {
                        if (event.hasNoModifiers()) {
                            handled = resurrectSelectionIfNeeded()
                                    || sequenceScroll(FOCUS_FORWARD);
                        } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                            handled = resurrectSelectionIfNeeded()
                                    || sequenceScroll(FOCUS_BACKWARD);
                        }
                    }
                    break;
            }
        }

        if (handled) {
            return true;
        }

        if (sendToTextFilter(keyCode, count, event)) {
            return true;
        }

        switch (action) {
            case KeyEvent.ACTION_DOWN:
                return super.onKeyDown(keyCode, event);
            case KeyEvent.ACTION_UP:
                return super.onKeyUp(keyCode, event);
            case KeyEvent.ACTION_MULTIPLE:
                return super.onKeyMultiple(keyCode, count, event);
            default:
                return false;
        }
    }

    /**
     * Scrolls up or down by the number of items currently present on screen.
     *
     * @param direction either {@link View#FOCUS_UP} or {@link View#FOCUS_DOWN}
     * @return whether selection was moved
     */
    boolean pageScroll(int direction) {
        int nextPage = -1;

        if (direction == FOCUS_UP) {
            nextPage = Math.max(0, mSelectedPosition - getChildCount());
        } else if (direction == FOCUS_DOWN) {
            nextPage = Math.min(mItemCount - 1, mSelectedPosition + getChildCount());
        }

        if (nextPage >= 0) {
            setSelectionInt(nextPage);
            invokeOnItemScrollListener();
            awakenScrollBars();
            return true;
        }

        return false;
    }

    /**
     * Go to the last or first item if possible.
     *
     * @param direction either {@link View#FOCUS_UP} or {@link View#FOCUS_DOWN}.
     *
     * @return Whether selection was moved.
     */
    boolean fullScroll(int direction) {
        boolean moved = false;
        if (direction == FOCUS_UP) {
            mLayoutMode = LAYOUT_SET_SELECTION;
            setSelectionInt(0);
            invokeOnItemScrollListener();
            moved = true;
        } else if (direction == FOCUS_DOWN) {
            mLayoutMode = LAYOUT_SET_SELECTION;
            setSelectionInt(mItemCount - 1);
            invokeOnItemScrollListener();
            moved = true;
        }
        
        if (moved) {
            awakenScrollBars();
        }

        return moved;
    }

    /**
     * Scrolls to the next or previous item, horizontally or vertically.
     *
     * @param direction either {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT},
     *        {@link View#FOCUS_UP} or {@link View#FOCUS_DOWN}
     *
     * @return whether selection was moved
     */
    boolean arrowScroll(int direction) {
        final int selectedPosition = mSelectedPosition;
        final int numColumns = mNumColumns;

        int startOfRowPos;
        int endOfRowPos;

        boolean moved = false;

        if (!mStackFromBottom) {
            startOfRowPos = (selectedPosition / numColumns) * numColumns;
            endOfRowPos = Math.min(startOfRowPos + numColumns - 1, mItemCount - 1);
        } else {
            final int invertedSelection = mItemCount - 1 - selectedPosition;
            endOfRowPos = mItemCount - 1 - (invertedSelection / numColumns) * numColumns;
            startOfRowPos = Math.max(0, endOfRowPos - numColumns + 1);
        }

        switch (direction) {
            case FOCUS_UP:
                if (startOfRowPos > 0) {
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(Math.max(0, selectedPosition - numColumns));
                    moved = true;
                }
                break;
            case FOCUS_DOWN:
                if (endOfRowPos < mItemCount - 1) {
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(Math.min(selectedPosition + numColumns, mItemCount - 1));
                    moved = true;
                }
                break;
            case FOCUS_LEFT:
                if (selectedPosition > startOfRowPos) {
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(Math.max(0, selectedPosition - 1));
                    moved = true;
                }
                break;
            case FOCUS_RIGHT:
                if (selectedPosition < endOfRowPos) {
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(Math.min(selectedPosition + 1, mItemCount - 1));
                    moved = true;
                }
                break;
        }

        if (moved) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            invokeOnItemScrollListener();
        }

        if (moved) {
            awakenScrollBars();
        }
        
        return moved;
    }

    /**
     * Goes to the next or previous item according to the order set by the
     * adapter.
     */
    boolean sequenceScroll(int direction) {
        int selectedPosition = mSelectedPosition;
        int numColumns = mNumColumns;
        int count = mItemCount;

        int startOfRow;
        int endOfRow;
        if (!mStackFromBottom) {
            startOfRow = (selectedPosition / numColumns) * numColumns;
            endOfRow = Math.min(startOfRow + numColumns - 1, count - 1);
        } else {
            int invertedSelection = count - 1 - selectedPosition;
            endOfRow = count - 1 - (invertedSelection / numColumns) * numColumns;
            startOfRow = Math.max(0, endOfRow - numColumns + 1);
        }

        boolean moved = false;
        boolean showScroll = false;
        switch (direction) {
            case FOCUS_FORWARD:
                if (selectedPosition < count - 1) {
                    // Move to the next item.
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(selectedPosition + 1);
                    moved = true;
                    // Show the scrollbar only if changing rows.
                    showScroll = selectedPosition == endOfRow;
                }
                break;

            case FOCUS_BACKWARD:
                if (selectedPosition > 0) {
                    // Move to the previous item.
                    mLayoutMode = LAYOUT_MOVE_SELECTION;
                    setSelectionInt(selectedPosition - 1);
                    moved = true;
                    // Show the scrollbar only if changing rows.
                    showScroll = selectedPosition == startOfRow;
                }
                break;
        }

        if (moved) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            invokeOnItemScrollListener();
        }

        if (showScroll) {
            awakenScrollBars();
        }

        return moved;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        int closestChildIndex = -1;
        if (gainFocus && previouslyFocusedRect != null) {
            previouslyFocusedRect.offset(mScrollX, mScrollY);

            // figure out which item should be selected based on previously
            // focused rect
            Rect otherRect = mTempRect;
            int minDistance = Integer.MAX_VALUE;
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                // only consider view's on appropriate edge of grid
                if (!isCandidateSelection(i, direction)) {
                    continue;
                }

                final View other = getChildAt(i);
                other.getDrawingRect(otherRect);
                offsetDescendantRectToMyCoords(other, otherRect);
                int distance = getDistance(previouslyFocusedRect, otherRect, direction);

                if (distance < minDistance) {
                    minDistance = distance;
                    closestChildIndex = i;
                }
            }
        }

        if (closestChildIndex >= 0) {
            setSelection(closestChildIndex + mFirstPosition);
        } else {
            requestLayout();
        }
    }

    /**
     * Is childIndex a candidate for next focus given the direction the focus
     * change is coming from?
     * @param childIndex The index to check.
     * @param direction The direction, one of
     *        {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}
     * @return Whether childIndex is a candidate.
     */
    private boolean isCandidateSelection(int childIndex, int direction) {
        final int count = getChildCount();
        final int invertedIndex = count - 1 - childIndex;

        int rowStart;
        int rowEnd;

        if (!mStackFromBottom) {
            rowStart = childIndex - (childIndex % mNumColumns);
            rowEnd = Math.max(rowStart + mNumColumns - 1, count);
        } else {
            rowEnd = count - 1 - (invertedIndex - (invertedIndex % mNumColumns));
            rowStart = Math.max(0, rowEnd - mNumColumns + 1);
        }

        switch (direction) {
            case View.FOCUS_RIGHT:
                // coming from left, selection is only valid if it is on left
                // edge
                return childIndex == rowStart;
            case View.FOCUS_DOWN:
                // coming from top; only valid if in top row
                return rowStart == 0;
            case View.FOCUS_LEFT:
                // coming from right, must be on right edge
                return childIndex == rowEnd;
            case View.FOCUS_UP:
                // coming from bottom, need to be in last row
                return rowEnd == count - 1;
            case View.FOCUS_FORWARD:
                // coming from top-left, need to be first in top row
                return childIndex == rowStart && rowStart == 0;
            case View.FOCUS_BACKWARD:
                // coming from bottom-right, need to be last in bottom row
                return childIndex == rowEnd && rowEnd == count - 1;
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
                        + "FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }

    /**
     * Set the gravity for this grid. Gravity describes how the child views
     * are horizontally aligned. Defaults to Gravity.LEFT
     *
     * @param gravity the gravity to apply to this grid's children
     *
     * @attr ref android.R.styleable#GridView_gravity
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Describes how the child views are horizontally aligned. Defaults to Gravity.LEFT
     *
     * @return the gravity that will be applied to this grid's children
     *
     * @attr ref android.R.styleable#GridView_gravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Set the amount of horizontal (x) spacing to place between each item
     * in the grid.
     *
     * @param horizontalSpacing The amount of horizontal space between items,
     * in pixels.
     *
     * @attr ref android.R.styleable#GridView_horizontalSpacing
     */
    public void setHorizontalSpacing(int horizontalSpacing) {
        if (horizontalSpacing != mRequestedHorizontalSpacing) {
            mRequestedHorizontalSpacing = horizontalSpacing;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Returns the amount of horizontal spacing currently used between each item in the grid.
     *
     * <p>This is only accurate for the current layout. If {@link #setHorizontalSpacing(int)}
     * has been called but layout is not yet complete, this method may return a stale value.
     * To get the horizontal spacing that was explicitly requested use
     * {@link #getRequestedHorizontalSpacing()}.</p>
     *
     * @return Current horizontal spacing between each item in pixels
     *
     * @see #setHorizontalSpacing(int)
     * @see #getRequestedHorizontalSpacing()
     *
     * @attr ref android.R.styleable#GridView_horizontalSpacing
     */
    public int getHorizontalSpacing() {
        return mHorizontalSpacing;
    }

    /**
     * Returns the requested amount of horizontal spacing between each item in the grid.
     *
     * <p>The value returned may have been supplied during inflation as part of a style,
     * the default GridView style, or by a call to {@link #setHorizontalSpacing(int)}.
     * If layout is not yet complete or if GridView calculated a different horizontal spacing
     * from what was requested, this may return a different value from
     * {@link #getHorizontalSpacing()}.</p>
     *
     * @return The currently requested horizontal spacing between items, in pixels
     *
     * @see #setHorizontalSpacing(int)
     * @see #getHorizontalSpacing()
     *
     * @attr ref android.R.styleable#GridView_horizontalSpacing
     */
    public int getRequestedHorizontalSpacing() {
        return mRequestedHorizontalSpacing;
    }

    /**
     * Set the amount of vertical (y) spacing to place between each item
     * in the grid.
     *
     * @param verticalSpacing The amount of vertical space between items,
     * in pixels.
     *
     * @see #getVerticalSpacing()
     *
     * @attr ref android.R.styleable#GridView_verticalSpacing
     */
    public void setVerticalSpacing(int verticalSpacing) {
        if (verticalSpacing != mVerticalSpacing) {
            mVerticalSpacing = verticalSpacing;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Returns the amount of vertical spacing between each item in the grid.
     *
     * @return The vertical spacing between items in pixels
     *
     * @see #setVerticalSpacing(int)
     *
     * @attr ref android.R.styleable#GridView_verticalSpacing
     */
    public int getVerticalSpacing() {
        return mVerticalSpacing;
    }

    /**
     * Control how items are stretched to fill their space.
     *
     * @param stretchMode Either {@link #NO_STRETCH},
     * {@link #STRETCH_SPACING}, {@link #STRETCH_SPACING_UNIFORM}, or {@link #STRETCH_COLUMN_WIDTH}.
     *
     * @attr ref android.R.styleable#GridView_stretchMode
     */
    public void setStretchMode(int stretchMode) {
        if (stretchMode != mStretchMode) {
            mStretchMode = stretchMode;
            requestLayoutIfNecessary();
        }
    }

    public int getStretchMode() {
        return mStretchMode;
    }

    /**
     * Set the width of columns in the grid.
     *
     * @param columnWidth The column width, in pixels.
     *
     * @attr ref android.R.styleable#GridView_columnWidth
     */
    public void setColumnWidth(int columnWidth) {
        if (columnWidth != mRequestedColumnWidth) {
            mRequestedColumnWidth = columnWidth;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Return the width of a column in the grid.
     *
     * <p>This may not be valid yet if a layout is pending.</p>
     *
     * @return The column width in pixels
     *
     * @see #setColumnWidth(int)
     * @see #getRequestedColumnWidth()
     *
     * @attr ref android.R.styleable#GridView_columnWidth
     */
    public int getColumnWidth() {
        return mColumnWidth;
    }

    /**
     * Return the requested width of a column in the grid.
     *
     * <p>This may not be the actual column width used. Use {@link #getColumnWidth()}
     * to retrieve the current real width of a column.</p>
     *
     * @return The requested column width in pixels
     *
     * @see #setColumnWidth(int)
     * @see #getColumnWidth()
     *
     * @attr ref android.R.styleable#GridView_columnWidth
     */
    public int getRequestedColumnWidth() {
        return mRequestedColumnWidth;
    }

    /**
     * Set the number of columns in the grid
     *
     * @param numColumns The desired number of columns.
     *
     * @attr ref android.R.styleable#GridView_numColumns
     */
    public void setNumColumns(int numColumns) {
        if (numColumns != mRequestedNumColumns) {
            mRequestedNumColumns = numColumns;
            requestLayoutIfNecessary();
        }
        mRequestedNumColumns = numColumns;
    }
    
    /**
     * Get the number of columns in the grid. 
     * Returns {@link #AUTO_FIT} if the Grid has never been laid out.
     *
     * @attr ref android.R.styleable#GridView_numColumns
     * 
     * @see #setNumColumns(int)
     */
    @ViewDebug.ExportedProperty
    public int getNumColumns() {  
        return mNumColumns;
    }

    /**
     * Make sure views are touching the top or bottom edge, as appropriate for
     * our gravity
     */
    private void adjustViewsUpOrDown() {
        final int childCount = getChildCount();

        if (childCount > 0) {
            int delta;
            View child;

            if (!mStackFromBottom) {
                // Uh-oh -- we came up short. Slide all views up to make them
                // align with the top
                child = getChildAt(0);
                delta = child.getTop() - mListPadding.top;
                if (mFirstPosition != 0) {
                    // It's OK to have some space above the first item if it is
                    // part of the vertical spacing
                    delta -= mVerticalSpacing;
                }
                if (delta < 0) {
                    // We only are looking to see if we are too low, not too high
                    delta = 0;
                }
            } else {
                // we are too high, slide all views down to align with bottom
                child = getChildAt(childCount - 1);
                delta = child.getBottom() - (getHeight() - mListPadding.bottom);
                
                if (mFirstPosition + childCount < mItemCount) {
                    // It's OK to have some space below the last item if it is
                    // part of the vertical spacing
                    delta += mVerticalSpacing;
                }
                
                if (delta > 0) {
                    // We only are looking to see if we are too high, not too low
                    delta = 0;
                }
            }

            if (delta != 0) {
                offsetChildrenTopAndBottom(-delta);
            }
        }
    }
    
    @Override
    protected int computeVerticalScrollExtent() {
        final int count = getChildCount();
        if (count > 0) {
            final int numColumns = mNumColumns;
            final int rowCount = (count + numColumns - 1) / numColumns;
            
            int extent = rowCount * 100;

            View view = getChildAt(0);
            final int top = view.getTop();
            int height = view.getHeight();
            if (height > 0) {
                extent += (top * 100) / height;
            }

            view = getChildAt(count - 1);
            final int bottom = view.getBottom();
            height = view.getHeight();
            if (height > 0) {
                extent -= ((bottom - getHeight()) * 100) / height;
            }

            return extent;
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if (mFirstPosition >= 0 && getChildCount() > 0) {
            final View view = getChildAt(0);
            final int top = view.getTop();
            int height = view.getHeight();
            if (height > 0) {
                final int numColumns = mNumColumns;
                final int rowCount = (mItemCount + numColumns - 1) / numColumns;
                // In case of stackFromBottom the calculation of whichRow needs
                // to take into account that counting from the top the first row
                // might not be entirely filled.
                final int oddItemsOnFirstRow = isStackFromBottom() ? ((rowCount * numColumns) -
                        mItemCount) : 0;
                final int whichRow = (mFirstPosition + oddItemsOnFirstRow) / numColumns;
                return Math.max(whichRow * 100 - (top * 100) / height +
                        (int) ((float) mScrollY / getHeight() * rowCount * 100), 0);
            }
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollRange() {
        // TODO: Account for vertical spacing too
        final int numColumns = mNumColumns;
        final int rowCount = (mItemCount + numColumns - 1) / numColumns;
        int result = Math.max(rowCount * 100, 0);
        if (mScrollY != 0) {
            // Compensate for overscroll
            result += Math.abs((int) ((float) mScrollY / getHeight() * rowCount * 100));
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TwsGridView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TwsGridView.class.getName());
    }

    private static class FixedGridViewInfo {

    	public View view;
        public ViewGroup viewContainer;
        public Object data;
        public boolean isSelectable;
    }

    private ArrayList<FixedGridViewInfo> mHeaderViewInfos = new ArrayList<FixedGridViewInfo>();

    private ArrayList<FixedGridViewInfo> mFooterViewInfos = new ArrayList<FixedGridViewInfo>();
    
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
  	
  	public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();

        if (adapter != null && !(adapter instanceof HeaderFooterViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add header view to grid -- setAdapter has already been called.");
        }

        FixedGridViewInfo info = new FixedGridViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);

        if (adapter != null) {
            ((HeaderFooterViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public void addFooterView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        
        if (adapter != null && !(adapter instanceof HeaderFooterViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add footer view to grid -- setAdapter has already been called.");
        }

        FixedGridViewInfo info = new FixedGridViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);

        if (adapter != null) {
            ((HeaderFooterViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    public int getHeaderViewCount() {
        return mHeaderViewInfos.size();
    }

    public int getFooterViewCount() {
        return mFooterViewInfos.size();
    }

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

    private void removeFixedViewInfo(View v, ArrayList<FixedGridViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedGridViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
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
            int targetWidth = TwsGridView.this.getMeasuredWidth()
                    - TwsGridView.this.getPaddingLeft()
                    - TwsGridView.this.getPaddingRight();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
                    MeasureSpec.getMode(widthMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
    
    private static class HeaderFooterViewGridAdapter implements WrapperListAdapter, Filterable {

        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        private final ListAdapter mAdapter;
        private int mNumColumns = 1;

        ArrayList<FixedGridViewInfo> mHeaderViewInfos;

        ArrayList<FixedGridViewInfo> mFooterViewInfos;

        boolean mAreAllFixedViewsSelectable;

        private final boolean mIsFilterable;

        public HeaderFooterViewGridAdapter(ArrayList<FixedGridViewInfo> headerViewInfos, ArrayList<FixedGridViewInfo> footerViewInfos, ListAdapter adapter) {
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

        private boolean areAllListInfosSelectable(ArrayList<FixedGridViewInfo> infos) {
            if (infos != null) {
                for (FixedGridViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean removeHeader(View v) {
            for (int i = 0; i < mHeaderViewInfos.size(); i++) {
                FixedGridViewInfo info = mHeaderViewInfos.get(i);
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
                FixedGridViewInfo info = mFooterViewInfos.get(i);
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
                return (getHeadersCount() * mNumColumns) + mAdapter.getCount() + 
                		(isFooterflag ? ((mAdapter.getCount() % mNumColumns) + (getFootersCount() * mNumColumns)) : 0);
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
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                return (position % mNumColumns == 0)
                        && mHeaderViewInfos.get(position / mNumColumns).isSelectable;
            }

            if (mAdapter != null && position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                final int adjPosition = position - numHeadersAndPlaceholders;
				int adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.isEnabled(adjPosition);
				}
            }

            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                return false;
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns) + numFootersAndPlaceholders) {
                return (position % mNumColumns == 0)
                        && mFooterViewInfos.get((position - numHeadersAndPlaceholders - mAdapter.getCount() - (mAdapter.getCount() % mNumColumns)) / mNumColumns).isSelectable;
            }

            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % mNumColumns == 0) {
                    return mHeaderViewInfos.get(position / mNumColumns).data;
                }
                return null;
            }

            if (mAdapter != null && position < numHeadersAndPlaceholders + mAdapter.getCount()) {
                final int adjPosition = position - numHeadersAndPlaceholders;
				int adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getItem(adjPosition);
				}
            }

            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                return null;
            }

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
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = mHeaderViewInfos
                        .get(position / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return headerViewContainer;
                } else {
                    convertView = new View(parent.getContext());
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }

            if (mAdapter != null && (position < numHeadersAndPlaceholders + mAdapter.getCount())) {
                final int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = 0;
				adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getView(adjPosition, convertView, parent);
				}
            }

            if (position < numHeadersAndPlaceholders + mAdapter.getCount() + (mAdapter.getCount() % mNumColumns)) {
                convertView = mAdapter.getView(mAdapter.getCount() - 1, convertView, parent);
                convertView.setVisibility(View.INVISIBLE);
                return convertView;
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders + mAdapter.getCount()  + (mAdapter.getCount() % mNumColumns) + numFootersAndPlaceholders) {
                View footerViewContainer = mFooterViewInfos
                        .get((position - numHeadersAndPlaceholders - mAdapter.getCount() - (mAdapter.getCount() % mNumColumns)) / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return footerViewContainer;
                } else {
                    convertView = new View(parent.getContext());
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
	private boolean mEnableBlur = true;
    
    @Override
    public void draw(Canvas canvas) {
    	mEnableBlur = false;
    	if (mEnableBlur) {
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
				int blurRectW = mBlurForBottomRect.right - mBlurForBottomRect.left;
				int blurRectH = mBlurBottomHeight;
				if (NativeBlurProcess.noBlurSo) {
					mForBottomBitmap = Bitmap.createBitmap(blurRectW > 0 ? blurRectW : 1, 
															blurRectH > 0 ? blurRectH : 1, 
																Config.ARGB_8888);
					mForBottomCanvas = new Canvas(mForBottomBitmap);
				}
				else {
					mForBottomBitmap = Bitmap.createBitmap((blurRectW / 10) > 0 ? (blurRectW / 10) : 1,
															(blurRectH / 10) > 0 ? (blurRectH / 10) : 1, 
																Config.ARGB_8888);
					mForBottomCanvas = new Canvas(mForBottomBitmap);
					mForBottomCanvas.scale(0.1f, 0.1f);
				}
			}
			
			if (mBlurTopHeight > 0) {
				int blurRectW = mBlurForTopRect.right - mBlurForTopRect.left;
				int blurRectH = mBlurTopHeight;
				if (NativeBlurProcess.noBlurSo) {
					mForTopBitmap = Bitmap.createBitmap(blurRectW > 0 ? blurRectW : 1,
															blurRectH > 0 ? blurRectH : 1, 
																Config.ARGB_8888);
					mForTopCanvas = new Canvas(mForTopBitmap);
				}
				else {
					mForTopBitmap = Bitmap.createBitmap((blurRectW / 10) > 0 ? (blurRectW / 10) : 1,
															(blurRectH / 10) > 0 ? (blurRectH / 10) : 1, 
																Config.ARGB_8888);
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
    
    private static boolean isFooterflag = false;
    
	public void setFooterBlank(boolean flag) {
		if (flag)
			this.mBlurBottomHeight = (int) getContext().getResources().getDimension(R.dimen.tws_actionbar_split_height);
		else
			this.mBlurBottomHeight = 0;
		isFooterflag = flag;
		
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
	
	public boolean setHeaderBlankWithStatusbar(boolean flag, int height) {
		if (flag)
			this.mBlurTopHeight = height;
		else
			this.mBlurTopHeight = 0;
		//tws-start recalculate scroll params::2015-1-9
//		try {
//			Class clazz = ReflectUtils.forClassName("android.view.View");
//			Method method = ReflectUtils.getDeclaredMethod(clazz, "twsSetParameters", int.class, int.class);
//			ReflectUtils.invoke(method, this, mBlurTopHeight, mBlurBottomHeight);
//		} catch (Exception e) {
//			Log.v("scrollbar", "twsSetParameters not found");
//		}
		twsSetParameters(mBlurTopHeight, mBlurBottomHeight);
		//tws-end recalculate scroll params::2015-1-9
		return flag;
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
		isFooterflag = flag;
		return flag;
	}
	
	public void changeTopBlurDrawable(Drawable drawable) {
		this.mBlurTopDrawable = drawable;
	}
	
	public void changeBottomBlurDrawable(Drawable drawable) {
		this.mBlurBottomDrawable = drawable;
	}
	
	public void setEnableBlur(boolean enable) {
		this.mEnableBlur = enable;
	}

    @Override
    protected int getDisplayItemTotalHeight(int childCount) {
        int totalHeight = 0;
        if (mRequestedNumColumns > 0) {
            int headerViewCount = getHeaderViewCount() * mRequestedNumColumns;
            int footerViewCount = getFooterViewCount() * mRequestedNumColumns;
            int totalCount = childCount - headerViewCount - footerViewCount;

            int horizontalCount = (int) Math.ceil((double) totalCount / mRequestedNumColumns);
            int verSpaceCount = 0;
            if (horizontalCount > 1) {
                verSpaceCount = horizontalCount - 1;
            }
            for (int i = headerViewCount; i < totalCount; i++) {
                if (i % mRequestedNumColumns == 0) {
                    totalHeight += getChildAt(i).getHeight();
                }
            }
            totalHeight += (verSpaceCount * mVerticalSpacing);
        }
        return totalHeight;
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
