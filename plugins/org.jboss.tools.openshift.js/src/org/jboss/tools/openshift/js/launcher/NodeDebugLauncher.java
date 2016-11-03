/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.js.launcher;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.jsdt.chromium.debug.core.model.BreakpointSynchronizer;
import org.eclipse.wst.jsdt.chromium.debug.core.model.LaunchParams;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.js.listeners.NodeDebugTerminateListener;
import org.jboss.tools.openshift.internal.js.storage.SessionStorage;
import org.jboss.tools.openshift.internal.js.util.NodeDebuggerUtil;

import com.openshift.restclient.model.IService;

/**
 * @author "Ilya Buziuk (ibuziuk)"
 */
public final class NodeDebugLauncher {

	public static void launch(IServer server, int port) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager
				.getLaunchConfigurationType(NodeDebuggerUtil.CHROMIUM_LAUNCH_CONFIGURATION_TYPE_ID);

		IProject project = OpenShiftServerUtils.getDeployProject(server);
		String projectName = project.getName();

		final ILaunchConfigurationWorkingCopy v8debugLaunch = type.newInstance(project, projectName);

		v8debugLaunch.setAttribute(LaunchParams.CHROMIUM_DEBUG_HOST, NodeDebuggerUtil.LOCALHOST);

		v8debugLaunch.setAttribute(LaunchParams.CHROMIUM_DEBUG_PORT, port);

		v8debugLaunch.setAttribute(LaunchParams.ADD_NETWORK_CONSOLE, true);

		v8debugLaunch.setAttribute(LaunchParams.BREAKPOINT_SYNC_DIRECTION,
				BreakpointSynchronizer.Direction.MERGE.name());

		v8debugLaunch.setAttribute(LaunchParams.SOURCE_LOOKUP_MODE, LaunchParams.LookupMode.EXACT_MATCH.name());

		v8debugLaunch.setAttribute(LaunchParams.ATTR_APP_PROJECT,
				OpenShiftServerUtils.getDeployProject(server).getName());

		v8debugLaunch.setAttribute(LaunchParams.ATTR_APP_PROJECT_RELATIVE_PATH,
				project.getFile(NodeDebuggerUtil.PACKAGE_JSON).getProjectRelativePath().toOSString());

		v8debugLaunch.setAttribute(LaunchParams.ATTR_REMOTE_HOME_DIR, getPodPath(server));

		v8debugLaunch.setAttribute(LaunchParams.PredefinedSourceWrapperIds.CONFIG_PROPERTY,
				NodeDebuggerUtil.encode(NodeDebuggerUtil.PREDEFIENED_WRAPPERS));

		DebugPlugin.getDefault().addDebugEventListener(new NodeDebugTerminateListener(v8debugLaunch, server));

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				DebugUITools.launch(v8debugLaunch, ILaunchManager.DEBUG_MODE);
				// Debug session has just started - adding server to tracker
				SessionStorage.get().add(server);
			}
		});
	}

	private static String getPodPath(IServer server) throws CoreException {
		IService service = OpenShiftServerUtils.getService(server);
		return OpenShiftServerUtils.loadPodPath(service, server);
	}

}
