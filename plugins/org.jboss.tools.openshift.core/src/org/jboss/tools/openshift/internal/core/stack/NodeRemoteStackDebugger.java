/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;

/**
 * @author Red Hat Developers
 *
 */
public class NodeRemoteStackDebugger implements RemoteStackDebugger {

	private static final String ID_REMOTE_NODE_APPLICATION = "org.eclipse.wildwebdeveloper.launchConfiguration.nodeDebugAttach";

	@Override
	public boolean isValid(String stackType, String stackVersion) {
		return "nodejs".equals(stackType);
	}

	@Override
	public void startRemoteDebugger(IProject project, String stackType, String stackVersion, int port, IProgressMonitor monitor) throws CoreException {
		String name = "OpenShift remote (Node) " + project.getName();
		ILaunchConfigurationType launchConfigurationType = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(ID_REMOTE_NODE_APPLICATION);
		ILaunchConfigurationWorkingCopy launchConfiguration = launchConfigurationType.newInstance(null, name);
		launchConfiguration.setAttribute("port", port); //$NON-NLS-1$
		launchConfiguration.setAttribute("address", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
		launchConfiguration.launch("debug", monitor);
	}
}
