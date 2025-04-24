package net.timelegend.chaka.viewer;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.SeekableInputStream;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.ArrayList;

public class MuPDFCore
{
	private int resolution;
	private Document doc;
	private Outline[] outline;
	private int basePageCount = -1;
	private int pageCount = -1;
	private boolean reflowable = false;
	private int currentPage;
	private Page page;
	private float pageWidth;
	private float pageHeight;
    private float pageLeft;         // crop margin render offset left
    private float pageTop;          // crop margin render offset top
    private final SparseArray<TextSelectionModel> tsModel = new SparseArray<>();
	private DisplayList displayList;
    private boolean singleColumnMode = false;
    private boolean textLeftMode = false;
    private boolean cropMarginMode = false;

	/* Default to "A Format" pocket book size. */
	private int layoutW = 312;
	private int layoutH = 504;
	private int layoutEM = 10;

	private MuPDFCore(Document doc) {
		this.doc = doc;
		if (!doc.needsPassword()) setup();
	}

	private void setup() {
		reflowable = doc.isReflowable();
		// PDFs use default pocket book size
		if (!reflowable) {
			doc.layout(layoutW, layoutH, layoutEM);
			correctPageCount(true);
		}
		// apply consistent css to all flowable docs
		else {
			StringBuilder sb = new StringBuilder();
			sb.append("@page{margin:2em !important;}");
			sb.append("body{display:block;margin:0 !important;padding:0 !important;}");
			sb.append("p{display:block;margin:0.6em 0 !important;}");
			com.artifex.mupdf.fitz.Context.setUserCSS(sb.toString());
		}
		resolution = 160;
		currentPage = -1;
	}

	public MuPDFCore(byte buffer[], String magic) {
		this(Document.openDocument(buffer, magic));
	}

	public MuPDFCore(SeekableInputStream stm, String magic) {
		this(Document.openDocument(stm, magic));
	}

	public String getTitle() {
		return doc.getMetaData(Document.META_INFO_TITLE);
	}

	public int countPages() {
		return pageCount;
	}

	public boolean isReflowable() {
		return reflowable;
	}

	// flowable documents use custom book size
	public synchronized int layout(int oldPage, int w, int h, int em) {
		if (w != layoutW || h != layoutH || em != layoutEM) {
			System.out.println("LAYOUT: " + w + "," + h);
			layoutW = w;
			layoutH = h;
			layoutEM = em;
			long mark = doc.makeBookmark(doc.locationFromPageNumber(realPage(oldPage)));
			doc.layout(layoutW, layoutH, layoutEM);
			correctPageCount(true);
			currentPage = -1;
			outline = null;
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
			return correctPage(doc.pageNumberFromLocation(doc.findBookmark(mark)));
		}
		return oldPage;
	}

    // the pageNum is correctPage
	private synchronized void gotoPage(int pageNum) {
		/* TODO: page cache */
		if (pageNum > pageCount-1)
			pageNum = pageCount-1;
		else if (pageNum < 0)
			pageNum = 0;
		if (pageNum != currentPage) {
			if (page != null)
				page.destroy();
			page = null;
			if (displayList != null)
				displayList.destroy();
			displayList = null;
			page = null;
			pageWidth = 0;
			pageHeight = 0;
            pageLeft = 0;
            pageTop = 0;

			if (doc != null) {
                pageNum = realPage(pageNum);
				page = doc.loadPage(pageNum);
                Rect b = page.getBounds();

                if (cropMarginMode) {
                    Rect bb = getBBox(b);
                    pageLeft = bb.x0 - b.x0;
                    pageTop = bb.y0 - b.y0;
                    b = bb;
                }

				pageWidth = b.x1 - b.x0;
				pageHeight = b.y1 - b.y0;
			}

			currentPage = pageNum;
		}
	}

    /*
     * page full size
     */
	public synchronized RectF getPageSize(int pageNum) {
		gotoPage(pageNum);
        // param order: left, top, right, bottom
		return new RectF(pageWidth, pageHeight, pageLeft, pageTop);
	}

