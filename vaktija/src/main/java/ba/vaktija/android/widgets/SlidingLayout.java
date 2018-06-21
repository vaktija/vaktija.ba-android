package ba.vaktija.android.widgets;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class SlidingLayout extends RelativeLayout {

	public static final String TAG = SlidingLayout.class.getSimpleName();

	public static interface SlidingLayoutListener{
		void onSlidingCompleted();
	}

	ViewDragHelper mDragHelper;
	SlidingLayoutListener mSlidingListener;
	
	ViewGroup parent;

	public SlidingLayout(Context context) {
		super(context);
		parent = this;
	}

	public SlidingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		parent = this;
	}

	public SlidingLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		parent = this;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallbacks());
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		if(mDragHelper.continueSettling(true)) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean shouldInterceptTouchEvent = mDragHelper.shouldInterceptTouchEvent(ev);
		return shouldInterceptTouchEvent;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mDragHelper.processTouchEvent(event);
		return true;
	}

	public void setSlidingListener(SlidingLayoutListener slidingListener){
		mSlidingListener = slidingListener;
	}


	class ViewDragHelperCallbacks extends ViewDragHelper.Callback {

		@Override
		public boolean tryCaptureView(View view, int pointerId) {

			if(view.getTag() != null && view.getTag().toString().equals("handle")){
				return true;
			}

			return false;
		}

		@Override
		public int clampViewPositionVertical(View child, int top, int dy) {
//			MyLog.d(TAG, "clampViewPositionVertical top: "+top+" dy: "+dy);
			
			return parent.getPaddingTop();
		}

		@Override
		public int clampViewPositionHorizontal(View child, int left, int dx) {
//			MyLog.d(TAG, "clampViewPositionHorizontal left: "+left+" dx: "+dx);

			if(left < parent.getPaddingLeft())
				return parent.getPaddingLeft();

			int x = parent.getMeasuredWidth() - child.getMeasuredWidth() - parent.getPaddingRight();

			if(child.getRight() > x){
				return x;
			}

			return left;
		}

		@Override
		public int getViewHorizontalDragRange(View child) {
			return parent.getMeasuredWidth() - child.getMeasuredWidth();
		}

		@Override
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			super.onViewReleased(releasedChild, xvel, yvel);
			
			boolean nearEnd = releasedChild.getLeft() > parent.getMeasuredWidth() * 0.75;

			if(xvel > 0 || nearEnd) {
				int x = parent.getMeasuredWidth() - releasedChild.getMeasuredWidth() - parent.getPaddingLeft();

				mDragHelper.settleCapturedViewAt(x, parent.getPaddingTop());

				if(mSlidingListener != null){
					mSlidingListener.onSlidingCompleted();
				}
			} else {
				mDragHelper.settleCapturedViewAt(parent.getPaddingLeft(), parent.getPaddingTop());
			}

			invalidate();
		}
	}
}
