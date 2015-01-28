/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.progress.UIJob;

/**
 * @author Andre Dietisheim
 */
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
