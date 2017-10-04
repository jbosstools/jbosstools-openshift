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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Poller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.MinishiftPoller;

public class CDK3ShutdownController extends AbstractCDKShutdownController {

	@Override
	protected AbstractCDKPoller getCDKPoller(IServer server) {
		if( server.getServerType().getId().equals(CDK3Server.CDK_V3_SERVER_TYPE)) {
			return new MinishiftPoller();
		}
		return new CDK32Poller();
	}

	protected String getShutdownArgs() {
		String profiles = CDK3LaunchController.getProfileString(getServer());
		String cmd = profiles + "stop";
		return cmd;
	}
	

	protected Process call(IServer s, String cmd, String launchConfigName) throws CoreException, IOException {
		return new CDKLaunchUtility().callMinishiftInteractive(getServer(), cmd, getServer().getName());
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
