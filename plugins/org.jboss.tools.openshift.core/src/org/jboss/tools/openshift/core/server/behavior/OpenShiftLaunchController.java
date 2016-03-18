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
package org.jboss.tools.openshift.core.server.behavior;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IService;

public class OpenShiftLaunchController extends AbstractSubsystemController
		implements ISubsystemController, ILaunchServerController {


	/**
	 * Get access to the ControllableServerBehavior
	 * @param configuration
	 * @return
	 * @throws CoreException
	 */
	protected static IControllableServerBehavior getServerBehavior(ILaunchConfiguration configuration) throws CoreException {
		IServer server = ServerUtil.getServer(configuration);
		IControllableServerBehavior behavior = (IControllableServerBehavior) server.getAdapter(IControllableServerBehavior.class);
		return behavior;
	}

	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IControllableServerBehavior beh = getServerBehavior(configuration);
		if( beh != null ) {
			((ControllableServerBehavior)beh).setServerStarting();
			int state = pollState();
			if( state == IServer.STATE_STARTED) {
				((ControllableServerBehavior)beh).setServerStarted();
				((ControllableServerBehavior)getControllableBehavior()).setRunMode(mode);
			} else {
				((ControllableServerBehavior)beh).setServerStopped();
				((ControllableServerBehavior)getControllableBehavior()).setRunMode(null);
			}
		} else {
			// TODO throw error
		}
	}

	protected int pollState() {
		IService service = null;
		Exception e = null;
		try {
			service = OpenShiftServerUtils.getService(getServer());
		} catch(OpenShiftException ose ) {
			e = ose;
		}
		if (service == null) {
			OpenShiftCoreActivator.pluginLog().logError("The OpenShift service for server " + getServer().getName() + " could not be reached.", e);
			return IServer.STATE_STOPPED;
		}
		return IServer.STATE_STARTED;
	}
	
	@Override
	public IStatus canStart(String launchMode) {
		return Status.OK_STATUS;
	}

	@Override
	public void setupLaunchConfiguration(
			ILaunchConfigurationWorkingCopy workingCopy,
			IProgressMonitor monitor) throws CoreException {
		// Do Nothing
	}

}
