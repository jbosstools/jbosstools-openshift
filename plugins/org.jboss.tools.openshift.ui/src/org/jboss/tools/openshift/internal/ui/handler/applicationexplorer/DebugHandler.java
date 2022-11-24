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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentFeature;
import org.jboss.tools.openshift.core.odo.ComponentInfo;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;
import org.jboss.tools.openshift.core.stack.RemoteStackProviderRegistry;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 */
public class DebugHandler extends FeatureHandler {

	public DebugHandler() {
		super(ComponentFeature.DEBUG);
	}

	@Override
	protected IStatus process(Odo odo, String project, Component component, Consumer<Boolean> callback)
			throws IOException {
		return super.process(odo, project, component, callback.andThen(b -> executeDebug(odo, project, component)));
	}

	public void executeDebug(Odo odo, String project, Component component) {
		try {
			if (component.getLiveFeatures().isDebug()) {
				ComponentInfo info = odo.getComponentInfo(project, component.getName(), component.getPath(),
						component.getInfo().getComponentKind());
				RemoteStackDebugger remoteDebugger = RemoteStackProviderRegistry.getInstance()
						.findBytype(info.getComponentTypeName());
				if (remoteDebugger != null) {
					DebugStatus debugStatus = odo.debugStatus(project, component.getPath(),
							component.getName());
					Integer port = debugStatus == DebugStatus.RUNNING ? getLocalPort(odo, project, component).get() : allocateLocalPort();
					if (debugStatus != DebugStatus.RUNNING) {
						executeInJob("Debug", monitor -> startDebug(odo, project, component, port));
					}
					executeInJob("Attach debugger", monitor -> createAndLaunchConfig(odo, project, 
							component, info, port.intValue(), remoteDebugger, monitor));

				}
			} else {
				MessageDialog.openError(shell, "Debug", "Debugging is not supported for this type of component");
			}
		} catch (IOException e) {
			OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
		}
	}

	private Optional<Integer> getLocalPort(Odo odo, String namespace, Component component) {
        Optional<Integer> port = Optional.empty();
        try {
            List<URL> urls = odo.listURLs(namespace, component.getPath(), component.getName());
            String[] ports = urls.stream().
                    map(URL::getContainerPort).toArray(String[]::new);
            if (ports.length == 1) {
                port = Optional.ofNullable(Integer.parseInt(urls.get(0).getLocalPort()));
            } else if (ports.length > 1) {
            	
            	int index = MessageDialog.open(MessageDialog.QUESTION, shell, "Choose debugger port", "The component " +
                                component.getName() +
                                " has several ports to connect to,\nchoose the one the debugger will connect to.", SWT.NONE, ports);
                if (index >=0) {
                    port = Optional.of(Integer.parseInt(urls.get(index).getLocalPort()));
                }
            }
        } catch (IOException e) {
        	OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
        }
        return port;
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
	private void createAndLaunchConfig(Odo odo, String project, Component component,
			ComponentInfo info, int port, RemoteStackDebugger remoteDebugger, IProgressMonitor monitor) {
		try {
			waitForDebugRunning(odo, project, component, monitor);
			IPath projectPath = new Path(component.getPath());
			IContainer ecliseProject = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(projectPath);
			if (ecliseProject instanceof IProject) {
				remoteDebugger.startRemoteDebugger((IProject) ecliseProject, info.getComponentTypeName(),
						 port, info.getEnv(), monitor);
			}
		} catch (CoreException e) {
			shell.getDisplay()
					.asyncExec(() -> MessageDialog.openError(shell, "Debug", "Error while connecting the debugger"));
		}
	}

	/**
	 * @param component
	 * @return
	 */
	private void startDebug(Odo odo, String project, Component component, Integer port) {
		try {
			odo.debug(project, component.getPath(), component.getName(), port);
		} catch (IOException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	/**
	 * @return
	 * @throws IOException
	 */
	private Integer allocateLocalPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return Integer.valueOf(socket.getLocalPort());
		}
	}

	private void waitForDebugRunning(Odo odo, String project, Component component,
			IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 60_000 && !monitor.isCanceled()) {
			try {
				DebugStatus status = odo.debugStatus(project, component.getPath(), component.getName());
				if (status != DebugStatus.RUNNING) {
					try {
						Thread.sleep(1_000L);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new CoreException(
								new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
					}
				} else {
					return;
				}
			} catch (IOException e) {
				throw new CoreException(
						new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getLocalizedMessage()));
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Can't connect to JVM"));
	}
}
