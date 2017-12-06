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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.server.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.util.DeployFolder;
import org.jboss.tools.openshift.express.internal.ui.ExpressException;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
abstract class AbstractImportApplicationOperation implements IImportApplicationStrategy {

	private String projectName;
	private IApplication application;
	private String remoteName;
	protected List<IResource> modifiedResources;
	private ExpressConnection user;
	private List<IOpenShiftMarker> markers;

	public AbstractImportApplicationOperation(String projectName, IApplication application, String remoteName,
			List<IOpenShiftMarker> markers, ExpressConnection user) {
		this.projectName = projectName;
		this.application = application;
		this.remoteName = remoteName;
		this.markers = markers;
		this.modifiedResources = new ArrayList<>();
		this.user = user;
	}

	protected String getProjectName() {
		return projectName;
	}

	/**
	 * Returns the workspace project with the given name if it exists. Returns
	 * <code>null</code> otherwise.
	 * 
	 * @param name
	 *            the project name
	 * @return the project with the given name
	 */
	private IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.isTrue(project != null && project.exists(),
				NLS.bind("Could not find project {0} in your workspace.", name));
		return project;
	}

	protected IProject getProject() {
		return getProject(getProjectName());
	}

	protected IApplication getApplication() {
		return application;
	}

	protected String getRemoteName() {
		return remoteName;
	}

	protected ExpressConnection getUser() {
		return user;
	}

	/**
	 * Marks the given resources as modified.
	 * 
	 * @param resources
	 *            the resources that shall be marked as modified
	 * 
	 * @see #addAndCommitModifiedResource(IProject, IProgressMonitor)
	 */
	protected void addToModified(Collection<IResource> resources) {
		if (resources == null) {
			return;
		}
		modifiedResources.addAll(resources);
	}

	/**
	 * 
	 * Marks the given resource as modified.
	 * 
	 * @param resource
	 *            the resource that shall be marked as modified
	 * 
	 * @see #addAndCommitModifiedResource(IProject, IProgressMonitor)
	 */
	protected void addToModified(IResource resource) {
		if (resource == null) {
			return;
		}
		modifiedResources.add(resource);
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
	 * @see #getRepositoryPath()
	 */
	protected File cloneRepository(IApplication application, String remoteName, File destination, boolean addToRepoView,
			IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning repository for application {0}...", application.getName()));
		EGitUIUtils.ensureEgitUIIsStarted();
		if (addToRepoView) {
			EGitUtils.cloneRepository(application.getGitUrl(), remoteName, destination,
					EGitUIUtils.ADD_TO_REPOVIEW_TASK, monitor);
		} else {
			EGitUtils.cloneRepository(application.getGitUrl(), remoteName, destination, monitor);
		}
		return destination;
	}

	/**
	 * Adds and commits all (modified) resources in the given project to the git
	 * repository that it is attached to.
	 * 
	 * @param project
	 *            the project to commit
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws CoreException
	 * @throws OpenShiftException
	 * @throws GitAPIException
	 * @throws IOException
	 * @throws NoWorkTreeException
	 * 
	 * @see #addToModified(Collection<IResource>)
	 * @see #addToModified(IResource)
	 * 
	 */
	protected void addAndCommitModifiedResource(IProject project, IProgressMonitor monitor)
			throws CoreException, OpenShiftException, NoWorkTreeException, IOException, GitAPIException {
		EGitUtils.checkedGetRepository(project);
		new AddToIndexOperation(modifiedResources).execute(monitor);
		if (EGitUtils.isDirty(project, monitor)) {
			EGitUtils.commit(project, monitor);
		}
	}

	/**
	 * Adds a predefined set of entries to the gitignore file in (root of) the
	 * given project. If no .gitignore exists yet, a fresh one is created.
	 * 
	 * @param project
	 *            the project to which the .gitignore shall be configured
	 * @return
	 * @throws IOException
	 * @throws CoreException
	 */
	protected IFile setupGitIgnore(IProject project, IProgressMonitor monitor) throws IOException, CoreException {
		GitIgnore gitIgnore = new GitIgnore(project);
		gitIgnore.add("target").add(".settings/*").add("!.settings/.jsdtscope")//To prevent nasty JSDT bugs
				.add(".project").add(".classpath");
		return gitIgnore.write(monitor);
	}

	protected void addRemote(String remoteName, String gitUrl, IProject project)
			throws MalformedURLException, URISyntaxException, IOException, OpenShiftException, CoreException {
		Repository repository = EGitUtils.getRepository(project);
		RemoteConfig config = EGitUtils.getRemoteByName(remoteName, repository);
		if (config != null) {
			if (EGitUtils.hasRemoteUrl(Pattern.compile(RegExUtils.escapeRegex(gitUrl)), config)) {
				return;
			}
			// we shouldn't get here, the UI should validate the remote name and
			// inform about an error in this case
			throw new ExpressException(
					"Could not enable OpenShift on project \"{0}\". There already is a remote called \"{1}\" that points to a different git repository.",
					project.getName(), remoteName);
		}

		EGitUtils.addRemoteTo(getRemoteName(), getApplication().getGitUrl(), repository);
	}

	protected IResource setupOpenShiftMavenProfile(IApplication application, IProject project, IProgressMonitor monitor)
			throws CoreException {
		if (!OpenShiftMavenProfile.isMavenProject(project)) {
			return null;
		}

		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(project, ExpressUIActivator.PLUGIN_ID);
		if (profile.existsInPom()) {
			return null;
		}

		profile.addToPom(project.getName(), getDeployFolder(application));
		return profile.savePom(monitor);
	}

	private String getDeployFolder(IApplication application) {
		DeployFolder deployFolder = DeployFolder.getByCartridgeName(application.getCartridge().getName());
		if (deployFolder == null) {
			return null;
		}
		return deployFolder.getDeployFolder();
	}

	protected List<IResource> setupMarkers(IProject project, IProgressMonitor monitor) throws CoreException {
		List<IResource> newMarkers = new ArrayList<>();
		for (IOpenShiftMarker marker : markers) {
			IFile file = marker.addTo(project, monitor);
			if (file != null) {
				newMarkers.add(file);
			}
		}
		return newMarkers;
	}

	protected IResource addSettingsFile(IProject project, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind("Adding settings to project {0}", project.getName()));
		// This is our project
		IApplication app = getApplication();
		// Add the settings here!
		ExpressServerUtils.updateOpenshiftProjectSettings(project, app, app.getDomain(), getUser(), getRemoteName(),
				ExpressServerUtils.getDefaultDeployFolder(app));
		return (IResource) project.getFolder(".settings");
	}

	protected IProject getSettingsProject(List<IProject> importedProjects) {
		if (importedProjects == null) {
			return null;
		}
		IProject mainProject = null;
		if (importedProjects.size() == 1) {
			if (EGitUtils.hasDotGitFolder(importedProjects.get(0))) {
				mainProject = importedProjects.get(0);
			}
		} else {
			for (IProject project : importedProjects) {
				if (EGitUtils.hasDotGitFolder(project)) {
					mainProject = project;
					break;
				}
			}
		}

		return mainProject;
	}
}
