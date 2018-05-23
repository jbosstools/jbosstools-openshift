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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;

import com.openshift.restclient.model.IResource;

public class OpenShiftSpringBootPublishUtils {

	public static final String BASE_PATH = "BOOT-INF";
	private static final String CLASSES_FOLDER = "classes";
	private static final String LIB_FOLDER = "lib";

	private OpenShiftSpringBootPublishUtils() {
	}

	public static IPath getRootModuleDeployPath() {
		return getProjectFolder();
	}

	public static IPath getChildModuleDeployPath() {
		return getProjectFolder();
	}

	private static IPath getProjectFolder() {
		return new Path(BASE_PATH)
				.append(CLASSES_FOLDER);
	}
	
	private static IPath getDependenciesFolder() {
		return new Path(BASE_PATH)
				.append(LIB_FOLDER);
	}

	public static RSync createRSync(IServer server, IProgressMonitor monitor) throws CoreException {
		final IResource resource = OpenShiftServerUtils.getResourceChecked(server, monitor);
		String podDeployDir = OpenShiftServerUtils.getOrLoadPodPath(server, resource, monitor);
		
		return OpenShiftServerUtils.createRSync(resource, new Path(podDeployDir).append(BASE_PATH).toString(), server);
	}
}
