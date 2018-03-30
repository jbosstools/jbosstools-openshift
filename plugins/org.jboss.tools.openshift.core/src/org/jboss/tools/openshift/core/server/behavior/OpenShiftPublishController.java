/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc..
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.core.server.internal.v7.DeploymentMarkerUtils;
import org.jboss.ide.eclipse.as.wtp.core.console.ServerConsoleModel;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IPublishController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.as.core.server.controllable.subsystems.StandardFileSystemPublishController;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.core.server.behavior.eap.OpenshiftEapProfileDetector;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.model.IResource;

public class OpenShiftPublishController extends StandardFileSystemPublishController implements IPublishController {

	private RSync rsync = null;
	protected boolean syncDownFailed = false;

	@Override
	public void publishStart(final IProgressMonitor monitor) throws CoreException {
		this.syncDownFailed = false;
		IServer server = getServer();
		this.rsync = createRsync(server, monitor);
		final File localDirectory = getLocalFolder();
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0,
				NLS.bind("Error while publishing server {0}.  Could not sync all pods to folder {1}", server.getName(),
						localDirectory.getAbsolutePath()),
				null);
		rsync.syncPodsToDirectory(localDirectory, status, ServerConsoleModel.getDefault().getConsoleWriter());
		if (!status.isOK()) {
			handleSyncDownFailure(status);
		}

		final IProject deployProject = OpenShiftServerUtils.checkedGetDeployProject(server);
		// If the magic project is *also* a module on the server, do nothing
		if (!modulesIncludesMagicProject(getServer(), deployProject)) {
			publishModule(monitor, deployProject, localDirectory);
		}
	}

	private void publishModule(final IProgressMonitor monitor, final IProject deployProject, final File localDirectory)
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

	protected void handleSyncDownFailure(final MultiStatus status) throws CoreException {
		this.syncDownFailed = true;
		if (isSyncDownFailureCritical()) {
			clearRsync();
			throw new CoreException(status);
		}
		OpenShiftCoreActivator.pluginLog().logWarning("Ignoring initial sync down error.", new CoreException(status));
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

	private void publishMagicProjectSimpleCopy(IServer server, File localDeploymentDirectory) throws CoreException {
		// TODO this is the dumb logic. If the magic project is in fact a
		// dynamic web project, we need to package it, not simply copy.
		String sourcePath = OpenShiftServerUtils.getSourcePath(server);
		if (StringUtils.isEmpty(sourcePath)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("Server {0} could not determine the source to publish.", server.getName())));
		}
		File source = new File(sourcePath);
		FileUtils.copyDir(source, localDeploymentDirectory, true, true, true, new FileFilter() {

			@Override
			public boolean accept(File file) {
				String filename = file.getName();
				return !filename.endsWith(".git") && !filename.endsWith(".gitignore") && !filename.endsWith(".svn")
						&& !filename.endsWith(".settings") && !filename.endsWith(".project")
						&& !filename.endsWith(".classpath");
			}
		});
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
		if (!hasRsync()) {
			return;
		}

		super.publishFinish(monitor);

		final File localFolder = getLocalFolder();
		final IResource resource = OpenShiftServerUtils.getResource(getServer(), monitor);
		syncUp(localFolder, resource);

		deleteDoDeployMarkers(localFolder);
		loadPodPathIfEmpty(resource);
	}

	protected void syncUp(final File localFolder, final IResource resource) throws CoreException {
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0,
				NLS.bind("Could not sync {0} to all pods running the service {1}", localFolder, resource.getName()),
				null);
		rsync.syncDirectoryToPods(localFolder, status, ServerConsoleModel.getDefault().getConsoleWriter());
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

	protected void loadPodPathIfEmpty(final IResource resource) throws CoreException {
		// If the pod path is not set on the project yet, we can do that now
		// to make future fetches faster
		String podPath = OpenShiftServerUtils.getPodPath(getServer());
		if (StringUtils.isEmpty(podPath)) {
			// Pod path is empty
			podPath = OpenShiftServerUtils.loadPodPath(resource, getServer());
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

	protected void deleteDoDeployMarkers(final File localFolder) {
		// Remove all *.dodeploy files from this folder.
		Stream.of(localFolder.listFiles()).filter(file -> file.getName().endsWith(DeploymentMarkerUtils.DO_DEPLOY))
				.forEach(File::delete);
	}

	@Override
	public int publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor) throws CoreException {
		if (syncDownFailed) {
			return super.publishModule(IServer.PUBLISH_CLEAN, deltaKind, module, monitor);
		} else {
			return super.publishModule(kind, deltaKind, module, monitor);
		}
	}

	@Override
	protected void launchUpdateModuleStateJob() throws CoreException {
		// No-op for now, until other problems are fixed
	}

	protected File getLocalFolder() throws CoreException {
		return new File(getDeploymentOptions().getDeploymentsRootFolder(true));
	}

	protected RSync createRsync(final IServer server, final IProgressMonitor monitor) throws CoreException {
		return OpenShiftServerUtils.createRSync(server, monitor);
	}

	protected boolean hasRsync() {
		return rsync != null;
	}

	protected void clearRsync() {
		this.rsync = null;
	}
}
