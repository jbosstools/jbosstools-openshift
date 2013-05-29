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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 *
 */
public class RetrieveApplicationJob extends Job {

	private IApplication application = null;
	
	private final IServer server;
	
	public RetrieveApplicationJob(final IServer server) {
		super("Identifying OpenShift Application from selected Server...");
		this.server = server;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		this.application = OpenShiftServerUtils.getApplication(server);
		if(application == null) {
			return OpenShiftUIActivator.createErrorStatus("Failed to retrieve Application from the selected Server.\n" +
					"Please verify that the associated OpenShift Application still exists.");
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
