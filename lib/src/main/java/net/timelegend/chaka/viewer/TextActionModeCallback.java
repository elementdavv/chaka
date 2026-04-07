package net.timelegend.chaka.viewer;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

public class TextActionModeCallback extends ActionMode.Callback2 {
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_ASSIST = 0;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_CUT = 4;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_COPY = 5;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_PASTE = 6;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_SHARE = 7;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL = 8;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_REPLACE = 9;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_AUTOFILL = 10;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_PASTE_AS_PLAIN_TEXT = 11;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_WEB_SEARCH= 20;
    // private static final int ACTION_MODE_MENU_ITEM_ORDER_SECONDARY_ASSIST_ACTIONS_START = 50;
    private static final int ACTION_MODE_MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;

    private final ReaderView mView;
    private ProcessTextIntentActionsHandler mProcessTextIntentActionsHandler;
        // private final Path mSelectionPath = new Path();
        // private final RectF mSelectionBounds = new RectF();
        // private final boolean mHasSelection;
        // private final int mHandleHeight;
        // private final AssistantCallbackHelper mHelper = new AssistantCallbackHelper(
        //         getSelectionActionModeHelper());

        TextActionModeCallback(ReaderView view) {
        mView = view;
        mProcessTextIntentActionsHandler = new ProcessTextIntentActionsHandler(mView);
        //     mHasSelection = mode == TextActionMode.SELECTION
        //             || (mTextIsSelectable && mode == TextActionMode.TEXT_LINK);
        //     if (mHasSelection) {
        //         SelectionModifierCursorController selectionController = getSelectionController();
        //         if (selectionController.mStartHandle == null) {
        //             // As these are for initializing selectionController, hide() must be called.
        //             loadHandleDrawables(false /* overwrite */);
        //             selectionController.initHandles();
        //             selectionController.hide();
        //         }
        //         mHandleHeight = Math.max(
        //                 mSelectHandleLeft.getMinimumHeight(),
        //                 mSelectHandleRight.getMinimumHeight());
        //     } else {
        //         InsertionPointCursorController insertionController = getInsertionController();
        //         if (insertionController != null) {
        //             insertionController.getHandle();
        //             mHandleHeight = mSelectHandleCenter.getMinimumHeight();
        //         } else {
        //             mHandleHeight = 0;
        //         }
        //     }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // mHelper.clearCallbackHandlers();
            mode.setTitle(null);
            mode.setSubtitle(null);
            mode.setTitleOptionalHint(true);
            populateMenuWithItems(menu);
            // Callback customCallback = getCustomCallback();
            // if (customCallback != null) {
            //     if (!customCallback.onCreateActionMode(mode, menu)) {
            //         // The custom mode can choose to cancel the action mode, dismiss selection.
            //         Selection.setSelection((Spannable) mTextView.getText(),
            //                 mTextView.getSelectionEnd());
            //         return false;
            //     }
            // }
            // if (mTextView.canProcessText()) {
                mProcessTextIntentActionsHandler.onInitializeMenu(menu);
            // }
            // if (mHasSelection && !mTextView.hasTransientState()) {
            //     mTextView.setHasTransientState(true);
            // }
            return true;
        }

        // private Callback getCustomCallback() {
        //     return mHasSelection
        //             ? mCustomSelectionActionModeCallback
        //             : mCustomInsertionActionModeCallback;
        // }

