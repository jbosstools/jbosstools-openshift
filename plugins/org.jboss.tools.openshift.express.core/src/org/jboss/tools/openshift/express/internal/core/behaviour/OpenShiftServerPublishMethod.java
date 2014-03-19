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
package org.jboss.tools.openshift.express.internal.core.behaviour;

import java.io.File;

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
import org.jboss.ide.eclipse.as.wtp.core.server.publish.LocalZippedModulePublishRunner;
import org.jboss.tools.as.core.internal.modules.ModuleDeploymentPrefsUtil;
import org.jboss.tools.as.core.server.controllable.util.PublishControllerUtility;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.core.IQuestionHandler;
import org.jboss.tools.openshift.express.core.OpenshiftCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;

/**
 * @author Rob Stryker
 */
public class OpenShiftServerPublishMethod  {

	public void publishStart(final IServer server, final IProgressMonitor monitor) throws CoreException {
		String destProjName = OpenShiftServerUtils.getExpressDeployProject(server);
		IProject magicProject = destProjName == null ? 
				null : ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if (magicProject == null || !magicProject.isAccessible()) {
			throw new CoreException(new Status(IStatus.ERROR,
					OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind(OpenShiftServerMessages.publishFailMissingProject, server.getName(), destProjName)));
		}
	}

	public int publishFinish(IServer server, IProgressMonitor monitor) throws CoreException {

		String destProjName = OpenShiftServerUtils.getExpressDeployProject(server);
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		boolean allSubModulesPublished = areAllPublished(server);

		if (destProj != null 
				&& destProj.exists()) {
		
			String destinationFolder = OpenShiftServerUtils.getExpressDeployFolder(server);
			IContainer destFolder = OpenShiftServerUtils.getDeployFolderResource(destinationFolder, destProj);
			
			if (allSubModulesPublished
					|| (destFolder != null && destFolder.isAccessible())) {
				refreshProject(destProj, submon(monitor, 100));
				commitAndPushProject(destProj, server, submon(monitor, 100));
			} // else ignore. (one or more modules not published AND magic
				// folder doesn't exist
				// The previous exception will be propagated.
		}
		
		return allSubModulesPublished ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;
	}

	protected boolean areAllPublished(IServer server) {
		IModule[] modules = server.getModules();
		boolean allpublished = true;
		for (int i = 0; i < modules.length; i++) {
			if (server.getModulePublishState(new IModule[] { modules[i] }) != IServer.PUBLISH_STATE_NONE)
				allpublished = false;
		}
		return allpublished;
	}

