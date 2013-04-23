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
package org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.project.GeneralProjectImportOperation;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.project.MavenProjectImportOperation;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportNewProject extends AbstractImportApplicationOperation {

	private File cloneDestination;

	public ImportNewProject(String projectName, IApplication application, String remoteName,
			File cloneDestination, Connection user) {
		super(projectName, application, remoteName, user);
		this.cloneDestination = cloneDestination;
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
	public IProject execute(IProgressMonitor monitor)
			throws OpenShiftException, CoreException, InterruptedException, URISyntaxException,
			InvocationTargetException, IOException, NoWorkTreeException, GitAPIException {
		if (cloneDestinationExists()) {
			throw new WontOverwriteException(
					NLS.bind("There's already a folder at {0}. The new OpenShift project would overwrite it. " +
							"Please choose another destination to clone to.",
							getCloneDestination().getAbsolutePath()));
		}

		File repositoryFolder =
				cloneRepository(getApplication(), getRemoteName(), cloneDestination, true, monitor);
		List<IProject> importedProjects = importProjectsFrom(repositoryFolder, monitor);
		if (importedProjects.size() == 0) {
			String projectName = getProjectName();
			if (StringUtils.isEmpty(projectName)) {
				projectName = getApplication().getName();
			}
			throw new ImportFailedException(
					NLS.bind("Could not import project {0}. One of the possible reasons is that there's already a " +
							"project in your workspace that matches the openshift application/maven name of the " +
							"OpenShift application. " +
							"Please rename your workspace project in that case and start over again."
							, projectName));
		}

		connectToGitRepo(importedProjects, repositoryFolder, monitor);
		// TODO: handle multiple projects (is this really possible?)
		IProject project = getSettingsProject(importedProjects);
		addToModified(setupGitIgnore(project, monitor));
		addSettingsFile(project, monitor);
		addAndCommitModifiedResource(project, monitor);
		
		return getSettingsProject(importedProjects);
	}
	
	@SuppressWarnings("unused")
	private void mergeWithApplicationRepository(Repository repository, IApplication application,
			IProgressMonitor monitor)
			throws MalformedURLException, URISyntaxException, IOException, OpenShiftException, CoreException,
			InvocationTargetException {
		URIish uri = new URIish(application.getGitUrl());
		EGitUtils.addRemoteTo("openshift", uri, repository);
		EGitUtils.mergeWithRemote(uri, "refs/remotes/openshift/HEAD", repository, monitor);
	}

	/**
	 * Imports the projects that are within the given folder. Supports maven and
	 * general projects
	 * 
	 * @param folder
	 *            the folder the projects are located in
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	private List<IProject> importProjectsFrom(final File folder, IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		MavenProjectImportOperation mavenImport = new MavenProjectImportOperation(folder);
		List<IProject> importedProjects = Collections.emptyList();
		if (mavenImport.isMavenProject()) {
			importedProjects = mavenImport.importToWorkspace(monitor);
		} else {
			importedProjects = new GeneralProjectImportOperation(folder).importToWorkspace(monitor);
		}
		return importedProjects;
	}

	private void connectToGitRepo(List<IProject> projects, File projectFolder, IProgressMonitor monitor)
			throws CoreException {
		for (IProject project : projects) {
			if (project != null) {
				EGitUtils.connect(project, monitor);
			}
		}
	}

	protected File getCloneDestination() {
		return cloneDestination;
	}

	protected boolean cloneDestinationExists() {
		return cloneDestination != null
				&& cloneDestination.exists();
	}
}
