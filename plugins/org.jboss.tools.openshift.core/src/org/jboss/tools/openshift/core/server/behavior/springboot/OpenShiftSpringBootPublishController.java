/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

import com.openshift.restclient.model.IResource;

public class OpenShiftSpringBootPublishController extends OpenShiftPublishController {

	private static final String POD_BASE_PATH = "/BOOT-INF/classes/";

	public OpenShiftSpringBootPublishController() {
		// keep for reflection instantiation
	}

	@Override
	protected RSync createRsync(IServer server, IProgressMonitor monitor) throws CoreException {
		final IResource resource = OpenShiftServerUtils.checkedGetResource(server, monitor);
		IPath podPath = new Path(OpenShiftServerUtils.getOrLoadPodPath(server, resource)).append(POD_BASE_PATH);

		return OpenShiftServerUtils.createRSync(resource, podPath.toString(), server);
	}

	@Override
	protected boolean treatAsBinaryModule(IModule[] module) {
		return true;
	}
}
