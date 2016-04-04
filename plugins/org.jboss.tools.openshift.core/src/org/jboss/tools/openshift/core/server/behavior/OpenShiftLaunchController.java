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

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IDeploymentConfig;
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
		IControllableServerBehavior behavior = server.getAdapter(IControllableServerBehavior.class);
		return behavior;
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IControllableServerBehavior serverBehavior = getServerBehavior(configuration);
		if( !(serverBehavior instanceof ControllableServerBehavior )) {
			throw toCoreException("Unable to find a IControllableServerBehavior instance");
		}
		ControllableServerBehavior beh = (ControllableServerBehavior) serverBehavior;
		
		beh.setServerStarting();
		
		IServer server = beh.getServer();
		
		IDeploymentConfig dc = OpenShiftServerUtils.getDeploymentConfig(server);
		if (dc == null) {
			beh.setServerStopped();
			throw toCoreException(NLS.bind("Could not find deployment config was for {0}. "
					+ "Your server adapter refers to an inexistant service"
					+ ", there are no pods for it "
					+ "or there are no labels on those pods pointing to the wanted deployment config.", 
					server.getName()));
		}
		String currentMode = beh.getServer().getMode();
		try {
			if( OpenShiftDebugUtils.DEBUG_MODE.equals(mode)) {
				OpenShiftDebugUtils.get().startDebugging(server, monitor);
			} else {//run, profile
				OpenShiftDebugUtils.get().stopDebugging(server, monitor);
			}
		} catch (CoreException e) {
			mode = currentMode;
			throw e;
		} finally {
			checkServerState(beh, currentMode, mode);
		}
		
	}

	private void checkServerState(ControllableServerBehavior beh, String oldMode, String mode) {
		int state = pollState();
		
		if (!Objects.equals(oldMode, mode)) {
			IModule[] modules = getServer().getModules();
			for( int i = 0; i < modules.length; i++ ) {
				((Server)getServer()).setModulePublishState(new IModule[]{modules[i]}, IServer.PUBLISH_STATE_FULL);
			}
			
			if( state == IServer.STATE_STARTED && Boolean.TRUE.equals(beh.getSharedData(OpenShiftServerBehaviour.CURRENTLY_RESTARTING))) {
				// Kick publish server job
				Job j = new Job("Publishing server " + getServer().getName()) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						return getServer().publish(IServer.PUBLISH_INCREMENTAL, monitor);
					}
				};
				j.schedule(3000);
				beh.putSharedData(OpenShiftServerBehaviour.CURRENTLY_RESTARTING, null);
			}
		}

		
		if( state == IServer.STATE_STARTED) {
			beh.setServerStarted();
			beh.setRunMode(mode);
		} else {
			beh.setServerStopped();
			((ControllableServerBehavior)getControllableBehavior()).setRunMode(null);
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
	
	private CoreException toCoreException(String msg, Exception e) {
		return new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, msg, e));
	}
	
	private CoreException toCoreException(String msg) {
		return toCoreException(msg, null);
	}
	
}
