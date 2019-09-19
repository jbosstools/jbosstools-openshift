/******************************************************************************* 
 * Copyright (c) 2015-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.VagrantPoller;

public class CDKShutdownController extends AbstractCDKShutdownController {
	@Override
	protected IServerStatePoller2 getPoller(IServer server) {
		return new VagrantPoller();
	}

	protected String getShutdownArgs() {
		String cmd = CDKConstants.VAGRANT_CMD_HALT + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR;
		return cmd;
	}

	protected Process call(IServer s, String args, String launchConfigName) throws CoreException, IOException {
		CDKServer cdk = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		return new CDKLaunchUtility().callInteractive(getServer(), args, getServer().getName(), cdk.skipUnregistration());
	}

	@Override
	protected boolean useTerminal() {
		return true;
	}

	@Override
	protected String getCommandLocation() {
		return BinaryUtility.VAGRANT_BINARY.getLocation();
	}

}
