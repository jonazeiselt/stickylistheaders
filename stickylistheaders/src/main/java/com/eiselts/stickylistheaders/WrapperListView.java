package com.eiselts.stickylistheaders;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

class WrapperListView extends ListView {

	interface LifeCycleListener {
		void onDispatchDrawOccurred(Canvas canvas);
	}

	private LifeCycleListener mLifeCycleListener;
	private List<View> mFooterViews;
	private int mTopClippingLength;
	private Rect mSelectorRect = new Rect();
	private boolean mBlockLayoutChildren = false;

	public WrapperListView(Context context) {
		super(context);
	}

	@Override
	public boolean performItemClick(View view, int position, long id) {
		if (view instanceof WrapperView) {
			view = ((WrapperView) view).mItem;
		}
		return super.performItemClick(view, position, id);
	}

	private void positionSelectorRect() {
		if (!mSelectorRect.isEmpty()) {
			int selectorPosition = getSelectorPosition();
			if (selectorPosition >= 0) {
				int firstVisibleItem = getFixedFirstVisibleItem();
				View v = getChildAt(selectorPosition - firstVisibleItem);
				if (v instanceof WrapperView) {
					WrapperView wrapper = ((WrapperView) v);
					mSelectorRect.top = wrapper.getTop() + wrapper.mItemTop;
				}
			}
		}
	}

	private int getSelectorPosition() {
		for (int i = 0; i < getChildCount(); i++) {
			if (getChildAt(i).getBottom() == mSelectorRect.bottom) {
				return i + getFixedFirstVisibleItem();
			}
		}
		return -1;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		positionSelectorRect();
		if (mTopClippingLength != 0) {
			canvas.save();
			Rect clipping = canvas.getClipBounds();
			clipping.top = mTopClippingLength;
			canvas.clipRect(clipping);
			super.dispatchDraw(canvas);
			canvas.restore();
		} else {
			super.dispatchDraw(canvas);
		}
		mLifeCycleListener.onDispatchDrawOccurred(canvas);
	}

	void setLifeCycleListener(LifeCycleListener lifeCycleListener) {
		mLifeCycleListener = lifeCycleListener;
	}

	@Override
	public void addFooterView(View v) {
		super.addFooterView(v);
		addInternalFooterView(v);
	}

	@Override
	public void addFooterView(View v, Object data, boolean isSelectable) {
		super.addFooterView(v, data, isSelectable);
		addInternalFooterView(v);
	}

	private void addInternalFooterView(View v) {
		if (mFooterViews == null) {
			mFooterViews = new ArrayList<>();
		}
		mFooterViews.add(v);
	}

	@Override
	public boolean removeFooterView(View v) {
		if (super.removeFooterView(v)) {
			mFooterViews.remove(v);
			return true;
		}
		return false;
	}

	boolean containsFooterView(View v) {
		if (mFooterViews == null) {
			return false;
		}
		return mFooterViews.contains(v);
	}

	void setTopClippingLength(int topClipping) {
		mTopClippingLength = topClipping;
	}

	int getFixedFirstVisibleItem() {
		return getFirstVisiblePosition();
	}

	@Override
	public void setClipToPadding(boolean clipToPadding) {
		super.setClipToPadding(clipToPadding);
	}

	public void setBlockLayoutChildren(boolean block) {
		mBlockLayoutChildren = block;
	}

	@Override
	protected void layoutChildren() {
		if (!mBlockLayoutChildren) {
			super.layoutChildren();
		}
	}
}
