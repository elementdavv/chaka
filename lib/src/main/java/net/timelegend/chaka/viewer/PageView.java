package net.timelegend.chaka.viewer;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Quad;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.FileUriExposedException;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.os.AsyncTask;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends ImageView {

	public OpaqueImageView(Context context) {
		super(context);
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
}

public class PageView extends ViewGroup {
	private final MuPDFCore mCore;

	private static final int HIGHLIGHT_COLOR = 0x80cc6600;
	private static final int LINK_COLOR = 0x800066cc;
	private static final int SELECTION_COLOR = 0x8090EE90;            // lightgreen
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private static final int PROGRESS_DIALOG_DELAY = 200;

	protected final Context mContext;

	protected     int       mPageNumber;
	private       Point     mParentSize;
	protected     Point     mSize;   // Size of page at minimum zoom
	protected     PointF    mRenderOff;   // crop margin render offset
	protected     float     mSourceScale;
    protected     int       mViewWidth;
    protected     int       mViewHeight;
    protected     float     mScale;                 // view vs source

	private       ImageView mEntire; // Image rendered at minimum zoom
	private       Bitmap    mEntireBm;
	private       Matrix    mEntireMat;
	private       AsyncTask<Void,Void,Link[]> mGetLinkInfo;
	private       CancellableAsyncTask<Void, Boolean> mDrawEntire;

	private       Point     mPatchViewSize; // View size on the basis of which the patch was created
	private       Rect      mPatchArea;
	private       ImageView mPatch;
	private       Bitmap    mPatchBm;
	private       CancellableAsyncTask<Void, Boolean> mDrawPatch;
    private       Bitmap    mColumnBm;
	private       Quad      mSearchBoxes[][];
	protected     Link      mLinks[];
	private       View      mSearchView;
	private       boolean   mIsBlank;
	private       boolean   mHighlightLinks;

	private       ImageView mErrorIndicator;

	private       ProgressBar mBusyIndicator;
	private final Handler   mHandler = new Handler(Looper.getMainLooper());

	public PageView(Context c, MuPDFCore core, Point parentSize, Bitmap sharedHqBm) {
		super(c);
		mContext = c;
		mCore = core;
		mParentSize = parentSize;
		setBackgroundColor(BACKGROUND_COLOR);
        // the parent is correct screen
		mEntireBm = Bitmap.createBitmap(parentSize.x, parentSize.y, Config.ARGB_8888);
		mPatchBm = sharedHqBm;
		mEntireMat = new Matrix();
	}

	private void reinit() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel();
			mDrawPatch = null;
		}

		if (mGetLinkInfo != null) {
			mGetLinkInfo.cancel(true);
			mGetLinkInfo = null;
		}

		mIsBlank = true;
		mPageNumber = 0;

		if (mSize == null)
			mSize = mParentSize;

		if (mEntire != null) {
			mEntire.setImageBitmap(null);
			mEntire.invalidate();
		}

		if (mPatch != null) {
			mPatch.setImageBitmap(null);
			mPatch.invalidate();
		}

		mPatchViewSize = null;
		mPatchArea = null;

		mSearchBoxes = null;
		mLinks = null;

		clearRenderError();
	}

	public void releaseResources() {
		reinit();

		if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}
		clearRenderError();
	}

	public synchronized void releaseBitmaps() {
		reinit();

		// recycle bitmaps before releasing them.

		if (mEntireBm!=null)
			mEntireBm.recycle();
		mEntireBm = null;

		if (mPatchBm!=null)
			mPatchBm.recycle();
		mPatchBm = null;

        if (mColumnBm!=null)
            mColumnBm.recycle();
        mColumnBm = null;
	}

	public void blank(int page) {
		reinit();
		mPageNumber = page;

		if (mBusyIndicator == null) {
			mBusyIndicator = new ProgressBar(mContext);
			mBusyIndicator.setIndeterminate(true);
			addView(mBusyIndicator);
		}

		setBackgroundColor(BACKGROUND_COLOR);
	}

	protected void clearRenderError() {
		if (mErrorIndicator == null)
			return;

		removeView(mErrorIndicator);
		mErrorIndicator = null;
		invalidate();
	}

	protected void setRenderError(String why) {

		int page = mPageNumber;
		reinit();
		mPageNumber = page;

		if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}
		if (mSearchView != null) {
			removeView(mSearchView);
			mSearchView = null;
		}

		if (mErrorIndicator == null) {
			mErrorIndicator = new OpaqueImageView(mContext);
			mErrorIndicator.setScaleType(ImageView.ScaleType.CENTER);
			addView(mErrorIndicator);
			Drawable mErrorIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_red_24dp, null);
			mErrorIndicator.setImageDrawable(mErrorIcon);
			mErrorIndicator.setBackgroundColor(BACKGROUND_COLOR);
		}

		setBackgroundColor(Color.TRANSPARENT);
		mErrorIndicator.bringToFront();
		mErrorIndicator.invalidate();
	}

    // the page is correctPage, the size is full size
	public void setPage(int page, RectF rSize) {
        PointF size;
        if (rSize == null) {
            setRenderError("Error loading page");
            size = new PointF(612, 792);
            mRenderOff = new PointF(0, 0);
        }
        else {
            size = new PointF(rSize.left, rSize.top);
            mRenderOff = new PointF(rSize.right, rSize.bottom);
        }

		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		mIsBlank = false;
		// Highlights may be missing because mIsBlank was true on last draw
		if (mSearchView != null)
			mSearchView.invalidate();

		mPageNumber = page;

		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		mSourceScale = Math.min(mParentSize.x/size.x, mParentSize.y/size.y);
		Point newSize = new Point((int)(size.x*mSourceScale), (int)(size.y*mSourceScale));
		mSize = newSize;

        if (mCore.isSplitPage(mPageNumber)) {
            mSize.x = (mSize.x + 1) / 2;
        }

		if (mErrorIndicator != null)
			return;

		if (mEntire == null) {
			mEntire = new OpaqueImageView(mContext);
			mEntire.setScaleType(ImageView.ScaleType.MATRIX);
			addView(mEntire);
		}

		mEntire.setImageBitmap(null);
		mEntire.invalidate();

		// Get the link info in the background
		mGetLinkInfo = new AsyncTask<Void,Void,Link[]>() {
			protected Link[] doInBackground(Void... v) {
				return getLinkInfo();
			}

			protected void onPostExecute(Link[] v) {
				mLinks = v;
				if (mSearchView != null)
					mSearchView.invalidate();
			}
		};

		mGetLinkInfo.execute();

		// Render the page in the background
		mDrawEntire = new CancellableAsyncTask<Void, Boolean>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

			@Override
			public void onPreExecute() {
				setBackgroundColor(BACKGROUND_COLOR);
				mEntire.setImageBitmap(null);
				mEntire.invalidate();

				if (mBusyIndicator == null) {
					mBusyIndicator = new ProgressBar(mContext);
					mBusyIndicator.setIndeterminate(true);
					addView(mBusyIndicator);
					mBusyIndicator.setVisibility(INVISIBLE);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (mBusyIndicator != null)
								mBusyIndicator.setVisibility(VISIBLE);
						}
					}, PROGRESS_DIALOG_DELAY);
				}
			}

			@Override
			public void onPostExecute(Boolean result) {
				removeView(mBusyIndicator);
				mBusyIndicator = null;
				if (result.booleanValue()) {
					clearRenderError();
                    if (mCore.isSplitPage(mPageNumber)) {
                        int cx = getColumnX(mSize.x, mEntireBm.getWidth());
                        mColumnBm = Bitmap.createBitmap(mEntireBm, cx, 0, mEntireBm.getWidth() / 2, mEntireBm.getHeight());
					    mEntire.setImageBitmap(mColumnBm);
                    }
                    else
					    mEntire.setImageBitmap(mEntireBm);
					mEntire.invalidate();
				} else {
					setRenderError("Error rendering page");
				}
				setBackgroundColor(Color.TRANSPARENT);
			}
		};

		mDrawEntire.execute();

		if (mSearchView == null) {
			mSearchView = new View(mContext) {
				@Override
				protected void onDraw(final Canvas canvas) {
					super.onDraw(canvas);
					// Work out current total scale factor
					// from source to view
		            mViewWidth = getWidth();
		            mViewHeight = getHeight();
		            mScale = mSourceScale*(float)mViewWidth/(float)mSize.x;

					final Paint paint = new Paint();

					if (!mIsBlank && mSearchBoxes != null) {
						paint.setColor(HIGHLIGHT_COLOR);
						for (Quad[] searchBox : mSearchBoxes) {
							for (Quad q : searchBox) {
								drawRect(q.toRect(), canvas, paint);
							}
						}
					}

					if (!mIsBlank && mLinks != null && mHighlightLinks) {
						paint.setColor(LINK_COLOR);
						for (Link link : mLinks) {
							drawRect(link.getBounds(), canvas, paint);
						}
					}

					MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(mPageNumber);
					if (tsmodel != null && tsmodel.selectionBoxes != null && tsmodel.selectionBoxes.length > 0) {
						paint.setColor(SELECTION_COLOR);
						for (Quad q : tsmodel.selectionBoxes) {
							drawRect(q.toRect(), canvas, paint);
						}
						Rect l = null, r = null;
						if ((tsmodel.dir & 1) > 0) {
							l = drawLeftHandle(tsmodel.textHandles[0].x, tsmodel.textHandles[0].y, canvas);
						}
						if ((tsmodel.dir & 2) > 0) {
							r = drawRightHandle(tsmodel.textHandles[1].x, tsmodel.textHandles[1].y, canvas);
						}
						tsmodel.rectHandles[0] = l;
						tsmodel.rectHandles[1] = r;
						mCore.putTSModel(mPageNumber, tsmodel);
					}
				}
			};

			addView(mSearchView);
		}
		requestLayout();
	}

    private void drawRect(com.artifex.mupdf.fitz.Rect r, Canvas canvas, Paint paint) {
        PointF p0 = src2View(r.x0, r.y0);
        PointF p1 = src2View(r.x1, r.y1);
        if (mCore.isSplitPage(mPageNumber)) {
            if (mCore.isRightPage(mPageNumber)) {
                if (p1.x < 0) return;
                if (p0.x < 0) p0.x = 0;
            }
            else {
                if (p0.x > mViewWidth) return;
            }
        }
        canvas.drawRect(p0.x, p0.y, p1.x, p1.y, paint);
    }

    private Rect drawLeftHandle(float x0, float y0, Canvas canvas) {
        PointF p = src2View(x0, y0);
        int x = Math.round(p.x), y = Math.round(p.y);
        // 176*88
        int hx1 = 132, hx2= 44, hy = 88;
        Drawable h;
        Rect r;
        if (x - hx1 >= 0 && y + hy <= mViewHeight) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_left_material);
            h.setBounds(x - hx1, y, x + hx2, y + hy);
            r = new Rect(x - hy, y, x, y + hy);
        }
        else if (x - hx1 >= 0) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_left_top_material);
            h.setBounds(x - hx1, y - hy, x + hx2, y);
            r = new Rect(x - hy, y - hy, x, y);
        }
        else if (y + hy <= mViewHeight) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_right_material);
            h.setBounds(x - hx2, y, x + hx1, y + hy);
            r = new Rect(x, y, x + hy, y + hy);
        }
        else {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_right_top_material);
            h.setBounds(x - hx2, y - hy, x + hx1, y);
            r = new Rect(x, y - hy, x + hy, y);
        }
        h.draw(canvas);
        return r;
    }

    private Rect drawRightHandle(float x1, float y1, Canvas canvas) {
        PointF p = src2View(x1, y1);
        int x = Math.round(p.x), y = Math.round(p.y);
        // 176*88
        int hx1 = 132, hx2= 44, hy = 88;
        Drawable h;
        Rect r;
        if (x + hx1 <= mViewWidth && y + hy <= mViewHeight) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_right_material);
            h.setBounds(x - hx2, y, x + hx1, y + hy);
            r = new Rect(x, y, x + hy, y + hy);
        }
        else if (x + hx1 <= mViewWidth) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_right_top_material);
            h.setBounds(x - hx2, y - hy, x + hx1, y);
            r = new Rect(x, y - hy, x + hy, y);
        }
        else if (y + hy <= mViewHeight) {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_left_material);
            h.setBounds(x - hx1, y, x + hx2, y + hy);
            r = new Rect(x - hy, y, x, y + hy);
        }
        else {
            h = ContextCompat.getDrawable(mContext, R.drawable.text_select_handle_left_top_material);
            h.setBounds(x - hx1, y - hy, x + hx2, y);
            r = new Rect(x - hy, y - hy, x, y);
        }
        h.draw(canvas);
        return r;
    }

	public void setSearchBoxes(Quad searchBoxes[][]) {
		mSearchBoxes = searchBoxes;
		if (mSearchView != null)
			mSearchView.invalidate();
	}

	public void setLinkHighlighting(boolean f) {
		mHighlightLinks = f;
		if (mSearchView != null)
			mSearchView.invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		switch(View.MeasureSpec.getMode(widthMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			x = mSize.x;
			break;
		default:
			x = View.MeasureSpec.getSize(widthMeasureSpec);
		}
		switch(View.MeasureSpec.getMode(heightMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		if (mBusyIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mBusyIndicator.measure(View.MeasureSpec.AT_MOST | limit, View.MeasureSpec.AT_MOST | limit);
		}
		if (mErrorIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mErrorIndicator.measure(View.MeasureSpec.AT_MOST | limit, View.MeasureSpec.AT_MOST | limit);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int w = right-left;
		int h = bottom-top;

		if (mEntire != null) {
			if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
				mEntireMat.setScale(w/(float)mSize.x, h/(float)mSize.y);
				mEntire.setImageMatrix(mEntireMat);
				mEntire.invalidate();
			}
			mEntire.layout(0, 0, w, h);
		}

		if (mSearchView != null) {
			mSearchView.layout(0, 0, w, h);
		}

		if (mPatchViewSize != null) {
			if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
				// Zoomed since patch was created
				mPatchViewSize = null;
				mPatchArea = null;
				if (mPatch != null) {
					mPatch.setImageBitmap(null);
					mPatch.invalidate();
				}
			} else {
				mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
			}
		}

		if (mBusyIndicator != null) {
			int bw = mBusyIndicator.getMeasuredWidth();
			int bh = mBusyIndicator.getMeasuredHeight();

			mBusyIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}

		if (mErrorIndicator != null) {
			int bw = (int) (8.5 * mErrorIndicator.getMeasuredWidth());
			int bh = (int) (11 * mErrorIndicator.getMeasuredHeight());
			mErrorIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}
	}

	public void updateHq(boolean update) {
		if (mErrorIndicator != null) {
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatch.invalidate();
			}
			return;
		}

		Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
		if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
			// If the viewArea's size matches the unzoomed size, there is no need for an hq patch
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatch.invalidate();
			}
		} else {
			final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
			final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

			// Intersect and test that there is an intersection
			if (!patchArea.intersect(viewArea))
				return;

			// Offset patch area to be relative to the view top left
			patchArea.offset(-viewArea.left, -viewArea.top);

			boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

			// If being asked for the same area as last time and not because of an update then nothing to do
			if (area_unchanged && !update)
				return;

			boolean completeRedraw = !(area_unchanged && update);

			// Stop the drawing of previous patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel();
				mDrawPatch = null;
			}

			// Create and add the image view if not already done
			if (mPatch == null) {
				mPatch = new OpaqueImageView(mContext);
				mPatch.setScaleType(ImageView.ScaleType.MATRIX);
				addView(mPatch);
				if (mSearchView != null)
					mSearchView.bringToFront();
			}

            // offset patch area for split right page
            int splitleft = patchArea.left;
            if (mCore.isSplitPage(mPageNumber) && mCore.isRightPage(mPageNumber)) {
                splitleft += viewArea.width();
            }

			CancellableTaskDefinition<Void, Boolean> task;

			if (completeRedraw)
				task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
								splitleft, patchArea.top,
								patchArea.width(), patchArea.height());
			else
				task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
						splitleft, patchArea.top,
						patchArea.width(), patchArea.height());

			mDrawPatch = new CancellableAsyncTask<Void, Boolean>(task) {

				public void onPostExecute(Boolean result) {
					if (result.booleanValue()) {
						mPatchViewSize = patchViewSize;
						mPatchArea = patchArea;
						clearRenderError();
                        if (mCore.isSplitPage(mPageNumber)) {
                            int cx = 0;
                            mColumnBm = Bitmap.createBitmap(mPatchBm, cx, 0, mPatchBm.getWidth() / 2, mPatchBm.getHeight());
					        mPatch.setImageBitmap(mColumnBm);
                        }
                        else
					        mPatch.setImageBitmap(mPatchBm);
						mPatch.invalidate();
						//requestLayout();
						// Calling requestLayout here doesn't lead to a later call to layout. No idea
						// why, but apparently others have run into the problem.
						mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
					} else {
						setRenderError("Error rendering patch");
					}
				}
			};

			mDrawPatch.execute();
		}
	}

	public void update() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel();
			mDrawPatch = null;
		}

		// Render the page in the background
		mDrawEntire = new CancellableAsyncTask<Void, Boolean>(getUpdatePageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

			public void onPostExecute(Boolean result) {
				if (result.booleanValue()) {
					clearRenderError();
                    if (mCore.isSplitPage(mPageNumber)) {
                        int cx = getColumnX(mSize.x, mEntireBm.getWidth());
                        mColumnBm = Bitmap.createBitmap(mEntireBm, cx, 0, mEntireBm.getWidth() / 2, mEntireBm.getHeight());
					    mEntire.setImageBitmap(mColumnBm);
                    }
                    else
					    mEntire.setImageBitmap(mEntireBm);
					mEntire.invalidate();
				} else {
					setRenderError("Error updating page");
				}
			}
		};

		mDrawEntire.execute();

		updateHq(true);
	}

	public void removeHq() {
			// Stop the drawing of the patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel();
				mDrawPatch = null;
			}

			// And get rid of it
			mPatchViewSize = null;
			mPatchArea = null;
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatch.invalidate();
			}
	}

	public int getPage() {
		return mPageNumber;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}

	public int hitLink(Link link) {
		if (link.isExternal()) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getURI()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
			try {
				mContext.startActivity(intent);
			} catch (FileUriExposedException x) {
				Tool.e(x.toString());
				Tool.toast("Android does not allow following file:// link: " + link.getURI());
			} catch (Throwable x) {
				Tool.e(x.toString());
				Tool.toast(x.getMessage());
			}
			return -1;
		} else {
			return mCore.resolveLink(link);
		}
	}

    /*
     * return
     * > -1: page number in the document
     * == -1: external link, handled
     * < -1: hit nothing, not handled
     */
	public int hitLink(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		PointF p = view2Src(x, y);

		if (mLinks != null)
			for (Link l: mLinks)
				if (l.getBounds().contains(p.x, p.y))
					return hitLink(l);
		return -2;
	}

    public boolean beginSelect(float x, float y) {
        MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(mPageNumber, true);
        if (tsmodel.sText == null) return false;

        PointF p = view2Src(x, y);
        com.artifex.mupdf.fitz.Point fp1 = new com.artifex.mupdf.fitz.Point(p.x, p.y);
        com.artifex.mupdf.fitz.Point fp2 = new com.artifex.mupdf.fitz.Point(p.x, p.y);
        Quad q = tsmodel.sText.snapSelection(fp1, fp2, 1);     // 0:char, 1:word, 2:line

        PointF ur = new PointF(q.ur_x, q.ur_y);
        PointF ll = new PointF(q.ll_x, q.ll_y);
        PointF lr = new PointF(q.lr_x, q.lr_y);
        PointF ml = new PointF(q.ul_x, (q.ul_y + q.ll_y) / 2);
        PointF mr = new PointF(q.lr_x, (q.lr_y + q.ur_y) / 2);
        boolean sel = (!ur.equals(lr)) && q.contains(p.x, p.y);
        if (sel) {
            tsmodel.selectionBoxes = new Quad[1];
            tsmodel.selectionBoxes[0] = q;
            tsmodel.textHandles[0] = ll;
            tsmodel.textHandles[1]= lr;
            tsmodel.boundries[0]= ml;
            tsmodel.boundries[1]= mr;
            tsmodel.dir = 3;
            mCore.putTSModel(mPageNumber, tsmodel);
            mSearchView.invalidate();
        }
        return sel;
    }

    public void moveSelect(int dir, Float x, Float y) {
        MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(mPageNumber, true);
        PointF p1, p2;
        if (dir == 1) {                 // left handle
            if (x != null)
                p1 = view2Src(x, y);
            else
                p1 = tsmodel.boundries[0];
            p2 = view2Src(getRight() - 1, getBottom() - 1);
        }
        else if (dir == 2) {            // right handle
            p1 = view2Src(getLeft() + 1, getTop() + 1);
            if (x != null)
                p2 = view2Src(x, y);
            else
                p2 = tsmodel.boundries[1];
        }
        else if (dir == 0) {            // no handle
            p1 = view2Src(getLeft() + 1, getTop() + 1);
            p2 = view2Src(getRight() - 1, getBottom() - 1);
        }
        else {                          // both handle
            p1 = view2Src(x, y);
            p2 = tsmodel.boundries[dir - 3];
            if (p1.y > p2.y) {
                PointF pt = p1;
                p1 = p2;
                p2 = pt;
            }
            dir = 3;
        }
        com.artifex.mupdf.fitz.Point fp1 = new com.artifex.mupdf.fitz.Point(p1.x, p1.y);
        com.artifex.mupdf.fitz.Point fp2 = new com.artifex.mupdf.fitz.Point(p2.x, p2.y);
        Quad[] qs = tsmodel.sText.highlight(fp1, fp2);
        if (
            (qs.length == 0)
            || (p1.y < qs[0].ul_y && (qs[0].ul_y - p1.y < qs[0].ll_y - qs[0].ul_y))
            || (p2.y > qs[qs.length - 1].lr_y && (p2.y - qs[qs.length - 1].lr_y < qs[qs.length - 1].lr_y - qs[qs.length - 1].ur_y))
        )
            return;

        PointF ll = new PointF(qs[0].ll_x, qs[0].ll_y);
        PointF lr = new PointF(qs[qs.length - 1].lr_x, qs[qs.length - 1].lr_y);
        PointF ml = new PointF(qs[0].ul_x, (qs[0].ul_y + qs[0].ll_y) / 2);
        PointF mr = new PointF(qs[qs.length - 1].lr_x, (qs[qs.length - 1].lr_y + qs[qs.length - 1].ur_y) / 2);
        tsmodel.selectionBoxes = qs;
        tsmodel.textHandles[0] = ll;
        tsmodel.textHandles[1]= lr;
        tsmodel.boundries[0]= ml;
        tsmodel.boundries[1]= mr;
        tsmodel.dir = dir;
        mCore.putTSModel(mPageNumber, tsmodel);
        mSearchView.invalidate();
    }

    public String copy(int position) {
        MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(position);
        if (tsmodel == null) return "";
        PointF p1 = tsmodel.boundries[0];
        PointF p2 = tsmodel.boundries[1];
        com.artifex.mupdf.fitz.Point fp1 = new com.artifex.mupdf.fitz.Point(p1.x, p1.y);
        com.artifex.mupdf.fitz.Point fp2 = new com.artifex.mupdf.fitz.Point(p2.x, p2.y);
        return tsmodel.sText.copy(fp1, fp2);
    }

    public void unSelect() {
        mCore.rmTSModel(mPageNumber);
        mSearchView.invalidate();
    }

    public void unSelect(int position) {
        mCore.rmTSModel(position);
    }

    public boolean isOnHandle(float x, float y, int ind) {
        float x1 = x - getLeft();
        float y1 = y - getTop();
        MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(mPageNumber);
        return tsmodel.rectHandles[ind].contains((int)x1, (int)y1);
    }

    public boolean isOnBox(float x, float y) {
        PointF p = view2Src(x, y);
        MuPDFCore.TextSelectionModel tsmodel = mCore.getTSModel(mPageNumber);
        for (Quad q : tsmodel.selectionBoxes) {
            if (q.contains(p.x, p.y))
                return true;
        }
        return false;
    }

	protected CancellableTaskDefinition<Void, Boolean> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
		return new MuPDFCancellableTaskDefinition<Void, Boolean>() {
			@Override
			public Boolean doInBackground(Cookie cookie, Void ... params) {
				if (bm == null)
					return false;
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
						Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					bm.eraseColor(0);
				try {
					mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
					return true;
				} catch (RuntimeException e) {
					return false;
				}
			}
		};

	}

	protected CancellableTaskDefinition<Void, Boolean> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY, final int patchWidth, final int patchHeight)
	{
		return new MuPDFCancellableTaskDefinition<Void, Boolean>() {
			@Override
			public Boolean doInBackground(Cookie cookie, Void ... params) {
				if (bm == null)
					return false;
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
						Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					bm.eraseColor(0);
				try {
					mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
					return true;
				} catch (RuntimeException e) {
					return false;
				}
			}
		};
	}

	protected Link[] getLinkInfo() {
		try {
			return mCore.getPageLinks(mPageNumber);
		} catch (RuntimeException e) {
			return null;
		}
	}

    public PointF view2Src(float x, float y) {
        if (mCore.isSplitPage(mPageNumber)) {
            if (mCore.isRightPage(mPageNumber)) {
                x += mViewWidth;
            }
        }
        float x1 = (x - getLeft())/mScale + mRenderOff.x;
        float y1 = (y - getTop())/mScale + mRenderOff.y;
        return new PointF(x1, y1);
    }

    public PointF src2View(float x, float y) {
        float x1 = (x - mRenderOff.x) * mScale;
        float y1 = (y - mRenderOff.y) * mScale;
        if (mCore.isSplitPage(mPageNumber)) {
            if (mCore.isRightPage(mPageNumber)) {
                x1 -= mViewWidth;
            }
        }
        return new PointF(x1, y1);
    }

    /*
     * for splitted page
     */
    private int getColumnX(int sx, int bmw) {
        if (!mCore.isRightPage(mPageNumber)) {
            return 0;
        }
        return 2 * sx > bmw ? Math.max(bmw - sx, 0) : sx;
    }
}
