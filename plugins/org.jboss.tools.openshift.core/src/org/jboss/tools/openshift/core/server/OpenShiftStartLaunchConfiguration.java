/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftStartLaunchConfiguration 
	extends AbstractJavaLaunchConfigurationDelegate 
	implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) 
			throws CoreException {
		final IServer s = ServerUtil.getServer(configuration);
		if( s == null ) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus("Unable to locate server from launch configuration."));
		}
		
		OpenShiftServerBehaviour beh = getBehavior(s);
		beh.setServerStarted();
	}

	public static OpenShiftServerBehaviour getBehavior(IServerAttributes server) {
		OpenShiftServerBehaviour behavior = (OpenShiftServerBehaviour) server.getAdapter(IControllableServerBehavior.class);
		if( behavior == null ) {
			behavior = (OpenShiftServerBehaviour) server.loadAdapter(OpenShiftServerBehaviour.class, new NullProgressMonitor());
		}
		return behavior;
	}

}
