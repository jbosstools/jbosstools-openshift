/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.debug;

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public class DebugLaunchConfigs {

	private ILaunchManager launchManager;

	public static DebugLaunchConfigs get() {
		return get(DebugPlugin.getDefault().getLaunchManager());
	}

	/** For testing purposes **/
	public static DebugLaunchConfigs get(ILaunchManager launchManager) {
		return new DebugLaunchConfigs(launchManager);
	}

	private DebugLaunchConfigs(ILaunchManager launchManager) {
		this.launchManager = launchManager;
	}

	public ILaunchConfiguration getRemoteDebuggerLaunchConfiguration(IServer server) throws CoreException {
		ILaunchConfigurationType launchConfigurationType = launchManager
				.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION);
		ILaunchConfiguration[] launchConfigs = launchManager.getLaunchConfigurations(launchConfigurationType);
		String name = getRemoteDebuggerLaunchConfigurationName(server);
		Optional<ILaunchConfiguration> maybeLaunch = Stream.of(launchConfigs).filter(lc -> name.equals(lc.getName()))
				.findFirst();

		return maybeLaunch.orElse(null);
	}

	public ILaunchConfigurationWorkingCopy createRemoteDebuggerLaunchConfiguration(IServer server)
			throws CoreException {
		String name = getRemoteDebuggerLaunchConfigurationName(server);
		ILaunchConfigurationType launchConfigurationType = launchManager
				.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION);
		return launchConfigurationType.newInstance(null, name);
	}

	public void setupRemoteDebuggerLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProject project,
			int debugPort) throws CoreException {
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
				IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);
		Map<String, String> connectMap = new HashMap<>(2);
		String portString = String.valueOf(debugPort);
		connectMap.put("port", portString); //$NON-NLS-1$
		connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);
		if (project != null) {
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		}
	}

	/**
	 * Returns {@code true} if the given launch configuration is running.
	 * 
	 * @param launchConfiguration
	 * @return
	 * 
	 * @
	 */
	public boolean isRunning(ILaunchConfiguration launchConfiguration) {
		return getLaunches().filter(l -> !l.isTerminated() && launchMatches(l, launchConfiguration)).findFirst()
				.isPresent();
	}

	private boolean launchMatches(ILaunch l, ILaunchConfiguration launchConfiguration) {
		return Objects.equals(l.getLaunchConfiguration(), launchConfiguration);
	}

	public static String getRemoteDebuggerLaunchConfigurationName(IServer server) {
		return "Remote debugger to " + server.getName();
	}

	public void terminateRemoteDebugger(IServer server) throws CoreException {
		ILaunchConfiguration launchConfig = getRemoteDebuggerLaunchConfiguration(server);
		if (launchConfig == null) {
			return;
		}
		List<IStatus> errors = new ArrayList<>();
		getLaunches().filter(l -> launchConfig.equals(l.getLaunchConfiguration())).filter(l -> l.canTerminate())
				.forEach(l -> terminate(l, errors));

		if (!errors.isEmpty()) {
			MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, IStatus.ERROR,
					errors.toArray(new IStatus[errors.size()]), "Failed to terminate remote launch configuration",
					null);
			throw new CoreException(status);
		}
	}

	private Stream<ILaunch> getLaunches() {
		return Stream.of(launchManager.getLaunches());
	}

	private void terminate(ILaunch launch, Collection<IStatus> errors) {
		try {
			launch.terminate();
		} catch (DebugException e) {
			errors.add(e.getStatus());
		}
	}

}