	public int publishModule(IServer server, int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {

		if (module.length > 1)
			return IServer.PUBLISH_STATE_UNKNOWN;

		// Magic Project
		String destProjName = OpenShiftServerUtils.getExpressDeployProject(server);
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
					OpenShiftCoreActivator.pluginLog().logStatus((e.getStatus()));
				}
			}
		} catch (Exception e) {
			OpenShiftCoreActivator.pluginLog().logError(e.getMessage(), e);
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
			public IModulePathFilter getFilter(IServer server, IModule[] module) {
				return ResourceModuleResourceUtil.findDefaultModuleFilter(module[module.length-1]);
			}
		};
	}
	protected IContainer getDestination(IServer server, IProject destProj) throws CoreException {
		String destinationFolder = OpenShiftServerUtils.getExpressDeployFolder(server);
		IContainer destFolder = OpenShiftServerUtils.getDeployFolderResource(destinationFolder, destProj);
		if (destFolder == null 
				|| !destFolder.isAccessible()) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(NLS.bind(
					OpenShiftServerMessages.publishFailMissingFolder,
					server.getName(),
					createMissingPath(destProj, destinationFolder, destFolder))));
		}
		return destFolder;
	}

	protected StringBuilder createMissingPath(IProject destProj, String destinationFolder, IContainer destFolder) {
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

	protected boolean isInDestProjectTree(String magicProject, IModule[] module) {
		IProject magic = magicProject == null ? null :
				ResourcesPlugin.getWorkspace().getRoot().getProject(magicProject);
		IProject moduleProject = module == null ? null : module.length == 0 ? null : module[module.length - 1]
				.getProject();
		if (magic == null || moduleProject == null)
			return false;

		IPath moduleProjectRoot = moduleProject.getLocation();
		IPath magicProjectRoot = magic.getLocation();
		boolean ret = magicProjectRoot.isPrefixOf(moduleProjectRoot);
		return ret;
	}

	protected PushOperationResult commitAndPushProject(IProject p, IServer server,
			IProgressMonitor monitor) throws CoreException {

		PushOperationResult result = null;
		int changes = OpenShiftServerUtils.countCommitableChanges(p, server, monitor);
		if (changes > 0) {
			String[] data = new String[]{
					NLS.bind(OpenShiftServerMessages.publishTitle, p.getName()),
					NLS.bind(OpenShiftServerMessages.commitAndPushMsg, changes, p.getName())
			};
			
			Object[] ret = OpenshiftCoreUIIntegration.openMultiReturnQuestion(IQuestionHandler.COMMIT_AND_PUSH_QUESTION, data);
			if( ret != null && ret.length > 0 && ret[0] != null && ((Boolean)ret[0]).booleanValue()) {
				String msg = (ret.length > 1 && ret[1] != null) ? ((String)ret[1]) : null;
				
				monitor.beginTask("Publishing " + p.getName(), 300);
				if( msg == null )
					EGitUtils.commit(p, new SubProgressMonitor(monitor, 100));
				else
					EGitUtils.commit(p, msg, new SubProgressMonitor(monitor, 100));
					
				OpenshiftCoreUIIntegration.displayConsoleView(server);
				result = push(p, server, monitor);
			}
		} else {
			try {
				String openShiftRemoteName =
						OpenShiftServerUtils.getExpressRemoteName(server);
				if (!EGitUtils.isAhead(p, openShiftRemoteName, monitor)) {
					if (OpenshiftCoreUIIntegration.requestApproval(
							NLS.bind(OpenShiftServerMessages.noChangesPushAnywayMsg, p.getName()),
							NLS.bind(OpenShiftServerMessages.publishTitle, p.getName()))) {
						OpenshiftCoreUIIntegration.displayConsoleView(server);
						result = push(p, server, monitor);
					}
				} else {
					if (OpenshiftCoreUIIntegration.requestApproval(
							NLS.bind(OpenShiftServerMessages.pushCommitsMsg, p.getName()),
							NLS.bind(OpenShiftServerMessages.publishTitle, p.getName()))) {
						OpenshiftCoreUIIntegration.displayConsoleView(server);
						result = push(p, server, monitor);
					}
				}
			} catch (Exception e) {
				OpenShiftCoreActivator.pluginLog().logError(e);
			}
		}

		return result;
	}
	
	protected PushOperationResult push(IProject project, IServer server, IProgressMonitor monitor) throws CoreException {
		String remoteName = OpenShiftServerUtils.getExpressRemoteName(server.createWorkingCopy());
		Repository repository = EGitUtils.getRepository(project);
		try {
			monitor.beginTask("Publishing " + project.getName(), 200);
			PushOperationResult result = EGitUtils.push(
					remoteName, repository, new SubProgressMonitor(monitor, 100),
					OpenshiftCoreUIIntegration.getConsoleOutputStream(server));
			monitor.done();
			return result;
		} catch (CoreException ce) {
			// Comes if push has failed
			if (ce.getMessage() != null && ce.getMessage().contains("UP_TO_DATE")) {
				OpenshiftCoreUIIntegration.appendToConsole(server, "\n\nRepository already uptodate.");
				return null;
			}

			try {
				if (OpenshiftCoreUIIntegration.requestApproval(
						"Error: '"
								+ ce.getMessage()
								+ "' occurred while pushing.\n\nIf the commit history is not correct on the remote repository, "
								+ "a forced push (git push -f) might be the right thing to do. This will though overwrite the remote repository!"
								+ "\n\n Do you want to do a forced push and overwrite any remote changes ? ",
						"Attempt push force ?", false)) {
					return EGitUtils.pushForce(
							remoteName, repository, new SubProgressMonitor(monitor, 100),
							OpenshiftCoreUIIntegration.getConsoleOutputStream(server));
				} else {
					// printing out variation of the standard git output
					// meesage.
					OpenshiftCoreUIIntegration.appendToConsole(
									server,
									"\n\nERROR: "
											+ ce.getLocalizedMessage()
											+ "\n\n"
											+ "To prevent you from losing history, non-fast-forward updates were rejected"
											+ "\nMerge the remote changes (e.g. 'Team > Fetch from Upstream' in Eclipse or 'git pull' on command line ) before pushing again. "
											+ "\nSee the 'Note about fast-forwards' section of 'git push --help' for details.");
				}
				monitor.done();
				return null;
			} catch (CoreException ce2) {
				if (ce.getMessage() != null 
						&& ce.getMessage().contains("UP_TO_DATE")) {
					OpenshiftCoreUIIntegration.appendToConsole(server, "\n(Forced push) Repository already uptodate.");
					return null;
				} else {
					// even the push force failed, and we don't have a valid
					// result to check :(
					throw ce2;
				}
			}
		}
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

	protected void refreshProject(final IProject project, IProgressMonitor monitor) throws CoreException {
		// Already inside a workspace scheduling rule
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	public static IProgressMonitor submon(final IProgressMonitor parent, final int ticks) {
		return submon(parent, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
	}

	public static IProgressMonitor submon(final IProgressMonitor parent,
			final int ticks, final int style) {
		return (parent == null ? new NullProgressMonitor() : new SubProgressMonitor(parent, ticks, style));
	}
	
	
	public int getPublishType(int kind, int deltaKind, int modulePublishState) {
		if( deltaKind == ServerBehaviourDelegate.ADDED ) 
			return PublishControllerUtility.FULL_PUBLISH;
		else if (deltaKind == ServerBehaviourDelegate.REMOVED) {
			return PublishControllerUtility.REMOVE_PUBLISH;
		} else if (kind == IServer.PUBLISH_FULL 
				|| modulePublishState == IServer.PUBLISH_STATE_FULL 
				|| kind == IServer.PUBLISH_CLEAN ) {
			return PublishControllerUtility.FULL_PUBLISH;
		} else if (kind == IServer.PUBLISH_INCREMENTAL 
				|| modulePublishState == IServer.PUBLISH_STATE_INCREMENTAL 
				|| kind == IServer.PUBLISH_AUTO) {
			if( ServerBehaviourDelegate.CHANGED == deltaKind ) 
				return PublishControllerUtility.INCREMENTAL_PUBLISH;
		} 
		return PublishControllerUtility.NO_PUBLISH;
	}
}
