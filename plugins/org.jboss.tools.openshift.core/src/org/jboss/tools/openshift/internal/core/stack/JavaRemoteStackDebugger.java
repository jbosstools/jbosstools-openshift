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

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;

/**
 * @author Red Hat Developers
 *
 */
public class JavaRemoteStackDebugger implements RemoteStackDebugger {

	@Override
	public boolean isValid(String stackType) {
		return "java".equals(stackType);
	}

	@Override
	public void startRemoteDebugger(IProject project, int port, IProgressMonitor monitor) throws CoreException {
		String name = "OpenShift remote (Java) " + project.getName();
		ILaunchConfigurationType launchConfigurationType = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION);
		ILaunchConfigurationWorkingCopy launchConfiguration = launchConfigurationType.newInstance(null, name);
		launchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
		launchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
				IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);
		Map<String, String> connectMap = new HashMap<>(2);
		connectMap.put("port", String.valueOf(port)); //$NON-NLS-1$
		connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
		launchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);
			launchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		launchConfiguration.launch("debug", monitor);
	}
}
