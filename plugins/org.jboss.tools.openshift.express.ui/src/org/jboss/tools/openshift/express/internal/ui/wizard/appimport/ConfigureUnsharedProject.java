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
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.jboss.tools.openshift.express.internal.ui.utils.FileUtils;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * Strategy that enables the given git shared project to be used on the chosen
 * OpenShift application.
 * 
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ConfigureUnsharedProject extends AbstractImportApplicationOperation {

	public ConfigureUnsharedProject(String projectName, IApplication application, String remoteName,
			IUser user) {
		super(projectName, application, remoteName);
	}

	/**
	 * Enables the user chosen project to be used on the chosen OpenShift
	 * application. *
	 * <ul>
	 * <li>clones the application git repository</li>
	 * <li>copies the configuration files to the user project (in the workspace)</li>
	 * <li>shares the given project with git</li>
	 * </ul>
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws URISyntaxException
	 *             The OpenShift application repository could not be cloned,
	 *             because the uri it is located at is not a valid git uri
	 * @throws OpenShiftException
	 * 
	 * @throws InvocationTargetException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation failed.
	 * @throws InterruptedException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation was interrupted.
	 * @throws IOException
	 *             The configuration files could not be copied from the git
	 *             clone to the user project
	 * @throws CoreException
	 *             The user project could not be shared with the git
	 * 
	 * @see #cloneRepository
	 * @see #copyOpenshiftConfiguration
	 * @see #shareProject
	 * @see #createServerAdapterIfRequired
	 */
	@Override
	public List<IProject> execute(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException {
		// File repositoryFile =
		// model.cloneRepository(monitor);
		// model.importProject(repositoryFile, monitor);
		// Repository repository =
		// model.shareProject(monitor);
		// model.mergeWithApplicationRepository(repository,
		// monitor);
		IProject project = getProject();

		copyOpenshiftConfigurations(getApplication(), getRemoteName(), project, monitor);
		createGitIgnore(project, monitor);
		shareProject(project, monitor);

		return Collections.singletonList(project);
	}

	private void shareProject(IProject project, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Sharing project {0}...", project.getName()));
		EGitUtils.share(project, monitor);
	}

	/**
	 * Copies the openshift configuration from the given source folder to the
	 * given project. Copies
	 * <ul>
	 * <li>.git</li>
	 * <li>.openshift</li>
	 * <li>deployments</li>
	 * <li>pom.xml</li>
	 * </ul>
	 * to the project in the workspace
	 * 
	 * @param sourceFolder
	 *            the source to copy the openshift config from
	 * @param project
	 *            the project to copy the configuration to.
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 * @throws OpenShiftException 
	 */
	private void copyOpenshiftConfigurations(IApplication application, String remoteName, IProject project, IProgressMonitor monitor)
			throws IOException, OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		Assert.isLegal(project != null);
		File projectFolder = project.getLocation().toFile();

		File tmpFolder = FileUtils.getRandomTmpFolder();
		cloneRepository(getApplication(), getRemoteName(), tmpFolder, false, monitor);

		monitor.subTask(NLS.bind("Copying openshift configuration to project {0}...", project.getName()));
		FileUtils.copy(new File(tmpFolder, ".git"), projectFolder, false);
		FileUtils.copy(new File(tmpFolder, ".openshift"), projectFolder, false);
		FileUtils.copy(new File(tmpFolder, "deployments"), projectFolder, false);
		FileUtils.copy(new File(tmpFolder, "pom.xml"), projectFolder, false);

		FileUtil.safeDelete(tmpFolder);

	}

	/**
	 * Adds a predefined set of entries to the gitignore file in (root of) the
	 * given project. If no .gitignore exists yet, a fresh one is created.
	 * 
	 * @param project
	 *            the project to which the .gitignore shall be configured
	 * @throws IOException
	 * @throws CoreException 
	 */
	private void createGitIgnore(IProject project, IProgressMonitor monitor) throws IOException, CoreException {
		GitIgnore gitIgnore = new GitIgnore(project);
		gitIgnore.add("target")
				.add(".settings")
				.add(".project")
				.add(".classpath")
				.add(".factorypath");
		gitIgnore.write(monitor);
	}

	// private void mergeWithApplicationRepository(Repository repository,
	// IApplication application,
	// IProgressMonitor monitor)
	// throws MalformedURLException, URISyntaxException, IOException,
	// OpenShiftException, CoreException,
	// InvocationTargetException {
	// URIish uri = new URIish(application.getGitUri());
	// EGitUtils.addRemoteTo("openshift", uri, repository);
	// EGitUtils.mergeWithRemote(uri, "refs/remotes/openshift/HEAD", repository,
	// monitor);
	// }
}
