package net.timelegend.chaka.viewer.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;

import com.artifex.mupdf.fitz.Document; /* for file name recognition */
import net.timelegend.chaka.viewer.DocumentActivity;

public class LibraryActivity extends Activity
{
	protected final int FILE_REQUEST = 42;
	protected boolean selectingDocument;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectingDocument = false;
	}

	public void onStart() {
		super.onStart();
		if (!selectingDocument)
		{
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
				// open the mime-types we know about
				"application/pdf",
				"application/vnd.ms-xpsdocument",
				"application/oxps",
				"application/x-cbz",
				"application/vnd.comicbook+zip",
				"application/epub+zip",
				"application/x-fictionbook",
				"application/x-mobipocket-ebook",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"application/vnd.openxmlformats-officedocument.presentationml.presentation",
				"text/html",
				"text/plain",
				"application/x-gzip-compressed",
				"application/zip",
				// ... and the ones android doesn't know about
				"application/octet-stream"
			});

			startActivityForResult(intent, FILE_REQUEST);
			selectingDocument = true;
		}
	}

	public void onActivityResult(int request, int result, Intent data) {
		if (request == FILE_REQUEST && result == Activity.RESULT_OK) {
			if (data != null) {
				Intent intent = new Intent(this, DocumentActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
				intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(data.getData(), data.getType());
				intent.putExtra(getComponentName().getPackageName() + ".ReturnToLibraryActivity", 1);
				startActivity(intent);
			}
			if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2)
				finish();
		} else if (request == FILE_REQUEST && result == Activity.RESULT_CANCELED) {
			finish();
		}
		selectingDocument = false;
	}
}
