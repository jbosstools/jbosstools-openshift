/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IOpenShiftResource;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class RefreshElementAction extends AbstractOpenShiftAction {

	public RefreshElementAction() {
		super(OpenShiftExpressUIMessages.REFRESH_USER_ACTION, true);
		setImageDescriptor(OpenShiftImages.REFRESH);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		Object resource = UIUtils.getFirstElement(getSelection(), IOpenShiftResource.class);
		if (resource == null) {
			resource = UIUtils.getFirstElement(getSelection(), Connection.class);
			if (resource == null) {
				return;
			}
		}

		refresh(resource);
	}

	private void refresh(final Object element) {
		Job job = new Job("Loading OpenShift information...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Loading OpenShift information...", IProgressMonitor.UNKNOWN);
					if(element instanceof Connection) {
						((Connection)element).refresh();
					} else if (element instanceof IOpenShiftResource) {
						((IOpenShiftResource)element).refresh();
					}

					//List<IApplication> applications = user.getApplications();
					RefreshViewerJob.refresh(viewer);
				} catch (OpenShiftException e) {
					Logger.error("Failed to refresh element", e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

}
