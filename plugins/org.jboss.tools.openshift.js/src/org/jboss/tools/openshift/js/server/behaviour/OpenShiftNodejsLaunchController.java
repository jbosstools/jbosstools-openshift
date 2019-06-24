
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
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController;
import org.jboss.tools.openshift.internal.core.Dialogs;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

public class OpenShiftNodejsLaunchController extends OpenShiftLaunchController implements ISubsystemController {

	private static final String WARN_NEWER_THAN_NODEJS7_UNSUPPORTED = "org.jboss.tools.openshift.js.dontwarnNodeJs7";

	public OpenShiftNodejsLaunchController() {
		super();
	}

	@Override
	protected void startDebugging(OpenShiftServerBehaviour beh, DebugContext context, IProgressMonitor monitor) {
		// https://issues.jboss.org/browse/JBIDE-26408
		Dialogs.INSTANCE.warn("Node.js 8+ unsupported	", 
				"Debugging is only supported up to Node.js 7. If your Node.js is newer, debugging will not work.\n"
				+ "If using an OpenShift template to create the application, set 'NODEJS_PROPERTY' to '6'", 
				WARN_NEWER_THAN_NODEJS7_UNSUPPORTED);
		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebugContext debuggingContext, IProgressMonitor monitor) throws CoreException {
				int localPort = startPortForwarding(debuggingContext, monitor);
				NodeDebugLauncher.launch(beh.getServer(), localPort);
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor) throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		context.setDebugListener(listener);
		new OpenShiftDebugMode(context).enableDebugging();
	}
}
