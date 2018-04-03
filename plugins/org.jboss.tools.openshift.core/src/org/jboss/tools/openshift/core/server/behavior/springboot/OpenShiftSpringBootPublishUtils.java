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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftSpringBootPublishUtils {

	public static final String BASE_PATH = "BOOT-INF";
	private static final String ROOT_MODULE_FOLDER = "classes";
	private static final String CHILD_MODULE_FOLDER = "lib";

	private static final String EXPLODED_EXTENSION = ".exploded";

	public static IPath getRootModuleDeployPath(IModule module)  throws CoreException {
		return getRootModuleFolder();
	}

	public static IPath getChildModuleDeployPath(IModule module) throws CoreException {
		String finalName = getMavenFinalName(module);
		if (finalName == null) {
			IStatus status = StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
					NLS.bind("Could not determine maven artifact final name for module {0}", module.getName()));
			throw new CoreException(status);
		}
		return getChildModuleDeployPath(finalName);
	}

	private static IPath getChildModuleDeployPath(String name) {
		return getChildModulesFolder()
				.append(name)
;//				.append(EXPLODED_EXTENSION);
	}

	private static IPath getChildModulesFolder() {
		return new Path(BASE_PATH)
				.append(CHILD_MODULE_FOLDER);
	}

	private static IPath getRootModuleFolder() {
		return new Path(BASE_PATH)
				.append(ROOT_MODULE_FOLDER);
	}

	/**
	 * Returns the maven final name of the (workspace-) project for the given
	 * module. Returns {@code null} otherwise-
	 * 
	 * @param module
	 * @return the maven final name for the given module
	 * @throws CoreException
	 */
	private static String getMavenFinalName(IModule module) throws CoreException {
		if (module == null) {
			return null;
		}

		MavenProject mavenProject = getMavenProject(module.getProject());
		if (mavenProject == null) {
			return null;
		}
		String finalName = mavenProject.getBuild().getFinalName();
		String packaging = mavenProject.getPackaging();
		if (StringUtils.isEmpty(finalName)
				|| StringUtils.isEmpty(packaging)) {
			return null;
		}

		return finalName + "." + packaging;
	}

	/**
	 * Returns the maven project for the given project. Returns {@code null}
	 * otherwise.
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	private static MavenProject getMavenProject(IProject project) throws CoreException {
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
		if (facade == null) {
			return null;
		}
		return facade.getMavenProject(new NullProgressMonitor());
	}
	
	public static RSync createRSync(IServer server, IProgressMonitor monitor) throws CoreException {
		final IResource resource = OpenShiftServerUtils.getResourceChecked(server, monitor);
		String podDeployDir = OpenShiftServerUtils.getOrLoadPodPath(server, resource, monitor);
		
		return OpenShiftServerUtils.createRSync(resource, new Path(podDeployDir).append(BASE_PATH).toString(), server);
	}
}
