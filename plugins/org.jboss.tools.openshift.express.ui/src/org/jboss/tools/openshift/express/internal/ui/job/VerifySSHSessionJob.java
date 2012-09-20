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
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftSshSessionFactory;

import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 * 
 */
public class VerifySSHSessionJob extends Job {

	private final IApplication application;

	private boolean validSession = false;

	public VerifySSHSessionJob(final IApplication application) {
		super("Verifying SSH session to retrieve Application's ports...");
		this.application = application;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			final boolean hasAlreadySSHSession = application.hasSSHSession();
			if (!hasAlreadySSHSession) {
				Logger.debug("Opening a new SSH Session for application '" + application.getName() + "'");
				Session session;
				session = OpenShiftSshSessionFactory.getInstance().createSession(
						application);
				application.setSSHSession(session);
			}
			// now, check if the session is valid (ie, not null and still
			// connected)
			this.validSession = application.hasSSHSession();
			return Status.OK_STATUS;
		} catch (OpenShiftSSHOperationException e) {
			return OpenShiftUIActivator.createErrorStatus(e.getMessage(), e.getCause());
		}
	}

	/**
	 * @return the hasSession
	 */
	public final boolean isValidSession() {
		return validSession;
	}

}
