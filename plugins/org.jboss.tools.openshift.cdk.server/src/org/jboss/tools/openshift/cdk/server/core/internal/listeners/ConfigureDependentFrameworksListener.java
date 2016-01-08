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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class ConfigureDependentFrameworksListener extends UnitedServerListener {
	public void serverChanged(final ServerEvent event) {
		if( serverSwitchesToState(event, IServer.STATE_STARTED) && canHandleServer(event.getServer())) {
			new Thread("Loading ADBInfo to configure dependent frameworks") {
				public void run() {
					if( canHandleServer(event.getServer()))
						launchChange(event.getServer());
				}
			}.start();
		}
	}
	
	private void launchChange(IServer server) {
		ADBInfo adb = ADBInfo.loadADBInfo(server);

		// save the adbinfo somewhere!
		ControllableServerBehavior beh = (ControllableServerBehavior)server.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
		if( beh != null ) {
			beh.putSharedData(ADBInfo.SHARED_INFO_KEY, adb);
		}
		
		if( adb != null ) {
			configureOpenshift(server, adb);
			configureDocker(server, adb);
		}
	}
	

	public boolean canHandleServer(IServer server) {
		if( server.getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE)) 
			return true;
		return false;
	}
	
	private void configureDocker(IServer server, ADBInfo adb) {
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
	
	private void configureOpenshift(IServer server, ADBInfo adb) {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		if( util.findExistingOpenshiftConnection(server, adb) == null ) {
			util.createOpenshiftConnection(server, adb);
		}
	}
}