	public synchronized void onDestroy() {
		if (displayList != null)
			displayList.destroy();
		displayList = null;
		if (page != null)
			page.destroy();
		page = null;
		if (doc != null)
			doc.destroy();
		doc = null;
	}

	public synchronized void drawPage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		gotoPage(pageNum);

		if (displayList == null && page != null)
			try {
				displayList = page.toDisplayList();
			} catch (Exception ex) {
				displayList = null;
			}

		if (displayList == null || page == null)
			return;

        if (isSplitPage(currentPage)) {
            pageW *= 2;
        }

		float zoom = resolution / 72;
		Matrix ctm = new Matrix(zoom, zoom);
        Rect b = page.getBounds();

        if (cropMarginMode) {
            b = getBBox(b);
        }

		RectI bbox = new RectI(b.transform(ctm));
		float xscale = (float)pageW / (float)(bbox.x1-bbox.x0);
		float yscale = (float)pageH / (float)(bbox.y1-bbox.y0);
		ctm.scale(xscale, yscale);

        if (cropMarginMode) {
            patchX += bbox.x0 * xscale;
            patchY += bbox.y0 * yscale;
        }

		AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
		try {
			displayList.run(dev, ctm, cookie);
			dev.close();
		} finally {
			dev.destroy();
		}
	}

	public synchronized void updatePage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH, cookie);
	}

    public void toggleSingleColumn() {
        singleColumnMode = !singleColumnMode;
        correctPageCount(false);
    }

    public void toggleTextLeft() {
        textLeftMode = !textLeftMode;
    }

    public void toggleCropMargin() {
        // force reread pagesize of current page
        // prevent display distort when uncrop margin after a page scale
        currentPage = -1;
        cropMarginMode = !cropMarginMode;
    }

	public synchronized Link[] getPageLinks(int pageNum) {
		gotoPage(pageNum);
		return page != null ? page.getLinks() : null;
	}

	public synchronized int resolveLink(Link link) {
        return correctPage(doc.pageNumberFromLocation(doc.resolveLink(link)));
	}

	public synchronized Quad[][] searchPage(int pageNum, String text) {
		gotoPage(pageNum);
		Quad[][] ret = page.search(text);
        if (!isSplitPage(pageNum))
            return ret;

        ArrayList<Quad[]> reslist = new ArrayList<>();
        float mid = pageWidth / 2;
        for (Quad[] r : ret) {
            for (Quad q : r) {
                if ( (!isRightPage(pageNum) && q.ul_x < mid) || (isRightPage(pageNum) && q.ur_x > mid) ) {
                    reslist.add(r);
                    break;
                }
            }
        }
        if (reslist.size() > 0) {
            Quad[][] res = new Quad[reslist.size()][];
            res = reslist.toArray(res);
            return res;
        }
        return null;
	}

	public synchronized boolean hasOutline() {
		if (outline == null) {
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
		}
		return outline != null;
	}

	private void flattenOutlineNodes(ArrayList<TocItem> result, Outline list[], int level) {
		for (Outline node : list) {
			if (node.title != null) {
				int pageNum = correctPage(doc.pageNumberFromLocation(doc.resolveLink(node)));
                int count = 0;
                if (node.down != null) {
                    count = node.down.length;
                }
				result.add(new TocItem(node.title, pageNum, level, count));
			    if (count > 0)
				    flattenOutlineNodes(result, node.down, level + 1);
			}
		}
	}

	public synchronized ArrayList<TocItem> getOutline() {
		ArrayList<TocItem> result = new ArrayList<TocItem>();
		flattenOutlineNodes(result, outline, 0);
		return result;
	}

    public long makeBookmark(int page) {
        return doc.makeBookmark(doc.locationFromPageNumber(page));
    }

    public int findBookmark(long mark) {
        return doc.pageNumberFromLocation(doc.findBookmark(mark));
    }

    public ChapterPage locatePage(int page) {
        Location loc = doc.locationFromPageNumber(page);
        return new ChapterPage(loc.chapter, loc.page, doc.countPages(loc.chapter));
    }

    public int estimatePage(ChapterPage cp) {
        int pageCount = doc.countPages(cp.chapter);
        int page = Math.round(pageCount * (cp.page + 1) / cp.pageCount) - 1;

        if (page < 0 || page >= pageCount) {
            page = (page < 0) ? 0 : pageCount - 1;
        }
        return doc.pageNumberFromLocation(new Location(cp.chapter, page));
    }

    private synchronized Rect getBBox(Rect b) {
        Rect r = page.getBBox();
        // if blank page r is invalid
        if (!r.isValid() || r.isInfinite() || r.isEmpty()) return b;
        r.inset(-2, -2, -2, -2);
        r.x0 = Math.max(r.x0, b.x0);
        r.y0 = Math.max(r.y0, b.y0);
        r.x1 = Math.min(r.x1, b.x1);
        r.y1 = Math.min(r.y1, b.y1);

        // an option: let r similar to b
        // that results in less better effect but consistent display
        //
        // float rw = r.x1 - r.x0;
        // float rh = r.y1 - r.y0;
        // float sr = rh / rw;
        // float sb = (b.y1 - b.y0) / (b.x1 - b.x0);
        // float delta;
        // if (sr < sb) {
        //     float rh2 = rw * sb;
        //     delta = (rh2 - rh) / 2;
        //     r.y0 -= delta;
        //     r.y1 += delta;
        // }
        // else if (sr > sb) {
        //     float rw2 = rh / sb;
        //     delta = (rw2 - rw) / 2;
        //     r.x0 -= delta;
        //     r.x1 += delta;
        // }
        //

        return r;
    }

	public synchronized StructuredText getSText(int pageNum) {
		gotoPage(pageNum);
		return page != null ? page.toStructuredText() : null;
	}

    public boolean isSingleColumn() {
        return singleColumnMode;
    }

    public boolean isSplitPage(int pageNum) {
        return singleColumnMode && pageNum > 0 && pageNum < (pageCount - 1);
    }

    /*
     * for splitted page
     */
    public boolean isRightPage(int pageNum) {
        return (textLeftMode && pageNum % 2 == 1) || (!textLeftMode && pageNum % 2 == 0);
    }

    public boolean isTextLeft() {
        return textLeftMode;
    }

    private void correctPageCount(boolean refresh) {
        if (refresh || basePageCount == -1) {
            basePageCount = doc.countPages();
            // until now, layout complete and UI reaady
        }
        if (singleColumnMode)
            // divide every page into 2 pages, except first and last page
            pageCount = basePageCount * 2 - 2;
        else
            pageCount = basePageCount ;
    }

    public int correctPage(int p) {
        if (singleColumnMode) {
            p = (p * 2) - 1;
            if (p < 0) p = 0;
        }
        return p;
    }

    public int realPage(int p) {
        if (singleColumnMode)
            return (p + 1) / 2;
        return p;
    }

	public synchronized boolean needsPassword() {
		return doc.needsPassword();
	}

	public synchronized boolean authenticatePassword(String password) {
		boolean authenticated = doc.authenticatePassword(password);
		if (authenticated) setup();
		return authenticated;
	}

    public TextSelectionModel getTSModel(int pageNum) {
        return getTSModel(pageNum, false);
    }

    public TextSelectionModel getTSModel(int pageNum, boolean create) {
        TextSelectionModel tsmodel = tsModel.get(pageNum);
        if (tsmodel == null) {
            if (create) {
                tsmodel = new TextSelectionModel();
                tsmodel.sText = getSText(pageNum);
            }
        }
        return tsmodel;
    }

    public void putTSModel(int pageNum, TextSelectionModel tsmodel) {
        tsModel.put(pageNum, tsmodel);
    }

    public void rmTSModel(int pageNum) {
        tsModel.remove(pageNum);
    }

    public void clearTSModel() {
        tsModel.clear();
    }

    public static class TextSelectionModel {
        public StructuredText sText;        // page text structure
        public Quad[]    selectionBoxes;    // selection result on source
        public PointF[]  textHandles = new PointF[2];       // handles point on source
        public PointF[]  boundries= new PointF[2];          // boundries point on source
        public int       dir;               // 0: none, 1: left, 2: right, 3: both
        public android.graphics.Rect[]    rectHandles = new android.graphics.Rect[2];      // handles rect on view
    }
}
