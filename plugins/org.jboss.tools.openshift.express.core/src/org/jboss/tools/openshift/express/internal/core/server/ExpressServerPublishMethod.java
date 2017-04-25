/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.core.modules.ResourceModuleResourceUtil;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilter;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilterProvider;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.util.PublishControllerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.publish.LocalZippedModulePublishRunner;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;

/**
 * @author Rob Stryker
 */
public class ExpressServerPublishMethod  {

	public void publishStart(final IServer server, final IProgressMonitor monitor) throws CoreException {
		String destProjName = ExpressServerUtils.getDeployProjectName(server);
		IProject magicProject = destProjName == null ? 
				null : ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if (magicProject == null 
				|| !magicProject.isAccessible()) {
			throw new CoreException(new Status(IStatus.ERROR,
					ExpressCoreActivator.PLUGIN_ID,
					NLS.bind(ExpressServerMessages.publishFailMissingProject, server.getName(), destProjName)));
		}
	}

	public int publishFinish(IServer server, IProgressMonitor monitor) throws CoreException {
		IProject project = ExpressServerUtils.getDeployProject(server);
		boolean allSubModulesPublished = areAllModulesPublished(server);

		if (ProjectUtils.exists(project)) {
			IContainer deployFolder = ServerUtils.getContainer(ExpressServerUtils.getDeployFolder(server), project);
			if (allSubModulesPublished
					|| (deployFolder != null && deployFolder.isAccessible())) {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				publish(project, server, monitor);
			} // else ignore. (one or more modules not published AND magic
				// folder doesn't exist
				// The previous exception will be propagated.
		}
		
		return allSubModulesPublished ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;
	}

	private boolean areAllModulesPublished(IServer server) {
		IModule[] modules = server.getModules();
		boolean allpublished = true;
		for (int i = 0; i < modules.length; i++) {
			if (server.getModulePublishState(new IModule[] { modules[i] }) != IServer.PUBLISH_STATE_NONE)
				allpublished = false;
		}
		return allpublished;
	}

