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

import java.io.IOException;
import java.net.ServerSocket;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;
import org.jboss.tools.openshift.core.stack.RemoteStackProviderRegistry;
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
			RemoteStackDebugger remoteDebugger = RemoteStackProviderRegistry.getInstance().findBytype(info.getComponentTypeName());
			if (remoteDebugger != null) {
				int port = allocateLocalPort();
				executeInJob("Debug", monitor -> startDebug(odo, project, application, component, port));
				executeInJob("Attach debugger", monitor -> createAndLaunchConfig(component.getWrapped().getPath(), port, remoteDebugger, monitor, shell));
			} else {
				MessageDialog.openError(shell, "Debug", "Debugging is not supported for this type of component");
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	/**
	 * @param path the project path
	 * @param port the port to connect to
	 * @throws CoreException 
	 */
	private void createAndLaunchConfig(String path, int port, RemoteStackDebugger remoteDebugger, IProgressMonitor monitor, Shell shell) {
		try {
			IPath projectPath = new Path(path);
			IContainer project = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
			if (project instanceof IProject) {
				remoteDebugger.startRemoteDebugger((IProject) project, port, monitor);
			}
		} catch (CoreException e) {
			shell.getDisplay().asyncExec(() -> MessageDialog.openError(shell, "Debug", "Error while connecting the debugger"));
		}
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
