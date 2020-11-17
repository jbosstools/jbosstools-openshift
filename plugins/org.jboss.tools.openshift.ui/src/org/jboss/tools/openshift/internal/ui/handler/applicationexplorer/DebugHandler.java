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
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
			ComponentInfo info = odo.getComponentInfo(client, project, application, component.getWrapped().getName(),
			    component.getWrapped().getPath(), component.getWrapped().getInfo().getComponentKind());
			RemoteStackDebugger remoteDebugger = RemoteStackProviderRegistry.getInstance().findBytype(info.getComponentTypeName(), info.getComponentTypeVersion());
			if (remoteDebugger != null) {
				int port = allocateLocalPort();
				executeInJob("Debug", monitor -> startDebug(odo, project, application, component, port));
				executeInJob("Attach debugger", monitor -> createAndLaunchConfig(component.getWrapped().getPath(), info.getComponentTypeName(), info.getComponentTypeVersion(), port, remoteDebugger, monitor, shell));
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
	 * @param stackType the stack type
	 * @param stackVersion the stack version
	 * @param port the port to connect to
	 * @param remoteDebugger the debugger provider for the stack
	 * @param monitor the progress monitor
	 * @param shell the shell to use for UI
	 * @throws CoreException 
	 */
	private void createAndLaunchConfig(String path, String stackType, String stackVersion, int port, RemoteStackDebugger remoteDebugger, IProgressMonitor monitor, Shell shell) {
		try {
			waitForPortAvailable(port, monitor);
			IPath projectPath = new Path(path);
			IContainer project = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
			if (project instanceof IProject) {
				remoteDebugger.startRemoteDebugger((IProject) project, stackType, stackVersion, port, monitor);
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
	
	private void waitForPortAvailable(int port, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 60_000 && !monitor.isCanceled()) {
			try (Socket socket = new Socket("localhost", port)) {
				return;
			} catch (ConnectException e) {
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e1) {
					throw new CoreException(
							new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
				}
			} catch (IOException e) {
				throw new CoreException(
						new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Can't connect to JVM"));
	}
}
