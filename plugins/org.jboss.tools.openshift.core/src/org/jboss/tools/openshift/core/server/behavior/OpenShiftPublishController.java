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

import java.io.File;
import java.io.FileFilter;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.ProjectModule;
import org.jboss.ide.eclipse.as.core.server.internal.v7.DeploymentMarkerUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IPublishController;
import org.jboss.tools.as.core.server.controllable.subsystems.internal.StandardFileSystemPublishController;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import com.openshift.restclient.model.IService;

public class OpenShiftPublishController extends StandardFileSystemPublishController implements IPublishController {

	private RSync rsync = null;
	private RSync createRSync(IServer server, IProgressMonitor monitor) throws CoreException {
		String location = OCBinary.getInstance().getLocation();
		if( location == null ) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Binary for oc-tools could not be found. Please open the OpenShift 3 Preference Page and set the location of the oc binary."));
		}
		
		
		IService service = OpenShiftServerUtils.getService(server);
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the service to publish to.", server.getName())));
		}

		String podPath = OpenShiftServerUtils.getPodPath(server);
		if (StringUtils.isEmpty(podPath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the destination directory to publish to.", server.getName())));
		}
		
		return new RSync(service, podPath, server);
	}

	
	
	public void publishStart(final IProgressMonitor monitor) 
			throws CoreException {
		IProject deployProject = getMagicProject(getServer());
		if (!ProjectUtils.isAccessible(deployProject)) {
			throw new CoreException(new Status(IStatus.ERROR,
					OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Server adapter {0} cannot publish. Required project {1} is missing or inaccessible.", 
							getServer().getName(), deployProject.getName())));
		}
		rsync = createRSync(getServer(), monitor);
		File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
				NLS.bind("Could not sync all pods to folder {0}", localDeploymentDirectory.getAbsolutePath()), null);
		rsync.syncPodsToDirectory(localDeploymentDirectory, status);
		
		// If the magic project is *also* a module on the server, do nothing
		if( !modulesIncludesMagicProject(getServer(), deployProject)) {
			// The project not also a module, so let's see if there exists a module at all
			IModule projectModule = OpenShiftServerUtils.findProjectModule(deployProject);
			if( projectModule == null ) { 
				// This project is not a module, so we'll do a simple copy
				publishMagicProjectSimpleCopy(getServer(), localDeploymentDirectory);
			} else {
				// This is a project-module which must be assembled and published (ie dynamic web, ear project, etc)
				publishModule(IServer.PUBLISH_FULL, ServerBehaviourDelegate.ADDED, 
						new IModule[]{projectModule}, monitor);
			}
		}
	}
	

	private void publishMagicProjectSimpleCopy(IServer server, File localDeploymentDirectory) throws CoreException {
		// TODO this is the dumb logic. If the magic project is in fact a 
		// dynamic web project, we need to package it, not simply copy. 
		String sourcePath = OpenShiftServerUtils.getSourcePath(server);
		if (StringUtils.isEmpty(sourcePath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the source to publish.", server.getName())));
		}
		File source = new File(sourcePath);
		FileUtils.copyDir(source, localDeploymentDirectory, true, true, true, new FileFilter() {

			@Override
			public boolean accept(File file) {
				String filename = file.getName();
				return !filename.endsWith(".git")
						&& !filename.endsWith(".gitignore")
						&& !filename.endsWith(".svn")
						&& !filename.endsWith(".settings")
						&& !filename.endsWith(".project")
						&& !filename.endsWith(".classpath");
			}
		});
	}
	
	private IProject getMagicProject(IServer server) {
		return ProjectUtils.getProject(OpenShiftServerUtils.getDeployProjectName(server));
	}

	private boolean modulesIncludesMagicProject(IServer server, IProject deployProject) {
		// If we have no modules, OR the module list doesn't include magic project, 
		// we need to initiate a publish for the magic project
		IModule[] all = server.getModules();
		if( all != null ) {
			for( int i = 0; i < all.length; i++ ) {
				if( all[i].getProject().equals(deployProject))
					return true;
			}
		}
		return false;
	}

	public void publishFinish(IProgressMonitor monitor) throws CoreException {
		super.publishFinish(monitor);
		try {
			if( rsync != null ) {
				File deployFolder = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
				IService service = OpenShiftServerUtils.getService(getServer());
				MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
						NLS.bind("Could not sync {0} to all pods running the service {1}", deployFolder, service.getName()), null);
				rsync.syncDirectoryToPods(deployFolder, status);
				
				// Remove all *.dodeploy files from this folder. 
				Stream.of(deployFolder.listFiles())
						.filter(p->p.getName().endsWith(".dodeploy"))
						.forEach(p->p.delete());
			}
		} finally {
			rsync = null;
		}
	}
	
	@Override
	protected boolean supportsJBoss7Markers() {
		// TODO this is the same hack as in the wizard, and should be replaced with something better
		IService service = OpenShiftServerUtils.getService(getServer());
		String templateName = service.getLabels().getOrDefault("template", "");
		return templateName.startsWith("eap");
	}
	
	@Override
	protected void launchUpdateModuleStateJob() throws CoreException {
		// No-op for now, until other problems are fixed
	}
}
