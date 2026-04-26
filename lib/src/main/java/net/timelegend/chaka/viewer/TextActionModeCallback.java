package net.timelegend.chaka.viewer;

/**
 * reference: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/Editor.java
 */
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextActionModeCallback extends ActionMode.Callback2 {
    private static final int ACTION_MODE_MENU_ITEM_ORDER_COPY = 5;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_SHARE = 7;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL = 8;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_WEB_SEARCH= 20;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;

    private final ReaderView mView;
    private ProcessTextIntentActionsHandler mProcessTextIntentActionsHandler;

    TextActionModeCallback(ReaderView view) {
        mView = view;
        mProcessTextIntentActionsHandler = new ProcessTextIntentActionsHandler(mView);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(null);
        mode.setSubtitle(null);
        mode.setTitleOptionalHint(true);
        populateMenuWithItems(menu);
        mProcessTextIntentActionsHandler.onInitializeMenu(menu);
        return true;
    }


    private void populateMenuWithItems(Menu menu) {
        menu.add(Menu.NONE, android.R.id.copy, ACTION_MODE_MENU_ITEM_ORDER_COPY, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, android.R.id.shareText, ACTION_MODE_MENU_ITEM_ORDER_SHARE, Tool.getResourceString(R.string.share_text))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, android.R.id.selectAll, ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // return false if nothing is done
    }


    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.copy:
            mView.copy();
            break;
        case android.R.id.shareText:
            mView.share();
            break;
        case android.R.id.selectAll:
            mView.selectAll();
            break;
        default:
            if (mProcessTextIntentActionsHandler.performMenuItemAction(item)) {
                mView.endSelect();
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
        outRect.set(mView.getSelectionRect());
    }

    /**
     * A helper for enabling and handling "PROCESS_TEXT" menu actions.
     * These allow external applications to plug into currently selected text.
     */
    static final class ProcessTextIntentActionsHandler {
        private final static int MAX_PARCEL_SIZE = 500 * 1024;
        private final ReaderView mView;
        private final Context mContext;
        private final PackageManager mPackageManager;
        private final String mPackageName;
        private final List<ResolveInfo> mSupportedActivities = new ArrayList<>();

        private ProcessTextIntentActionsHandler(ReaderView view) {
            mView = view;
            mContext = Objects.requireNonNull(mView.getContext());
            mPackageManager = Objects.requireNonNull(mContext.getPackageManager());
            mPackageName = Objects.requireNonNull(mContext.getPackageName());
        }

        /**
         * Adds "PROCESS_TEXT" menu items to the specified menu.
         */
        public void onInitializeMenu(Menu menu) {
            Intent wsintent = new Intent(Intent.ACTION_WEB_SEARCH);

            if (wsintent.resolveActivity(mPackageManager) != null) {
                menu.add(Menu.NONE, Menu.NONE,
                        ACTION_MODE_MENU_ITEM_ORDER_WEB_SEARCH,
                        Tool.getResourceString(R.string.web_search))
                        .setIntent(wsintent)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            loadSupportedActivities();
            final int size = mSupportedActivities.size();

            for (int i = 0; i < size; i++) {
                final ResolveInfo resolveInfo = mSupportedActivities.get(i);
                menu.add(Menu.NONE, Menu.NONE,
                        ACTION_MODE_MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START + i,
                        getLabel(resolveInfo))
                        .setIntent(createProcessTextIntentForResolveInfo(resolveInfo))
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
        }

        /**
         * Performs a "PROCESS_TEXT" action if there is one associated with the specified
         * menu item.
         *
         * @return True if the action was performed, false otherwise.
         */
        public boolean performMenuItemAction(MenuItem item) {
            return fireIntent(item.getIntent());
        }

        private boolean fireIntent(Intent intent) {
            if (intent != null) {
                String selectedText = mView.getSelectedText();

                if (selectedText.length() > MAX_PARCEL_SIZE) {
                    Tool.toast("Selection size " + selectedText.length() + " too large");
                    return false;
                }
                if (intent.getAction() == Intent.ACTION_WEB_SEARCH) {
                    intent.putExtra(SearchManager.QUERY, selectedText);
                }
                else if (intent.getAction() == Intent.ACTION_PROCESS_TEXT) {
                    intent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText);
                }
                else
                    return false;

                mContext.startActivity(intent);
                return true;
            }
            return false;
        }

        private void loadSupportedActivities() {
            mSupportedActivities.clear();
            List<ResolveInfo> unfiltered =
                    mPackageManager.queryIntentActivities(createProcessTextIntent(), 0);
            for (ResolveInfo info : unfiltered) {
                if (isSupportedActivity(info)) {
                    mSupportedActivities.add(info);
                }
            }
        }

        private boolean isSupportedActivity(ResolveInfo info) {
            return mPackageName.equals(info.activityInfo.packageName)
                    || info.activityInfo.exported
                            && (info.activityInfo.permission == null
                                    || mContext.checkSelfPermission(info.activityInfo.permission)
                                            == PackageManager.PERMISSION_GRANTED);
        }

        private Intent createProcessTextIntentForResolveInfo(ResolveInfo info) {
            return createProcessTextIntent()
                    .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    .setClassName(info.activityInfo.packageName, info.activityInfo.name);
        }

        private Intent createProcessTextIntent() {
            return new Intent()
                    .setAction(Intent.ACTION_PROCESS_TEXT)
                    .setType("text/plain");
        }

        private CharSequence getLabel(ResolveInfo resolveInfo) {
            return resolveInfo.loadLabel(mPackageManager);
        }
    }
}
