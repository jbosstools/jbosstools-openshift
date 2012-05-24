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
package org.jboss.tools.openshift.express.internal.core.portforward;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * 
 */
public class RemoteOpenShiftApplicationDebuggingLaunchConfigurationDelegate extends
		JavaRemoteApplicationLaunchConfigurationDelegate {

	public final static String ATTR_CONNECT_HOSTNAME = "hostname"; // see usage in SocketAttachConnector
	public final static String ATTR_CONNECT_PORT = "port"; // see usage in SocketAttachConnector

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration,
	 * java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor.beginTask("Preparing for debugging remote OpenShift application", 1);
			Map<String, String> argMap = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP,
					(Map) null);
			if (argMap != null) {
				final String applicationName = configuration.getAttribute(
						RemoteOpenShiftApplicationConfigurationTab.LAUNCH_CONFIG_APPLICATION, "");
				final IApplication application = lookupApplication(applicationName);
				/*
				final ApplicationPortForwardUtil applicationPortsForwarder = new ApplicationPortForwardUtil();
				for (ApplicationPortForward port : applicationPortsForwarder.listPorts(application)) {
					if (port.getRemotePort() == "8787") {
						final ApplicationPortForward forwardedPort = null; //applicationPortsForwarder.forwardPort(application, port);
						argMap.put(ATTR_CONNECT_HOSTNAME, forwardedPort.getLocalAddress());
						argMap.put(ATTR_CONNECT_PORT, forwardedPort.getLocalPort());
					}
				}*/

			}
			monitor.worked(1);
			super.launch(configuration, mode, launch, monitor);
		} catch (Exception e) {
			Logger.error("Failed to launch debugger for Remote OpenShift Application", e);
		}
	}

	private IApplication lookupApplication(String applicationName) {
		try {
			for (UserDelegate user : UserModel.getDefault().getUsers()) {
				final IApplication application = user.getApplicationByName(applicationName);
				if (application != null) {
					return application;
				}
			}
		} catch (OpenShiftException e) {
			Logger.error("Failed to retrieve applications from user", e);
		}
		return null;
	}

}
