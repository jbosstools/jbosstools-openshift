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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationJob extends AbstractDelegatingMonitorJob {

	private IApplication application;
	private LoadApplicationJob applicationJob;

	public RestartApplicationJob(IApplication application) {
		super(NLS.bind(OpenShiftExpressUIMessages.RESTARTING_APPLICATION, application.getName()));
		this.application = application;
	}

	public RestartApplicationJob(LoadApplicationJob applicationJob) {
		super(NLS.bind(OpenShiftExpressUIMessages.RESTARTING_APPLICATION, ""));
		this.applicationJob = applicationJob;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		Logger.debug(OpenShiftExpressUIMessages.RESTARTING_APPLICATION);
		try {
			IApplication application = getApplication();
			if (application != null) {
				getApplication().restart();
			}
		} catch(OpenShiftTimeoutException e) {
			// intentionally swallow
		} catch (OpenShiftException e) {
			return OpenShiftUIActivator.createErrorStatus(NLS.bind(
					"Could not restart application \"{0}\"", application.getName()), e);
		}
		return Status.OK_STATUS;
	}
	
	private IApplication getApplication() {
		if (application != null) {
			return application;
		} else {
			return applicationJob.getApplication();
		}
	}
}
