/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.js.server.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftShutdownController;
import org.jboss.tools.openshift.js.launcher.NodeDebugLauncher;

public class OpenShiftNodejsShutdownController extends OpenShiftShutdownController implements ISubsystemController {

	public OpenShiftNodejsShutdownController() {
		super();
	}

	@Override
	public void stop(boolean force) {
		OpenShiftServerBehaviour behavior = getBehavior();
		behavior.setServerStopping();
		try {
			NodeDebugLauncher.terminate(behavior.getServer());
			behavior.setServerStopped();
		} catch (CoreException ce) {
			log(IStatus.ERROR, "Error shutting down server", ce);
			getBehavior().setServerStarted();
		}
	}

}
