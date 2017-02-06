/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

public class OpenshiftServerLifecycleListener implements IServerLifecycleListener {
	@Override
	public void serverRemoved(IServer server) {
		// Take no action when a server is removed
	}
	@Override
	public void serverChanged(IServer server) {
		// Take no action when a server is changed
	}
	@Override
	public void serverAdded(IServer server) {
		if( server != null ) {
			String typeId = server.getServerType().getId();
			boolean start = server.getAttribute(OpenShiftServerUtils.SERVER_START_ON_CREATION, false); 
			if( OpenShiftServer.SERVER_TYPE_ID.equals(typeId) && start) {
				startServerInJob(server);
			}
		}
	}
	
	private void startServerInJob(IServer server) {
		new Job("Waiting for OpenShift pods to be ready") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					server.start("run", new NullProgressMonitor());
				} catch(CoreException ce) {
					OpenShiftCoreActivator.pluginLog().logError("Error starting server", ce);
				}
				return Status.OK_STATUS;
			}
			
		}.schedule(3000);
	}
	
}
