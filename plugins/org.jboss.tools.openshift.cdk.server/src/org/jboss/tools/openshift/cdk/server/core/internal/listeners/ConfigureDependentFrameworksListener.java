/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class ConfigureDependentFrameworksListener extends UnitedServerListener {
	public void serverChanged(final ServerEvent event) {
		if( serverSwitchesToState(event, IServer.STATE_STARTED) && canHandleServer(event.getServer())) {
			new Job("Loading service-manager to configure additional frameworks that CDK depends on.") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						if( canHandleServer(event.getServer()))
							launchChange(event.getServer());
						return Status.OK_STATUS;
					} catch(CoreException ce) {
						return ce.getStatus();
					}
				}
			}.schedule(1000);
		}
	}
	
	private void launchChange(IServer server) throws CoreException {
		ServiceManagerEnvironment adb = ServiceManagerEnvironment.getOrLoadServiceManagerEnvironment(server, true);
		if( adb != null ) {
			configureOpenshift(server, adb);
			configureDocker(server, adb);
		} else {
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Unable to configure docker and openshift. Calls to vagrant service-manager are returning empty environments."));
		}
	}
	

	public boolean canHandleServer(IServer server) {
		if( server.getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE)) 
			return true;
		return false;
	}
	
	private void configureDocker(IServer server, ServiceManagerEnvironment adb) {
		try {
			CDKDockerUtility util = new CDKDockerUtility();
			if( !util.dockerConnectionExists(adb)) {
				util.createDockerConnection(server, adb);
			}
		} catch(DockerException de) {
			CDKCoreActivator.pluginLog().logError(
					"Error while creating docker connection for server " + server.getName(), de);
		}
	}
	
	private void configureOpenshift(IServer server, ServiceManagerEnvironment adb) {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		if( util.findExistingOpenshiftConnection(server, adb) == null ) {
			util.createOpenshiftConnection(server, adb);
		}
	}
}
