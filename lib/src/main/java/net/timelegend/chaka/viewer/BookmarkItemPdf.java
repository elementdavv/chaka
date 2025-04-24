package net.timelegend.chaka.viewer;

import java.util.Objects;

public class BookmarkItemPdf extends BookmarkItem {
    public int right;

    public BookmarkItemPdf(String title, int page, int realPage, long bookmark, int right) {
        super(title, page, realPage, bookmark);
        this.right = right;
    }

    @Override
    public int compareTo(BookmarkItem bi) {
        return page - bi.page;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(super.toString());
        sb.append(", right=");
        sb.append(right);
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BookmarkItemPdf) {
            BookmarkItemPdf bi = (BookmarkItemPdf)obj;
            return bi.realPage == realPage && bi.right == right;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(realPage, right);
    }

}
