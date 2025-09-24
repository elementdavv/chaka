package net.timelegend.chaka.viewer;

import android.app.ProgressDialog;
import android.content.Context;

public class ProgressDialogX extends ProgressDialog {
	public ProgressDialogX(Context context) {
		super(context);
	}

	public ProgressDialogX(Context context, int theme) {
		super(context, theme);
	}

	private boolean mCancelled = false;

	public boolean isCancelled() {
		return mCancelled;
	}

	@Override
	public void cancel() {
		mCancelled = true;
		super.cancel();
	}
}
