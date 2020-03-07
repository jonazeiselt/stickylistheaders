package com.eiselts.stickylistheaders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.ListPopupWindow.MATCH_PARENT;

/**
 * Even though this is a FrameLayout subclass we still consider it a ListView.
 * This is because of 2 reasons:
 *   1. It acts like as ListView.
 *   2. It used to be a ListView subclass and refactoring the name would cause compatibility errors.
 *
 * @author Emil Sjölander
 */
public class StickyListHeadersListView extends FrameLayout {

    public interface OnHeaderClickListener {
        void onHeaderClick(View header, int position, long headerId);
    }

    public interface OnScrollWhenTopReachedListener {
        void onScrollTopReached();
    }

    /* --- Children --- */
    private WrapperListView mList;
    private View mHeader;

    /* --- Header state --- */
    private Long mHeaderId;
    // used to not have to call getHeaderId() all the time
    private Integer mHeaderPosition;
    private Integer mHeaderOffset;

    /* --- Delegates --- */
    private OnScrollListener mOnScrollListenerDelegate;
    private WrapperAdapter mAdapter;

    /* --- Settings --- */
    private boolean mAreHeadersSticky = true;
    private boolean mClippingToPadding = true;
    private boolean mIsDrawingListUnderStickyHeader = true;
    private int mPaddingLeft = 0;
    private int mPaddingTop = 0;
    private int mPaddingRight = 0;
    private int mPaddingBottom = 0;

    /* --- Touch handling --- */
    private float mDownY;
    private boolean mHeaderOwnsTouch;
    private float mTouchSlop;

    /* --- Other --- */
    private OnHeaderClickListener mOnHeaderClickListener;
    private AdapterWrapperDataSetObserver mDataSetObserver;
    private Drawable mDivider;
    private int mDividerHeight;

    public StickyListHeadersListView(Context context) {
        this(context, null);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.stickyListHeadersListViewStyle);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        // Initialize the wrapped list
        mList = new WrapperListView(context);

