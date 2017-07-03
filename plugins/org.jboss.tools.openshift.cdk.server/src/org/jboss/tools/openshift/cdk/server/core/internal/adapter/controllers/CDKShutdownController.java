/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
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
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.VagrantBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;

public class CDKShutdownController extends AbstractCDKShutdownController {

	protected AbstractCDKPoller getCDKPoller() {
		return new VagrantPoller();
	}

	protected String getShutdownArgs() {
		String cmd = CDKConstants.VAGRANT_CMD_HALT + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR;
		return cmd;
	}
	
	protected Process call(IServer s, String cmd, String launchConfigName) throws CoreException, IOException {
		return new CDKLaunchUtility().callInteractive(getServer(), cmd, getServer().getName());
	}

	@Override
	protected boolean useTerminal() {
		return true;
	}
	
	@Override
	protected String getCommandLocation() {
		return VagrantBinaryUtility.getVagrantLocation();
	}

}