        private void populateMenuWithItems(Menu menu) {
            // if (mTextView.canCut()) {
            //     menu.add(Menu.NONE, TextView.ID_CUT, ACTION_MODE_MENU_ITEM_ORDER_CUT,
            //             com.android.internal.R.string.cut)
            //                     .setAlphabeticShortcut('x')
            //                     .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            // }
            // if (mTextView.canCopy()) {
                menu.add(Menu.NONE, android.R.id.copy, ACTION_MODE_MENU_ITEM_ORDER_COPY, android.R.string.copy)
                                // .setAlphabeticShortcut('c')
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            // }
            // if (mTextView.canPaste()) {
            //     menu.add(Menu.NONE, TextView.ID_PASTE, ACTION_MODE_MENU_ITEM_ORDER_PASTE,
            //             com.android.internal.R.string.paste)
            //                     .setAlphabeticShortcut('v')
            //                     .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            // }
            // if (mTextView.canShare()) {
                menu.add(Menu.NONE, android.R.id.shareText, ACTION_MODE_MENU_ITEM_ORDER_SHARE, Tool.getResourceString(R.string.share_text))
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            // }
            // if (mTextView.canRequestAutofill()) {
            //     final String selected = mTextView.getSelectedText();
            //     if (selected == null || selected.isEmpty()) {
            //         menu.add(Menu.NONE, TextView.ID_AUTOFILL, ACTION_MODE_MENU_ITEM_ORDER_AUTOFILL,
            //                 com.android.internal.R.string.autofill)
            //                 .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            //     }
            // }
            // if (mTextView.canPasteAsPlainText()) {
            //     menu.add(
            //             Menu.NONE,
            //             TextView.ID_PASTE_AS_PLAIN_TEXT,
            //                     ACTION_MODE_MENU_ITEM_ORDER_PASTE_AS_PLAIN_TEXT,
            //             com.android.internal.R.string.paste_as_plain_text)
            //             .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            // }
            updateSelectAllItem(menu);
            // updateReplaceItem(menu);
            // mHelper.updateAssistMenuItems(menu, null);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // updateSelectAllItem(menu);
            // updateReplaceItem(menu);
            // mHelper.updateAssistMenuItems(menu, null);
            // Callback customCallback = getCustomCallback();
            // if (customCallback != null) {
            //     return customCallback.onPrepareActionMode(mode, menu);
            // }
            return true;
        }

        private void updateSelectAllItem(Menu menu) {
        //     boolean canSelectAll = mTextView.canSelectAllText();
        //     boolean selectAllItemExists = menu.findItem(TextView.ID_SELECT_ALL) != null;
        //     if (canSelectAll && !selectAllItemExists) {
                menu.add(Menu.NONE, android.R.id.selectAll, ACTION_MODE_MENU_ITEM_ORDER_SELECT_ALL, android.R.string.selectAll)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //     } else if (!canSelectAll && selectAllItemExists) {
        //         menu.removeItem(TextView.ID_SELECT_ALL);
        //     }
        }

