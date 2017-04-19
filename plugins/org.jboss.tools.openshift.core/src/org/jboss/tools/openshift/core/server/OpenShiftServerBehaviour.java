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
package org.jboss.tools.openshift.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.UpdateModuleStateJob;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.CachedPublisherProfileBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IModuleStateController;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * The OpenShiftServerBehavior is an {@link IControllableServerBehavior},
 * which means it pulls its behavior from the various subsystems and profiles
 * declared via xml. 
 * 
 * This server requires caching the Publish controller, since the publish controller maintains some state between calls
 */
public class OpenShiftServerBehaviour extends CachedPublisherProfileBehavior {
	public static final String PROFILE_OPENSHIFT3 = "openshift3";
	
	private static final String CURRENTLY_RESTARTING = "openshift.server.restarting";
	
	@Override
	public void setServerStarted() {
		super.setServerStarted();
		launchPostStartupJobs();
	}

	@Override
	public void restart(String launchMode) throws CoreException {
		setRestarting(true);
		super.restart(launchMode);
	}

	public boolean isRestarting() {
		return Boolean.TRUE.equals(getSharedData(CURRENTLY_RESTARTING));
	}
	
	public void setRestarting(boolean restarting) {
		putSharedData(CURRENTLY_RESTARTING, restarting);
	}

	protected void launchPostStartupJobs() {
		try {
			// Once the server is marked started, we want to update the deployment scanners and module publish state
			IServer s = getServer();
			IModuleStateController modules = getModuleStateController();
			Job moduleStateJob = null;
			if( modules != null ) {
				moduleStateJob = new UpdateModuleStateJob(modules, s, true, 10000);
				moduleStateJob.schedule();
			}
		} catch(CoreException ce) {
			OpenShiftCoreActivator.pluginLog().logError(ce);
		}
	}


}
