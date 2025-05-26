package net.timelegend.chaka.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.os.AsyncTask;

public class PageAdapter extends BaseAdapter {
	private final Context mContext;
	private final MuPDFCore mCore;
	private final SparseArray<RectF> mPageSizes = new SparseArray<RectF>();
	private       Bitmap mSharedHqBm;

	public PageAdapter(Context c, MuPDFCore core) {
		mContext = c;
		mCore = core;
	}

	public int getCount() {
		return mCore.countPages();
	}

	public boolean isReflowable() {
		return mCore.isReflowable();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	public synchronized void releaseBitmaps()
	{
		//  recycle and release the shared bitmap.
		if (mSharedHqBm!=null)
			mSharedHqBm.recycle();
		mSharedHqBm = null;
	}

	public void refresh() {
		mPageSizes.clear();
		mCore.clearTSModel();
	}

    /*
     * cacheable views should have same screen size
     * in single column mode, only cache splitted pages
     */
    public boolean cacheable(int position) {
        return !mCore.isSingleColumn() || mCore.isSplitPage(position);
    }

    // the position is correctPage
	public synchronized View getView(final int position, View convertView, ViewGroup parent) {
		final PageView pageView;
		if (convertView == null) {
            // screen width and height
            int pw = parent.getWidth();
            int ph = parent.getHeight();
            if (mCore.isSplitPage(position)) {
                pw *= 2;
            }
			if (mSharedHqBm == null || mSharedHqBm.getWidth() != pw || mSharedHqBm.getHeight() != ph)
			{
				if (pw > 0 && ph > 0)
					mSharedHqBm = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888);
				else
					mSharedHqBm = null;
			}

			pageView = new PageView(mContext, mCore, new Point(pw, ph), mSharedHqBm);
		} else {
			pageView = (PageView) convertView;
		}

		RectF pageSize = mPageSizes.get(position);
		if (pageSize != null) {
			// We already know the page size. Set it up
			// immediately
			pageView.setPage(position, pageSize);
		} else {
			// Page size as yet unknown. Blank it for now, and
			// start a background task to find the size
			pageView.blank(position);
			AsyncTask<Void,Void,RectF> sizingTask = new AsyncTask<Void,Void,RectF>() {
				@Override
				protected RectF doInBackground(Void... arg0) {
					try {
						return mCore.getPageSize(position);
					} catch (RuntimeException e) {
						return null;
					}
				}

				@Override
				protected void onPostExecute(RectF result) {
					super.onPostExecute(result);
					// We now know the page size
					mPageSizes.put(position, result);
					// Check that this view hasn't been reused for
					// another page since we started
					if (pageView.getPage() == position)
						pageView.setPage(position, result);
				}
			};

			sizingTask.execute((Void)null);
		}
		return pageView;
	}
}