        // null out divider, dividers are handled by adapter so they look good with headers
        mDivider = mList.getDivider();
        mDividerHeight = mList.getDividerHeight();
        mList.setDivider(null);
        mList.setDividerHeight(0);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.StickyListHeadersListView, defStyle, 0);

            try {
                // -- View attributes --
                int padding = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_padding, 0);
                mPaddingLeft = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingLeft, padding);
                mPaddingTop = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingTop, padding);
                mPaddingRight = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingRight, padding);
                mPaddingBottom = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingBottom, padding);

                setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);

                // Set clip to padding on the list and reset value to default on
                // wrapper
                mClippingToPadding = a.getBoolean(R.styleable.StickyListHeadersListView_android_clipToPadding, true);
                super.setClipToPadding(true);
                mList.setClipToPadding(mClippingToPadding);

                // scrollbars
                final int scrollBars = a.getInt(R.styleable.StickyListHeadersListView_android_scrollbars, 0x00000200);
                mList.setVerticalScrollBarEnabled((scrollBars & 0x00000200) != 0);
                mList.setHorizontalScrollBarEnabled((scrollBars & 0x00000100) != 0);

                // overscroll
                mList.setOverScrollMode(a.getInt(R.styleable.StickyListHeadersListView_android_overScrollMode, 0));

                // -- ListView attributes --
                mList.setFadingEdgeLength(a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_fadingEdgeLength,
                        mList.getVerticalFadingEdgeLength()));
                final int fadingEdge = a.getInt(R.styleable.StickyListHeadersListView_android_requiresFadingEdge, 0);
                if (fadingEdge == 0x00001000) {
                    mList.setVerticalFadingEdgeEnabled(false);
                    mList.setHorizontalFadingEdgeEnabled(true);
                } else if (fadingEdge == 0x00002000) {
                    mList.setVerticalFadingEdgeEnabled(true);
                    mList.setHorizontalFadingEdgeEnabled(false);
                } else {
                    mList.setVerticalFadingEdgeEnabled(false);
                    mList.setHorizontalFadingEdgeEnabled(false);
                }
                mList.setCacheColorHint(a
                        .getColor(R.styleable.StickyListHeadersListView_android_cacheColorHint, mList.getCacheColorHint()));
                mList.setChoiceMode(a.getInt(R.styleable.StickyListHeadersListView_android_choiceMode,
                        mList.getChoiceMode()));
                mList.setDrawSelectorOnTop(a.getBoolean(R.styleable.StickyListHeadersListView_android_drawSelectorOnTop, false));
                mList.setFastScrollEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_fastScrollEnabled,
                        mList.isFastScrollEnabled()));
                mList.setFastScrollAlwaysVisible(a.getBoolean(
                        R.styleable.StickyListHeadersListView_android_fastScrollAlwaysVisible,
                        mList.isFastScrollAlwaysVisible()));

                mList.setScrollBarStyle(a.getInt(R.styleable.StickyListHeadersListView_android_scrollbarStyle, 0));

                if (a.hasValue(R.styleable.StickyListHeadersListView_android_listSelector)) {
                    mList.setSelector(a.getDrawable(R.styleable.StickyListHeadersListView_android_listSelector));
                }

                mList.setScrollingCacheEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_scrollingCache,
                        mList.isScrollingCacheEnabled()));

                if (a.hasValue(R.styleable.StickyListHeadersListView_android_divider)) {
                    mDivider = a.getDrawable(R.styleable.StickyListHeadersListView_android_divider);
                }

                mList.setStackFromBottom(a.getBoolean(R.styleable.StickyListHeadersListView_android_stackFromBottom, false));

                mDividerHeight = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_dividerHeight,
                        mDividerHeight);

                mList.setTranscriptMode(a.getInt(R.styleable.StickyListHeadersListView_android_transcriptMode,
                        ListView.TRANSCRIPT_MODE_DISABLED));

                // -- StickyListHeaders attributes --
                mAreHeadersSticky = a.getBoolean(R.styleable.StickyListHeadersListView_hasStickyHeaders, true);
                mIsDrawingListUnderStickyHeader = a.getBoolean(
                        R.styleable.StickyListHeadersListView_isDrawingListUnderStickyHeader,
                        true);
            } finally {
                a.recycle();
            }
        }

        // attach some listeners to the wrapped list
        mList.setLifeCycleListener(new WrapperViewListLifeCycleListener());
        mList.setOnScrollListener(new WrapperListScrollListener());

        addView(mList);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureHeader(mHeader);
    }

    private void ensureHeaderHasCorrectLayoutParams(View header) {
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            header.setLayoutParams(lp);
        } else if (lp.height == MATCH_PARENT || lp.width == WRAP_CONTENT) {
            lp.height = WRAP_CONTENT;
            lp.width = MATCH_PARENT;
            header.setLayoutParams(lp);
        }
    }

    private void measureHeader(View header) {
        if (header != null) {
            final int width = getMeasuredWidth() - mPaddingLeft - mPaddingRight;
            final int parentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    width, MeasureSpec.EXACTLY);
            final int parentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
            measureChild(header, parentWidthMeasureSpec,
                    parentHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mList.layout(0, 0, mList.getMeasuredWidth(), getHeight());
        if (mHeader != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeader.getLayoutParams();
            int headerTop = lp.topMargin;
            mHeader.layout(mPaddingLeft, headerTop, mHeader.getMeasuredWidth()
                    + mPaddingLeft, headerTop + mHeader.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Only draw the list here.
        // The header should be drawn right after the lists children are drawn.
        // This is done so that the header is above the list items
        // but below the list decorators (scroll bars etc).
        if (mList.getVisibility() == VISIBLE || mList.getAnimation() != null) {
            drawChild(canvas, mList, 0);
        }
    }

    // Reset values tied the header. also remove header form layout
    // This is called in response to the data set or the adapter changing
    private void clearHeader() {
        if (mHeader != null) {
            removeView(mHeader);
            mHeader = null;
            mHeaderId = null;
            mHeaderPosition = null;
            mHeaderOffset = null;

            // reset the top clipping length
            mList.setTopClippingLength(0);
            updateHeaderVisibilities();
        }
    }

    private void updateOrClearHeader(int firstVisiblePosition) {
        final int adapterCount = mAdapter == null ? 0 : mAdapter.getCount();
        if (adapterCount == 0 || !mAreHeadersSticky) {
            return;
        }

        final int headerViewCount = mList.getHeaderViewsCount();
        int headerPosition = firstVisiblePosition - headerViewCount;
        if (mList.getChildCount() > 0) {
            View firstItem = mList.getChildAt(0);
            if (firstItem.getBottom() < stickyHeaderTop()) {
                headerPosition++;
            }
        }

        // It is not a mistake to call getFirstVisiblePosition() here.
        // Most of the time getFixedFirstVisibleItem() should be called
        // but that does not work great together with getChildAt()
        final boolean doesListHaveChildren = mList.getChildCount() != 0;
        final boolean isFirstViewBelowTop = doesListHaveChildren
                && mList.getFirstVisiblePosition() == 0
                && mList.getChildAt(0).getTop() >= stickyHeaderTop();
        final boolean isHeaderPositionOutsideAdapterRange = headerPosition > adapterCount - 1
                || headerPosition < 0;
        if (!doesListHaveChildren || isHeaderPositionOutsideAdapterRange || isFirstViewBelowTop) {
            clearHeader();
            return;
        }

        updateHeader(headerPosition);
    }

    private void updateHeader(int headerPosition) {
        // check if there is a new header should be sticky
        if (mHeaderPosition == null || mHeaderPosition != headerPosition) {
            mHeaderPosition = headerPosition;
            final long headerId = mAdapter.getHeaderId(headerPosition);
            if (mHeaderId == null || mHeaderId != headerId) {
                mHeaderId = headerId;
                final View header = mAdapter.getHeaderView(mHeaderPosition, mHeader, this);
                if (mHeader != header) {
                    if (header == null) {
                        throw new NullPointerException("header may not be null");
                    }
                    swapHeader(header);
                }
                ensureHeaderHasCorrectLayoutParams(mHeader);
                measureHeader(mHeader);

                // Reset mHeaderOffset to null ensuring
                // that it will be set on the header and
                // not skipped for performance reasons.
                mHeaderOffset = null;
            }
        }

        int headerOffset = stickyHeaderTop();

        // Calculate new header offset
        // Skip looking at the first view. it never matters because it always
        // results in a headerOffset = 0
        for (int i = 0; i < mList.getChildCount(); i++) {
            final View child = mList.getChildAt(i);
            final boolean doesChildHaveHeader = child instanceof WrapperView && ((WrapperView) child).hasHeader();
            final boolean isChildFooter = mList.containsFooterView(child);
            if (child.getTop() >= stickyHeaderTop() && (doesChildHaveHeader || isChildFooter)) {
                headerOffset = Math.min(child.getTop() - mHeader.getMeasuredHeight(), headerOffset);
                break;
            }
        }

        setHeaderOffset(headerOffset);

        if (!mIsDrawingListUnderStickyHeader) {
            mList.setTopClippingLength(mHeader.getMeasuredHeight() + mHeaderOffset);
        }

        updateHeaderVisibilities();
    }

    private void swapHeader(View newHeader) {
        if (mHeader != null) {
            removeView(mHeader);
        }
        mHeader = newHeader;
        addView(mHeader);
        if (mOnHeaderClickListener != null) {
            mHeader.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnHeaderClickListener.onHeaderClick(mHeader, mHeaderPosition, mHeaderId);
                }
            });
        }
        mHeader.setClickable(true);
    }

    // hides the headers in the list under the sticky header.
    // Makes sure the other ones are showing
    private void updateHeaderVisibilities() {
        int top = stickyHeaderTop();
        int childCount = mList.getChildCount();
        for (int i = 0; i < childCount; i++) {

            // ensure child is a wrapper view
            View child = mList.getChildAt(i);
            if (!(child instanceof WrapperView)) {
                continue;
            }

            // ensure wrapper view child has a header
            WrapperView wrapperViewChild = (WrapperView) child;
            if (!wrapperViewChild.hasHeader()) {
                continue;
            }

            // update header views visibility
            View childHeader = wrapperViewChild.mHeader;
            if (wrapperViewChild.getTop() < top) {
                if (childHeader.getVisibility() != View.INVISIBLE) {
                    childHeader.setVisibility(View.INVISIBLE);
                }
            } else {
                if (childHeader.getVisibility() != View.VISIBLE) {
                    childHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // Wrapper around setting the header offset in different ways depending on the API version
    private void setHeaderOffset(int offset) {
        if (mHeaderOffset == null || mHeaderOffset != offset) {
            mHeaderOffset = offset;
            mHeader.setTranslationY(mHeaderOffset);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            mDownY = ev.getY();
            mHeaderOwnsTouch = mHeader != null && mDownY <= mHeader.getHeight() + mHeaderOffset;
        }

        boolean handled;
        if (mHeaderOwnsTouch) {
            if (mHeader != null && Math.abs(mDownY - ev.getY()) <= mTouchSlop) {
                handled = mHeader.dispatchTouchEvent(ev);
            } else {
                if (mHeader != null) {
                    MotionEvent cancelEvent = MotionEvent.obtain(ev);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    mHeader.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                MotionEvent downEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), ev.getAction(), ev.getX(), mDownY, ev.getMetaState());
                downEvent.setAction(MotionEvent.ACTION_DOWN);
                handled = mList.dispatchTouchEvent(downEvent);
                downEvent.recycle();
                mHeaderOwnsTouch = false;
            }
        } else {
            handled = mList.dispatchTouchEvent(ev);
        }

        return handled;
    }

    private class AdapterWrapperDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            clearHeader();
        }

        @Override
        public void onInvalidated() {
            clearHeader();
        }
    }

    private class WrapperListScrollListener implements OnScrollListener {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            if (mOnScrollListenerDelegate != null) {
                mOnScrollListenerDelegate.onScroll(view, firstVisibleItem, visibleItemCount,
                        totalItemCount);
            }

            updateOrClearHeader(mList.getFixedFirstVisibleItem());
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mOnScrollListenerDelegate != null) {
                mOnScrollListenerDelegate.onScrollStateChanged(view, scrollState);
            }
        }
    }

    private class WrapperViewListLifeCycleListener implements WrapperListView.LifeCycleListener {

        @Override
        public void onDispatchDrawOccurred(Canvas canvas) {
            // onScroll is not called often at all before froyo
            // therefore we need to update the header here as well.
            if (mHeader != null) {
                if (mClippingToPadding) {
                    canvas.save();
                    canvas.clipRect(0, mPaddingTop, getRight(), getBottom());
                    drawChild(canvas, mHeader, 0);
                    canvas.restore();
                } else {
                    drawChild(canvas, mHeader, 0);
                }
            }
        }

    }

    private class AdapterWrapperHeaderClickHandler implements WrapperAdapter.OnHeaderClickListener {

        @Override
        public void onHeaderClick(View header, int itemPosition, long headerId) {
            mOnHeaderClickListener.onHeaderClick(header, itemPosition, headerId);
        }
    }

    private int stickyHeaderTop() {
        int stickyHeaderTopOffset = 0;
        return stickyHeaderTopOffset + (mClippingToPadding ? mPaddingTop : 0);
    }

    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
        if (mAdapter != null) {
            if (mOnHeaderClickListener != null) {
                mAdapter.setOnHeaderClickListener(new AdapterWrapperHeaderClickHandler());

                if (mHeader != null) {
                    mHeader.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mOnHeaderClickListener.onHeaderClick(mHeader, mHeaderPosition,
                                    mHeaderId);
                        }
                    });
                }
            } else {
                mAdapter.setOnHeaderClickListener(null);
            }
        }
    }

    /* ---------- ListView delegate methods ---------- */

    public void setAdapter(StickyListHeadersAdapter adapter) {
        if (adapter == null) {
            if (mAdapter != null) {
                mAdapter.mDelegate = null;
            }
            mList.setAdapter(null);
            clearHeader();
            return;
        }

        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        mAdapter = new WrapperAdapter(getContext(), adapter);
        mDataSetObserver = new AdapterWrapperDataSetObserver();
        mAdapter.registerDataSetObserver(mDataSetObserver);

        mAdapter.setOnHeaderClickListener(mOnHeaderClickListener != null ?
                new AdapterWrapperHeaderClickHandler() : null);

        mAdapter.setDivider(mDivider, mDividerHeight);

        mList.setAdapter(mAdapter);
        clearHeader();
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListenerDelegate = onScrollListener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnTouchListener(final OnTouchListener l) {
        if (l != null) {
            mList.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return l.onTouch(StickyListHeadersListView.this, event);
                }
            });
        } else {
            mList.setOnTouchListener(null);
        }
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return mList.isVerticalScrollBarEnabled();
    }

    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return mList.isHorizontalScrollBarEnabled();
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        mList.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        mList.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
    }

    @Override
    public int getOverScrollMode() {
        return mList.getOverScrollMode();
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (mList != null) {
            mList.setOverScrollMode(mode);
        }
    }

    @Override
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        mList.setOnCreateContextMenuListener(l);
    }

    @Override
    public boolean showContextMenu() {
        return mList.showContextMenu();
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (mList != null) {
            mList.setClipToPadding(clipToPadding);
        }
        mClippingToPadding = clipToPadding;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;

        if (mList != null) {
            mList.setPadding(left, top, right, bottom);
        }
        super.setPadding(0, 0, 0, 0);
        requestLayout();
    }

    @Override
    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    @Override
    public int getPaddingTop() {
        return mPaddingTop;
    }

    @Override
    public int getPaddingRight() {
        return mPaddingRight;
    }

    @Override
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public void setScrollBarStyle(int style) {
        mList.setScrollBarStyle(style);
    }

    public int getScrollBarStyle() {
        return mList.getScrollBarStyle();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (superState != BaseSavedState.EMPTY_STATE) {
            throw new IllegalStateException("Handling non empty state of parent class is not implemented");
        }
        return mList.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
        mList.onRestoreInstanceState(state);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mList.canScrollVertically(direction);
    }

    public void setOnScrollWhenTopReachedListener(final OnScrollWhenTopReachedListener
                                                          onScrollWhenTopReachedListener) {
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (onScrollWhenTopReachedListener != null && getChildAt(0) != null
                        && firstVisibleItem == 0 && !canScrollVertically(-1)) {
                    onScrollWhenTopReachedListener.onScrollTopReached();
                }
            }
        });
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mList.setOnItemClickListener(onItemClickListener);
    }
}
