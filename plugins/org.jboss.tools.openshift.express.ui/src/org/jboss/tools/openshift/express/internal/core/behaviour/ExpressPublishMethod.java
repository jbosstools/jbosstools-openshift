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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.archives.webtools.modules.LocalZippedPublisherUtil;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IDeployableServerBehaviour;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethod;
import org.jboss.ide.eclipse.as.core.server.IPublishCopyCallbackHandler;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;

/**
 * @author Rob Stryker
 */
public class ExpressPublishMethod implements IJBossServerPublishMethod {

	@Override
	public void publishStart(final IDeployableServerBehaviour behaviour, final IProgressMonitor monitor) throws CoreException {
		String destProjName = ExpressServerUtils.getExpressDeployProject(behaviour.getServer());
		IProject magicProject = destProjName == null ? 
				null : ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if (magicProject == null || !magicProject.isAccessible()) {
			throw new CoreException(new Status(IStatus.ERROR,
					OpenShiftUIActivator.PLUGIN_ID,
					NLS.bind(ExpressMessages.publishFailMissingProject, behaviour.getServer().getName(), destProjName)));
		}
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				ConsoleUtils.displayConsoleView(behaviour.getServer());
			}
		});
	}

	@Override
	public int publishFinish(IDeployableServerBehaviour behaviour, IProgressMonitor monitor) throws CoreException {

		String destProjName = ExpressServerUtils.getExpressDeployProject(behaviour.getServer());
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		boolean allSubModulesPublished = areAllPublished(behaviour);

		if (destProj != null 
				&& destProj.exists()) {
		
			String destinationFolder = ExpressServerUtils.getExpressDeployFolder(behaviour.getServer());
			IContainer destFolder = ExpressServerUtils.getDeployFolderResource(destinationFolder, destProj);
			
			if (allSubModulesPublished 
					|| (destFolder != null && destFolder.isAccessible())) {
				refreshProject(destProj, submon(monitor, 100));
				commitAndPushProject(destProj, behaviour, submon(monitor, 100));
			} // else ignore. (one or more modules not published AND magic
				// folder doesn't exist
				// The previous exception will be propagated.
		}

		return allSubModulesPublished ? IServer.PUBLISH_STATE_NONE : IServer.PUBLISH_STATE_INCREMENTAL;
	}

	protected boolean areAllPublished(IDeployableServerBehaviour behaviour) {
		IModule[] modules = behaviour.getServer().getModules();
		boolean allpublished = true;
		for (int i = 0; i < modules.length; i++) {
			if (behaviour.getServer().getModulePublishState(new IModule[] { modules[i] }) != IServer.PUBLISH_STATE_NONE)
				allpublished = false;
		}
		return allpublished;
	}

	@Override
	public int publishModule(IDeployableServerBehaviour behaviour, int kind,
			int deltaKind, IModule[] module, IProgressMonitor monitor)
			throws CoreException {

		if (module.length > 1)
			return IServer.PUBLISH_STATE_UNKNOWN;

		// Magic Project
		String destProjName = ExpressServerUtils.getExpressDeployProject(behaviour.getServer());
		if (isInDestProjectTree(destProjName, module))
			return IServer.PUBLISH_STATE_NONE;

		// Cannot be null, checked for in publishStart
		IProject destProj = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjName);
		if (destProj.equals(module[module.length - 1].getProject()))
			return IServer.PUBLISH_STATE_NONE;

		IContainer destFolder = getDestination(behaviour, destProj);
		IPath destPath = destFolder.getLocation();

		if (module.length == 0)
			return IServer.PUBLISH_STATE_NONE;
		int modulePublishState = behaviour.getServer().getModulePublishState(module);
		int publishType = behaviour.getPublishType(kind, deltaKind, modulePublishState);

		IModuleResourceDelta[] delta = new IModuleResourceDelta[] {};
		if (deltaKind != ServerBehaviourDelegate.REMOVED)
			delta = behaviour.getPublishedResourceDelta(module);

		try {
			LocalZippedPublisherUtil util = new LocalZippedPublisherUtil();
			IPath path = util.getOutputFilePath(behaviour.getServer(), module, destPath.toString()); // where
																										// it's
																										// deployed
			IResource changedResource = destFolder.getFile(new Path(path.lastSegment()));
			IResource[] resource = new IResource[] { changedResource };

			if (deltaKind == ServerBehaviourDelegate.REMOVED) {
				changedResource.delete(false, monitor); // uses resource api
			} else {
				monitor.beginTask("Moving module to " + destFolder.getName(), 100);
				util.publishModule(behaviour.getServer(), destPath.toString(), module, publishType, delta,
						new SubProgressMonitor(monitor, 20));
				// util used file api, so a folder refresh is required
				destFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 20));
				final AddToIndexOperation operation = new AddToIndexOperation(resource);
				try {
					operation.execute(new SubProgressMonitor(monitor, 60));
				} catch (CoreException e) {
					OpenShiftUIActivator.log(e.getStatus());
				}
			}
		} catch (Exception e) {
			OpenShiftUIActivator.log(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, e.getMessage(), e));
		}
		return IServer.PUBLISH_STATE_NONE;
	}

	protected IContainer getDestination(IDeployableServerBehaviour behaviour, IProject destProj) throws CoreException {
		String destinationFolder = ExpressServerUtils.getExpressDeployFolder(behaviour.getServer());
		IContainer destFolder = ExpressServerUtils.getDeployFolderResource(destinationFolder, destProj);
;
		if (destFolder == null 
				|| !destFolder.isAccessible()) {
			throw new CoreException(OpenShiftUIActivator.createErrorStatus(NLS.bind(
					ExpressMessages.publishFailMissingFolder,
					behaviour.getServer().getName(),
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

	protected PushOperationResult commitAndPushProject(IProject p, IDeployableServerBehaviour behaviour,
			IProgressMonitor monitor) throws CoreException {

		PushOperationResult result = null;
		int changes = ExpressServerUtils.countCommitableChanges(p, behaviour.getServer(), monitor);
		if (changes > 0) {
			if (requestApproval(
					NLS.bind(ExpressMessages.commitAndPushMsg, changes, p.getName()),
					NLS.bind(ExpressMessages.publishTitle, p.getName()))) {
				monitor.beginTask("Publishing " + p.getName(), 300);
				EGitUtils.commit(p, new SubProgressMonitor(monitor, 100));
				result = push(p, behaviour.getServer(), monitor);
			}
		} else {
			try {
				String openShiftRemoteName =
						ExpressServerUtils.getExpressRemoteName(behaviour.getServer());
				if (!EGitUtils.isAhead(p, openShiftRemoteName, monitor)) {
					if (requestApproval(
							NLS.bind(ExpressMessages.noChangesPushAnywayMsg, p.getName()),
							NLS.bind(ExpressMessages.publishTitle, p.getName()))) {
						result = push(p, behaviour.getServer(), monitor);
					}
				} else {
					if (requestApproval(
							NLS.bind(ExpressMessages.pushCommitsMsg, p.getName()),
							NLS.bind(ExpressMessages.publishTitle, p.getName()))) {
						result = push(p, behaviour.getServer(), monitor);
					}
				}
			} catch (Exception e) {
				OpenShiftUIActivator.log(e);
			}
		}

		if (result != null) {
			ConsoleUtils.appendGitPushToConsole(behaviour.getServer(), result);
		}

		return result;
	}

	protected PushOperationResult push(IProject project, IServer server, IProgressMonitor monitor) throws CoreException {
		String remoteName = ExpressServerUtils.getExpressRemoteName(server.createWorkingCopy());
		Repository repository = EGitUtils.getRepository(project);
		try {
			monitor.beginTask("Publishing " + project.getName(), 200);
			PushOperationResult result = EGitUtils.push(
					remoteName, repository, new SubProgressMonitor(monitor, 100),
					ConsoleUtils.getConsoleOutputStream(server));
			monitor.done();
			return result;
		} catch (CoreException ce) {
			// Comes if push has failed
			if (ce.getMessage() != null && ce.getMessage().contains("UP_TO_DATE")) {
				ConsoleUtils.appendToConsole(server, "\n\nRepository already uptodate.");
				return null;
			}

			try {
				if (requestApproval(
						"Error: '"
								+ ce.getMessage()
								+ "' occurred while pushing.\n\nIf the commit history is not correct on the remote repository, "
								+ "a forced push (git push -f) might be the right thing to do. This will though overwrite the remote repository!"
								+ "\n\n Do you want to do a forced push and overwrite any remote changes ? ",
						"Attempt push force ?", false)) {
					return EGitUtils.pushForce(
							remoteName, repository, new SubProgressMonitor(monitor, 100),
							ConsoleUtils.getConsoleOutputStream(server));
				} else {
					// printing out variation of the standard git output
					// meesage.
					ConsoleUtils
							.appendToConsole(
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
					ConsoleUtils.appendToConsole(server, "\n(Forced push) Repository already uptodate.");
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

	protected boolean requestApproval(final String message, final String title) {
		final boolean[] b = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				b[0] = openQuestion(getActiveShell(), title, message, true);
			}
		});
		return b[0];
	}

	private boolean openQuestion(Shell shell, String title, String message, boolean defaultAnswer) {
		String[] labels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
		int defaultValue = defaultAnswer ? 0 : 1;
		MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, labels,
				defaultValue);
		return dialog.open() == 0;
	}

	/**
	 * Opens question dialog where you can control what will be the default
	 * button activated.
	 * 
	 * @parem defaultAnswer if true Yes is the default answer, if false No
	 * 
	 **/
	protected boolean requestApproval(final String message, final String title, final boolean defaultAnswer) {
		final boolean[] b = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				b[0] = openQuestion(getActiveShell(), title, message, defaultAnswer);
			}
		});
		return b[0];
	}

	protected static Shell getActiveShell() {
		Display display = Display.getDefault();
		final Shell[] ret = new Shell[1];
		display.syncExec(new Runnable() {
			public void run() {
				ret[0] = Display.getCurrent().getActiveShell();
			}
		});
		return ret[0];
	}

	public IPublishCopyCallbackHandler getCallbackHandler(IPath path,
			IServer server) {
		return null;
	}

	public IPublishCopyCallbackHandler getCallbackHandler(IPath deployPath, IPath tmpFolder, IServer server) {
		return null;
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
}
