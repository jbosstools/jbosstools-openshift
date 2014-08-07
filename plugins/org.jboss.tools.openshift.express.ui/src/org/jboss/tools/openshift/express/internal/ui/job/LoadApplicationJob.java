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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 *
 */
public class LoadApplicationJob extends Job {

	private IApplication application = null;

	private final IServer server;

	public LoadApplicationJob(final IServer server) {
		super(NLS.bind("Identifying OpenShift Application for server adapter {0}...", server.getName()));
		this.server = server;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		this.application = OpenShiftServerUtils.getApplication(server);
		if (application == null) {
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Failed to retrieve Application from server adapter {0}.\n" +
							"Please verify that the associated OpenShift application and workspace project still exist.", 
							server.getName()));
		}
		return Status.OK_STATUS;
	}

	/**
	 * @return the application
	 */
	public final IApplication getApplication() {
		return application;
	}

}
