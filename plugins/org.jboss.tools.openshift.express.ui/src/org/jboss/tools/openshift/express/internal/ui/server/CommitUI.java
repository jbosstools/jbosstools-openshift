/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitJob;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Based on org.eclipse.egit.ui.internal.commit.CommitUI with minimal changes which allow us customizations.
 * (which will get contributed back and this class removed).
 */
public class CommitUI  {

	private IndexDiff indexDiff;

	private Set<String> notIndexed;

	private Set<String> indexChanges;

	private Set<String> notTracked;

	private Set<String> files;

	private boolean amending;

	private Shell shell;

	private Repository repo;

	private IResource[] selectedResources;

	private boolean preselectAll;

	private Runnable pushRunnable;

	private String remote;

	private String applicationName;

	/**
	 * Constructs a CommitUI object
	 * @param shell
	 *            Shell to use for UI interaction. Must not be null.
	 * @param repo
	 *            Repository to commit. Must not be null
	 * @param selectedResources
	 *            Resources selected by the user. A file is preselected in the
	 *            commit dialog if the file is contained in selectedResources or
	 *            if selectedResources contains a resource that is parent of the
	 *            file. selectedResources must not be null.
	 * @param preselectAll
	 * 			  preselect all changed files in the commit dialog.
	 * 			  If set to true selectedResources are ignored.
	 */
	public CommitUI(Shell shell, Repository repo, String remote, String applicationName, Runnable pushRunnable) {
		this(shell, repo, new IResource[] {}, true);
		this.remote = remote;
		this.applicationName = applicationName;
		this.pushRunnable = pushRunnable;
	}
	
	/**
	 * Constructs a CommitUI object
	 * @param shell
	 *            Shell to use for UI interaction. Must not be null.
	 * @param repo
	 *            Repository to commit. Must not be null
	 * @param selectedResources
	 *            Resources selected by the user. A file is preselected in the
	 *            commit dialog if the file is contained in selectedResources or
	 *            if selectedResources contains a resource that is parent of the
	 *            file. selectedResources must not be null.
	 * @param preselectAll
	 * 			  preselect all changed files in the commit dialog.
	 * 			  If set to true selectedResources are ignored.
	 */
	public CommitUI(Shell shell, Repository repo,
			IResource[] selectedResources, boolean preselectAll) {
		this.shell = shell;
		this.repo = repo;
		this.selectedResources = new IResource[selectedResources.length];
		// keep our own copy
		System.arraycopy(selectedResources, 0, this.selectedResources, 0,
				selectedResources.length);
		this.preselectAll = preselectAll;
	}

