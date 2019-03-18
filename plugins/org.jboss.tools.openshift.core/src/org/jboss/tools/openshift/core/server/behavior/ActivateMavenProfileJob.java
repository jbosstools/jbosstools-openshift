/*******************************************************************************
 * Copyright (c) 2016-2019 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.util.MavenProfile;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Andre Dietisheim
 */
class ActivateMavenProfileJob extends Job {

	public enum Action {
		ACTIVATE,
		DEACTIVATE
	}

	private final IProject project;
	private Action action;

	public ActivateMavenProfileJob(Action action, IProject project) {
		super(NLS.bind("{0} maven profile \"openshift\" for project {1}", 
				action == Action.ACTIVATE? "Activating" : "Deactivating", project.getName()));
		this.action = action;
		this.project = project;
		// need to lock the whole workspace to prevent dead locks with other, concurrent
		// jobs that escalate to the workspace lock.
		setRule(ResourcesPlugin.getWorkspace().getRoot());
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			MavenProfile profile = new MavenProfile(MavenProfile.OPENSHIFT_MAVEN_PROFILE, project);

			switch(action) {
			case ACTIVATE:
				profile.activate(monitor);
				break;
			case DEACTIVATE:
				profile.deactivate(monitor);
				break;
			default:
				break;
			}
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Could not {0} maven profile {1}", 
							this.action.toString().toLowerCase(), MavenProfile.OPENSHIFT_MAVEN_PROFILE));
        }
	}
}
