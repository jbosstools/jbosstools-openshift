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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IServerShutdownController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.server.debug.DebugLaunchConfigs;

public class OpenShiftShutdownController extends AbstractSubsystemController
		implements ISubsystemController, IServerShutdownController {

	@Override
	public IStatus canStop() {
		return Status.OK_STATUS;
	}

	protected void log(int status, String message, Exception e) {
		OpenShiftCoreActivator.getDefault().getLog()
				.log(new Status(status, OpenShiftCoreActivator.PLUGIN_ID, message, e));
	}

	public OpenShiftServerBehaviour getBehavior() {
		return (OpenShiftServerBehaviour) getServer().loadAdapter(OpenShiftServerBehaviour.class,
				new NullProgressMonitor());
	}

	@Override
	public void stop(boolean force) {
		OpenShiftServerBehaviour behavior = getBehavior();
		behavior.setServerStopping();
		try {
			DebugLaunchConfigs configs = DebugLaunchConfigs.get();
			if( configs != null ) { 
				configs.terminateRemoteDebugger(behavior.getServer());
			}
			// configs should only be null if workspace is shutting down, so set server to stopped anyway
			behavior.setServerStopped();
		} catch (CoreException ce) {
			log(IStatus.ERROR, "Error shutting down server", ce);
			getBehavior().setServerStarted();
		}
	}

}
