/*******************************************************************************
 * Copyright (c) 2016-2019 Red Hat Inc..
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
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.wtp.core.console.ServerConsoleModel;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IPublishController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.as.core.server.controllable.subsystems.StandardFileSystemPublishController;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.OutputNamesCacheFactory;
import org.jboss.tools.openshift.core.server.OutputNamesCacheFactory.OutputNamesCache;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.core.server.behavior.eap.OpenshiftEapProfileDetector;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftPublishController extends StandardFileSystemPublishController implements IPublishController {

	@Override
	public void publishStart(final IProgressMonitor monitor) throws CoreException {
		IServer server = getServer();

		final File localDirectory = getDeploymentsRootFolder();
		final IProject deployProject = OpenShiftServerUtils.checkedGetDeployProject(server);
		// If the magic project is *also* a module on the server, do nothing
		if (!modulesIncludesMagicProject(server, deployProject)) {
			publishRootModule(monitor, deployProject, localDirectory);
		}

		syncPodsToDirectory(monitor);
		deleteOldDeployments(getServer(), monitor);
	}

	private void publishRootModule(final IProgressMonitor monitor, final IProject deployProject, final File localDirectory)
			throws CoreException {
		// The project not also a module, so let's see if there exists a module at all
		IModule projectModule = OpenShiftServerUtils.findProjectModule(deployProject);
		if (projectModule == null) {
			// This project is not a module, so we'll do a simple copy
			publishMagicProjectSimpleCopy(getServer(), localDirectory);
		} else {
			// This is a project-module which must be assembled and published (ie dynamic
			// web, ear project, etc)
			publishModule(IServer.PUBLISH_FULL, ServerBehaviourDelegate.ADDED, new IModule[] { projectModule },
					monitor);
		}
	}

	private void publishMagicProjectSimpleCopy(IServer server, File localDeploymentDirectory) throws CoreException {
		// TODO this is the dumb logic. If the magic project is in fact a
		// dynamic web project, we need to package it, not simply copy.
		String sourcePath = OpenShiftServerUtils.getSourcePath(server);
		if (StringUtils.isEmpty(sourcePath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("Server {0} could not determine the source to publish.", server.getName())));
		}
		File source = new File(sourcePath);
		FileUtils.copyDir(source, localDeploymentDirectory, true, true, true, file -> {
				String filename = file.getName();
				return !filename.endsWith(".git") 
						&& !filename.endsWith(".gitignore") 
						&& !filename.endsWith(".svn")
						&& !filename.endsWith(".settings") 
						&& !filename.endsWith(".project")
						&& !filename.endsWith(".classpath");
			}
		);
	}

	private boolean modulesIncludesMagicProject(IServer server, IProject deployProject) {
		// If we have no modules, OR the module list doesn't include magic project,
		// we need to initiate a publish for the magic project
		IModule[] all = server.getModules();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (all[i].getProject().equals(deployProject))
					return true;
			}
		}
		return false;
	}

	@Override
	public void publishFinish(IProgressMonitor monitor) throws CoreException {
		super.publishFinish(monitor);

		syncDirectoryToPods(monitor);

		final IResource resource = OpenShiftServerUtils.getResource(getServer(), monitor);
		loadPodPathIfEmpty(resource, monitor);
	}

	protected void syncDirectoryToPods(IProgressMonitor monitor) throws CoreException {
		syncDirectoryToPods(getDeploymentsRootFolder(), monitor);
	}

	protected void syncDirectoryToPods(final File localFolder, IProgressMonitor monitor) throws CoreException {
		RSync rsync = createRsync(getServer(), monitor);
		MultiStatus status = rsync.syncDirectoryToPods(localFolder, ServerConsoleModel.getDefault().getConsoleWriter());
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

	protected void syncPodsToDirectory(IProgressMonitor monitor) throws CoreException {
		syncPodsToDirectory(getDeploymentsRootFolder(), monitor);
	}

	protected void syncPodsToDirectory(final File localFolder, IProgressMonitor monitor) throws CoreException {
		RSync rsync = createRsync(getServer(), monitor);
		MultiStatus status = rsync.syncPodsToDirectory(localFolder, ServerConsoleModel.getDefault().getConsoleWriter());
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

	protected void loadPodPathIfEmpty(final IResource resource, IProgressMonitor monitor) throws CoreException {
		// If the pod path is not set on the project yet, we can do that now
		// to make future fetches faster
		String podPath = OpenShiftServerUtils.getPodPath(getServer());
		if (StringUtils.isEmpty(podPath)) {
			// Pod path is empty
			podPath = OpenShiftServerUtils.loadPodPath(resource, getServer(), monitor);
			if (!StringUtils.isEmpty(podPath)) {
				fireUpdatePodPath(getServer(), podPath);
			}
		}
	}

	private void fireUpdatePodPath(final IServer server, final String podPath) {
		new Job("Updating Pod Path") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// Only set the pod path on the server object
				IServerWorkingCopy wc = server.createWorkingCopy();
				wc.setAttribute(OpenShiftServerUtils.ATTR_POD_PATH, podPath);
				try {
					wc.save(true, new NullProgressMonitor());
				} catch (CoreException ce) {
					return ce.getStatus();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@Override
	protected void launchUpdateModuleStateJob() throws CoreException {
		// No-op for now, until other problems are fixed
	}

	protected File getDeploymentsRootFolder() throws CoreException {
		return new File(getDeploymentOptions().getDeploymentsRootFolder(true));
	}

	protected RSync createRsync(final IServer server, final IProgressMonitor monitor) throws CoreException {
		return OpenShiftServerUtils.createRSync(server, monitor);
	}

	protected boolean isSyncDownFailureCritical() {
		return !isEapProfile();
	}

	@Override
	protected boolean supportsJBoss7Markers() {
		return isEapProfile();
	}

	protected boolean isEapProfile() {
		// quick and dirty
		String profile = ServerProfileModel.getProfile(getServer());
		return OpenshiftEapProfileDetector.PROFILE.equals(profile);
	}

	protected void deleteOldDeployments(final IServer server, IProgressMonitor monitor) throws CoreException {
		File deploymentsRootFolder = getDeploymentsRootFolder();
		MultiStatus multiStatus = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, "Could not delete old deployment(s)", null);
		Arrays.stream(server.getModules()).forEach(module -> {
			try {
				deleteOldDeployment(module, deploymentsRootFolder, server, monitor);
			} catch (CoreException e) {
				multiStatus.add(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
						NLS.bind("Could not delete old deployment for module {0}", module.getName())));
			}
		});
		if (!multiStatus.isOK()) {
			throw new CoreException(multiStatus);
		}
	}

	private void deleteOldDeployment(final IModule module, final File deploymentsRootFolder, IServer server,
			IProgressMonitor monitor) throws CoreException {
		OutputNamesCache outputNamesCache = OutputNamesCacheFactory.INSTANCE.get(server);
		if (outputNamesCache.isModified(module)) {
			File moduleOutputFile = getModuleOutputFile(module, outputNamesCache, deploymentsRootFolder);
			deleteDeploymentFile(moduleOutputFile, monitor);
			outputNamesCache.reset(module);
		}
	}

	protected File deleteDeploymentFile(File deployment, IProgressMonitor monitor) throws CoreException {
		if (deployment == null 
				|| !deployment.exists()) {
			return null;
		}
		FileUtils.remove(deployment);
		return deployment;
	}

	private File getModuleOutputFile(final IModule module, final OutputNamesCache moduleOutputNames,
			File deploymentsRootFolder) {
		String outputName = moduleOutputNames.getOldOutputName(module);
		if (outputName == null) {
			return null;
		}
		return new File(deploymentsRootFolder, outputName);
	}
	
}
