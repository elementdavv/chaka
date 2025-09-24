package net.timelegend.chaka.viewer;

import com.artifex.mupdf.fitz.SeekableInputStream;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocumentActivity extends AppCompatActivity
{
	/* The core rendering instance */
	enum TopBarMode {Main, Search, More};

	private MuPDFCore    core;
	private String       mDocTitle;
	private String       mDocKey;
	private ReaderView   mDocView;
	private View         mButtonsView;
	private boolean      mButtonsVisible;
	private EditText     mPasswordView;
	private TextView     mDocNameView;
	private SeekBar      mPageSlider;
	private int          mPageSliderRes;
	private TextView     mPageNumberView;
	private ImageButton  mCopyButton;
	private ImageButton  mSingleColumnButton;
	private ImageButton  mTextLeftButton;
	private ImageButton  mFlipVerticalButton;
	private ImageButton  mLockButton;
	private ImageButton  mCropMarginButton;
	private ImageButton  mFocusButton;
	private ImageButton  mSmartFocusButton;
	private ImageButton  mColorButton;
	private ImageButton  mShareButton;
	private ImageButton  mHelpButton;
	private ImageButton  mSearchButton;
	private ImageButton  mOutlineButton;
	private ViewAnimator mTopBarSwitcher;
	private ImageButton  mLinkButton;
	private TopBarMode   mTopBarMode = TopBarMode.Main;
    private ImageButton  mSearchClear;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private ImageButton  mSearchClose;
	private EditText     mSearchText;
	private SearchTask   mSearchTask;
	private AlertDialog.Builder mAlertBuilder;
    private boolean    mSingleColumnHighlight = false;
    private boolean    mTextLeftHighlight = false;
    private boolean    mFlipVerticalHighlight = false;
    private boolean    mLockHighlight = false;
    private boolean    mCropMarginHighlight = false;
    private boolean    mFocusHighlight = false;
    private boolean    mSmartFocusHighlight = false;
	private boolean    mLinkHighlight = false;
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private ArrayList<TocItem> mFlatOutline;
	private boolean mReturnToLibraryActivity = false;
    private boolean mNavigationBar;

    /**
     * if navigation bar is hidden. ime will bring it up, cause docview resize,
     * result in page number changes in flowable documents.
     */
    // to notify onSizeChanged ime change by user
    private boolean mKeyboardChanged = false;
    // to notify setOnSystemUiVisibilityChangeListener ime change by user
    private boolean mKeyboardChanged2 = false;
    // to notify onSizeChanged ime close by system button (this is a guess)
    private boolean mKeyboardChanged3 = false;
    private boolean mOrientationChanged = false;
    private int mOrientation;
    private int lastPage = -1;
    private long firstClickTime = 0;
    private boolean single = false;
    private boolean leftText = false;
    private boolean vertical = false;
    private Uri uri;
    private String mMimeType;

    private int highlightColor = Color.argb(0xFF, 0x3C, 0xB3, 0x71);
    private int highunlightColor = Color.argb(0xFF, 255, 255, 255);
    private int enabledColor = Color.argb(255, 255, 255, 255);
    private int disabledColor = Color.argb(255, 128, 128, 128);

	protected int mDisplayDPI;
	private int mLayoutEM;      // read from prefs
	private int mLayoutW = 312;
	private int mLayoutH = 504;

	protected View mLayoutButton;
	protected PopupMenu mLayoutPopupMenu;
	protected ListPopupWindow mColorPopupWindow;

	private MuPDFCore openBuffer(byte buffer[], String magic)
	{
		try
		{
			core = new MuPDFCore(this, buffer, magic);
		}
		catch (Exception e)
		{
			Tool.e("Error opening document buffer: " + e);
			return null;
		}
		return core;
	}

	private MuPDFCore openStream(SeekableInputStream stm, String magic)
	{
		try
		{
			core = new MuPDFCore(this, stm, magic);
		}
		catch (Exception e)
		{
			Tool.e("Error opening document stream: " + e);
			return null;
		}
		return core;
	}

	private MuPDFCore openCore(Uri uri, long size, String mimetype) throws IOException {
		ContentResolver cr = getContentResolver();

		Tool.i("Opening document " + uri);

		InputStream is = cr.openInputStream(uri);
		byte[] buf = null;
		int used = -1;
		try {
			final int limit = 8 * 1024 * 1024;
			if (size < 0) { // size is unknown
				buf = new byte[limit];
				used = is.read(buf);
				boolean atEOF = is.read() == -1;
				if (used < 0 || (used == limit && !atEOF)) // no or partial data
					buf = null;
			} else if (size <= limit) { // size is known and below limit
				buf = new byte[(int) size];
				used = is.read(buf);
				if (used < 0 || used < size) // no or partial data
					buf = null;
			}
			if (buf != null && buf.length != used) {
				byte[] newbuf = new byte[used];
				System.arraycopy(buf, 0, newbuf, 0, used);
				buf = newbuf;
			}
		} catch (OutOfMemoryError e) {
			buf = null;
		} finally {
			is.close();
		}

		if (buf != null) {
			Tool.i("  Opening document from memory buffer of size " + buf.length);
			return openBuffer(buf, mimetype);
		} else {
			Tool.i("  Opening document from stream");
			return openStream(new ContentInputStream(cr, uri, size), mimetype);
		}
	}

	private void showCannotOpenDialog(String reason) {
		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(String.format(Locale.ROOT, getResources().getString(R.string.cannot_open_document_Reason), reason));
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		alert.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Tool.create(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		// DisplayMetrics metrics = new DisplayMetrics();
		// getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDisplayDPI = (int)metrics.densityDpi;

		mAlertBuilder = new AlertDialog.Builder(this, R.style.MyDialog);

		setUserCss();

		if (core == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey("DocTitle")) {
				mDocTitle = savedInstanceState.getString("DocTitle");
			}
		}
		if (core == null) {
			Intent intent = getIntent();

			mReturnToLibraryActivity = intent.getIntExtra(getComponentName().getPackageName() + ".ReturnToLibraryActivity", 0) != 0;

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				uri = intent.getData();
				String mimetype = getIntent().getType();

				if (uri == null)  {
					showCannotOpenDialog("No document uri to open");
					return;
				}

				mDocKey = uri.toString();

				Tool.i("OPEN URI " + uri.toString());
				Tool.i("  MAGIC (Intent) " + mimetype);

				// in case of cbz recognized as zip
				// .gz recognized as */*
				if ("application/zip".equals(mimetype) || "*/*".equals(mimetype)) {
					mimetype = null;
				}

				mDocTitle = null;
				long size = -1;
				Cursor cursor = null;

				try {
					cursor = getContentResolver().query(uri, null, null, null, null);
					if (cursor != null && cursor.moveToFirst()) {
						int idx;

						idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_STRING)
							mDocTitle = cursor.getString(idx);

						idx = cursor.getColumnIndex(OpenableColumns.SIZE);
						if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_INTEGER)
							size = cursor.getLong(idx);

						if (size == 0)
							size = -1;
					}
				} catch (Exception x) {
					// Ignore any exception and depend on default values for title
					// and size (unless one was decoded
				} finally {
					if (cursor != null)
						cursor.close();
				}

				Tool.i("  NAME " + mDocTitle);
				Tool.i("  SIZE " + size);

				if (mimetype == null || mimetype.equals("application/octet-stream")) {
					mimetype = getContentResolver().getType(uri);
					Tool.i("  MAGIC (Resolved) " + mimetype);
				}

				mMimeType = mimetype;

				if (mimetype == null || mimetype.equals("application/octet-stream")) {
					mimetype = mDocTitle;
					Tool.i("  MAGIC (Filename) " + mimetype);
				}

				try {
					core = openCore(uri, size, mimetype);
					SearchTaskResult.set(null);
				} catch (Exception x) {
					showCannotOpenDialog(x.toString());
					return;
				}

				if (core != null) {
					String docTitle = core.getTitle();
					if (docTitle != null && !"".equals(docTitle)) {
						mDocTitle = docTitle;
					}
					String fileDigest = Tool.getDigest(getContentResolver(), uri);
					mDocKey = mDocTitle + "." + String.valueOf(size) + "." + fileDigest;
				}
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0)
			{
				core = null;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(R.string.cannot_open_document);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			alert.show();
			return;
		}

		createUI(savedInstanceState);
		mOrientation = getResources().getConfiguration().orientation;
		HelpActivity.delay = 600;
		OutlineActivity.content = core.hasOutline() ? OutlineActivity.CONTENTS.TOC : OutlineActivity.CONTENTS.BOOKMARK;
		BookmarkRepository.getInstance().setup(core, mDocKey);
	}


	private void setUserCss() {
		StringBuilder sb = new StringBuilder();
		sb.append("@page{margin:2em}");
		sb.append("body{margin:0;padding:0;overflow-wrap:break-word;}");
		sb.append("p{margin:0.6em 0;}");
		sb.append("table{border-collapse:collapse;}");
		com.artifex.mupdf.fitz.Context.setUserCSS(sb.toString());
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Tool.fullScreen(getWindow());
	}

	public void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (core.authenticatePassword(mPasswordView.getText().toString())) {
							createUI(savedInstanceState);
						} else {
							requestPassword(savedInstanceState);
						}
					}
				});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}

	public void relayoutDocument() {
		core.layout(mDocView.mCurrent, mLayoutW, mLayoutH, mLayoutEM);
	}

	public void afterRelayout(int loc) {
		mFlatOutline = null;
		mDocView.mHistory.clear();
		mDocView.refresh();

        if (lastPage > -1 ) {
            HelpActivity.updateReadme();
            if (lastPage < core.countPages())
                loc = lastPage;
            lastPage = -1;
        }

		BookmarkRepository.getInstance().onSizeChange();
		mDocView.setDisplayedViewIndex(loc);

		if (vertical) {
			vertical = false;
			toggleFlipVerticalHighlight();
		}
	}

	public void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new ReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;

		        updatePageNumView(i);
                updatePageSlider(i);
				super.onMoveToChild(i);
			}

			@Override
			protected void onTapMainDocArea() {
				if (!mButtonsVisible) {
					showButtons();
				} else {
					// if (mTopBarMode == TopBarMode.Main)
						hideButtons();
				}
			}

			@Override
			protected void onDocMotion() {
				hideButtons();
			}

			@Override
			public void onSizeChanged(int w, int h, int oldw, int oldh) {
                // ime changed by user
                if (mKeyboardChanged) {
                    mKeyboardChanged = false;
                    if (!mOrientationChanged) return;
                }

                // ime closed by system button (a guess)
                if (mKeyboardChanged3) {
                    mKeyboardChanged3 = false;
                    if (!mOrientationChanged) return;
                }

                // ajust doc name width
                mHandler.postDelayed(new Runnable(){
                    public void run() {
                        updateTopBar(w);
                    }}, 200);

				if (core.isReflowable()) {
					mLayoutW = w * 72 * 2 / mDisplayDPI;
					mLayoutH = h * 72 * 2 / mDisplayDPI;
					relayoutDocument();
				} else {
					refresh();
				}
			}
		};
		mDocView.setAdapter(new PageAdapter(this, core));

		mSearchTask = new SearchTask(this, core) {
			@Override
			protected void onTextFound(SearchTaskResult result) {
				SearchTaskResult.set(result);
				// Ask the ReaderView to move to the resulting page
				mDocView.setDisplayedViewIndex(result.pageNumber);
				// Make the ReaderView act on the change to SearchTaskResult
				// via overridden onChildSetup method.
				mDocView.resetupChildren();
			}
		};

		// Make the buttons overlay, and store all its
		// controls in variables
		makeButtonsView();

        // below android 8 (api26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            TooltipCompat.setTooltipText(mCopyButton, getString(R.string.copy));
            TooltipCompat.setTooltipText(mSingleColumnButton, getString(R.string.single_column));
            TooltipCompat.setTooltipText(mTextLeftButton, getString(R.string.text_left));
            TooltipCompat.setTooltipText(mFlipVerticalButton, getString(R.string.flip_vertical));
            TooltipCompat.setTooltipText(mLockButton, getString(R.string.lock));
            TooltipCompat.setTooltipText(mCropMarginButton, getString(R.string.crop_margin));
            TooltipCompat.setTooltipText(mFocusButton, getString(R.string.focus));
            TooltipCompat.setTooltipText(mSmartFocusButton, getString(R.string.smart_focus));
            TooltipCompat.setTooltipText(mLinkButton, getString(R.string.link));
            TooltipCompat.setTooltipText(mSearchButton, getString(R.string.text_search));
            TooltipCompat.setTooltipText(mLayoutButton, getString(R.string.format_size));
            TooltipCompat.setTooltipText(mColorButton, getString(R.string.color));
            TooltipCompat.setTooltipText(mShareButton, getString(R.string.share));
            TooltipCompat.setTooltipText(mOutlineButton, getString(R.string.contents));
            TooltipCompat.setTooltipText(mHelpButton, getString(R.string.help));
        }

		// Set up the page slider
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;

		// Set the file-name text
		mDocNameView.setText(mDocTitle);
		TooltipCompat.setTooltipText(mDocNameView, mDocNameView.getText());

		// Activate the seekbar
		mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				mDocView.pushHistory();
                if (!mTextLeftHighlight)
				    mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
                else
				    mDocView.setDisplayedViewIndex(core.countPages() - 1 - (seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
                if (!mTextLeftHighlight)
				    updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
                else
				    updatePageNumView(core.countPages() - 1 - (progress+mPageSliderRes/2)/mPageSliderRes);
			}
		});

        mDocNameView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long secondClickTime = System.currentTimeMillis();;

                if (secondClickTime - firstClickTime < ViewConfiguration.getDoubleTapTimeout()) {
                    exit();
                }
                firstClickTime = secondClickTime;
            }
        });

        mCopyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copy();
            }
        });

        if (core.isReflowable()) {
            mSingleColumnButton.setVisibility(View.GONE);
        }
        else {
            mSingleColumnButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleSingleColumnHighlight();
                }
            });
        }

        if (core.isReflowable()) {
            mTextLeftButton.setVisibility(View.GONE);
        }
        else {
            mTextLeftButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleTextLeftHighlight();
                }
            });
        }

        mFlipVerticalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFlipVerticalHighlight();
            }
        });

        mLockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleLock();
            }
        });

        mCropMarginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleCropMargin();
            }
        });

        mFocusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFocus();
            }
        });

        if (core.isReflowable()) {
            mSmartFocusButton.setVisibility(View.GONE);
        }
        else {
            mSmartFocusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleSmartFocus();
                }
            });
        }

        makeColorPopupWindow();

        mColorButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mColorPopupWindow.setAnchorView(v);
                mColorPopupWindow.show();
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType(mMimeType);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
            }
        });

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(DocumentActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});

		mSearchClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
			}
		});

		// Search invoking buttons are disabled while there is no text specified
		setButtonEnabled(mSearchBack, false);
		setButtonEnabled(mSearchFwd, false);
		setButtonEnabled(mSearchClear, false);

		// React to interaction with the text widget
		mSearchText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				boolean haveText = s.toString().trim().length() > 0;
				setButtonEnabled(mSearchBack, haveText);
				setButtonEnabled(mSearchFwd, haveText);
				setButtonEnabled(mSearchClear, haveText);

                if (!haveText) {
                    mSearchText.requestFocus();
                    showKeyboard();
                }

				// Remove any previous search results
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().trim().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.set(null);
					mDocView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		//React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
				return false;
			}
		});

        mSearchText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                case MotionEvent.ACTION_UP:
                    mSearchText.requestFocus();
                    showKeyboard();
                    return true;
                default:
                    break;
                }
                return false;
            }
        });

        mSearchClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSearchText.setText("");
            }
        });

		// Activate search invoking buttons
		mSearchBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(-1);
			}
		});

		mSearchFwd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});

		mLinkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setLinkHighlight(!mLinkHighlight);
			}
		});

		if (core.isReflowable()) {
			mLayoutButton.setVisibility(View.VISIBLE);
			mLayoutPopupMenu = new PopupMenu(this, mLayoutButton);
			mLayoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, mLayoutPopupMenu.getMenu());
			mLayoutPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					float oldLayoutEM = mLayoutEM;
					int id = item.getItemId();
					if (id == R.id.action_layout_6pt) mLayoutEM = 6;
					else if (id == R.id.action_layout_8pt) mLayoutEM = 8;
					else if (id == R.id.action_layout_10pt) mLayoutEM = 10;
					else if (id == R.id.action_layout_12pt) mLayoutEM = 12;
					else if (id == R.id.action_layout_14pt) mLayoutEM = 14;
					else if (id == R.id.action_layout_16pt) mLayoutEM = 16;
					else if (id == R.id.action_layout_16pt) mLayoutEM = 18;
					if (oldLayoutEM != mLayoutEM) {
						relayoutDocument();
			            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			            SharedPreferences.Editor edit = prefs.edit();
                        edit.putInt("layoutem"+mDocKey, mLayoutEM);
			            edit.apply();
                    }
					return true;
				}
			});
			mLayoutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
                    Menu menu = mLayoutPopupMenu.getMenu();
                    for (int mi = 0; mi < menu.size(); mi++) {
                        MenuItem item = menu.getItem(mi);
                        item.setCheckable(false);
                        String title = item.getTitle().toString();
                        if (title.equals(String.valueOf(mLayoutEM) + "pt")) {
                            item.setCheckable(true).setChecked(true);
                        }
                    }
					mLayoutPopupMenu.show();
				}
			});
		}

        mOutlineButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);

                if (mFlatOutline == null)
                    if (core.hasOutline())
                        mFlatOutline = core.getOutline();

                if (mFlatOutline != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
                    bundle.putSerializable("OUTLINE", mFlatOutline);
                    intent.putExtra("PALLETBUNDLE", Pallet.sendBundle(bundle));
                }
                activityLauncher.launch(intent);
            }
        });

        int black = ContextCompat.getColor(this, R.color.black1);
        int white = ContextCompat.getColor(this, R.color.white1);

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mLayoutEM = prefs.getInt("layoutem"+mDocKey, 12);
		lastPage = prefs.getInt("page" + mDocKey, 0);
		single = prefs.getBoolean("single" + mDocKey, false);
		leftText = prefs.getBoolean("lefttext" + mDocKey, false);
		vertical = prefs.getBoolean("vertical" + mDocKey, false);
		black = prefs.getInt("black" + mDocKey, black);
		white = prefs.getInt("white" + mDocKey, white);

		core.setTintColor(black, white);

        if (!core.isReflowable()) {
            HelpActivity.updateReadme();

            if (leftText) {
                leftText = false;
                toggleTextLeftHighlight();
            }

            if (single) {
                single = false;
                toggleSingleColumnHighlight();
            }

            if (lastPage < core.countPages())
                mDocView.setDisplayedViewIndex(lastPage);

            lastPage = -1;

            if (vertical) {
                vertical = false;
                toggleFlipVerticalHighlight();
            }
        }

		if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			showButtons();

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.setBackgroundColor(Color.DKGRAY);
		layout.addView(mDocView);
		layout.addView(mButtonsView);
		setContentView(layout);

        watchNavigationBar();
	}

    private void makeColorPopupWindow() {
        List<ColorItem> itemList = new ArrayList<>();

        itemList.add(new ColorItem("Day", ContextCompat.getColor(this, R.color.black1), ContextCompat.getColor(this, R.color.white1)));
        itemList.add(new ColorItem("Night", ContextCompat.getColor(this, R.color.black2), ContextCompat.getColor(this, R.color.white2)));
        itemList.add(new ColorItem("Paper", ContextCompat.getColor(this, R.color.black3), ContextCompat.getColor(this, R.color.white3)));
        itemList.add(new ColorItem("Sepia", ContextCompat.getColor(this, R.color.black4), ContextCompat.getColor(this, R.color.white4)));
        itemList.add(new ColorItem("Twilight", ContextCompat.getColor(this, R.color.black5), ContextCompat.getColor(this, R.color.white5)));
        itemList.add(new ColorItem("Console", ContextCompat.getColor(this, R.color.black6), ContextCompat.getColor(this, R.color.white6)));

        ColorAdapter adapter = new ColorAdapter(this, itemList, new ColorAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ColorItem item =  itemList.get(position);
                if (core.setTintColor(item.black, item.white)) {
                    mDocView.refresh();
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putInt("black"+mDocKey, item.black);
                    edit.putInt("white"+mDocKey, item.white);
                    edit.apply();
                }
                mColorPopupWindow.dismiss();
            }
        });

        mColorPopupWindow = new ListPopupWindow(this);
        mColorPopupWindow.setAdapter(adapter);
        mColorPopupWindow.setWidth(adapter.getWidth() + 100);
        mColorPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        mColorPopupWindow.setModal(true);
    }

    private ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    int pagetogo  = data.getExtras().getInt("pagetogo");

                    if (mDocView != null) {
                        mDocView.pushHistory();
                        mDocView.setDisplayedViewIndex(pagetogo - RESULT_FIRST_USER);
                    }
                }
            }
        });


	private void setButtonEnabled(ImageButton button, boolean enabled) {
		button.setEnabled(enabled);
		button.setColorFilter(enabled ? enabledColor : disabledColor);
	}

    @SuppressWarnings("deprecation")
    private void watchNavigationBar() {
        View decorView = getWindow().getDecorView();
        // below android 11 (api30)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // run after onSizeChanged
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    // if changed by keyboard, do not report
                    if (mKeyboardChanged2) {
                        mKeyboardChanged2 = false;
                        return;
                    }
                    mNavigationBar = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                }
            });
        }
        // run before onSizeChanged
        decorView.setOnApplyWindowInsetsListener((v, insets) -> {
            // below android 11 (api30)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                /**
                 * insetTop: 96: statusBar, insetBottom: 0/168: navigationBar
                 */
                // Tool.i("stable");
                // RectI rs = new RectI(insets.getStableInsetLeft(),insets.getStableInsetTop()
                //         ,insets.getStableInsetRight(),insets.getStableInsetBottom());
                // Tool.i(rs);
            }
            else {
                // Tool.i("ime:" + insets.isVisible(WindowInsets.Type.ime()));
                // Insets bar = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                // Tool.i("inset");
                // RectI rb = new RectI(bar.left, bar.top, bar.right, bar.bottom);
                // Tool.i(rb);

                // if changed by keyboard, do not report
                if (mKeyboardChanged2) {
                    mKeyboardChanged2 = false;
                    return v.onApplyWindowInsets(insets);
                }
                mNavigationBar  = insets.isVisible(WindowInsets.Type.navigationBars());
            }
            /**
             * this return will lock window size so docview won't be disturbed by ime
             * but the immersed navigation bar overlaps with the slider, so do not use it
             */
            // return insets.CONSUMED;
            return v.onApplyWindowInsets(insets);
        });
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mDocKey != null && mDocView != null) {
			if (mDocTitle != null)
				outState.putString("DocTitle", mDocTitle);

			savePrefs();
		}

		if (!mButtonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarMode == TopBarMode.Search)
			outState.putBoolean("SearchMode", true);

		BookmarkRepository.getInstance().save();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mSearchTask != null)
			mSearchTask.stop();

		if (mDocKey != null && mDocView != null) {
			savePrefs();
		}
		BookmarkRepository.getInstance().save();
	}

	private void savePrefs() {
		// Store current page in the prefs against the file name,
		// so that we can pick it up each time the file is loaded
		// Other info is needed only for screen-orientation change,
		// so it can go in the bundle
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("page" + mDocKey, mDocView.getDisplayedViewIndex());
		edit.putBoolean("single" + mDocKey, mSingleColumnHighlight);
		edit.putBoolean("lefttext" + mDocKey, mTextLeftHighlight);
		edit.putBoolean("vertical" + mDocKey, mFlipVerticalHighlight);
		edit.apply();
	}

	@Override
	protected void onDestroy() {
		if (mDocView != null) {
			mDocView.applyToChildren(new ReaderView.ViewMapper() {
				@Override
				public void applyToView(View view) {
					((PageView)view).releaseBitmaps();
				}
			});
		}
		if (core != null)
			core.onDestroy();
		core = null;
		super.onDestroy();
	}

    private void exit() {
        finish();
    }

    private void copy() {
        mDocView.copy();
    }

    private void toggleSingleColumnHighlight() {
        int index;
        mSingleColumnHighlight = !mSingleColumnHighlight;
		// COLOR tint
		mSingleColumnButton.setColorFilter(mSingleColumnHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
        core.toggleSingleColumn();
		mDocView.toggleSingleColumn();
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;
		index = mDocView.getDisplayedViewIndex();
		updatePageNumView(index);
		updatePageSlider(index);
		mFlatOutline = null;
		BookmarkRepository.getInstance().toggleSingleColumn();
    }

    private void toggleTextLeftHighlight() {
		mTextLeftHighlight = !mTextLeftHighlight;
		// COLOR tint
		mTextLeftButton.setColorFilter(mTextLeftHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		core.toggleTextLeft();
		mDocView.toggleTextLeft();
		int index = mDocView.getDisplayedViewIndex();
		updatePageNumView(index);
        updatePageSlider(index);
	}

    private void toggleFlipVerticalHighlight() {
		mFlipVerticalHighlight = !mFlipVerticalHighlight;
		// COLOR tint
		mFlipVerticalButton.setColorFilter(mFlipVerticalHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleFlipVertical();
	}

    private void toggleLock() {
		mLockHighlight = !mLockHighlight ;
		// COLOR tint
		mLockButton.setColorFilter(mLockHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleLock();
    }

    private void toggleCropMargin() {
		mCropMarginHighlight = !mCropMarginHighlight;
		// COLOR tint
		mCropMarginButton.setColorFilter(mCropMarginHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		core.toggleCropMargin();
		mDocView.toggleCropMargin();
    }

    private void toggleFocus() {
		mFocusHighlight = !mFocusHighlight;
		// COLOR tint
		mFocusButton.setColorFilter(mFocusHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleFocus(core.isReflowable());
    }

    private void toggleSmartFocus() {
		mSmartFocusHighlight = !mSmartFocusHighlight;
		// COLOR tint
		mSmartFocusButton.setColorFilter(mSmartFocusHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleSmartFocus();
    }

	private void setLinkHighlight(boolean highlight) {
		mLinkHighlight = highlight;
		// LINK_COLOR tint
		mLinkButton.setColorFilter(highlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.setLinksEnabled(highlight);
	}

	private void showButtons() {
		if (core == null)
			return;
		if (!mButtonsVisible) {
			mButtonsVisible = true;
			// Update page number text and slider
			int index = mDocView.getDisplayedViewIndex();
			updatePageNumView(index);
            updatePageSlider(index);
			if (mTopBarMode == TopBarMode.Search) {
                if ("".equals(mSearchText.getText().toString().trim())) {
				    mSearchText.requestFocus();
				    showKeyboard();
                }
			}

			Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mTopBarSwitcher.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageSlider.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageNumberView.setVisibility(View.VISIBLE);
				}
			});
			mPageSlider.startAnimation(anim);
		}
	}

	private void hideButtons() {
		if (mButtonsVisible) {
			mButtonsVisible = false;
			hideKeyboard();

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageNumberView.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageSlider.setVisibility(View.INVISIBLE);
				}
			});
			mPageSlider.startAnimation(anim);
		}
	}

	private void searchModeOn() {
		if (mTopBarMode != TopBarMode.Search) {
			mTopBarMode = TopBarMode.Search;
			//Focus on EditTextWidget
			mSearchText.requestFocus();
            if ("".equals(mSearchText.getText().toString().trim())) {
			    showKeyboard();
            }
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		}
	}

	private void searchModeOff() {
		if (mTopBarMode == TopBarMode.Search) {
			mTopBarMode = TopBarMode.Main;
			hideKeyboard();
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
			SearchTaskResult.set(null);
			// Make the ReaderView act on the change to mSearchTaskResult
			// via overridden onChildSetup method.
			mDocView.resetupChildren();
		}
	}

    private void updatePageSlider(int index) {
        if (core == null)
            return;
		mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
        if (!mTextLeftHighlight)
			mPageSlider.setProgress(index * mPageSliderRes);
        else
			mPageSlider.setProgress((core.countPages() - 1 - index) * mPageSliderRes);

    }

	private void updatePageNumView(int index) {
        if (core == null)
            return;
		mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
	}

	private void makeButtonsView() {
		mButtonsView = getLayoutInflater().inflate(R.layout.document_activity, null);
		mDocNameView = (TextView)mButtonsView.findViewById(R.id.docNameText);
		mPageSlider = (SeekBar)mButtonsView.findViewById(R.id.pageSlider);
		mPageNumberView = (TextView)mButtonsView.findViewById(R.id.pageNumber);
		mSearchButton = (ImageButton)mButtonsView.findViewById(R.id.searchButton);
        mCopyButton = (ImageButton)mButtonsView.findViewById(R.id.copyButton);
        mSingleColumnButton = (ImageButton)mButtonsView.findViewById(R.id.singleColumnButton);
        mTextLeftButton = (ImageButton)mButtonsView.findViewById(R.id.textLeftButton);
        mFlipVerticalButton = (ImageButton)mButtonsView.findViewById(R.id.flipVerticalButton);
        mFocusButton = (ImageButton)mButtonsView.findViewById(R.id.focusButton);
        mLockButton = (ImageButton)mButtonsView.findViewById(R.id.lockButton);
        mCropMarginButton = (ImageButton)mButtonsView.findViewById(R.id.cropMarginButton);
        mSmartFocusButton = (ImageButton)mButtonsView.findViewById(R.id.smartFocusButton);
        mColorButton = (ImageButton)mButtonsView.findViewById(R.id.colorButton);
        mShareButton = (ImageButton)mButtonsView.findViewById(R.id.shareButton);
        mHelpButton = (ImageButton)mButtonsView.findViewById(R.id.helpButton);
		mOutlineButton = (ImageButton)mButtonsView.findViewById(R.id.outlineButton);
		mTopBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.switcher);
		mSearchClear = (ImageButton)mButtonsView.findViewById(R.id.searchClear);
		mSearchBack = (ImageButton)mButtonsView.findViewById(R.id.searchBack);
		mSearchFwd = (ImageButton)mButtonsView.findViewById(R.id.searchForward);
		mSearchClose = (ImageButton)mButtonsView.findViewById(R.id.searchClose);
		mSearchText = (EditText)mButtonsView.findViewById(R.id.searchText);
		mLinkButton = (ImageButton)mButtonsView.findViewById(R.id.linkButton);
		mLayoutButton = mButtonsView.findViewById(R.id.layoutButton);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
		mPageNumberView.setVisibility(View.INVISIBLE);
		mPageSlider.setVisibility(View.INVISIBLE);
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(mSearchText, 0, rr);
        }
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0, rr);
        }
	}

    private ResultReceiver rr = new ResultReceiver(mHandler) {
        @Override
        protected void onReceiveResult(int code, Bundle data) {
            if (code == InputMethodManager.RESULT_SHOWN
                    || code == InputMethodManager.RESULT_HIDDEN) {
                if (!mNavigationBar) {
                    mKeyboardChanged = true;
                    mKeyboardChanged2 = true;
                    mKeyboardChanged3 = (code == InputMethodManager.RESULT_SHOWN);
                }
            }
        }
    };

    @SuppressWarnings("deprecation")
    public void updateTopBar(Integer w) {
        if (w == null) {

            // below android 11 (api30)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                w = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		        w = (int)displayMetrics.widthPixels;
            }
        }
        int BUTTON_WIDTH = 160;
        // topbar button count
        int cbut = 9;
        if (mCopyButton.getVisibility() == View.VISIBLE) cbut++;
        if (mSingleColumnButton.getVisibility() == View.VISIBLE) cbut++;
        if (mTextLeftButton.getVisibility() == View.VISIBLE) cbut++;
        if (mSmartFocusButton.getVisibility() == View.VISIBLE) cbut++;
        if (mLayoutButton.getVisibility() == View.VISIBLE) cbut++;
        if (mOutlineButton.getVisibility() == View.VISIBLE) cbut++;
        int tw = w - BUTTON_WIDTH * cbut;
        int titlebytelen = mDocNameView.getText().toString().getBytes().length;
        int titlewidth = titlebytelen * 32;
        int minwidth = Math.min(titlewidth, 360);
        tw = Math.max(tw, minwidth);
        mDocNameView.setWidth(tw);
    }

    public void showCopyButton(int vis) {
        if (mCopyButton.getVisibility() != vis) {
            mCopyButton.setVisibility(vis);
            updateTopBar(null);
        }
    }

    public void showSingleColumnButton(int vis) {
        if (mOrientationChanged) {
            mOrientationChanged = false;
        }
        else if (core.isReflowable()) {
            return;
        }
        else if (mSingleColumnButton.getVisibility() != vis) {
            mSingleColumnButton.setVisibility(vis);
            updateTopBar(null);
        }
    }

	public void createBookmark() {
		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.create_bookmark);
		alert.setMessage(getString(R.string.create_bookmark_message));
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					BookmarkRepository.getInstance().create(mDocView.getDisplayedViewIndex());
				}
			}
		);
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			}
		);
		alert.show();
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            mOrientationChanged = true;
            searchModeOff();
        }
    }

	private void search(int direction) {
		hideKeyboard();
		int displayPage = mDocView.getDisplayedViewIndex();
		SearchTaskResult r = SearchTaskResult.get();
		int searchPage = r != null ? r.pageNumber : -1;
		mSearchTask.go(mSearchText.getText().toString().trim(), direction, displayPage, searchPage);
	}

	@Override
	public boolean onSearchRequested() {
		if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOn();
		}
		return super.onSearchRequested();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (mDocView == null || (mDocView != null && !mDocView.popHistory())) {
			super.onBackPressed();
			if (mReturnToLibraryActivity) {
				Intent intent = getPackageManager().getLaunchIntentForPackage(getComponentName().getPackageName());
				startActivity(intent);
			}
		}
	}
}
