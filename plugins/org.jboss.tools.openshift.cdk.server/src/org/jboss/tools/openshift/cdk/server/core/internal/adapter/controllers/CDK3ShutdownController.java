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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Poller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.MinishiftPoller;

public class CDK3ShutdownController extends AbstractCDKShutdownController {
	@Override
	public void stop(boolean force) {
		getBehavior().setServerStopping();
		CDK3Server cdk3 = (CDK3Server) getServer().loadAdapter(CDK3Server.class, new NullProgressMonitor());
		String msHome = cdk3.getMinishiftHome();
		if (!(new File(msHome).exists())) {
			// The minishift home doesn't exist. We need to mark server as stopped and log an error
			String msg = "The minishift-home for server \"" + getServer().getName() + "\" does not exist: " + msHome
					+ "\n\nPlease make sure that the virtual machine associated with this server has been properly shutdown.";
			IStatus err = new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, msg, new Exception(msg));
			CDKCoreActivator.pluginLog().logStatus(err);
			getBehavior().setServerStopped();
			return;
		}

		pollState();
		if (getServer().getServerState() == IServer.STATE_STOPPED) {
			return;
		}
		issueShutdownCommand();
	}

	@Override
	protected AbstractCDKPoller getCDKPoller(IServer server) {
		if (server.getServerType().getId().equals(CDK3Server.CDK_V3_SERVER_TYPE)) {
			return new MinishiftPoller();
		}
		return new CDK32Poller();
	}

	protected String getShutdownArgs() {
		IServer s = getServer();
		CDKServer cdk = null;
		if (s != null)
			cdk = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());

		String profiles = CDK3LaunchController.getProfileString(getServer());
		String cmd = profiles + "stop";

		if (cdk != null) {
			boolean skipUnregistration = cdk.skipUnregistration();
			if (skipUnregistration) {
				cmd += " --skip-unregistration";
			}
		}

		return cmd;
	}

	protected Process call(IServer s, String cmd, String launchConfigName) throws CoreException, IOException {
		CDKServer cdk = (CDKServer)getServer().loadAdapter(CDKServer.class, new NullProgressMonitor());
		return new CDKLaunchUtility().callMinishiftInteractive(getServer(), cmd, getServer().getName(), cdk.skipUnregistration());
	}

	@Override
	protected boolean useTerminal() {
		return false;
	}

	@Override
	protected String getCommandLocation() {
		return MinishiftBinaryUtility.getMinishiftLocation(getServer());
	}
}