	/**
	 * Performs a commit
	 * @return true if a commit operation was triggered
	 */
	public boolean commit() {
		// let's see if there is any dirty editor around and
		// ask the user if they want to save or abort
		if (!UIUtils.saveAllEditors(repo))
			return false;

		BasicConfigurationDialog.show(new Repository[]{repo});

		resetState();
		final IProject[] projects = getProjectsOfRepositories();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						buildIndexHeadDiffList(projects, monitor);
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.CommitAction_errorComputingDiffs, e.getCause(),
					true);
			return false;
		} catch (InterruptedException e) {
			return false;
		}

		CommitHelper commitHelper = new CommitHelper(repo);

		if (!commitHelper.canCommit()) {
			MessageDialog.openError(
					shell,
					UIText.CommitAction_cannotCommit,
					commitHelper.getCannotCommitMessage());
			return false;
		}
		boolean amendAllowed = commitHelper.amendAllowed();
		if (files.isEmpty()) {
			if (amendAllowed && commitHelper.getPreviousCommit() != null) {
				boolean result = MessageDialog.openQuestion(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendCommit);
				if (!result)
					return false;
				amending = true;
			} else {
				MessageDialog.openWarning(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendNotPossible);
				return false;
			}
		}

		CommitDialog commitDialog = new CommitDialog(remote, applicationName, shell);
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFiles(repo, files, indexDiff);
		commitDialog.setPreselectedFiles(getSelectedFiles());
		commitDialog.setPreselectAll(preselectAll);
		commitDialog.setAuthor(commitHelper.getAuthor());
		commitDialog.setCommitter(commitHelper.getCommitter());
		commitDialog.setAllowToChangeSelection(!commitHelper.isMergedResolved() && !commitHelper.isCherryPickResolved());
		commitDialog.setCommitMessage(commitHelper.getCommitMessage());

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return false;

		final CommitOperation commitOperation;
		
		if (commitDialog.isPushOnlyRequested()) {
			new Job("Publishing to OpenShift") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					pushRunnable.run();
					return Status.OK_STATUS;
				}
				
			}.schedule();
		} else {
			try {
				commitOperation= new CommitOperation(
						repo,
						commitDialog.getSelectedFiles(), notTracked, commitDialog.getAuthor(),
						commitDialog.getCommitter(), commitDialog.getCommitMessage());
			} catch (CoreException e1) {
				Activator.handleError(UIText.CommitUI_commitFailed, e1, true);
				return false;
			}
			if (commitDialog.isAmending())
				commitOperation.setAmending(true);
			commitOperation.setComputeChangeId(commitDialog.getCreateChangeId());
			commitOperation.setCommitAll(commitHelper.isMergedResolved());
			if (commitHelper.isMergedResolved())
				commitOperation.setRepository(repo);
			Job commitJob = new CommitJob(repo, commitOperation);	
//			.setPushUpstream(commitDialog.isPushRequested());
			if (pushRunnable != null) {
				if (commitDialog.isPushRequested()) {
					commitJob.addJobChangeListener(new JobChangeAdapter() {

						@Override
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								pushRunnable.run();
							}
						}

					});
				}
			} 
			commitJob.schedule();
		}

		return true;
	}

	private IProject[] getProjectsOfRepositories() {
		Set<IProject> ret = new HashSet<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repo)
				ret.add(project);
		}
		return ret.toArray(new IProject[ret.size()]);
	}

	private void resetState() {
		files = new LinkedHashSet<String>();
		notIndexed = new LinkedHashSet<String>();
		indexChanges = new LinkedHashSet<String>();
		notTracked = new LinkedHashSet<String>();
		amending = false;
		indexDiff = null;
	}

	/**
	 * Retrieves a collection of files that may be committed based on the user's
	 * selection when they performed the commit action. That is, even if the
	 * user only selected one folder when the action was performed, if the
	 * folder contains any files that could be committed, they will be returned.
	 *
	 * @return a collection of files that is eligible to be committed based on
	 *         the user's selection
	 */
	private Set<String> getSelectedFiles() {
		Set<String> preselectionCandidates = new LinkedHashSet<String>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// iterate through all the files that may be committed
		for (String fileName : files) {
			URI uri = new File(repo.getWorkTree(), fileName).toURI();
			IFile[] workspaceFiles = root.findFilesForLocationURI(uri);
			if (workspaceFiles.length > 0) {
				IFile file = workspaceFiles[0];
				for (IResource resource : selectedResources) {
					// if any selected resource contains the file, add it as a
					// preselection candidate
					if (resource.contains(file)) {
						preselectionCandidates.add(fileName);
						break;
					}
				}
			} else {
				// could be file outside of workspace
				for (IResource resource : selectedResources) {
					if(resource.getFullPath().toFile().equals(new File(uri))) {
						preselectionCandidates.add(fileName);
					}
				}
			}
		}
		return preselectionCandidates;
	}

	private void buildIndexHeadDiffList(IProject[] selectedProjects,
			IProgressMonitor monitor) throws IOException,
			OperationCanceledException {

		monitor.beginTask(UIText.CommitActionHandler_calculatingChanges, 1000);
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);
		CountingVisitor counter = new CountingVisitor();
		for (IProject p : selectedProjects) {
			try {
				p.accept(counter);
			} catch (CoreException e) {
				// ignore
			}
		}
		WorkingTreeIterator it = IteratorService.createInitialIterator(repo);
		if (it == null)
			throw new OperationCanceledException(); // workspace is closed
		indexDiff = new IndexDiff(repo, Constants.HEAD, it);
		indexDiff.diff(jgitMonitor, counter.count, 0, NLS.bind(
				UIText.CommitActionHandler_repository, repo.getDirectory()
						.getPath()));

		includeList(indexDiff.getAdded(), indexChanges);
		includeList(indexDiff.getChanged(), indexChanges);
		includeList(indexDiff.getRemoved(), indexChanges);
		includeList(indexDiff.getMissing(), notIndexed);
		includeList(indexDiff.getModified(), notIndexed);
		includeList(indexDiff.getUntracked(), notTracked);
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		monitor.done();
	}

	static class CountingVisitor implements IResourceVisitor {
		int count;
		public boolean visit(IResource resource) throws CoreException {
			count++;
			return true;
		}
	}

	private void includeList(Set<String> added, Set<String> category) {
		for (String filename : added) {
			if (!files.contains(filename))
				files.add(filename);
			category.add(filename);
		}
	}

}