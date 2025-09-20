package net.timelegend.chaka.viewer;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Stack;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

public class ReaderView
		extends AdapterView<Adapter>
		implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, Runnable {
	private Context mContext;
	private boolean mLinksEnabled = false;
	private boolean tapDisabled = false;
	private int tapPageMargin;

	private static final int MOVING_DIAGONALLY = 0;
	private static final int MOVING_LEFT       = 1;
	private static final int MOVING_RIGHT      = 2;
	private static final int MOVING_UP         = 3;
	private static final int MOVING_DOWN       = 4;

	private static final int FLING_MARGIN      = 100;
	private static final int GAP               = 0;

	private static final float MIN_SCALE        = 1.0f;
	private static final float MAX_SCALE        = 64.0f;

    // private static final boolean HORIZONTAL_SCROLLING = true;

    private static final int MOVE_DELAY         = 100;

    public enum SELECT{
        NO_SELECT,
        SELECTING,
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_IN,
        MOVE_NONE
    }

	private PageAdapter           mAdapter;
	protected int               mCurrent;    // Adapter's index for the current view
	private boolean           mResetLayout;
	private final SparseArray<View>
				  mChildViews = new SparseArray<View>(7);
					       // Shadows the children of the adapter view
					       // but with more sensible indexing
	private final LinkedList<View>
				  mViewCache = new LinkedList<View>();
	private boolean           mUserInteracting;  // Whether the user is interacting
	private boolean           mScaling;    // Whether the user is currently pinch zooming
	private float             mScale     = 1.0f;
	private int               mXScroll;    // Scroll amounts recorded from events.
	private int               mYScroll;    // and then accounted for in onLayout
	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	private Scroller    mScroller;
	private Stepper     mStepper;
	private int               mScrollerLastX;
	private int               mScrollerLastY;
	private float		  mLastScaleFocusX;
	private float		  mLastScaleFocusY;
    private boolean       mSingleColumn = false;
    private boolean       mTextLeft = false;
    private boolean       mHorizontalScrolling = true;
    private boolean       mFocus = false;
    private boolean       mSmartFocus = false;
    private boolean       mLock = false;
    private int           mPrevLeft;
    private int           mPrevTop;
    private SELECT        mSelecting = SELECT.NO_SELECT;    // text select state
    private int           mSelectLeftView;                  // view pageNumber with text select left handle
    private int           mSelectRightView;                 // view pageNumber with text select right handle
    private boolean       mLongPress = false;               // as longpress
    private MotionEvent   mLongPressEvent;
    private Runnable      mLongPressRunnable;;
    private long          mMoveTime = 0L;
    private boolean       mBookmarked = false;

	protected Stack<Integer> mHistory;

	public interface ViewMapper {
		void applyToView(View view);
	}

	public ReaderView(Context context) {
		super(context);
		setup(context);
	}

	public ReaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context);
	}

	public ReaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context);
	}

	private void setup(Context context)
	{
		mContext = context;
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setIsLongpressEnabled(false);
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mScroller = new Scroller(context);
		mStepper = new Stepper(this, this);
		mHistory = new Stack<Integer>();

		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = ((DocumentActivity)context).getResources().getDisplayMetrics();
		// DisplayMetrics dm = new DisplayMetrics();
		// WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		// wm.getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100)
			tapPageMargin = 100;
		if (tapPageMargin > dm.widthPixels/5)
			tapPageMargin = dm.widthPixels/5;
	}

	public boolean popHistory() {
		if (mHistory.empty())
			return false;
		setDisplayedViewIndex(mHistory.pop());
		return true;
	}

	public void pushHistory() {
		mHistory.push(mCurrent);
	}

	public void clearHistory() {
		mHistory.clear();
	}

	public int getDisplayedViewIndex() {
		return mCurrent;
	}

	public void setDisplayedViewIndex(int i) {
		if (0 <= i && i < mAdapter.getCount()) {
            if (mFocus) {
                savePosition(i);
            }
			onMoveOffChild(mCurrent);
			mCurrent = i;
			onMoveToChild(i);
			mResetLayout = true;
			requestLayout();
            amendment();
		}
	}

	public void moveToNext() {
		View v = mChildViews.get(mCurrent+1);
		if (v != null)
			slideViewOntoScreen(v);
	}

	public void moveToPrevious() {
		View v = mChildViews.get(mCurrent-1);
		if (v != null)
			slideViewOntoScreen(v);
	}

	// When advancing down the page, we want to advance by about
	// 90% of a screenful. But we'd be happy to advance by between
	// 80% and 95% if it means we hit the bottom in a whole number
	// of steps.
	private int smartAdvanceAmount(int screenHeight, int max) {
		int advance = (int)(screenHeight * 0.9 + 0.5);
		int leftOver = max % advance;
		int steps = max / advance;
		if (leftOver == 0) {
			// We'll make it exactly. No adjustment
		} else if ((float)leftOver / steps <= screenHeight * 0.05) {
			// We can adjust up by less than 5% to make it exact.
			advance += (int)((float)leftOver/steps + 0.5);
		} else {
			int overshoot = advance - leftOver;
			if ((float)overshoot / steps <= screenHeight * 0.1) {
				// We can adjust down by less than 10% to make it exact.
				advance -= (int)((float)overshoot/steps + 0.5);
			}
		}
		if (advance > max)
			advance = max;
		return advance;
	}

	public void smartMoveForwards() {
		View v = mChildViews.get(mCurrent);
		if (v == null)
			return;

        if (mFocus) {
            scrollOne(1);
            return;
        }

		// The following code works in terms of where the screen is on the views;
		// so for example, if the currentView is at (-100,-100), the visible
		// region would be at (100,100). If the previous page was (2000, 3000) in
		// size, the visible region of the previous page might be (2100 + GAP, 100)
		// (i.e. off the previous page). This is different to the way the rest of
		// the code in this file is written, but it's easier for me to think about.
		// At some point we may refactor this to fit better with the rest of the
		// code.

		// screenWidth/Height are the actual width/height of the screen. e.g. 480/800
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		// We might be mid scroll; we want to calculate where we scroll to based on
		// where this scroll would end, not where we are now (to allow for people
		// bashing 'forwards' very fast.
		int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
		int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
		// right/bottom is in terms of pixels within the scaled document; e.g. 1000
		int top = -(v.getTop() + mYScroll + remainingY);
		int right = screenWidth -(v.getLeft() + mXScroll + remainingX);
		int bottom = screenHeight+top;
		// docWidth/Height are the width/height of the scaled document e.g. 2000x3000
		int docWidth = v.getMeasuredWidth();
		int docHeight = v.getMeasuredHeight();

		int xOffset, yOffset;
		if (bottom >= docHeight) {
			// We are flush with the bottom. Advance to next column.
			if (right + screenWidth > docWidth) {
				// No room for another column - go to next page
				View nv = mChildViews.get(mCurrent+1);
				if (nv == null) // No page to advance to
					return;
				int nextTop = -(nv.getTop() + mYScroll + remainingY);
				int nextLeft = -(nv.getLeft() + mXScroll + remainingX);
				int nextDocWidth = nv.getMeasuredWidth();
				int nextDocHeight = nv.getMeasuredHeight();

				// Allow for the next page maybe being shorter than the screen is high
				yOffset = (nextDocHeight < screenHeight ? ((nextDocHeight - screenHeight)>>1) : 0);

				if (nextDocWidth < screenWidth) {
					// Next page is too narrow to fill the screen. Scroll to the top, centred.
					xOffset = (nextDocWidth - screenWidth)>>1;
				} else {
					// Reset X back to the left hand column
					xOffset = right % screenWidth;
					// Adjust in case the previous page is less wide
					if (xOffset + screenWidth > nextDocWidth)
						xOffset = nextDocWidth - screenWidth;
				}
				xOffset -= nextLeft;
				yOffset -= nextTop;
			} else {
				// Move to top of next column
				xOffset = screenWidth;
				yOffset = screenHeight - bottom;
			}
		} else {
			// Advance by 90% of the screen height downwards (in case lines are partially cut off)
			xOffset = 0;
			yOffset = smartAdvanceAmount(screenHeight, docHeight - bottom);
		}
		mScrollerLastX = mScrollerLastY = 0;
		mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
		mStepper.prod();
	}

	public void smartMoveBackwards() {
		View v = mChildViews.get(mCurrent);
		if (v == null)
			return;

        if (mFocus) {
            scrollOne(-1);
            return;
        }

		// The following code works in terms of where the screen is on the views;
		// so for example, if the currentView is at (-100,-100), the visible
		// region would be at (100,100). If the previous page was (2000, 3000) in
		// size, the visible region of the previous page might be (2100 + GAP, 100)
		// (i.e. off the previous page). This is different to the way the rest of
		// the code in this file is written, but it's easier for me to think about.
		// At some point we may refactor this to fit better with the rest of the
		// code.

		// screenWidth/Height are the actual width/height of the screen. e.g. 480/800
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		// We might be mid scroll; we want to calculate where we scroll to based on
		// where this scroll would end, not where we are now (to allow for people
		// bashing 'forwards' very fast.
		int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
		int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
		// left/top is in terms of pixels within the scaled document; e.g. 1000
		int left = -(v.getLeft() + mXScroll + remainingX);
		int top = -(v.getTop() + mYScroll + remainingY);
		// docWidth/Height are the width/height of the scaled document e.g. 2000x3000
		int docHeight = v.getMeasuredHeight();

		int xOffset, yOffset;
		if (top <= 0) {
			// We are flush with the top. Step back to previous column.
			if (left < screenWidth) {
				/* No room for previous column - go to previous page */
				View pv = mChildViews.get(mCurrent-1);
				if (pv == null) /* No page to advance to */
					return;
				int prevDocWidth = pv.getMeasuredWidth();
				int prevDocHeight = pv.getMeasuredHeight();

				// Allow for the next page maybe being shorter than the screen is high
				yOffset = (prevDocHeight < screenHeight ? ((prevDocHeight - screenHeight)>>1) : 0);

				int prevLeft = -(pv.getLeft() + mXScroll);
				int prevTop = -(pv.getTop() + mYScroll);
				if (prevDocWidth < screenWidth) {
					// Previous page is too narrow to fill the screen. Scroll to the bottom, centred.
					xOffset = (prevDocWidth - screenWidth)>>1;
				} else {
					// Reset X back to the right hand column
					xOffset = (left > 0 ? left % screenWidth : 0);
					if (xOffset + screenWidth > prevDocWidth)
						xOffset = prevDocWidth - screenWidth;
					while (xOffset + screenWidth*2 < prevDocWidth)
						xOffset += screenWidth;
				}
				xOffset -= prevLeft;
                // cause page align to bottom
                // when moving back in vertical flip mode with page height smaller than screenHeight
				// yOffset -= prevTop-prevDocHeight+screenHeight;
				yOffset -= prevTop;
			} else {
				// Move to bottom of previous column
				xOffset = -screenWidth;
				yOffset = docHeight - screenHeight + top;
			}
		} else {
			// Retreat by 90% of the screen height downwards (in case lines are partially cut off)
			xOffset = 0;
			yOffset = -smartAdvanceAmount(screenHeight, top);
		}
		mScrollerLastX = mScrollerLastY = 0;
		mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
		mStepper.prod();
	}

	public void resetupChildren() {
		for (int i = 0; i < mChildViews.size(); i++)
			onChildSetup(mChildViews.keyAt(i), mChildViews.valueAt(i));
	}

	public void applyToChildren(ViewMapper mapper) {
		for (int i = 0; i < mChildViews.size(); i++)
			mapper.applyToView(mChildViews.valueAt(i));
	}

	public void refresh() {
		mResetLayout = true;

		mScale = 1.0f;
		mXScroll = mYScroll = 0;
		mSelecting = SELECT.NO_SELECT;

		/* All page views need recreating since both page and screen has changed size,
		 * invalidating both sizes and bitmaps. */
		mAdapter.refresh();
		for (int i = 0; i < mChildViews.size(); i++) {
			View v = mChildViews.valueAt(i);
			onNotInUse(v);
			removeViewInLayout(v);
		}
		mChildViews.clear();
		mViewCache.clear();

		requestLayout();
        amendment();
	}

	public View getView(int i) {
		return mChildViews.get(i);
	}

	public View getDisplayedView() {
		return mChildViews.get(mCurrent);
	}

	public void run() {
		if (!mScroller.isFinished()) {
			mScroller.computeScrollOffset();
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();
			mXScroll += x - mScrollerLastX;
			mYScroll += y - mScrollerLastY;
			mScrollerLastX = x;
			mScrollerLastY = y;
			requestLayout();
			mStepper.prod();
		}
		else if (!mUserInteracting) {
			// End of an inertial scroll and the user is not interacting.
			// The layout is stable
			View v = mChildViews.get(mCurrent);

			if (v != null) {
                // handle scroll end at page middle
                if ((mHorizontalScrolling && !mTextLeft) || (!mHorizontalScrolling && mTextLeft))
                    slideViewOntoScreen(v);

				if (mScroller.isFinished())
				    postSettle(v);
            }
		}
	}

	public boolean onDown(MotionEvent e) {
        if (mLongPressRunnable == null) {
            mLongPressRunnable = new Runnable() {
                public void run() {
                    if (mLongPress) {
                        mLongPress = false;
                        beginSelect(mLongPressEvent.getX(), mLongPressEvent.getY());
                    }
                    mLongPressEvent = null;
                }
            };
        }
        if (mSelecting == SELECT.NO_SELECT) {
            removeCallbacks(mLongPressRunnable);
            mLongPress = true;
            mLongPressEvent = e;
            postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
        }
        else if (mSelecting == SELECT.SELECTING) {
            inSelect(e.getX(), e.getY());
        }

		mScroller.forceFinished(true);
		return true;
	}

    private void beginSelect(float x, float y) {
        for (int i = 0; i < mChildViews.size(); i++) {
            PageView pv = (PageView) mChildViews.valueAt(i);
            if (pointInView(x, y, pv)) {
                if (pv.beginSelect(x, y)) {
                    mSelecting = SELECT.SELECTING;
                    mSelectLeftView = mSelectRightView = mChildViews.keyAt(i);
                    ((DocumentActivity)mContext).showCopyButton(View.VISIBLE);
                    return;
                }
            }
        }
        mBookmarked = true;
        ((DocumentActivity)mContext).createBookmark();
    }

    private void inSelect(float x, float y) {
        for (int i = 0; i < mChildViews.size(); i++) {
            PageView pv = (PageView) mChildViews.valueAt(i);
            if (pointInView(x, y, pv)) {
                int key = mChildViews.keyAt(i);
                if (key == mSelectLeftView && pv.isOnHandle(x, y, 0)) {
                    mSelecting = SELECT.MOVE_LEFT;
                }
                else if (key == mSelectRightView && pv.isOnHandle(x, y, 1)) {
                    mSelecting = SELECT.MOVE_RIGHT;
                }
                else if (key >= mSelectLeftView && key <= mSelectRightView && pv.isOnBox(x, y)) {
                    mSelecting = SELECT.MOVE_IN;
                }
                else
                    mSelecting = SELECT.MOVE_NONE;
                break;
            }
        }
    }

    private void moveSelect(float x, float y) {
        long mt = System.currentTimeMillis();
        if (mt - mMoveTime < MOVE_DELAY) return;

        int v1 = (mSelecting == SELECT.MOVE_LEFT) ? mSelectRightView : mSelectLeftView;
        int v2 = -1;
        for (int i = 0; i < mChildViews.size(); i++) {
            PageView pv = (PageView) mChildViews.valueAt(i);
            if (pointInView(x, y, pv)) {
                v2 = mChildViews.keyAt(i);
                break;
            }
        }
        if (v2 == -1)
            return;

        mSelectLeftView = Math.min(v1, v2);
        mSelectRightView = Math.max(v1, v2);
        int ind = (v1 < v2) ? 1 : ((v1 > v2) ? -1 : 0);
        for (int i = 0; i < mChildViews.size(); i++) {
            PageView pv = (PageView) mChildViews.valueAt(i);
            int key = mChildViews.keyAt(i);
            if (ind == 1) {
                if (key == v1) {
                    pv.moveSelect(1, null, null);
                }
                else if (key == v2) {
                    pv.moveSelect(2, x, y);
                }
                else if (key > v1 && key < v2) {
                    pv.moveSelect(0, null, null);
                }
                else {
                    pv.unSelect();
                }
            }
            else if (ind == -1) {
                if (key == v2) {
                    pv.moveSelect(1, x, y);
                }
                else if (key == v1) {
                    pv.moveSelect(2, null, null);
                }
                else if (key > v2 && key < v1) {
                    pv.moveSelect(0, null, null);
                }
                else {
                    pv.unSelect();
                }
            }
            else {  // ind == 0
                if (key == v1) {
                    int m = (mSelecting == SELECT.MOVE_RIGHT) ? 0 : 1;
                    pv.moveSelect(3 + m, x, y);
                }
                else {
                    pv.unSelect();
                }
            }
        }
        mMoveTime = System.currentTimeMillis();
    }

    public void copy() {
        if (mSelecting != SELECT.SELECTING) return;

        StringBuffer buffer = new StringBuffer("");
        for (int i = mSelectLeftView; i <= mSelectRightView; i++) {
		    PageView pv = (PageView)getDisplayedView();
            buffer.append(pv.copy(i)).append("\n");
        }
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, buffer.toString()));
        endSelect();
    }

    private void endSelect() {
        int minv = 1000, maxv = 0;
        for (int i = 0; i < mChildViews.size(); i++) {
            int key = mChildViews.keyAt(i);
            if (key < minv) minv = key;
            if (key > maxv) maxv = key;
        }
        for (int i = mSelectLeftView; i <= mSelectRightView; i++) {
            PageView pv;
            if (i >= minv && i <= maxv) {
                pv = (PageView) mChildViews.get(i);
                pv.unSelect();
            }
            else {
		        pv = (PageView)getDisplayedView();
                pv.unSelect(i);
            }
        }
        ((DocumentActivity)mContext).showCopyButton(View.GONE);
        mSelecting = SELECT.NO_SELECT;
    }

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (mScaling)
			return true;

        if (mLongPress)
            mLongPress = false;
        if (mSelecting != SELECT.NO_SELECT)
            mSelecting = SELECT.SELECTING;

		View v = mChildViews.get(mCurrent);
		if (v != null) {
            float offx = e1.getX() - e2.getX();
            float offy = e1.getY() - e2.getY();
            if (mLock) {
                if (mHorizontalScrolling) velocityY = 0; else velocityX = 0;
            }
            float vx = Math.abs(velocityX);
            float vy = Math.abs(velocityY);
            if ((vx > vy && mHorizontalScrolling) || (vx < vy && !mHorizontalScrolling)) {
                scrollMore(Math.round(offx), Math.round(offy), velocityX, velocityY);
                return true;
            }
			mScrollerLastX = mScrollerLastY = 0;
			// If the page has been dragged out of bounds then we want to spring back
			// nicely. fling jumps back into bounds instantly, so we don't want to use
			// fling in that case. On the other hand, we don't want to forgo a fling
			// just because of a slightly off-angle drag taking us out of bounds other
			// than in the direction of the drag, so we test for out of bounds only
			// in the direction of travel.
			//
			// Also don't fling if out of bounds in any direction by more than fling
			// margin
			Rect bounds = getScrollBounds(v);
			Rect expandedBounds = new Rect(bounds);
			expandedBounds.inset(-FLING_MARGIN, -FLING_MARGIN);

			if(withinBoundsInDirectionOfTravel(bounds, velocityX, velocityY)
					&& expandedBounds.contains(0, 0)) {
				mScroller.fling(0, 0, (int)velocityX, (int)velocityY, bounds.left, bounds.right, bounds.top, bounds.bottom);
				mStepper.prod();
			}
		}

		return true;
	}

	public void onLongPress(MotionEvent e) { }

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
        if (mLongPress)
            mLongPress = false;
        if (mSelecting == SELECT.MOVE_LEFT || mSelecting == SELECT.MOVE_RIGHT) {
            moveSelect(e2.getX(), e2.getY());
            return true;
        }
        if (mSelecting != SELECT.NO_SELECT)
            mSelecting = SELECT.SELECTING;

		if (!tapDisabled)
			onDocMotion();
		if (!mScaling) {
			if (!mLock || mHorizontalScrolling) mXScroll -= distanceX;
			if (!mLock || !mHorizontalScrolling) mYScroll -= distanceY;
			requestLayout();
		}
		return true;
	}

	public void onShowPress(MotionEvent e) { }

	public boolean onScale(ScaleGestureDetector detector) {
		float previousScale = mScale;
		mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), MIN_SCALE), MAX_SCALE);

		{
			float factor = mScale/previousScale;

			View v = mChildViews.get(mCurrent);
			if (v != null) {
				float currentFocusX = detector.getFocusX();
				float currentFocusY = detector.getFocusY();
				// Work out the focus point relative to the view top left
				int viewFocusX = (int)currentFocusX - (v.getLeft() + mXScroll);
				int viewFocusY = (int)currentFocusY - (v.getTop() + mYScroll);
				// Scroll to maintain the focus point
				mXScroll += viewFocusX - viewFocusX * factor;
				mYScroll += viewFocusY - viewFocusY * factor;

				if (mLastScaleFocusX>=0)
					mXScroll+=currentFocusX-mLastScaleFocusX;
				if (mLastScaleFocusY>=0)
					mYScroll+=currentFocusY-mLastScaleFocusY;

				mLastScaleFocusX=currentFocusX;
				mLastScaleFocusY=currentFocusY;
				requestLayout();
			}
		}
		return true;
	}

	public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (mLongPress)
            mLongPress = false;
        if (mSelecting != SELECT.NO_SELECT)
            mSelecting = SELECT.SELECTING;

		tapDisabled = true;
		mScaling = true;
		// Ignore any scroll amounts yet to be accounted for: the
		// screen is not showing the effect of them, so they can
		// only confuse the user
		mXScroll = mYScroll = 0;
		mLastScaleFocusX = mLastScaleFocusY = -1;
		return true;
	}

	public void onScaleEnd(ScaleGestureDetector detector) {
		mScaling = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		mScaleGestureDetector.onTouchEvent(event);
		mGestureDetector.onTouchEvent(event);

		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
			mUserInteracting = true;
		}
		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // move ended
            if (mSelecting == SELECT.MOVE_LEFT || mSelecting == SELECT.MOVE_RIGHT)
                mSelecting = SELECT.SELECTING;

			mUserInteracting = false;

			View v = mChildViews.get(mCurrent);
			if (v != null) {
				if ((mHorizontalScrolling && !mTextLeft) || (!mHorizontalScrolling && mTextLeft)) {
				    if (mScroller.isFinished()) {
				        // If, at the end of user interaction, there is no
				        // current inertial scroll in operation then animate
				        // the view onto screen if necessary
				        slideViewOntoScreen(v);
				    }
				}

				if (mScroller.isFinished()) {
					// If still there is no inertial scroll in operation
					// then the layout is stable
					postSettle(v);
				}
			}
		}

		// requestLayout();
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int n = getChildCount();
		for (int i = 0; i < n; i++)
			measureView(getChildAt(i));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		try {
			onLayout2(changed, left, top, right, bottom);
		}
		catch (java.lang.OutOfMemoryError e) {
			System.out.println("Out of memory during layout");
		}
	}

	private void onLayout2(boolean changed, int left, int top, int right,
			int bottom) {

		// "Edit mode" means when the View is being displayed in the Android GUI editor. (this class
		// is instantiated in the IDE, so we need to be a bit careful what we do).
		if (isInEditMode())
			return;

		View cv = mChildViews.get(mCurrent);
		Point cvOffset;

		if (!mResetLayout) {
			// Move to next or previous if current is sufficiently off center
			if (cv != null) {
				boolean move;
				cvOffset = subScreenSizeOffset(cv);
				// cv.getRight() may be out of date with the current scale
				// so add left to the measured width for the correct position
				if (mHorizontalScrolling)
					move = cv.getLeft() + cv.getMeasuredWidth() + cvOffset.x + GAP/2 + mXScroll < getWidth()/2;
				else
					move = cv.getTop() + cv.getMeasuredHeight() + cvOffset.y + GAP/2 + mYScroll < getHeight()/2;

                if (!mTextLeft) {
				if (move && mCurrent + 1 < mAdapter.getCount()) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					mStepper.prod();

					onMoveOffChild(mCurrent);
					mCurrent++;
					onMoveToChild(mCurrent);
				}
                }
                else {
				if (move && mCurrent > 0) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					mStepper.prod();

					onMoveOffChild(mCurrent);
					mCurrent--;
					onMoveToChild(mCurrent);
				}
                }

				if (mHorizontalScrolling)
					move = cv.getLeft() - cvOffset.x - GAP/2 + mXScroll >= getWidth()/2;
				else
					move = cv.getTop() - cvOffset.y - GAP/2 + mYScroll >= getHeight()/2;
                if (!mTextLeft) {
				if (move && mCurrent > 0) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					mStepper.prod();

					onMoveOffChild(mCurrent);
					mCurrent--;
					onMoveToChild(mCurrent);
				}
                }
                else {
				if (move && mCurrent + 1 < mAdapter.getCount()) {
					postUnsettle(cv);
					// post to invoke test for end of animation
					// where we must set hq area for the new current view
					mStepper.prod();

					onMoveOffChild(mCurrent);
					mCurrent++;
					onMoveToChild(mCurrent);
				}
                }
			}

			// Remove not needed children and hold them for reuse
			int numChildren = mChildViews.size();
			int childIndices[] = new int[numChildren];
			for (int i = 0; i < numChildren; i++)
				childIndices[i] = mChildViews.keyAt(i);

			for (int i = 0; i < numChildren; i++) {
				int ai = childIndices[i];
				if (ai < mCurrent - 3 || ai > mCurrent + 3) {
					View v = mChildViews.get(ai);
					onNotInUse(v);
                    if (mAdapter.cacheable(ai))
					    mViewCache.add(v);
					removeViewInLayout(v);
					mChildViews.remove(ai);
				}
			}
		} else {
			mResetLayout = false;
			mXScroll = mYScroll = 0;

			// Remove all children and hold them for reuse
			int numChildren = mChildViews.size();
			for (int i = 0; i < numChildren; i++) {
				View v = mChildViews.valueAt(i);
				onNotInUse(v);
                int ai = mChildViews.keyAt(i);
                if (mAdapter.cacheable(ai))
				    mViewCache.add(v);
				removeViewInLayout(v);
			}
			mChildViews.clear();

			// post to ensure generation of hq area
			mStepper.prod();
		}

		// Ensure current view is present
		int cvLeft, cvRight, cvTop, cvBottom;
		boolean notPresent = (mChildViews.get(mCurrent) == null);
		cv = getOrCreateChild(mCurrent);
		// When the view is sub-screen-size in either dimension we
		// offset it to center within the screen area, and to keep
		// the views spaced out
		cvOffset = subScreenSizeOffset(cv);
		if (notPresent) {
            if (mFocus ) {
                cvLeft = mPrevLeft;
                cvTop = mPrevTop;
            }
            else {
			    // Main item not already present. Just place it top left
			    cvLeft = cvOffset.x;
			    cvTop = cvOffset.y;
                /**
                 * during many calls of the method for a notPresent, cv Measured dimension always changed
                 * here the centering code is not accurate, amend it after jump end
                 */
                if (mHorizontalScrolling) {
                    if (cv.getMeasuredWidth() < getWidth()) {
                        cvLeft += (getWidth() - cv.getMeasuredWidth()) / 2;
                    }
                    else if (mTextLeft) {
                        cvLeft -= cv.getMeasuredWidth() - getWidth();
                    }
                }
                else {
                    if (cv.getMeasuredHeight() < getHeight()) {
                        cvTop += (getHeight() - cv.getMeasuredHeight()) / 2;
                    }
                }
            }
		} else {
            // check for scroll outside page
            if (mHorizontalScrolling) {
                mYScroll = validateYScroll(cv, mYScroll);
                mXScroll = validateXBeyond(cv, mXScroll);
            }
            else {
                mXScroll = validateXScroll(cv, mXScroll);
                mYScroll = validateYBeyond(cv, mYScroll);
            }
			// Main item already present. Adjust by scroll offsets
			cvLeft = cv.getLeft() + mXScroll;
			cvTop = cv.getTop() + mYScroll;
		}
		cvRight = cvLeft + cv.getMeasuredWidth();
		cvBottom = cvTop + cv.getMeasuredHeight();
		Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));

		// if (!mUserInteracting && mScroller.isFinished()) {
		// 	if (anyway to identify a notPresent here, don't know) {
		// 	    cvRight += corr.x;
		// 	    cvLeft += corr.x;
		// 	    cvTop += corr.y;
		// 	    cvBottom += corr.y;
		// 	}
		// } else if (mHorizontalScrolling && cv.getMeasuredHeight() <= getHeight()) {
		if (mHorizontalScrolling && cv.getMeasuredHeight() <= getHeight()) {
			// When the current view is as small as the screen in height, clamp
			// it vertically
			cvTop += corr.y;
			cvBottom += corr.y;
		} else if (!mHorizontalScrolling && cv.getMeasuredWidth() <= getWidth()) {
			// When the current view is as small as the screen in width, clamp
			// it horizontally
			cvLeft += corr.x;
			cvRight += corr.x;
		}

		cv.layout(cvLeft, cvTop, cvRight, cvBottom);

        if (mCurrent > 0) {
            View lrv = null;
            if (!mTextLeft)
                lrv = leftView(cv, -1, true);
            else
                lrv = rightView(cv, -1, true);
            if (lrv != null && mCurrent > 1) {
                View lrv2 = null;
                if (!mTextLeft)
                    lrv2 = leftView(lrv, -2, true);
                else
                    lrv2 = rightView(lrv, -2, true);
                if (lrv2 != null && mCurrent > 2) {
                    if (!mTextLeft)
                        leftView(lrv2, -3, false);
                    else
                        rightView(lrv2, -3, false);
                }
            }
        }

        if (mCurrent + 1 < mAdapter.getCount()) {
            View lrv = null;
            if (!mTextLeft)
                lrv = rightView(cv, 1, true);
            else
                lrv = leftView(cv, 1, true);
            if (lrv != null && mCurrent + 2 < mAdapter.getCount()) {
                View lrv2 = null;
                if (!mTextLeft)
                    lrv2 = rightView(lrv, 2, true);
                else
                    lrv2 = leftView(lrv, 2, true);
                if (lrv != null && mCurrent + 3 < mAdapter.getCount()) {
                    if (!mTextLeft)
                        rightView(lrv2, 3, false);
                    else
                        leftView(lrv2, 3, false);
                }
            }
        }

		mXScroll = mYScroll = 0;
		invalidate();
	}

    private View leftView(View v, int n, boolean force) {
        Point off = subScreenSizeOffset(v);
        View v2 = null;
        if (mHorizontalScrolling && (force || mXScroll > 0)) {                 // move content to right
            v2 = getOrCreateChild(mCurrent + n);
            Point off2 = subScreenSizeOffset(v2);
            int gap = off.x + GAP + off2.x;
            v2.layout(v.getLeft() - gap - v2.getMeasuredWidth()
                    , (v.getTop() + v.getBottom() - v2.getMeasuredHeight()) / 2
                    , v.getLeft() - gap
                    , (v.getTop() + v.getBottom() + v2.getMeasuredHeight()) / 2
                    );
        } else if (!mHorizontalScrolling && (force || mYScroll > 0)){          // move content to bottom
            v2 = getOrCreateChild(mCurrent + n);
            Point off2 = subScreenSizeOffset(v2);
            int gap = off.y + GAP + off2.y;
            v2.layout((v.getLeft() + v.getRight() - v2.getMeasuredWidth()) / 2
                    , v.getTop() - gap - v2.getMeasuredHeight()
                    , (v.getLeft() + v.getRight() + v2.getMeasuredWidth()) / 2
                    , v.getTop() - gap
                    );
        }
        else {
            rmExtraView(mChildViews.get(mCurrent + n), mCurrent + n);
        }
        return v2;
    }

    private View rightView(View v, int n, boolean force) {
        Point off = subScreenSizeOffset(v);
        View v2 = null;
        if (mHorizontalScrolling && (force || mXScroll < 0)) {                 // move content to left
            v2 = getOrCreateChild(mCurrent + n);
            Point off2 = subScreenSizeOffset(v2);
            int gap = off.x + GAP + off2.x;
            v2.layout(v.getRight() + gap
                    , (v.getTop() + v.getBottom() - v2.getMeasuredHeight()) / 2
                    , v.getRight() + gap + v2.getMeasuredWidth()
                    , (v.getTop() + v.getBottom() + v2.getMeasuredHeight()) / 2
                    );
        } else if (!mHorizontalScrolling && (force || mYScroll < 0)){          // move content to top
            v2 = getOrCreateChild(mCurrent + n);
            Point off2 = subScreenSizeOffset(v2);
            int gap = off.y + GAP + off2.y;
            v2.layout((v.getLeft() + v.getRight() - v2.getMeasuredWidth()) / 2
                    , v.getBottom() + gap
                    , (v.getLeft() + v.getRight() + v2.getMeasuredWidth()) / 2
                    , v.getBottom() + gap + v2.getMeasuredHeight()
                    );
        }
        else {
            rmExtraView(mChildViews.get(mCurrent + n), mCurrent + n);
        }
        return v2;
    }

    private void rmExtraView(View v, int ai) {
        if (v == null) return;
        onNotInUse(v);
        if (mAdapter.cacheable(ai))
            mViewCache.add(v);
        removeViewInLayout(v);
        mChildViews.remove(ai);
    }

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		if (mAdapter != null && mAdapter != adapter)
			mAdapter.releaseBitmaps();
		mAdapter = (PageAdapter) adapter;

		requestLayout();
	}

	@Override
	public void setSelection(int arg0) {
		throw new UnsupportedOperationException(getContext().getString(R.string.not_supported));
	}

	private View getCached() {
		if (mViewCache.size() == 0)
			return null;
		else
			return mViewCache.removeFirst();
	}

	private View getOrCreateChild(int i) {
		View v = mChildViews.get(i);
		if (v == null) {
			v = mAdapter.getView(i, getCached(), this);
			addAndMeasureChild(i, v);
			onChildSetup(i, v);
		}

		return v;
	}

	private void addAndMeasureChild(int i, View v) {
		LayoutParams params = v.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(v, 0, params, true);
		mChildViews.append(i, v); // Record the view against its adapter index
		measureView(v);
	}

	private void measureView(View v) {
		// See what size the view wants to be
		v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

		// Work out a scale that will fit it to this view
        float sx = (float)getWidth()/(float)v.getMeasuredWidth();
        float sy = (float)getHeight()/(float)v.getMeasuredHeight();
        float scale;
        if ((mHorizontalScrolling && !mTextLeft) || (!mHorizontalScrolling && mTextLeft))
		    scale = Math.min(sx,sy);
        else if (mHorizontalScrolling)
		    scale = sy;
        else
            scale = sx;
		// Use the fitting values scaled by our current scale factor
		v.measure(View.MeasureSpec.EXACTLY | (int)(v.getMeasuredWidth()*scale*mScale),
				View.MeasureSpec.EXACTLY | (int)(v.getMeasuredHeight()*scale*mScale));
	}

	private Rect getScrollBounds(int left, int top, int right, int bottom) {
		int xmin = getWidth() - right;
		int xmax = -left;
		int ymin = getHeight() - bottom;
		int ymax = -top;

		// In either dimension, if view smaller than screen then
		// constrain it to be central
		if (xmin > xmax) xmin = xmax = (xmin + xmax)/2;
		if (ymin > ymax) ymin = ymax = (ymin + ymax)/2;

		return new Rect(xmin, ymin, xmax, ymax);
	}

	private Rect getScrollBounds(View v) {
		// There can be scroll amounts not yet accounted for in
		// onLayout, so add mXScroll and mYScroll to the current
		// positions when calculating the bounds.
		return getScrollBounds(v.getLeft() + mXScroll,
				v.getTop() + mYScroll,
				v.getLeft() + v.getMeasuredWidth() + mXScroll,
				v.getTop() + v.getMeasuredHeight() + mYScroll);
	}

	private Point getCorrection(Rect bounds) {
		return new Point(Math.min(Math.max(0,bounds.left),bounds.right),
				Math.min(Math.max(0,bounds.top),bounds.bottom));
	}

	private void postSettle(final View v) {
		// onSettle and onUnsettle are posted so that the calls
		// won't be executed until after the system has performed
		// layout.
		post (new Runnable() {
			public void run () {
				onSettle(v);
			}
		});
	}

	private void postUnsettle(final View v) {
		post (new Runnable() {
			public void run () {
				onUnsettle(v);
			}
		});
	}

	private boolean slideViewOntoScreen(View v) {
		Point corr = getCorrection(getScrollBounds(v));
		if (corr.x != 0 || corr.y != 0) {
			mScrollerLastX = mScrollerLastY = 0;
			mScroller.startScroll(0, 0, corr.x, corr.y, 1000);
			mStepper.prod();
            return true;
		}
        return false;
	}

	private Point subScreenSizeOffset(View v) {
        return new Point(0,0);
		// return new Point(Math.max((getWidth() - v.getMeasuredWidth())/2, 0),
		// 		Math.max((getHeight() - v.getMeasuredHeight())/2, 0));
	}

	private static int directionOfTravel(float vx, float vy) {
		if (Math.abs(vx) > 2 * Math.abs(vy))
			return (vx > 0) ? MOVING_RIGHT : MOVING_LEFT;
		else if (Math.abs(vy) > 2 * Math.abs(vx))
			return (vy > 0) ? MOVING_DOWN : MOVING_UP;
		else
			return MOVING_DIAGONALLY;
	}

	private static boolean withinBoundsInDirectionOfTravel(Rect bounds, float vx, float vy) {
		switch (directionOfTravel(vx, vy)) {
		case MOVING_DIAGONALLY: return bounds.contains(0, 0);
		case MOVING_LEFT: return bounds.left <= 0;
		case MOVING_RIGHT: return bounds.right >= 0;
		case MOVING_UP: return bounds.top <= 0;
		case MOVING_DOWN: return bounds.bottom >= 0;
		default: throw new NoSuchElementException();
		}
	}

	protected void onTapMainDocArea() {}
	protected void onDocMotion() {}

	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
		invalidate();
	}

	public boolean onSingleTapUp(MotionEvent e) {
        if (mLongPress)
            mLongPress = false;
        if (mSelecting == SELECT.SELECTING)
            return true;
        if (mBookmarked) {
            mBookmarked = false;
            return true;
        }
        if (mSelecting == SELECT.MOVE_NONE) {
            endSelect();
            return true;
        }
        if (mSelecting != SELECT.NO_SELECT)
            mSelecting = SELECT.SELECTING;

		if (!tapDisabled) {
			PageView pageView = (PageView) getDisplayedView();
			if (mLinksEnabled && pageView != null) {
				int page = pageView.hitLink(e.getX(), e.getY());
				if (page > -1) {
					pushHistory();
					setDisplayedViewIndex(page);
                    return true;
                // external link, handled
				} else if (page == -1){
                    return true;
				}
			}
            // page < -1, hit nothing, proceed
            if (e.getX() < tapPageMargin) {
				if (mTextLeft) smartMoveForwards(); else smartMoveBackwards();
			} else if (e.getX() > super.getWidth() - tapPageMargin) {
				if (mTextLeft) smartMoveBackwards(); else smartMoveForwards();
			} else if (e.getY() < tapPageMargin) {
				if (mTextLeft) smartMoveForwards(); else smartMoveBackwards();
			} else if (e.getY() > super.getHeight() - tapPageMargin) {
				if (mTextLeft) smartMoveBackwards(); else smartMoveForwards();
			} else {
				onTapMainDocArea();
			}
		}
		return true;
	}

    /*
     * both even or odd page
     */
    private boolean sameSide(int i, int j) {
        return (i + j) % 2 == 0;
    }

    private boolean pointInView(float x, float y, View v) {
        return (x > v.getLeft() && x < v.getRight() && y > v.getTop() && y < v.getBottom());
    }


    /**
     * before go to absolute view, save current position
     * smart focus accounted
     */
    private void savePosition(int i) {
        View cv = getDisplayedView();
        mPrevLeft = cv.getLeft();
        if (mSmartFocus && !sameSide(mCurrent, i)) {
            mPrevLeft = -(cv.getRight() - getWidth());
        }
        mPrevTop = cv.getTop();
    }

    private void amendment() {
        postDelayed(new Runnable() {
            public void run() {
                View v = getDisplayedView();
                if (slideViewOntoScreen(v)) {
                    if (mScroller.isFinished())
                        postSettle(v);
                }
            }
        }, 200);
    }

    /**
     * for vertical flip, do not allow scroll beyond left and right side
     */
    private int validateXScroll(View v, int xScroll) {
        if (xScroll > 0) {                          // move content to right
            int left = -(v.getLeft());
            if (xScroll > left) xScroll = left;
        }
        else if (xScroll < 0) {                     // move content to left
            int right = -(v.getRight() - getWidth());
            if (xScroll < right) xScroll = right;
        }
        return xScroll;
    }

    /**
     * for vertical flip, do not allow scroll beyond first and last view
     */
    private int validateYBeyond(View v, int yScroll) {
        boolean hclam = v.getMeasuredHeight() < getHeight();
        int clam = (getHeight() - v.getMeasuredHeight()) / 2;
        if (mCurrent == 0) {
            if (!mTextLeft && yScroll > 0) {
                int top = -(v.getTop());
		        if (hclam) top += clam;
                if (yScroll > top) yScroll = top;
            }
            else if (mTextLeft && yScroll < 0) {
                int bottom = -(v.getBottom() - getHeight());
		        if (hclam) bottom -= clam;
                if (yScroll < bottom) yScroll = bottom;
            }
        }
        if (mCurrent == mAdapter.getCount() - 1) {
            if (!mTextLeft && yScroll < 0) {
                int bottom = -(v.getBottom() - getHeight());
		        if (hclam) bottom -= clam;
                if (yScroll < bottom) yScroll = bottom;
            }
            else if (mTextLeft && yScroll > 0) {
                int top = -(v.getTop());
		        if (hclam) top += clam;
                if (yScroll > top) yScroll = top;
            }
        }
        return yScroll;
    }

    /**
     * for horizontal flip, do not allow scroll beyond top and bottom side
     */
    private int validateYScroll(View v, int yScroll) {
        if (yScroll > 0) {                      // move content to bottom
            int top = -(v.getTop());
            if (yScroll > top) yScroll = top;
        }
        else if (yScroll < 0) {                 // move content to top
            int bottom = -(v.getBottom() - getHeight());
            if (yScroll < bottom) yScroll = bottom;
        }
        return yScroll;
    }

    /**
     * for horizontal flip, do not allow scroll beyond first and last view
     */
    private int validateXBeyond(View v, int xScroll) {
        boolean wclam = v.getMeasuredWidth() < getWidth();
        int clam = (getWidth() - v.getMeasuredWidth()) / 2;
        if (mCurrent == 0) {
            if (!mTextLeft && xScroll > 0) {
                int left = -(v.getLeft());
		        if (wclam) left += clam;
                if (xScroll > left) xScroll = left;
            }
            else if (mTextLeft && xScroll < 0) {
                int right = -(v.getRight() - getWidth());
		        if (wclam) right -= clam;
                if (xScroll < right) xScroll = right;
            }
        }
        if (mCurrent == mAdapter.getCount() - 1) {
            if (!mTextLeft && xScroll < 0) {
                int right = -(v.getRight() - getWidth());
		        if (wclam) right -= clam;
                if (xScroll < right) xScroll = right;
            }
            else if (mTextLeft && xScroll > 0) {
                int left = -(v.getLeft());
		        if (wclam) left += clam;
                if (xScroll > left) xScroll = left;
            }
        }
        return xScroll;
    }

    /*
     * within focus mode
     * scroll to prev or next page
     * smart focus accounted
     * dir: -1/+1
     */
    private boolean scrollOne(int dir) {
        if (!(dir == 1 || dir == -1)) return false;

        /*
         * generally, v.left <= 0, v.right >= getWidth()(screen width)
         * v.right - v.left = v.width
         */
        View v = mChildViews.get(mCurrent);
        if (v == null)
            return false;

        /*
         * when nv is far off the screen
         * both nv.left and nv.right are negative or positive
         */
        View nv = mChildViews.get(mCurrent + dir);
        if (nv == null)
            return false;

        int xOffset = 0, yOffset = 0, smart = 0;

        if ((mTextLeft && dir == 1) || (!mTextLeft && dir == -1)) {
            if (mSmartFocus && nv.getWidth() > getWidth()) {
                smart = nv.getRight() - getWidth() + nv.getLeft() + 2 * nv.getWidth();
            }
        }
        else {
            if (mSmartFocus && v.getWidth() > getWidth()) {
                smart = v.getRight() - getWidth() + v.getLeft();
            }
        }

        if (mHorizontalScrolling) {
            if ((mTextLeft && dir == 1) || (!mTextLeft && dir == -1)) {
                xOffset += nv.getWidth();
            }
            else {
                xOffset -= v.getWidth();
            }
        }
        else {
            if ((mTextLeft && dir == 1) || (!mTextLeft && dir == -1)) {
                yOffset += nv.getHeight();
                if (mSmartFocus && nv.getWidth() > getWidth())
                    smart -= 2 * nv.getWidth();
            }
            else {
                yOffset -= v.getHeight();
            }
        }

		xOffset -= smart;
		mScrollerLastX = mScrollerLastY = 0;
		mScroller.startScroll(0, 0, xOffset, yOffset, 400);
		mStepper.prod();
        return true;
    }

    /**
     * continuous scroll
     * un-relevent to focus mode
     */
    private void scrollMore(int xOffset, int yOffset, float xv, float yv) {
        View v = mChildViews.get(mCurrent);
        if (v == null) return;

        int speed = 2000;

        if (mHorizontalScrolling) {
            xOffset += (int)(v.getWidth() * xv / speed);
        }
        else {
            yOffset += (int)(v.getHeight() * yv / speed);
        }

		mScrollerLastX = mScrollerLastY = 0;
        mScroller.fling(0, 0, (int)xv, (int)yv
                , Math.min(0, xOffset)
                , Math.max(0, xOffset)
                , Math.min(0, yOffset)
                , Math.max(0, yOffset));
		mStepper.prod();
    }

    public void toggleSingleColumn() {
        mSingleColumn = !mSingleColumn ;
        if (mChildViews.size() == 0) return;
        if (mSingleColumn) {
            mCurrent = (mCurrent * 2) - 1;
            if (mCurrent < 0) mCurrent = 0;
        }
        else {
            mCurrent = (mCurrent + 1) / 2;
        }
        refresh();
    }

    public void toggleTextLeft() {
        mTextLeft = !mTextLeft;
        if (mChildViews.size() == 0) return;
        endSelect();
        postDelayed(new Runnable(){
            public void run() {
                PageView pv = (PageView) getDisplayedView();
                if (mTextLeft) {
                    int del = getWidth() - pv.getRight();
		            mScrollerLastX = mScrollerLastY = 0;
                    mScroller.startScroll(0,0,del,0,1000);
		            mStepper.prod();
                }
                else {
                    // to make slide work
                    mYScroll = 1;
                    slideViewOntoScreen(pv);
				    if (mScroller.isFinished())
				        postSettle(pv);
                }
            }
        }, 200);
    }

    public void toggleFlipVertical() {
        mHorizontalScrolling = !mHorizontalScrolling;
        if (mChildViews.size() == 0) return;
		requestLayout();
    }

    public void toggleLock() {
        mLock = !mLock;
    }

    public void toggleCropMargin() {
        refresh();
    }

    float scaleTo, scaleStep;
    int xScrollStep, yScrollStep, step;
    Stepper focusStepper;

    public void toggleFocus(boolean isReflowable) {
        mFocus = !mFocus;
        if (isReflowable) return;

        mPrevLeft = mPrevTop = 0;
        PageView pv = (PageView) getDisplayedView();
        float pvwidth = (float)pv.getWidth();
        float pvheight = (float)pv.getHeight();
        float rx = getWidth() / pvwidth;
        float ry = getHeight() / pvheight;
        int xScrollTo, yScrollTo;
        if (mFocus) {
            if (ry > rx) {
                scaleTo = ry / rx;
                xScrollTo = Math.round((getWidth() - ry * pvwidth) / 2);
                yScrollTo = 0;
            }
            else {
                scaleTo = rx / ry;
                xScrollTo = 0;
                yScrollTo = Math.round((getHeight() - rx * pvheight) / 2);
            }
        }
        else {
            scaleTo = 1.0f;
            if (ry > rx) {
                xScrollTo = 0;
                yScrollTo = Math.round((getHeight() - rx * pvheight) / 2);
            }
            else {
                yScrollTo = 0;
                xScrollTo = Math.round((getWidth() - ry * pvwidth) / 2);
            }
        }
        /*
         * scroll:value < 0 when page move toward left/top off screen
         */
        step = 10;
        scaleStep = (scaleTo - mScale) / step;
        xScrollStep = (xScrollTo - pv.getLeft()) / step;
        yScrollStep = (yScrollTo - pv.getTop()) / step;
        focusStepper = new Stepper(this, new Runnable() {
            @Override
            public void run() {
                mScale += scaleStep;
                mXScroll = xScrollStep;
                mYScroll = yScrollStep;
                if (--step > 0) {
		            requestLayout();
                    focusStepper.prod();
                }
                else {
                    mScale = scaleTo;
                    requestLayout();
		            if (mScroller.isFinished()) {
			            postSettle(pv);
		            }
                }
            }
        });
        focusStepper.prod();
    }

    public void toggleSmartFocus() {
        mSmartFocus = !mSmartFocus;
    }

	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((PageView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((PageView) v).setSearchBoxes(null);

		((PageView) v).setLinkHighlighting(mLinksEnabled);
	}

	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}

	protected void onMoveOffChild(int i) {
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
        // this will update from original data when zoomed to make text clear
		((PageView) v).updateHq(false);
        if (mCurrent == ((PageView) v).getPage() && !mAdapter.isReflowable()) {
            boolean vis = mCurrent > 0
                    && mCurrent < (mAdapter.getCount() - 1);
            ((DocumentActivity)mContext).showSingleColumnButton(vis ? View.VISIBLE : View.GONE);
        }
        if (mCurrent > 0) {
            View v1 = mChildViews.get(mCurrent - 1);
            if ((mHorizontalScrolling && (v1.getLeft() + v1.getMeasuredWidth()) > 0)
                    || (!mHorizontalScrolling && (v1.getTop() + v1.getMeasuredHeight()) > 0)) {
                ((PageView) v1).updateHq(false);
            }
        }
        if (mCurrent < mAdapter.getCount() - 1) {
            View v2 = mChildViews.get(mCurrent + 1);
            if ((mHorizontalScrolling && v2.getLeft() < getWidth())
                    || (!mHorizontalScrolling && v2.getTop() < getHeight())) {
                ((PageView) v2).updateHq(false);
            }
        }
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((PageView) v).removeHq();
        if (mCurrent > 0) {
            View v1 = mChildViews.get(mCurrent - 1);
            ((PageView) v1).removeHq();
        }
        if (mCurrent < mAdapter.getCount() - 1) {
            View v2 = mChildViews.get(mCurrent + 1);
            ((PageView) v2).removeHq();
        }
	}

	protected void onNotInUse(View v) {
		((PageView) v).releaseResources();
	}
}
