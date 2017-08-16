
/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.js.server.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.internal.js.OpenShiftNodejsActivator;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

import com.openshift.restclient.model.IReplicationController;

public class OpenShiftNodejsLaunchController extends OpenShiftLaunchController implements ISubsystemController {
	
	public OpenShiftNodejsLaunchController() {
		super();
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		OpenShiftServerBehaviour beh = OpenShiftServerUtils.getOpenShiftServerBehaviour(configuration);
		String currentMode = beh.getServer().getMode();
		beh.setServerStarting();
		launchServerProcess(beh, launch, monitor);
		try {
			if (waitForDeploymentConfigReady(beh.getServer(), monitor)) {
				DebugContext context = createDebugContext(beh, monitor);
				// always toggle since we might have to disable debug mode
				toggleDebugging(mode, beh, context, monitor);
				if (!isDebugMode(mode)) {
					enableDevMode(context);
				}
				new OpenShiftDebugMode(context).execute(monitor);
			}
		} catch (Exception e) {
			mode = currentMode;
			throw new CoreException(StatusFactory.errorStatus(OpenShiftNodejsActivator.PLUGIN_ID,
					NLS.bind("Could not launch server {0}", beh.getServer().getName()), e));
		} finally {
			setServerState(beh, mode, monitor);
		}
	}
	
	/**
	 * Enables the dev mode environment variable in the given
	 * {@link IReplicationController} for Node.js projects (by default). The devmode
	 * is required for nodejs so that it takes (file) changes into account.
	 * 
	 * @throws CoreException
	 *
	 * @see <a href="https://issues.jboss.org/browse/JBIDE-22362">JBIDE-22362</a>
	 */
	private void enableDevMode(DebugContext context) {
		new OpenShiftDebugMode(context).enableDevmode();
	}
	
	@Override
	protected void startDebugging(OpenShiftServerBehaviour beh, DebugContext context, IProgressMonitor monitor) {
		IDebugListener listener = new IDebugListener() {
			
			@Override
			public void onDebugChange(DebugContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				int localPort = mapPortForwarding(debuggingContext, monitor);
				NodeDebugLauncher.launch(beh.getServer(), localPort);
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		context.setDebugListener(listener);
		new OpenShiftDebugMode(context).enableDebugging();
	}
}
