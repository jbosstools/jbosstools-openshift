/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 */
public class DebugHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			OpenShiftClient client = component.getRoot().getClient();
			String project = component.getParent().getParent().getWrapped().getMetadata().getName();
			String application = component.getParent().getWrapped().getName();
			ComponentInfo info = odo.getComponentInfo(client, project, application, component.getWrapped().getName());
			if (isDebuggable(info.getComponentTypeName())) {
				int port = allocateLocalPort();
				executeInJob("Debug", () -> startDebug(odo, project, application, component, port));
				createAndLaunchConfig(component.getWrapped().getPath(), port);
			} else {
				MessageDialog.openError(shell, "Debug", "Debugging is not supported for this type of component");
			}
			return Status.OK_STATUS;
		} catch (IOException | CoreException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	/**
	 * @param path the project path
	 * @param port the port to connect to
	 * @throws CoreException 
	 */
	private void createAndLaunchConfig(String path, int port) throws CoreException {
		IPath projectPath = new Path(path);
		IContainer project = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
		if (project instanceof IProject) {
			String name = "OpenShift remote " + project.getName();
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
			launchConfiguration.launch("debug", new NullProgressMonitor());
		}
	}

	/**
	 * @param componentTypeName
	 * @return
	 */
	private boolean isDebuggable(String componentTypeName) {
		return "java".equals(componentTypeName);
	}

	/**
	 * @param component
	 * @return
	 */
	private void startDebug(Odo odo, String project, String application, ComponentElement component, int port) {
		try {
			odo.debug(project, application, component.getWrapped().getPath(), component.getWrapped().getName(), port);
		} catch (IOException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	/**
	 * @return
	 * @throws IOException 
	 */
	private int allocateLocalPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
