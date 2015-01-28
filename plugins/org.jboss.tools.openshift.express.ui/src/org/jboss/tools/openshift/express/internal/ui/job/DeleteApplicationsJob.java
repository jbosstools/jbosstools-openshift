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
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class DeleteApplicationsJob extends AbstractDelegatingMonitorJob {

	private List<IApplication> applications;
	private LoadApplicationJob job;

	public DeleteApplicationsJob(LoadApplicationJob job) {
		super(ExpressUIMessages.DeletingOpenShiftApplications); 
		this.job = job;
	}

	public DeleteApplicationsJob(final List<IApplication> applications) {
		super(ExpressUIMessages.DeletingOpenShiftApplications); 
		this.applications = applications;
	}
	
	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		List<IApplication> applications = getApplications();
		int totalWork = applications.size();
		monitor.beginTask(ExpressUIMessages.DeletingOpenShiftApplications, totalWork);
		try{
			for (final IApplication application : applications) {
				if (application == null) {
					monitor.worked(1);
					continue;
				}
				final String appName = application.getName();
				try {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					monitor.setTaskName(NLS.bind(ExpressUIMessages.DeletingApplication, appName));
					application.destroy();
					monitor.worked(1);
				} catch (OpenShiftException e) {
					return ExpressUIActivator.createErrorStatus(
							NLS.bind(ExpressUIMessages.FailedToDeleteApplication, appName), e);
				}
			}
		}finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private List<IApplication> getApplications() {
		if (applications != null) {
			return applications;
		} else {
			return Collections.singletonList(job.getApplication());
		}
	}
}
