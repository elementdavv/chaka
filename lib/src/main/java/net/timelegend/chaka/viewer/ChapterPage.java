package net.timelegend.chaka.viewer;

import java.io.Serializable;
import java.util.Objects;

public class ChapterPage implements Serializable {
    public int chapter;
    public int page;
    public int pageCount;

    public ChapterPage(int chapter, int page, int pageCount) {
        this.chapter = chapter;
        this.page = page;
        this.pageCount = pageCount;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("chapter=");
        sb.append(chapter);
        sb.append("page=");
        sb.append(page);
        sb.append("pageCount=");
        sb.append(pageCount);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChapterPage) {
            ChapterPage cp = (ChapterPage)obj;
            return cp.chapter == chapter && cp.page == page && cp.pageCount == pageCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chapter, page, pageCount);
    }
}
