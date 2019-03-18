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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IServerShutdownController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.behavior.ActivateMavenProfileJob.Action;
import org.jboss.tools.openshift.core.util.MavenCharacter;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.server.debug.DebugLaunchConfigs;

public class OpenShiftShutdownController extends AbstractSubsystemController
		implements ISubsystemController, IServerShutdownController {

	@Override
	public IStatus canStop() {
		return Status.OK_STATUS;
	}

	protected OpenShiftServerBehaviour getBehavior() {
		return (OpenShiftServerBehaviour) getServer().loadAdapter(OpenShiftServerBehaviour.class,
				new NullProgressMonitor());
	}

	@Override
	public void stop(boolean force) {
		OpenShiftServerBehaviour behavior = getBehavior();
		behavior.setServerStopping();

		terminateRemoteDebugger(behavior);
		updateProject(behavior, getServerOrWC());

		behavior.setServerStopped();
	}

	private void updateProject(OpenShiftServerBehaviour behavior, IServerAttributes server) {
		IProject project = OpenShiftServerUtils.getDeployProject(getServerOrWC());
	    try {
		    if (!new MavenCharacter(project).hasNature()) {
				behavior.setServerStopped();
			} else {
				new ActivateMavenProfileJob(Action.DEACTIVATE, project).schedule();
			}
		} catch (CoreException ce) {
			log(IStatus.ERROR, "Could determine maven nature of project {0} ", ce);
		}
	}

	private void terminateRemoteDebugger(OpenShiftServerBehaviour behavior) {
		try {
			DebugLaunchConfigs configs = DebugLaunchConfigs.get();			
			if (configs != null) {
				configs.terminateRemoteDebugger(behavior.getServer());
			}
		} catch (CoreException ce) {
			log(IStatus.ERROR, "Could not terminate remote debugger ", ce);
		}
	}

	protected void log(int status, String message, Exception e) {
		OpenShiftCoreActivator.getDefault().getLog()
				.log(new Status(status, OpenShiftCoreActivator.PLUGIN_ID, message, e));
	}

}
