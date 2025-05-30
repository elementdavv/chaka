package net.timelegend.chaka.viewer;

import com.artifex.mupdf.fitz.Cookie;

public abstract class MuPDFCancellableTaskDefinition<Params, Result> implements CancellableTaskDefinition<Params, Result>
{
	private Cookie cookie;

	public MuPDFCancellableTaskDefinition()
	{
		this.cookie = new Cookie();
	}

	@Override
	public void doCancel()
	{
		if (cookie == null)
			return;

		cookie.abort();
	}

	@Override
	public void doCleanup()
	{
		if (cookie == null)
			return;

		cookie.destroy();
		cookie = null;
	}

	@Override
	@SafeVarargs
	public final Result doInBackground(Params ... params)
	{
		return doInBackground(cookie, params);
	}

	@SuppressWarnings("unchecked")
	public abstract Result doInBackground(Cookie cookie, Params ... params);
}
