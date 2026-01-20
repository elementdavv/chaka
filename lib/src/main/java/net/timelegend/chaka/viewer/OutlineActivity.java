package net.timelegend.chaka.viewer;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
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
    private MotionEvent e0 = null;
    private long mFlingTime = 0L;
    private int mDirection = 0;         // animation direction, 0:default, 1:left to right, 2:right to left
    private MenuItem mItem;
    private int idx = -1;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if (Tool.mFullscreen) {
            Tool.cutout(getWindow(), true);
        }
        setContentView(R.layout.outline_activity);

        ActionBar ab = getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);

		idx = getIntent().getIntExtra(Tool.PALLETBUNDLE, -1);

        if (idx > -1) {
            initToc();
        }
        initBookmarks();
        refresh(ab);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Tool.fullScreen(getWindow());
    }

    private void initToc() {
        rvOutline = (RecyclerView)findViewById(R.id.rvOutline);

        rvOutline.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e0 == null) {
                    e0 = MotionEvent.obtain(e);
                    mFlingTime = System.currentTimeMillis();
                    return false;
                }
                else {
                    long mt = System.currentTimeMillis();
                    if (mt - mFlingTime < 2 * ViewConfiguration.getDoubleTapTimeout()) {
                        if (Math.abs(e.getY() - e0.getY()) < Tool.SWIPE_MAX_OFF_PATH) {
                            float diffx = e.getX() - e0.getX();
                            if (diffx > Tool.SWIPE_MIN_DISTANCE) {
                                e0.recycle();
                                e0 = null;
                                close();
                                return true;
                            }
                            else if (diffx < -1 * Tool.SWIPE_MIN_DISTANCE) {
                                e0.recycle();
                                e0 = null;
                                mDirection = 2;
                                onOptionsItemSelected(mItem);
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                    }
                }
                e0.recycle();
                e0 = null;
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

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

        rvBookmark.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e0 == null) {
                    e0 = MotionEvent.obtain(e);
                    mFlingTime = System.currentTimeMillis();
                    return false;
                }
                else {
                    long mt = System.currentTimeMillis();
                    if (mt - mFlingTime < 2 * ViewConfiguration.getDoubleTapTimeout()) {
                        if (Math.abs(e.getY() - e0.getY()) < Tool.SWIPE_MAX_OFF_PATH) {
                            float diffx = e.getX() - e0.getX();
                            if (diffx > Tool.SWIPE_MIN_DISTANCE) {
                                e0.recycle();
                                e0 = null;
                                if (idx > -1) {
                                    mDirection = 1;
                                    onOptionsItemSelected(mItem);
                                }
                                else {
                                    close();
                                }
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                    }
                }
                e0.recycle();
                e0 = null;
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        BookmarkAdapter adapter = new BookmarkAdapter(this);
        rvBookmark.setAdapter(adapter);
        rvBookmark.setLayoutManager(new LinearLayoutManager(this));
    }

    public void close() {
        super.onBackPressed();
    }

    private void refresh(ActionBar ab) {
        switch (content) {
        case TOC:
            if (mDirection == 0) {
                rvOutline.setVisibility(View.VISIBLE);
                rvBookmark.setVisibility(View.INVISIBLE);
                ab.setTitle(R.string.toc);
            }
            // swipe from left to right
            else if (mDirection == 1) {
                Animation anim = new TranslateAnimation(-rvOutline.getWidth(), 0, 0, 0);
                anim.setDuration(200);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                        rvOutline.setVisibility(View.VISIBLE);
                        rvOutline.bringToFront();
                    }
                    public void onAnimationRepeat(Animation animation) {}
                    public void onAnimationEnd(Animation animation) {
                        rvBookmark.setVisibility(View.INVISIBLE);
                        ab.setTitle(R.string.toc);
                    }
                });
                rvOutline.startAnimation(anim);
            }
            // swipe from right to left
            else if (mDirection == 2) {
            }
            break;
        case BOOKMARK:
            if (mDirection == 0) {
                rvBookmark.setVisibility(View.VISIBLE);
                ab.setTitle(R.string.bookmarks);
            }
            // swipe from left to right
            else if (mDirection == 1) {
            }
            // swipe from right to left
            else if (mDirection == 2) {
                Animation anim = new TranslateAnimation(rvBookmark.getWidth(), 0, 0, 0);
                anim.setDuration(200);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                        rvBookmark.setVisibility(View.VISIBLE);
                        rvBookmark.bringToFront();
                    }
                    public void onAnimationRepeat(Animation animation) {}
                    public void onAnimationEnd(Animation animation) {
                        rvOutline.setVisibility(View.INVISIBLE);
                        ab.setTitle(R.string.bookmarks);
                    }
                });
                rvBookmark.startAnimation(anim);
            }
            break;
        }
        mDirection = 0;
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
            mItem = menu.findItem(R.id.menu_content);

            if (updateMenu(mItem))
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
