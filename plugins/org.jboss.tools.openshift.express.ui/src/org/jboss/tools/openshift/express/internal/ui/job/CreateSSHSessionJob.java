/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHSessionRepository;

import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 * 
 */
public class CreateSSHSessionJob extends Job {

	private IApplication application;

	private boolean validSession = false;

	private LoadApplicationJob applicationJob;

	public CreateSSHSessionJob(final IApplication application) {
		super("Verifying SSH session...");
		this.application = application;
	}

	public CreateSSHSessionJob(final LoadApplicationJob job) {
		super("Verifying SSH session...");
		this.applicationJob = job;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IApplication application = getApplication();
		if (application == null) {
			return OpenShiftUIActivator.createErrorStatus("Could not verify SSH seesion. Application was not found.");
		}
		try {		
			final boolean hasAlreadySSHSession = application.hasSSHSession();
			if (!hasAlreadySSHSession) {
				Logger.debug(NLS.bind("Opening a new SSH Session for application {0}.", application.getName()));
				Session session = SSHSessionRepository.getInstance().getSession(application);
				application.setSSHSession(session);
			}
			// now, check if the session is valid (ie, not null and still
			// connected)
			this.validSession = application.hasSSHSession();
			return Status.OK_STATUS;
		} catch (OpenShiftSSHOperationException e) {
			return OpenShiftUIActivator.createErrorStatus(NLS.bind("Could not verify SSH session for application {0}", application));
		}
	}

	private IApplication getApplication() {
		if (application != null) {
			return application;
		} else {
			return applicationJob.getApplication();
		}
	}

	public final boolean isValidSession() {
		return validSession;
	}

}
