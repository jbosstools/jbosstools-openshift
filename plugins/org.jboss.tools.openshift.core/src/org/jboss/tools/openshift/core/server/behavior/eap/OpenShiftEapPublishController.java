/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.eap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

public class OpenShiftEapPublishController extends OpenShiftPublishController implements ISubsystemController {

	public OpenShiftEapPublishController() {
		super();
	}

	@Override
	public int publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor) throws CoreException {
		return super.publishModule(IServer.PUBLISH_CLEAN, deltaKind, module, monitor);
	}

}
