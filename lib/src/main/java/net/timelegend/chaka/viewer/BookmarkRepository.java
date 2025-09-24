package net.timelegend.chaka.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class BookmarkRepository {
    private final static String BASE_BOOKMARK_NAME = "Bookmark_";
    private final static String BOOKMARK_FOLDER_NAME = "bk";

    private static BookmarkRepository instance = new BookmarkRepository();;
    private MuPDFCore core;
    private String docKey;
	private Map<Integer, BookmarkItem> bookmarks;
    private int maxnum;
    private boolean active;

    public static BookmarkRepository getInstance() {
        return instance;
    }

    public void setup(MuPDFCore core, String docKey) {
        this.core = core;
        this.docKey = docKey.replaceAll("[^a-zA-Z0-9\\._]+", "_");
        bookmarks = null;
        maxnum = 0;
        active = false;
    }

    public void create(int page) {
        if (bookmarks == null)
            load();

        if (bookmarks.get(page) != null) {
            Tool.toastFromResource(R.string.bookmark_already_exists);
            return;
        }

        String title = BASE_BOOKMARK_NAME + String.valueOf(++maxnum);
        int realPage = core.realPage(page);
        long bookmark = 0L;
        BookmarkItem bi;

        if (!core.isReflowable()) {
            int right = (core.isSplitPage(page) && core.isRightPage(page)) ? 1 : 0;
            bi = new BookmarkItemPdf(title, page, realPage, bookmark, right);
        }
        else {
            // disable use of findBookmark
            // bookmark = core.makeBookmark(realPage);
            int pageCount = core.countPages();
            bi = new BookmarkItemFlowable(title, page, realPage, bookmark, pageCount);
            ((BookmarkItemFlowable) bi).chapterPage = core.locatePage(realPage);
        }
        bookmarks.put(page, bi);
        active = true;
        Tool.toastFromResource(R.string.bookmark_created);
    }

    public void remove(BookmarkItem bi) {
        if (bookmarks != null) {
            if (bookmarks.remove(bi.page) != null) {
                active = true;
            }
        }
    }

    public void onEdit(BookmarkItem bi) {
        if (bookmarks.get(bi.page) != null) {
            bookmarks.put(bi.page, bi);
            getMaxnum();
            active = true;
        }
    }

    public void toggleSingleColumn() {
        if (bookmarks != null) {
            Map<Integer, BookmarkItem> marks = new HashMap<>();

            for (Integer key : bookmarks.keySet()) {
                BookmarkItemPdf bi =(BookmarkItemPdf) bookmarks.get(key);
                int page = core.correctPage(bi.realPage);
                if (core.isSingleColumn()) page += bi.right;
                bi.page = page;
                marks.put(page, bi);
            }
            bookmarks = marks;
            Tool.i(bookmarks);
        }
    }

    public void onSizeChange() {
        if (bookmarks != null) {
            Map<Integer, BookmarkItem> marks = new HashMap<>();
            int pageCount = core.countPages();

            for (Integer key : bookmarks.keySet()) {
                BookmarkItemFlowable bi = (BookmarkItemFlowable) bookmarks.get(key);

                /*
                 * for some epubs, findBookmark may take much time
                 * so disable it's use
                 */
                if (bi.pageCount == pageCount) {
                    bi.page = bi.realPage;

                    // if (bi.bookmark == 0L) {
                    //     bi.bookmark = core.makeBookmark(bi.realPage);
                    // }
                }
                else if (bi.pageCount2 == pageCount) {
                    bi.page = bi.realPage2;

                    // if (bi.bookmark == 0L) {
                    //     bi.bookmark = core.makeBookmark(bi.realPage2);
                    // }
                }
                // else if (bi.bookmark != 0L) {
                //     bi.realPage2 = core.findBookmark(bi.bookmark);
                //     bi.pageCount2 = pageCount;
                //     bi.page = bi.realPage2;
                // }
                else {
                    bi.realPage2 = core.estimatePage(bi.chapterPage);
                    bi.pageCount2 = pageCount;
                    bi.page = bi.realPage2;
                }
                marks.put(bi.page, bi);
            }
            bookmarks = marks;
            Tool.i(bookmarks);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, BookmarkItem> load() {
        if (bookmarks == null) {
            File dir = Tool.getDataDir(BOOKMARK_FOLDER_NAME);
            File f = new File(dir, docKey);
            try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(f))) {
                int pageCount = os.readInt();
                boolean isSingleColumn = os.readBoolean();
                bookmarks = (Map<Integer, BookmarkItem>)os.readObject();
                Tool.i(bookmarks);

                if (core.isReflowable()) {
                    if (pageCount != core.countPages()) onSizeChange();
                }
                else {
                    if (isSingleColumn != core.isSingleColumn()) toggleSingleColumn();
                }
                getMaxnum();
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                bookmarks = new HashMap<>();
            }
        }
        return bookmarks;
    }

    public void save() {
        if (!active) return;
        active = false;
        File dir = Tool.getDataDir(BOOKMARK_FOLDER_NAME);
        if (!dir.exists()) dir.mkdir();
        File f = new File(dir, docKey);

        if (bookmarks.size() > 0) {
            try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f))) {
                os.writeInt(core.countPages());
                os.writeBoolean(core.isSingleColumn());
                os.writeObject(bookmarks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            if (f.exists()) f.delete();
        }
    }

    private void getMaxnum() {
        int v = 0;

        if (bookmarks != null) {
            for (Integer key : bookmarks.keySet()) {
                BookmarkItem bi = bookmarks.get(key);
                String title = bi.title;

                if (title.indexOf(BASE_BOOKMARK_NAME) > -1) {
                    try {
                        v = Integer.valueOf(title.substring(BASE_BOOKMARK_NAME.length()));
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                        v = 0;
                    }
                    if (maxnum < v) maxnum = v;
                }
            }
        }
    }

}
