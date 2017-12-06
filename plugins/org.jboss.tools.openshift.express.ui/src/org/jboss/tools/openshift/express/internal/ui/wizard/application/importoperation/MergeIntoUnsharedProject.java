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
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.util.ResourceUtils;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * Strategy that enables the given git shared project to be used on the chosen
 * OpenShift application.
 * 
 * @author André Dietisheim <adietish@redhat.com>
 */
public class MergeIntoUnsharedProject extends AbstractImportApplicationOperation {

	public MergeIntoUnsharedProject(String projectName, IApplication application, String remoteName,
			List<IOpenShiftMarker> markers, ExpressConnection connection) {
		super(projectName, application, remoteName, markers, connection);
	}

	/**
	 * Enables the user chosen project to be used on the chosen OpenShift
	 * application. *
	 * <ul>
	 * <li>clones the application git repository</li>
	 * <li>copies the configuration files to the user project (in the workspace)
	 * </li>
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
	public IProject execute(IProgressMonitor monitor) throws OpenShiftException, InvocationTargetException,
			InterruptedException, IOException, CoreException, URISyntaxException {
		// File repositoryFile =
		// model.cloneRepository(monitor);
		// model.importProject(repositoryFile, monitor);
		// Repository repository =
		// model.shareProject(monitor);
		// model.mergeWithApplicationRepository(repository,
		// monitor);
		IProject project = getProject();

		copyOpenshiftConfigurations(getApplication(), getRemoteName(), project, monitor);
		setupGitIgnore(project, monitor);
		setupOpenShiftMavenProfile(getApplication(), project, monitor);
		addSettingsFile(project, monitor);
		setupMarkers(project, monitor);
		shareProject(project, monitor);
		addRemote(getRemoteName(), getApplication().getGitUrl(), project);

		return project;
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
	 * @throws CoreException
	 */
	private void copyOpenshiftConfigurations(IApplication application, String remoteName, IProject project,
			IProgressMonitor monitor) throws IOException, OpenShiftException, InvocationTargetException,
			InterruptedException, URISyntaxException, CoreException {
		Assert.isLegal(project != null);

		monitor.subTask(NLS.bind("Copying openshift configuration to project {0}...", project.getName()));
		File tmpFolder = FileUtils.getRandomTmpFolder();
		cloneRepository(getApplication(), getRemoteName(), tmpFolder, false, monitor);
		ResourceUtils.copy(tmpFolder, new String[] { ".git", ".openshift", "deployments", "pom.xml" }, project,
				monitor);

		FileUtil.safeDelete(tmpFolder);

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
