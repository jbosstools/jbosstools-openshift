/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
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
import java.util.List;
import java.util.Optional;
import org.eclipse.core.commands.ExecutionEvent;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;
import org.jboss.tools.openshift.core.stack.RemoteStackProviderRegistry;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;

/**
 * @author Red Hat Developers
 */
public class DebugHandler extends OdoJobHandler {

	private int index;
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement componentElement = UIUtils.getFirstElement(selection, ComponentElement.class);
		Component component = componentElement.getWrapped();
		NamespaceElement namespaceElement = componentElement.getParent();
		executeDebug(componentElement.getRoot().getOdo(), namespaceElement.getWrapped(), component);
		return Status.OK_STATUS;
	}

	public void executeDebug(Odo odo, String project, Component component) {
		try {
			if (component.getLiveFeatures().isDebug()) {
				ComponentInfo info = odo.getComponentInfo(project, component.getName(), component.getPath(),
						component.getInfo().getComponentKind());
				RemoteStackDebugger remoteDebugger = RemoteStackProviderRegistry.getInstance()
						.findBytype(info.getComponentTypeName());
				if (remoteDebugger != null) {
					Integer port = getLocalPort(odo, project, component).orElse(allocateLocalPort());
					executeInJob("Attach debugger", monitor -> createAndLaunchConfig(component, info, port.intValue(),
							remoteDebugger, monitor));

				}
			} else {
				Display.getDefault().syncExec(() -> MessageDialog.openError(UIUtils.getShell(), "Debug",
						"Debugging is not supported for this type of component"));
			}
		} catch (IOException e) {
			OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
		}
	}

	private Optional<Integer> getLocalPort(Odo odo, String namespace, Component component) {
		Optional<Integer> port = Optional.empty();
		try {
			List<URL> urls = odo.listURLs(namespace, component.getPath(), component.getName());
			String[] ports = urls.stream().map(URL::getContainerPort).toArray(String[]::new);
			if (ports.length == 1) {
				port = Optional.ofNullable(Integer.valueOf(urls.get(0).getLocalPort()));
			} else if (ports.length > 1) {
				Display.getDefault().syncExec(() -> setIndex(MessageDialog.open(MessageDialog.QUESTION,
						Display.getDefault().getActiveShell(), "Choose debugger port",
						"The component " + component.getName()
								+ " has several ports to connect to,\nchoose the one the debugger will connect to.",
						SWT.NONE, ports)));
				if (index >= 0) {
					port = Optional.of(Integer.valueOf(urls.get(index).getLocalPort()));
				}
			}
		} catch (IOException e) {
			OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
		}
		return port;
	}

	private void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @param path           the project path
	 * @param stackType      the stack type
	 * @param stackVersion   the stack version
	 * @param port           the port to connect to
	 * @param remoteDebugger the debugger provider for the stack
	 * @param monitor        the progress monitor
	 * @param shell          the shell to use for UI
	 * @throws CoreException
	 */
	private void createAndLaunchConfig(Component component, ComponentInfo info, int port,
			RemoteStackDebugger remoteDebugger, IProgressMonitor monitor) {
		try {
			IPath projectPath = new Path(component.getPath());
			IContainer eclipseProject = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
			if (eclipseProject instanceof IProject) {
				remoteDebugger.startRemoteDebugger((IProject) eclipseProject, info.getComponentTypeName(), port,
						info.getEnv(), monitor);
			}
		} catch (CoreException e) {
			Display.getDefault().syncExec(() -> MessageDialog.openError(UIUtils.getShell(), "Debug",
					"Error while connecting the debugger: " + e.getLocalizedMessage()));
		}
	}

	private Integer allocateLocalPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return Integer.valueOf(socket.getLocalPort());
		}
	}
}
