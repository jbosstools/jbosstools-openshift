/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.core.Trace;
import org.jboss.ide.eclipse.as.core.server.internal.DelegatingServerBehavior;
import org.jboss.ide.eclipse.as.core.util.LaunchCommandPreferences;

/**
 * @author Rob Stryker
 */
public class OpenshiftDelegatingStartLaunchConfiguration 
	extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IServer server = ServerUtil.getServer(configuration);
		ServerBehaviourDelegate del = (ServerBehaviourDelegate) server.loadAdapter(ServerBehaviourDelegate.class, new NullProgressMonitor());
		IStatus s = del.canStart(mode);

		Trace.trace(Trace.STRING_FINEST, "Ensuring Server can start: " + s.getMessage()); //$NON-NLS-1$
		if (!s.isOK())
			throw new CoreException(s);
		if (LaunchCommandPreferences.isIgnoreLaunchCommand(server)) {
			Trace.trace(Trace.STRING_FINEST, "Server is marked as ignore Launch. Marking as started."); //$NON-NLS-1$
			((Server)server).setServerState(IServer.STATE_STARTING);
			((Server)server).setServerState(IServer.STATE_STARTED);
		}
	}

}
