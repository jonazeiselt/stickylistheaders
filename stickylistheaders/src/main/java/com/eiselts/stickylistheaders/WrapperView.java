package com.eiselts.stickylistheaders;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

/**
 * The view that wraps a divider header and a normal list item. The listview sees this as 1 item
 * @author Emil Sjölander
 */
public class WrapperView extends ViewGroup {

	View mItem;
	Drawable mDivider;
	int mDividerHeight;
	View mHeader;
	int mItemTop;

	WrapperView(Context c) {
		super(c);
	}

	public boolean hasHeader() {
		return mHeader != null;
	}

	public View getItem() {
		return mItem;
	}

	public View getHeader() {
		return mHeader;
	}

	void update(View item, View header, Drawable divider, int dividerHeight) {
		// Every wrapperview must have a list item
		if (item == null) {
			throw new NullPointerException("List view item must not be null.");
		}

		// Only remove the current item if it is not the same as the new item. this can happen if
		// wrapping a recycled view
		if (this.mItem != item) {
			removeView(this.mItem);
			this.mItem = item;
			final ViewParent parent = item.getParent();
			if (parent != this && parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(item);
			}
			addView(item);
		}

		// Same logic as above but for the header
		if (this.mHeader != header) {
			if (this.mHeader != null) {
				removeView(this.mHeader);
			}
			this.mHeader = header;
			if (header != null) {
				addView(header);
			}
		}

		if (this.mDivider != divider) {
			this.mDivider = divider;
			this.mDividerHeight = dividerHeight;
			invalidate();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, EXACTLY);
		int measuredHeight = 0;

		// Measure header or divider. When there is a header visible it acts as the divider
		if (mHeader != null) {
			LayoutParams params = mHeader.getLayoutParams();
			if (params != null && params.height > 0) {
				mHeader.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(params.height,
						EXACTLY));
			} else {
				mHeader.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(0,
						UNSPECIFIED));
			}
			measuredHeight += mHeader.getMeasuredHeight();
		} else if (mDivider != null && mItem.getVisibility() != GONE) {
			measuredHeight += mDividerHeight;
		}

		// Measure item
		LayoutParams params = mItem.getLayoutParams();

		// Enable hiding listview item, eg. toggle visibility of items in group
		if (mItem.getVisibility() == GONE){
			mItem.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(0, EXACTLY));
		} else if (params != null && params.height >= 0) {
			mItem.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(params.height,
					EXACTLY));
			measuredHeight += mItem.getMeasuredHeight();
		} else {
			mItem.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(0, UNSPECIFIED));
			measuredHeight += mItem.getMeasuredHeight();
		}
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		left = 0;
		top = 0;
		right = getWidth();
		bottom = getHeight();

		if (mHeader != null) {
			int headerHeight = mHeader.getMeasuredHeight();
			mHeader.layout(left, top, right, headerHeight);
			mItemTop = headerHeight;
			mItem.layout(left, headerHeight, right, bottom);
		} else if (mDivider != null) {
			mDivider.setBounds(left, top, right, mDividerHeight);
			mItemTop = mDividerHeight;
			mItem.layout(left, mDividerHeight, right, bottom);
		} else {
			mItemTop = top;
			mItem.layout(left, top, right, bottom);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHeader == null && mDivider != null && mItem.getVisibility() != GONE) {
			mDivider.draw(canvas);
		}
	}
}
