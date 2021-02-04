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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.DebugInfo;
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
			  DebugInfo debugInfo = odo.debugInfo(project, application, component.getWrapped().getPath(), component.getWrapped().getName());
			  int port = debugInfo.getStatus() == DebugStatus.RUNNING?debugInfo.getLocalPort():allocateLocalPort();
			  if (debugInfo.getStatus() != DebugStatus.RUNNING) {
	        executeInJob("Debug", monitor -> startDebug(odo, project, application, component, port));
			  }
				executeInJob("Attach debugger", monitor -> createAndLaunchConfig(odo, project, application, component.getWrapped(), info, port, remoteDebugger, monitor, shell));
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
	private void createAndLaunchConfig(Odo odo, String project, String application, Component component, ComponentInfo info, int port, RemoteStackDebugger remoteDebugger, IProgressMonitor monitor, Shell shell) {
		try {
			waitForDebugRunning(odo, project, application, component, monitor);
			IPath projectPath = new Path(component.getPath());
			IContainer ecliseProject = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
			if (ecliseProject instanceof IProject) {
				remoteDebugger.startRemoteDebugger((IProject) ecliseProject, info.getComponentTypeName(), info.getComponentTypeVersion(), port, info.getEnv(), monitor);
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
	
	private void waitForDebugRunning(Odo odo, String project, String application, Component component, IProgressMonitor monitor) throws CoreException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 60_000 && !monitor.isCanceled()) {
      try {
        DebugInfo info = odo.debugInfo(project, application, component.getPath(), component.getName());
        if (info.getStatus() != DebugStatus.RUNNING) {
          try {
            Thread.sleep(1_000L);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
          }
        } else {
          return;
        }
      } catch (IOException e) {
        throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
      }
    }
    throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Can't connect to JVM"));
	}
}
