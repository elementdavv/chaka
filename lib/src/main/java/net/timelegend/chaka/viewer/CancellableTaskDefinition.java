package net.timelegend.chaka.viewer;

public interface CancellableTaskDefinition <Params, Result>
{
	@SuppressWarnings("unchecked")
	public Result doInBackground(Params ... params);
	public void doCancel();
	public void doCleanup();
}
