/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftSpringBootLaunchController extends OpenShiftLaunchController {

	@Override
	protected void setMode(String mode, DebugContext context, OpenShiftServerBehaviour beh, IProgressMonitor monitor) throws CoreException {
		super.setMode(mode, context, beh, monitor);

		IResource resource = OpenShiftServerUtils.getResource(getServer(), monitor);
		String podPath = OpenShiftServerUtils.getOrLoadPodPath(getServer(), resource, monitor);
//		Arrays.stream(getServer().getModules())
//			.flatMap(module -> Arrays.stream(getServer().getChildModules(new IModule[] {module}, monitor)))
//			.forEach(module -> {
//				try {
//						IPath moduleDestination = OpenShiftSpringBootPublishUtils.getChildModuleDeployPath(module);
//						IPath podDestination = new Path(podPath).append(moduleDestination);
//						new OpenShiftDebugMode(context).putEnvVar("JAVA_CLASSPATH", podDestination.toString() + "/");
//					} catch (CoreException e) {
//						OpenShiftCoreActivator.logError(
//							NLS.bind("Could not get deployment destination for module {0}", module.getName()), e);
//					}
//			});
		new OpenShiftDebugMode(context).putEnvVar("JAVA_CLASSPATH", "/deployments/BOOT-INF/lib/");
	}

}
