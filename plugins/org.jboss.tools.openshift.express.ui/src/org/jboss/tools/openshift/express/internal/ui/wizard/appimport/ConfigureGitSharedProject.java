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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
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
public class ConfigureGitSharedProject extends AbstractImportApplicationOperation {

	private ArrayList<IResource> modifiedResources;

	public ConfigureGitSharedProject(String projectName, IApplication application, String remoteName,
			IUser user) {
		super(projectName, application, remoteName);
		this.modifiedResources = new ArrayList<IResource>();
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

		copyOpenshiftConfigurations(getApplication(), getRemoteName(), project, monitor);

		setupGitIgnore(project);
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

		EGitUtils.addRemoteTo(
				getRemoteName(),
				getApplication().getGitUri(),
				EGitUtils.getRepository(project));
		setupOpenShiftMavenProfile(project);
		addAndCommitModifiedResource(project, monitor);

		return Collections.singletonList(project);
	}

	private void addAndCommitModifiedResource(IProject project, IProgressMonitor monitor) throws CoreException {
		new AddToIndexOperation(modifiedResources).execute(monitor);
		EGitUtils.commit(project, monitor);
	}

	private void setupOpenShiftMavenProfile(IProject project) throws CoreException {
		Assert.isLegal(OpenShiftMavenProfile.isMavenProject(project));

		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(project, OpenShiftUIActivator.PLUGIN_ID);
		if (profile.existsInPom()) {
			return;
		}
		profile.addToPom(project.getName());
		IFile pomFile = profile.savePom();
		modifiedResources.add(pomFile);
	}

	/**
	 * Copies the openshift configuration from the given source folder to the
	 * given project. Copies
	 * <ul>
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
	 * @return
	 * @throws IOException
	 * @throws CoreException
	 * @throws URISyntaxException 
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 * @throws OpenShiftException 
	 */
	private void copyOpenshiftConfigurations(IApplication application, String remoteName, IProject project, IProgressMonitor monitor)
			throws IOException, CoreException, OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		Assert.isLegal(project != null);
		monitor.subTask(NLS.bind("Copying openshift configuration to project {0}...", project.getName()));

		File tmpFolder = FileUtils.getRandomTmpFolder();
		cloneRepository(application, remoteName, tmpFolder, false, monitor);

		Collection<IResource> copiedResources =
				copyResources(tmpFolder, new String[] { ".openshift", "deployments" }, project);
		modifiedResources.addAll(copiedResources);
		FileUtil.safeDelete(tmpFolder);
	}

	private Collection<IResource> copyResources(File sourceFolder, String[] sourcePaths, IProject project)
			throws IOException {
		List<IResource> resources = new ArrayList<IResource>();
		File projectFolder = project.getLocation().toFile();

		for (String sourcePath : sourcePaths) {
			File source = new File(sourceFolder, sourcePath);

			if (!FileUtils.canRead(source)) {
				continue;
			}

			FileUtils.copy(source, projectFolder, false);

			if (source.isDirectory()) {
				resources.add(project.getFolder(sourcePath));
			} else {
				resources.add(project.getFile(sourcePath));
			}
		}
		return resources;
	}

	/**
	 * Adds a predefined set of entries to the gitignore file in (root of) the
	 * given project. If no .gitignore exists yet, a fresh one is created.
	 * 
	 * @param project
	 *            the project to which the .gitignore shall be configured
	 * @throws IOException
	 */
	private void setupGitIgnore(IProject project) throws IOException {
		GitIgnore gitIgnore = new GitIgnore(project);
		gitIgnore.add("target")
				.add(".settings")
				.add(".project")
				.add(".classpath")
				.add(".factorypath");
		File file = gitIgnore.write(false);
		IFile gitIgnoreFile = project.getFile(file.getName());
		modifiedResources.add(gitIgnoreFile);
	}
}
