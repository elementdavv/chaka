package net.timelegend.chaka.viewer;

import java.io.Serializable;

public abstract class BookmarkItem implements Comparable<BookmarkItem>, Serializable {
    public String title;
    public int page;
    public int realPage;
    public transient long bookmark;

    public BookmarkItem(String title, int page, int realPage, long bookmark) {
        this.title = title;
        this.page = page;
        this.realPage = realPage;
        this.bookmark = bookmark;
    }

    @Override
    abstract public int compareTo(BookmarkItem bi);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("title=");
        sb.append(title);
        sb.append(", page=");
        sb.append(page);
        sb.append(", realPage=");
        sb.append(realPage);
        sb.append(", bookmark=");
        sb.append(bookmark);
        return sb.toString();
    }

}