	public int publishModule(IServer server, int kind, int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {

		if (module.length > 1)
			return IServer.PUBLISH_STATE_UNKNOWN;

		// Magic Project
		String destProjName = ExpressServerUtils.getDeployProjectName(server);
		if (isInDestProjectTree(destProjName, module))
			return IServer.PUBLISH_STATE_NONE;

		// Cannot be null, checked for in publishStart
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if (destProj.equals(module[module.length - 1].getProject()))
			return IServer.PUBLISH_STATE_NONE;

		IContainer destFolder = getDestination(server, destProj);
		IPath destPath = destFolder.getLocation();

		if (module.length == 0)
			return IServer.PUBLISH_STATE_NONE;
		int modulePublishState = server.getModulePublishState(module);
		int publishType = getPublishType(kind, deltaKind, modulePublishState);

		IModuleResourceDelta[] delta = new IModuleResourceDelta[] {};
		if (deltaKind != ServerBehaviourDelegate.REMOVED)
			delta = ((Server)server).getPublishedResourceDelta(module);

		try {
			IPath outputFileFullPath = getModuleNestedDeployPath(module, destPath.toOSString(), server);
			String outputFileName = outputFileFullPath.lastSegment();
			IResource changedResource = destFolder.getFile(new Path(outputFileName));
			IResource[] resource = new IResource[] { changedResource };
			
			if (deltaKind == ServerBehaviourDelegate.REMOVED) {
				changedResource.delete(false, monitor); // uses resource api
			} else {
				LocalZippedModulePublishRunner runner = createZippedRunner(server, module[0], outputFileFullPath);
				monitor.beginTask("Moving module to " + destFolder.getName(), 100);
				runner.fullPublishModule(new SubProgressMonitor(monitor, 20));
				// util used file api, so a folder refresh is required
				destFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 20));
				final AddToIndexOperation operation = new AddToIndexOperation(resource);
				try {
					operation.execute(new SubProgressMonitor(monitor, 60));
				} catch (CoreException e) {
					ExpressCoreActivator.pluginLog().logStatus((e.getStatus()));
				}
			}
		} catch (Exception e) {
			ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
		}
		return IServer.PUBLISH_STATE_NONE;
	}

	
	public static IPath getModuleNestedDeployPath(IModule[] moduleTree, String rootFolder, IServer server) {
		return new ModuleDeploymentPrefsUtil().getModuleNestedDeployPath(moduleTree, rootFolder, server);
	}
	
	private LocalZippedModulePublishRunner createZippedRunner(IServer server, IModule m, IPath p) {
		return new LocalZippedModulePublishRunner(server, m,p, getModulePathFilterProvider());
	}

	private IModulePathFilterProvider getModulePathFilterProvider() {
		return new IModulePathFilterProvider() {
			@Override
			public IModulePathFilter getFilter(IServer server, IModule[] module) {
				return ResourceModuleResourceUtil.findDefaultModuleFilter(module[module.length-1]);
			}
		};
	}

	private IContainer getDestination(IServer server, IProject destProj) throws CoreException {
		String destinationFolder = ExpressServerUtils.getDeployFolder(server);
		IContainer destFolder = ServerUtils.getContainer(destinationFolder, destProj);
		if (destFolder == null 
				|| !destFolder.isAccessible()) {
			throw new CoreException(ExpressCoreActivator.statusFactory().errorStatus(NLS.bind(
					ExpressServerMessages.publishFailMissingFolder,
					server.getName(),
					createMissingPath(destProj, destinationFolder, destFolder))));
		}
		return destFolder;
	}

	private StringBuilder createMissingPath(IProject destProj, String destinationFolder, IContainer destFolder) {
		StringBuilder missingPath = new StringBuilder();
		if (destFolder != null) {
			missingPath.append(destFolder.getName());
		} else {
			missingPath.append(destProj.getName());
			missingPath.append(File.separatorChar);
			missingPath.append(destinationFolder);
		}
		return missingPath;
	}

	private boolean isInDestProjectTree(String magicProject, IModule[] modules) {
		IProject magic = magicProject == null ? null :
				ResourcesPlugin.getWorkspace().getRoot().getProject(magicProject);
		IProject moduleProject = getModuleProject(modules);
		if (magic == null 
				|| moduleProject == null) {
			return false;
		}

		IPath moduleProjectRoot = moduleProject.getLocation();
		IPath magicProjectRoot = magic.getLocation();
		return magicProjectRoot.isPrefixOf(moduleProjectRoot);
	}
	
	private IProject getModuleProject(IModule[] modules) {
		if (modules == null) {
			return null;
		} else if (modules.length == 0) {
			return null;
		} else {
			return modules[modules.length - 1].getProject();
		}
	}
	
	protected PushOperationResult publish(final IProject project, final IServer server, final IProgressMonitor monitor) 
			throws CoreException {
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 200);
		try (Repository repo = EGitUtils.getRepository(project)) {
			boolean uncommittedChanges = EGitUtils.countChanges(
					repo, true, new NullProgressMonitor()) > 0;
			if (uncommittedChanges) {
				String remote = ExpressServerUtils.getRemoteName(server);
				String applicationName = ExpressServerUtils.getApplicationName(server);
				ExpressCoreUIIntegration.openCommitDialog(project, remote, applicationName, 
						new PublishJob(applicationName, project, server));
			} else {
				if (ExpressCoreUIIntegration.requestApproval(
						getPushQuestion(project, server, subMonitor),
						NLS.bind(ExpressServerMessages.publishTitle, project.getName()))) {
					return push(project, server, subMonitor);
				}
			}
		} catch (Exception e) {
			IStatus status = ExpressCoreActivator.statusFactory().errorStatus(e);
			ExpressCoreActivator.pluginLog().logStatus(status);
			throw new CoreException(status);
		} finally {
			subMonitor.done();
		}
		return null;
	}
		
	private String getPushQuestion(IProject project, IServer server, IProgressMonitor monitor)
			throws IOException, InvocationTargetException, URISyntaxException {
		String openShiftRemoteName = ExpressServerUtils.getRemoteName(server);
		if (!EGitUtils.isAhead(project, openShiftRemoteName, monitor)) {
			return NLS.bind(ExpressServerMessages.noChangesPushAnywayMsg, project.getName());
		} else {
			return NLS.bind(ExpressServerMessages.committedChangesNotPushedYet, project.getName());
		}
	}
	
	private PushOperationResult push(IProject project, IServer server, IProgressMonitor monitor) throws CoreException {
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
		Repository repository = EGitUtils.getRepository(project);
		ExpressCoreUIIntegration.displayConsoleView(server);
		String remoteName = ExpressServerUtils.getRemoteName(server.createWorkingCopy());
		try {
			return EGitUtils.push(
					remoteName, repository, subMonitor,
					ExpressCoreUIIntegration.getConsoleOutputStream(server));
		} catch (CoreException ce) {
			// Comes if push has failed
			subMonitor.worked(100);
			if (isUpToDateError(ce)) {
				ExpressCoreUIIntegration.appendToConsole(server, "\n\nRepository already uptodate.");
				return null;
			}

			try {
				if (ExpressCoreUIIntegration.requestApproval(
						"Error: '"
								+ ce.getMessage()
								+ "' occurred while pushing.\n\nIf the commit history is not correct on the remote repository, "
								+ "a forced push (git push -f) might be the right thing to do. This will though overwrite the remote repository!"
								+ "\n\n Do you want to do a forced push and overwrite any remote changes ? ",
						"Attempt push force ?", false)) {
					return EGitUtils.pushForce(
							remoteName, repository, subMonitor,
							ExpressCoreUIIntegration.getConsoleOutputStream(server));
				} else {
					// printing out variation of the standard git output
					// meesage.
					ExpressCoreUIIntegration.appendToConsole(
									server,
									"\n\nERROR: "
											+ ce.getLocalizedMessage()
											+ "\n\n"
											+ "To prevent you from losing history, non-fast-forward updates were rejected"
											+ "\nMerge the remote changes (e.g. 'Team > Fetch from Upstream' in Eclipse or 'git pull' on command line ) before pushing again. "
											+ "\nSee the 'Note about fast-forwards' section of 'git push --help' for details.");
				}
				return null;
			} catch (CoreException ce2) {
				if (isUpToDateError(ce)) {
					ExpressCoreUIIntegration.appendToConsole(server, "\n(Forced push) Repository already uptodate.");
					return null;
				} else {
					// even the push force failed, and we don't have a valid
					// result to check :(
					throw ce2;
				}
			}
		} finally {
			subMonitor.done();
		}
	}

	private boolean isUpToDateError(CoreException ce) {
		return ce.getMessage() != null
				&& ce.getMessage().contains("UP_TO_DATE");
	}

	protected String getModuleProjectName(IModule[] module) {
		return module[module.length - 1].getProject().getName();
	}


	public String getPublishDefaultRootFolder(IServer server) {
		IDeployableServer s = ServerConverter.getDeployableServer(server);
		return s.getDeployFolder();
	}

	public String getPublishDefaultRootTempFolder(IServer server) {
		IDeployableServer s = ServerConverter.getDeployableServer(server);
		return s.getTempDeployFolder();
	}
	
	private int getPublishType(int kind, int deltaKind, int modulePublishState) {
		if( deltaKind == ServerBehaviourDelegate.ADDED ) 
			return PublishControllerUtil.FULL_PUBLISH;
		else if (deltaKind == ServerBehaviourDelegate.REMOVED) {
			return PublishControllerUtil.REMOVE_PUBLISH;
		} else if (kind == IServer.PUBLISH_FULL 
				|| modulePublishState == IServer.PUBLISH_STATE_FULL 
				|| kind == IServer.PUBLISH_CLEAN ) {
			return PublishControllerUtil.FULL_PUBLISH;
		} else if (kind == IServer.PUBLISH_INCREMENTAL 
				|| modulePublishState == IServer.PUBLISH_STATE_INCREMENTAL 
				|| kind == IServer.PUBLISH_AUTO) {
			if( ServerBehaviourDelegate.CHANGED == deltaKind ) 
				return PublishControllerUtil.INCREMENTAL_PUBLISH;
		} 
		return PublishControllerUtil.NO_PUBLISH;
	}

	private class PublishJob extends Job {

		private IProject project;
		private IServer server;

		PublishJob(String applicationName, IProject project, IServer server) {
			super(NLS.bind("Publishing to {0} on OpenShift...", applicationName));
			this.project = project;
			this.server = server;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				push(project, server, monitor);
				return Status.OK_STATUS;
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
	}
}
