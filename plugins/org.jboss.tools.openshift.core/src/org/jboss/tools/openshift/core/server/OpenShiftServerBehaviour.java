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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.internal.deployables.J2EEDeployableFactory;
import org.eclipse.jst.jee.internal.deployables.JEEDeployableFactory;
import org.eclipse.m2e.core.internal.IMavenConstants;
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
	
	@SuppressWarnings("restriction")
    @Override
	public IStatus publish(int kind, IProgressMonitor monitor) {
	    try {
    	    IProject project = OpenShiftServerUtils.getDeployProject(getServer());
    	    if (project.hasNature(IMavenConstants.NATURE_ID)) {
    	        clearJSTcache(project);
    	    }
	    } catch (Exception e) {
    	    OpenShiftCoreActivator.logError("JST cache wasn't cleared properly", e);
    	}
	    return super.publish(kind, monitor);
	}
	
	/*
	 * This is a workadround of the bug in org.eclipse.jst.j2ee.
	 * Cache is not cleaned properly and we get an incorrect name of the module for deployment
	 * https://issues.jboss.org/browse/JBIDE-22138#comment-13617731
	 */
	private void clearJSTcache(IProject project) throws Exception {
        invokeClearCache(project);
        invokeCleanAllDelegates();
	}

	@SuppressWarnings("restriction")
	private void invokeClearCache(IProject project)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method clearCacheMethod = J2EEDeployableFactory.class.getDeclaredMethod("clearCache", IProject.class);
        clearCacheMethod.setAccessible(true);
        clearCacheMethod.invoke(JEEDeployableFactory.jeeInstance(), project);
	}

	@SuppressWarnings("restriction")
	private void invokeCleanAllDelegates()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method cleanAllDelegatesMethod = J2EEDeployableFactory.class.getDeclaredMethod("cleanAllDelegates");
        cleanAllDelegatesMethod.setAccessible(true);
        cleanAllDelegatesMethod.invoke(JEEDeployableFactory.jeeInstance());
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
			if (modules != null) {
				moduleStateJob = new UpdateModuleStateJob(modules, s, true, 10000);
				moduleStateJob.schedule();
			}
		} catch (CoreException ce) {
			OpenShiftCoreActivator.pluginLog().logError(ce);
		}
	}
}
