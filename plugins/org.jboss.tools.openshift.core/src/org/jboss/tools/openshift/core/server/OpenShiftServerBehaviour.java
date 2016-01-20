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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerBehaviour extends ServerBehaviourDelegate {
	private IAdaptable publishAdaptableInfo;
	
	public IStatus publish(int kind, IProgressMonitor monitor) {
		IStatus ret = super.publish(kind, monitor);
		setServerPublishState(ret.isOK() ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL);
		return ret;
	}
	
	public void publish(int kind, List<IModule[]> modules, IProgressMonitor monitor, IAdaptable info) throws CoreException {
		publishAdaptableInfo = info;
		try {
			super.publish(kind, modules, monitor, info);
		} finally {
			publishAdaptableInfo = null;
		}
	}
	
	public IAdaptable getPublishAdaptableInfo() {
		return publishAdaptableInfo;
	}
	
	public boolean canRestartModule(IModule[] module){
		if( module.length == 1 ) 
			return true;
		return false;
	}
	public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor) throws CoreException {
		// Do no setup
	}

	@Override
	public void stop(boolean force) {
		// No stopping either
	}
	
	
	/*
	 * Publishing code below
	 */
	private OpenShiftServerPublishMethod publishMethod;
	private OpenShiftServerPublishMethod getPublishMethod() {
		if( publishMethod == null ) {
			publishMethod = new OpenShiftServerPublishMethod();
		}
		return publishMethod;
	}
	
	@Override
	protected void publishStart(IProgressMonitor monitor) throws CoreException {
		getPublishMethod().publishStart(getServer(), monitor);
	}

	@Override
	protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor) throws CoreException {
		int state = getPublishMethod().publishModule(getServer(), kind, deltaKind, module, monitor);
		setModulePublishState(module, state);
	}

	@Override
	protected void publishFinish(IProgressMonitor monitor) throws CoreException {
		int serverSyncState = getPublishMethod().publishFinish(getServer(), monitor);
		setServerPublishState(serverSyncState);
	}
	
	@Override
	public IStatus canStart(String launchMode) {
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus canRestart(String mode) {
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus canStop() {
		return Status.CANCEL_STATUS;
	}	
	
}
