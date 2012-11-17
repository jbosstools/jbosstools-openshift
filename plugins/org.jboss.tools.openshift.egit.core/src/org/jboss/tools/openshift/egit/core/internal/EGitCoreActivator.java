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
package org.jboss.tools.openshift.egit.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.GitProjectData;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class EGitCoreActivator implements BundleActivator {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.egit.core";
	
	private static BundleContext context;
	
	private IResourceChangeListener egitChangeListener;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		EGitCoreActivator.context = bundleContext;
		if (egitChangeListener == null) {
			egitChangeListener = new IResourceChangeListener() {
				
				@Override
				public void resourceChanged(IResourceChangeEvent event) {
					IResource project = event.getResource();
					if (project != null && project instanceof IProject) {
						GitProjectData.reconfigureWindowCache();
					}
				}
			};
			ResourcesPlugin.getWorkspace().addResourceChangeListener(egitChangeListener, IResourceChangeEvent.PRE_DELETE);
		}
		//DelegatingJBoss7ServerBehavior.addDelegateMapping(EgitBehaviourDelegate.ID, EgitBehaviourDelegate.class);
		// THIS is not relevant at the moment. Egit support will need to be revisited
	}


	public void stop(BundleContext bundleContext) throws Exception {
		EGitCoreActivator.context = null;
		if (egitChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(egitChangeListener);
			egitChangeListener = null;
		}
	}
	
	public static IStatus createErrorStatus(String message) {
		return createStatus(IStatus.ERROR, message, null);
	}

	public static IStatus createErrorStatus(String message, Throwable throwable) {
		return createStatus(IStatus.ERROR, message, throwable);
	}
	
	public static IStatus createStatus(int severity, String message, Throwable throwable) {
		if (throwable == null) {
			return new Status(severity, PLUGIN_ID, message);
		} else {
			return new Status(severity, PLUGIN_ID, message, throwable);
		}
	}
}
