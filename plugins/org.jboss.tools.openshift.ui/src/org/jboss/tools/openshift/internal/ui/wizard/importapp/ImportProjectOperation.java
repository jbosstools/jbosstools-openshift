/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.GeneralProjectImportOperation;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.MavenProjectImportOperation;
import org.jboss.tools.openshift.internal.common.ui.application.importoperation.WontOverwriteException;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportProjectOperation {

	private static final String PLATFORM_SEPARATOR = Matcher.quoteReplacement(File.separator);
	
	private File cloneDestination;
	private String gitUrl;
	private String gitRef;
	private Collection<String> filters;
	private boolean reuseGitRepo;
	private boolean checkoutBranch;

	/**
	 * Constructor to skip the clone and simply import the filtered projects from the destination
	 * @param gitRef the git ref (branch) to import
	 * @param cloneDestination the destination to clone to
	 * @param filters the project to import
	 */
	public ImportProjectOperation(String gitUrl, String gitRef, File cloneDestination, Collection<String> filters, boolean checkoutBranch) {
		this(gitUrl, gitRef, cloneDestination, filters, checkoutBranch, true);
	}

	/**
	 * Constructor to both clone the repository and import the filtered projects
	 * @param gitUrl the git repo to clone from
	 * @param gitRef the git ref (branch) to import
	 * @param cloneDestination the destination to clone to
	 * @param filters the project to import
	 */
	public ImportProjectOperation(String gitUrl, String gitRef, File cloneDestination, Collection<String> filters) {
		this(gitUrl, gitRef, cloneDestination, filters, true, false);
	}

	protected ImportProjectOperation(String gitUrl, String gitRef, File cloneDestination, Collection<String> filters, 
			boolean checkoutBranch, boolean reuseGitRepo) {
		this.gitUrl = gitUrl;
		this.gitRef = gitRef;
		this.cloneDestination = cloneDestination;
		this.filters = sanitize(filters);
		this.reuseGitRepo = reuseGitRepo;
		this.checkoutBranch = checkoutBranch;
	}

	/**
	 * Imports the (new) project that the user has chosen into the workspace.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws OpenShiftException
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 * @throws IOException 
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	public void execute(IProgressMonitor monitor)
			throws OpenShiftException, CoreException, InterruptedException, URISyntaxException,
			InvocationTargetException, IOException, NoWorkTreeException, GitAPIException {
		if (!reuseGitRepo) {
			importCloning(gitUrl, gitRef, cloneDestination, filters, monitor);
		} else {
			importReusingExistingRepo(gitUrl, gitRef, cloneDestination, filters, checkoutBranch, monitor);
		}
	}

	private void importCloning(String gitUrl, String gitRef, File cloneDestination, Collection<String> filters, IProgressMonitor monitor) 
			throws CoreException, OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		if (cloneDestinationExists()) {
			throw new WontOverwriteException(NLS.bind(
					"There's already a folder at {0}. The new OpenShift project would overwrite it. "
							+ "Please choose another destination to clone to.",
					getCloneDestination().getAbsolutePath()));
		}
		File repositoryFolder = cloneRepository(gitUrl, cloneDestination, gitRef, monitor);
		List<IProject> importedProjects = importProjectsFrom(repositoryFolder, filters, monitor);
		connectToGitRepo(importedProjects, repositoryFolder, monitor);
	}
	
	private void importReusingExistingRepo(String gitUrl, String gitRef, File repositoryFolder, Collection<String> filters, boolean checkoutBranch, 
			IProgressMonitor monitor) throws CoreException, InterruptedException {
		List<IProject> importedProjects = importProjectsFrom(repositoryFolder, filters, monitor);
		connectToGitRepo(importedProjects, repositoryFolder, monitor);
		if (checkoutBranch) {
			checkoutBranch(gitRef, gitUrl, importedProjects, repositoryFolder, monitor);
		}
	}
	
	protected void checkoutBranch(String gitRef, String gitUrl, List<IProject> importedProjects, File repositoryFolder, IProgressMonitor monitor) throws CoreException {
		if (!StringUtils.isEmpty(gitRef)
				&& !CollectionUtils.isEmpty(importedProjects)) {
			// all projects are in the same repo
			IProject project = importedProjects.get(0);
			Repository repository = EGitUtils.getRepository(project);
			try {
				if (!EGitUtils.hasBranch(gitRef, repository)) {
					Pattern gitURIPattern = Pattern.compile(RegExUtils.escapeRegex(gitUrl));
					RemoteConfig remote = EGitUtils.getRemoteByUrl(gitURIPattern, repository);
					fetchBranch(gitRef, remote, repository, monitor);
				}
				CheckoutResult result = EGitUtils.checkoutBranch(gitRef, repository, monitor);
				switch(result.getStatus()) {
					case CONFLICTS:
					case ERROR:
						throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
							NLS.bind("Could not check out the branch {0} of the (reused) local repository at {1} because of {2}."
									+ " Please resolve the problem and check out the branch manually so that it matches the code that's used in your OpenShift application.",
									new Object[] { gitRef, repositoryFolder, result.getStatus().toString().toLowerCase() })));
					default:
				}
			} catch (IOException e) {
				throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
						NLS.bind("Could check that branch {0} exists within the (reused) local repository at {1}.",
								gitRef, repositoryFolder), e));
			} catch (InvocationTargetException e) {
				throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
						NLS.bind("Could not fetch branch {0} from check that branch {0} exists within the (reused) local repository at {1}.",
								gitRef, repositoryFolder), e));
			}
		}
	}

	/**
	 * Fetches and creates the branch with the given name from the given remote.
	 * @param gitRef
	 * @param project
	 * @param repository
	 * @param monitor
	 * @throws CoreException
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	private void fetchBranch(String gitRef, RemoteConfig remote, Repository repository, IProgressMonitor monitor) 
			throws CoreException, InvocationTargetException, IOException {
		if (remote == null) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID, 
					NLS.bind("Could not fetch determine the remote for the repo at {0} that we should fetch branch {1} from.", 
							repository.getDirectory(), gitRef)));
		} 
		EGitUtils.fetch(remote, 
				Arrays.asList(new RefSpec(Constants.R_HEADS + gitRef + ":" + Constants.R_REMOTES + remote.getName() + "/" + gitRef)), 
				repository, monitor);
		RevCommit commit = EGitUtils.getLatestCommit(gitRef, remote.getName(), repository);
		EGitUtils.createBranch(gitRef, commit, repository, monitor);
	}
	
	/**
	 * Imports the projects that are within the given folder. Supports maven and
	 * general projects
	 * 
	 * @param folder
	 *            the folder the projects are located in
	 * @param filters 
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	private List<IProject> importProjectsFrom(final File folder, Collection<String> filters, IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		MavenProjectImportOperation mavenImport = new MavenProjectImportOperation(folder);
		mavenImport.setFilters(filters);
		List<IProject> importedProjects;
		if (mavenImport.isMavenProject()) {
			importedProjects = mavenImport.importToWorkspace(monitor);
		} else {
			importedProjects = new GeneralProjectImportOperation(folder).importToWorkspace(monitor);
		}
		return importedProjects;
	}

	protected void connectToGitRepo(List<IProject> projects, File projectFolder, IProgressMonitor monitor)
			throws CoreException {
		for (IProject project : projects) {
			if (project != null) {
				EGitUtils.connect(project, monitor);
			}
		}
	}

	/**
	 * Clones the repository of the selected OpenShift application to the user
	 * provided path.
	 * 
	 * @param application
	 *            the application to clone
	 * @param remoteName
	 *            the name of the remote repo to clone
	 * @param destination
	 *            the destination to clone to
	 * @param gitRef
	 *            the git reference to check-out
	 * @param addToRepoView
	 *            if true, the clone repo will get added to the (egit)
	 *            repositories view
	 * @param monitor
	 *            the monitor to report progress to
	 * 
	 * @return the location of the cloned repository
	 * @throws OpenShiftException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * 
	 * @see AbstractImportApplicationOperation#getApplication()
	 * @see #getCloneDestination()
	 */
	protected File cloneRepository(String gitUrl, File destination, String gitRef, IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning  {0}...", gitUrl));
		EGitUIUtils.ensureEgitUIIsStarted();
		EGitUtils.cloneRepository(
					gitUrl, Constants.DEFAULT_REMOTE_NAME, gitRef, destination, EGitUIUtils.ADD_TO_REPOVIEW_TASK, monitor);
		return destination;
	}

	protected File getCloneDestination() {
		return cloneDestination;
	}

	protected boolean cloneDestinationExists() {
		return cloneDestination != null
				&& cloneDestination.exists();
	}

	private static Collection<String> sanitize(Collection<String> filters) {
		Collection<String> sanitized = null;
		if (filters != null) {
			sanitized = new LinkedHashSet<>(filters.size());
			for (String path : filters) {
				if (StringUtils.isNotBlank(path)) {
					sanitized.add(makePlatformDependent(path));
				}
			}
		}
		return sanitized;
	}

	private static String makePlatformDependent(String path) {
		return path.replaceAll("/", PLATFORM_SEPARATOR)
				   .replaceAll("\\\\", PLATFORM_SEPARATOR);
	}
}
