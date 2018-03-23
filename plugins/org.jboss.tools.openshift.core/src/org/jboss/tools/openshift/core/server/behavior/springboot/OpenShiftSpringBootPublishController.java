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

import java.io.File;

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
import org.jboss.ide.eclipse.as.core.util.RemotePath;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftSpringBootPublishController extends OpenShiftPublishController {

	private static final String DEPLOY_LIBS_FOLDER = "libs";
	private static final String POD_BASE_PATH = "/BOOT-INF/classes/";

	public OpenShiftSpringBootPublishController() {
		// keep for reflection instantiation
	}

	@Override
	protected RSync createRsync(IServer server, final IProgressMonitor monitor) throws CoreException {
		final IResource resource = OpenShiftServerUtils.getResourceChecked(server, monitor);
		IPath podPath = new Path(OpenShiftServerUtils.getOrLoadPodPath(server, resource)).append(POD_BASE_PATH);
		return OpenShiftServerUtils.createRSync(resource, podPath.toString(), server);
	}

	@Override
	protected boolean treatAsBinaryModule(IModule[] module) {
		return module.length == 1;
	}

	@Override
	protected boolean forceZipModule(IModule[] moduleTree) {
		return false;
	}

	@Override
	protected IPath getModuleDeployRoot(IModule[] module, boolean isBinaryObject) throws CoreException {
		if (module == null) {
			return null;
		}

		if (isRootModule(module)) {
			return super.getModuleDeployRoot(module, isBinaryObject);
		} else {
			IModule childModule = module[module.length - 1];	
			return getChildModuleDeployPath(childModule);
		}
	}

	private IPath getChildModuleDeployPath(IModule childModule) throws CoreException {
		IPath finalName = getMavenFinalName(childModule);
		if (finalName == null) {
			IStatus status = StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
					NLS.bind("Could not determine maven artifact final name for module {0}", childModule.getName()));
			throw new CoreException(status);
		}

		File destination = ServerUtil.getServerStateLocation(getServer())
				.append(DEPLOY_LIBS_FOLDER)
				.append(finalName)
				.toFile();
		destination.mkdirs();
		return new RemotePath(
				destination.getAbsolutePath(), 
				getDeploymentOptions().getPathSeparatorCharacter());
	}

	private boolean isRootModule(IModule[] module) {
		return module != null
				&& module.length == 1;
	}
	
	private IPath getMavenFinalName(IModule module) throws CoreException {
		if (module == null) {
			return null;
		}

		MavenProject mavenProject = getMavenProject(module.getProject());
		if (mavenProject != null) {
			String finalName = mavenProject.getBuild().getFinalName();
			String packaging = mavenProject.getPackaging();
			if (StringUtils.isEmpty(finalName)
					|| StringUtils.isEmpty(packaging)) {
				return null;
			}

			return new Path(finalName + "." + packaging);
		}
		return null;
	}

	private MavenProject getMavenProject(IProject project) throws CoreException {
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
		if (facade == null) {
			return null;
		}
		return facade.getMavenProject(new NullProgressMonitor());
	}

}
