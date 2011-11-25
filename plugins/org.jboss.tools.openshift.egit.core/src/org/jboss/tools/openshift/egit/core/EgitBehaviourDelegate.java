/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.ide.eclipse.as.core.server.internal.DelegatingServerBehavior;
import org.jboss.ide.eclipse.as.core.server.internal.IJBossBehaviourDelegate;

public class EgitBehaviourDelegate implements IJBossBehaviourDelegate {

	public static final String ID = "egit";

	@Override
	public String getBehaviourTypeId() {
		return ID;
	}

	@Override
	public void setActualBehaviour(DelegatingServerBehavior actualBehaviour) {
	}

	@Override
	public void stop(boolean force) {
	}

	@Override
	public void publishStart(IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public void publishFinish(IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public void onServerStarting() {
		// do nothing
	}

	@Override
	public void onServerStopping() {
		// do nothing
	}

	@Override
	public IStatus canChangeState(String launchMode) {
		// do nothing
		return Status.OK_STATUS;
	}

	/**
	 * remove from interface
	 */
	@Override
	public String getDefaultStopArguments() throws CoreException {
		return null;
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public void onServerStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onServerStopped() {
		// TODO Auto-generated method stub
		
	}

}
