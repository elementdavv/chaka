package net.timelegend.chaka.viewer;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OutlineActivity extends ComponentActivity
{
    public enum CONTENTS {
        TOC,
        BOOKMARK
    }

    public static CONTENTS content;
    private RecyclerView rvOutline;
    private RecyclerView rvBookmark;
    int idx = -1;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Tool.fullScreen(getWindow());
        setContentView(R.layout.outline_activity);

        ActionBar ab = getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);

		idx = getIntent().getIntExtra("PALLETBUNDLE", -1);

        if (idx > -1) {
            initToc();
        }
        initBookmarks();
        refresh(ab);
    }

    private void initToc() {
        rvOutline = (RecyclerView)findViewById(R.id.rvOutline);
		Bundle bundle = Pallet.receiveBundle(idx);
        TocAdapter adapter = new TocAdapter(this, bundle);
        rvOutline.setAdapter(adapter);
        rvOutline.setLayoutManager(new LinearLayoutManager(this));
        int found = adapter.getfound();

        if (found > 0)
            rvOutline.scrollToPosition(found);
    }

    private void initBookmarks() {
        rvBookmark = (RecyclerView)findViewById(R.id.rvBookmark);
        BookmarkAdapter adapter = new BookmarkAdapter(this);
        rvBookmark.setAdapter(adapter);
        rvBookmark.setLayoutManager(new LinearLayoutManager(this));
    }

    private void refresh(ActionBar ab) {
        switch (content) {
        case TOC:
            ab.setTitle(R.string.toc);
            rvOutline.setVisibility(View.VISIBLE);
            rvBookmark.setVisibility(View.INVISIBLE);
            break;
        case BOOKMARK:
            ab.setTitle(R.string.bookmarks);
            rvBookmark.setVisibility(View.VISIBLE);

            if (idx > -1)
                rvOutline.setVisibility(View.INVISIBLE);

            break;
        }
    }

    private boolean updateMenu(MenuItem item) {
        switch (content) {
        case TOC:
            item.setTitle(R.string.bookmarks);
            item.setIcon(R.drawable.ic_bookmarks_white_24dp);
            return true;
        case BOOKMARK:
            item.setTitle(R.string.toc);
            item.setIcon(R.drawable.ic_toc_white_24dp);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (idx > -1) {
            getMenuInflater().inflate(R.menu.content_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (idx > -1) {
            MenuItem item = menu.findItem(R.id.menu_content);

            if (updateMenu(item))
                return true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        else if (id == R.id.menu_content) {
            ActionBar ab = getActionBar();

            switch (content) {
            case TOC:
                content = CONTENTS.BOOKMARK;
                break;
            case BOOKMARK:
                content = CONTENTS.TOC;
                break;
            default:
                return super.onOptionsItemSelected(item);
            }
            updateMenu(item);
            refresh(ab);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
