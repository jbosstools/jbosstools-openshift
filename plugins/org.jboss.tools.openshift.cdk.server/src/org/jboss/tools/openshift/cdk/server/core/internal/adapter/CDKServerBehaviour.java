/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.adapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IModuleStateController;

public class CDKServerBehaviour extends ControllableServerBehavior implements IControllableServerBehavior  {
	
	public static final String PROP_CACHED_PASSWORD = "CDKServerBehaviour.CACHED_PASSWORD"; 
	public static final String PROP_CACHED_USER = "CDKServerBehaviour.CACHED_USER";
	
	public CDKServerBehaviour() {
	}

	
	// Unused  methods below since we don't support modules
	
	@Override
	protected void publishFinish(IProgressMonitor monitor) throws CoreException {
	}
	@Override
	protected void publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor) throws CoreException {
	}
	@Override
	protected void publishServer(int kind, IProgressMonitor monitor) throws CoreException {
	}
	@Override
	protected void publishStart(IProgressMonitor monitor) throws CoreException {
	}
	@Override
	public boolean canRestartModule(IModule[] module) {
		return false;
	}
	@Override
	public void startModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
	}
	@Override
	public void stopModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
	}
	@Override
	public void restartModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
		IModuleStateController controller = getModuleStateController();
		int newState = controller.restartModule(module, monitor);
		setModuleState(module, newState);
	}

	@Override
	public IStatus canPublish() {
		return Status.CANCEL_STATUS;
	}
	
	@Override
	public boolean canPublishModule(IModule[] module){
		return false;
	}
}
