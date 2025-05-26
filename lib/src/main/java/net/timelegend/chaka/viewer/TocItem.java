package net.timelegend.chaka.viewer;

import java.io.Serializable;
import java.util.Objects;

public class TocItem implements Serializable {
    public String title;
    public int page;
    public int level;
    public int count;
    public boolean open;

    public TocItem(String title, int page, int level, int count) {
        this.title = title;
        this.page = page;
        this.level = level;
        this.count = count;
        this.open = false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append(",");
        sb.append(page);
        sb.append(",");
        sb.append(level);
        sb.append(",");
        sb.append(count);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TocItem) {
            TocItem item = (TocItem)obj;
            return item.title.equals(title)
                    && item.page == page
                    && item.level == level
                    && item.count == count;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, page, level, count);
    }

}