        // private void updateReplaceItem(Menu menu) {
        //     boolean canReplace = mTextView.isSuggestionsEnabled() && shouldOfferToShowSuggestions();
        //     boolean replaceItemExists = menu.findItem(TextView.ID_REPLACE) != null;
        //     if (canReplace && !replaceItemExists) {
        //         menu.add(Menu.NONE, TextView.ID_REPLACE, ACTION_MODE_MENU_ITEM_ORDER_REPLACE,
        //                 com.android.internal.R.string.replace)
        //             .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //     } else if (!canReplace && replaceItemExists) {
        //         menu.removeItem(TextView.ID_REPLACE);
        //     }
        // }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.copy:
            mView.copy();
            return true;
        case android.R.id.shareText:
            mView.share();
            return true;
        case android.R.id.selectAll:
            mView.selectAll();
            return true;
        }
            // getSelectionActionModeHelper()
            //         .onSelectionAction(item.getItemId(), item.getTitle().toString());
            if (mProcessTextIntentActionsHandler.performMenuItemAction(item)) {
                return true;
            }
            // Callback customCallback = getCustomCallback();
            // if (customCallback != null && customCallback.onActionItemClicked(mode, item)) {
            //     return true;
            // }
            // if (item.getGroupId() == TextView.ID_ASSIST && mHelper.onAssistMenuItemClicked(item)) {
            //     return true;
            // }
            // return mTextView.onTextContextMenuItem(item.getItemId());
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clear mTextActionMode not to recursively destroy action mode by clearing selection.
            // getSelectionActionModeHelper().onDestroyActionMode();
            // mTextActionMode = null;
            // Callback customCallback = getCustomCallback();
            // if (customCallback != null) {
            //     customCallback.onDestroyActionMode(mode);
            // }
            // if (!mPreserveSelection) {
            //     /*
            //      * Leave current selection when we tentatively destroy action mode for the
            //      * selection. If we're detaching from a window, we'll bring back the selection
            //      * mode when (if) we get reattached.
            //      */
            //     Selection.setSelection((Spannable) mTextView.getText(),
            //             mTextView.getSelectionEnd());
            // }
            // if (mSelectionModifierCursorController != null) {
            //     mSelectionModifierCursorController.hide();
            // }
            // mHelper.clearCallbackHandlers();
            // mRequestingLinkActionMode = false;
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            // if (!view.equals(mTextView) || mTextView.getLayout() == null) {
            //     super.onGetContentRect(mode, view, outRect);
            //     return;
            // }
            // final int selectionStart = mTextView.getSelectionStartTransformed();
            // final int selectionEnd = mTextView.getSelectionEndTransformed();
            // final Layout layout = mTextView.getLayout();
            // if (selectionStart != selectionEnd) {
            //     // We have a selection.
            //     mSelectionPath.reset();
            //     layout.getSelectionPath(selectionStart, selectionEnd, mSelectionPath);
            //     mSelectionPath.computeBounds(mSelectionBounds, true);
            //     mSelectionBounds.bottom += mHandleHeight;
            // } else {
            //     // We have a cursor.
            //     int line = layout.getLineForOffset(selectionStart);
            //     float primaryHorizontal =
            //             clampHorizontalPosition(null, layout.getPrimaryHorizontal(selectionEnd));
            //     mSelectionBounds.set(
            //             primaryHorizontal,
            //             layout.getLineTop(line),
            //             primaryHorizontal,
            //             layout.getLineBottom(line) + mHandleHeight);
            // }
            // // Take TextView's padding and scroll into account.
            // int textHorizontalOffset = mTextView.viewportToContentHorizontalOffset();
            // int textVerticalOffset = mTextView.viewportToContentVerticalOffset();
            // outRect.set(
            //         (int) Math.floor(mSelectionBounds.left + textHorizontalOffset),
            //         (int) Math.floor(mSelectionBounds.top + textVerticalOffset),
            //         (int) Math.ceil(mSelectionBounds.right + textHorizontalOffset),
            //         (int) Math.ceil(mSelectionBounds.bottom + textVerticalOffset));
            outRect.set(100,500,300,1000);
        }

    /**
     * A helper for enabling and handling "PROCESS_TEXT" menu actions.
     * These allow external applications to plug into currently selected text.
     */
    static final class ProcessTextIntentActionsHandler {
        private final static int MAX_PARCEL_SIZE = 500 * 1024;
        // private final Editor mEditor;
        // private final TextView mTextView;
        private final ReaderView mView;
        private final Context mContext;
        private final PackageManager mPackageManager;
        private final String mPackageName;
        // private final SparseArray<Intent> mAccessibilityIntents = new SparseArray<>();
        // private final SparseArray<AccessibilityAction> mAccessibilityActions =
        //         new SparseArray<>();
        private final List<ResolveInfo> mSupportedActivities = new ArrayList<>();

        private ProcessTextIntentActionsHandler(ReaderView view) {
            // mEditor = Objects.requireNonNull(editor);
            // mTextView = Objects.requireNonNull(mEditor.mTextView);
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

        /**
         * Initializes and caches "PROCESS_TEXT" accessibility actions.
         */
        // public void initializeAccessibilityActions() {
            // mAccessibilityIntents.clear();
            // mAccessibilityActions.clear();
            // int i = 0;
            // loadSupportedActivities();
            // for (ResolveInfo resolveInfo : mSupportedActivities) {
            //     int actionId = TextView.ACCESSIBILITY_ACTION_PROCESS_TEXT_START_ID + i++;
            //     mAccessibilityActions.put(
            //             actionId,
            //             new AccessibilityAction(actionId, getLabel(resolveInfo)));
            //     mAccessibilityIntents.put(
            //             actionId, createProcessTextIntentForResolveInfo(resolveInfo));
            // }
        // }

        /**
         * NOTE: This needs a prior call to {@link #initializeAccessibilityActions()} to make the
         * latest accessibility actions available for this call.
         * Adds "PROCESS_TEXT" accessibility actions to the specified accessibility node info.
         */
        // public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo nodeInfo) {
            // for (int i = 0; i < mAccessibilityActions.size(); i++) {
            //     nodeInfo.addAction(mAccessibilityActions.valueAt(i));
            // }
        // }

        /**
         * Performs a "PROCESS_TEXT" action if there is one associated with the specified
         * accessibility action id.
         *
         * @return True if the action was performed, false otherwise.
         */
        // public boolean performAccessibilityAction(int actionId) {
        //     return fireIntent(mAccessibilityIntents.get(actionId));
        // }

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
