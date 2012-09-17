package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.progress.UIJob;

public abstract class UIUpdatingJob extends Job {

	public UIUpdatingJob(String name) {
		super(name);
		addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				new UIJob(getName()) {
					
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						return updateUI(monitor);
					}
				}.schedule();
			}});
	}
	
	@Override
	protected abstract IStatus run(IProgressMonitor monitor);

	protected IStatus updateUI(IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}
	
}
