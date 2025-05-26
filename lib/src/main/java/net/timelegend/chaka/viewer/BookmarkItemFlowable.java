package net.timelegend.chaka.viewer;

import java.util.Objects;

public class BookmarkItemFlowable extends BookmarkItem {
    public int pageCount;
    public ChapterPage chapterPage;
    public int realPage2;
    public int pageCount2;

    public BookmarkItemFlowable(String title, int page, int realPage, long bookmark, int pageCount) {
        super(title, page, realPage, bookmark);
        this.pageCount = pageCount;
    }

    @Override
    public int compareTo(BookmarkItem bi) {
        return page - bi.page;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(super.toString());
        sb.append(", pageCount=");
        sb.append(pageCount);
        sb.append(", chapterPage=");
        sb.append(chapterPage);
        sb.append(", realPage2=");
        sb.append(realPage2);
        sb.append(", pageCount2=");
        sb.append(pageCount2);
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BookmarkItemFlowable) {
            BookmarkItemFlowable bi = (BookmarkItemFlowable)obj;
            return bi.realPage == realPage && bi.pageCount == pageCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(realPage, pageCount);
    }

}
