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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIException;
import org.jboss.tools.openshift.express.internal.ui.UnCommittedChangesException;
import org.jboss.tools.openshift.express.internal.ui.utils.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.ResourceUtils;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * Strategy that enables the given git shared project to be used on the chosen
 * OpenShift application.
 * 
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ConfigureGitSharedProject extends AbstractImportApplicationOperation {

	public ConfigureGitSharedProject(String projectName, IApplication application, String remoteName,
			IUser user) {
		super(projectName, application, remoteName);
	}

	/**
	 * Enables the user chosen project to be used on the chosen OpenShift
	 * application.
	 * <ul>
	 * <li>clones the application git repository</li>
	 * <li>copies the configuration files to the user project (in the workspace)
	 * </li>
	 * <li>adds the appication git repo as remote</li>
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
		IProject project = getProject();
		Assert.isTrue(EGitUtils.isSharedWithGit(project));

		if (EGitUtils.isDirty(EGitUtils.getRepository(project))) {
			throw new UnCommittedChangesException(
					"The project {0} has uncommitted changes. Please commit those changes first.", project.getName());
		}

		addToModified(copyOpenshiftConfigurations(getApplication(), getRemoteName(), project, monitor));
		addToModified(setupGitIgnore(project, monitor));
		addToModified(setupOpenShiftMavenProfile(project, monitor));
		addRemote(getRemoteName(), getApplication().getUUID(), project);

		addAndCommitModifiedResource(project, monitor);

		return Collections.singletonList(project);
	}

	private void addRemote(String remoteName, String uuid, IProject project)
			throws MalformedURLException, URISyntaxException, IOException, OpenShiftException, CoreException {
		Repository repository = EGitUtils.getRepository(project);
		Assert.isTrue(repository != null);

		RemoteConfig config = EGitUtils.getRemoteByName(remoteName, repository);
		if (config != null) {
			if (EGitUtils.hasRemoteUrl(
					Pattern.compile(RegExUtils.escapeRegex(getApplication().getGitUri())), config)) {
				return;
			}
			// we shouldn't get here, the UI should validate the remote name and
			// inform about an error in this case
			throw new OpenShiftUIException(
					"Could not enable OpenShift on project {0}. There's already a a remote called {1} that points to a differnt git repository.",
					project.getName(), remoteName);
		}

		EGitUtils.addRemoteTo(getRemoteName(), getApplication().getGitUri(), repository);
	}

	/**
	 * Copies the openshift configuration from the given application to the
	 * given project. Clones the application to a tmp-folder, copies
	 * <ul>
	 * <li>.openshift</li>
	 * <li>deployments</li>
	 * </ul>
	 * to the project in the workspace. Deployments and .openshift and kept
	 * as-is if they already exist in the project.
	 * 
	 * @param sourceFolder
	 *            the source to copy the openshift config from
	 * @param project
	 *            the project to copy the configuration to.
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @return
	 * @throws IOException
	 * @throws CoreException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 * @throws OpenShiftException
	 */
	private Collection<IResource> copyOpenshiftConfigurations(IApplication application, String remoteName,
			IProject project, IProgressMonitor monitor)
			throws IOException, CoreException, OpenShiftException, InvocationTargetException, InterruptedException,
			URISyntaxException {
		Assert.isLegal(project != null);
		monitor.subTask(NLS.bind("Copying openshift configuration to project {0}...", project.getName()));

		File tmpFolder = FileUtils.getRandomTmpFolder();
		cloneRepository(application, remoteName, tmpFolder, false, monitor);

		Collection<IResource> copiedResources =
				ResourceUtils.copy(tmpFolder, new String[] {
						".openshift",
						"deployments",
						"pom.xml" }, project, monitor);
		FileUtil.safeDelete(tmpFolder);
		return copiedResources;
	}
}
