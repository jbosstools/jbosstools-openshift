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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.internal.util.ComponentUtilities;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.GitIgnore;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.common.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.common.MavenImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.common.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.wizard.projectimport.GeneralProjectImportOperation;
import org.jboss.tools.openshift.express.internal.ui.wizard.projectimport.MavenProjectImportOperation;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportProjectWizardModel extends ObservableUIPojo {

	private static final int CLONE_TIMEOUT = 10 * 1024;

	private HashMap<String, Object> dataModel = new HashMap<String, Object>();

	public static final String NEW_PROJECT = "enableProject";
	public static final String USER = "user";
	public static final String APPLICATION = "application";
	public static final String REMOTE_NAME = "remoteName";
	public static final String REPOSITORY_PATH = "repositoryPath";
	public static final String PROJECT_NAME = "projectName";
	public static final String MERGE_URI = "mergeUri";

	public ImportProjectWizardModel() {
		dataModel.put(NEW_PROJECT, false);
	}

	/**
	 * Returns the destination folder that the OpenShift application will get
	 * cloned to.
	 * 
	 * @return the destination that the application will get cloned to.
	 * 
	 * @see #setRepositoryPath(String)
	 * @see #getRepositoryPath()
	 * @see #getApplicationName()
	 */
	public File getCloneDestination() {
		String repositoryPath = getRepositoryPath();
		if (repositoryPath == null
				|| repositoryPath.length() == 0) {
			return null;
		}
		return new File(repositoryPath, getApplicationName());
	}

	private void shareProject(IProject project, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Sharing project {0}...", project.getName()));
		EGitUtils.share(project, monitor);
	}

	/**
	 * Copies the openshift configuration from the given source folder to the
	 * given project.
	 * 
	 * @param sourceFolder
	 *            the source to copy the openshift config from
	 * @param project
	 *            the project to copy the configuration to.
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws IOException
	 */
	private void copyOpenshiftConfiguration(final File sourceFolder, IProject project, IProgressMonitor monitor)
			throws IOException {
		Assert.isLegal(project != null);
		File projectFolder = project.getLocation().toFile();
		monitor.subTask(NLS.bind("Copying openshift configuration to project {0}...", project.getName()));
		FileUtils.copy(new File(sourceFolder, ".git"), projectFolder, false);
		FileUtils.copy(new File(sourceFolder, ".openshift"), projectFolder, false);
		FileUtils.copy(new File(sourceFolder, "deployments"), projectFolder, false);
		FileUtils.copy(new File(sourceFolder, "pom.xml"), projectFolder, false);
		createGitIgnore(projectFolder);
	}

	/**
	 * Creates the git ignore file with a predefined set of entries. An existing
	 * .gitignore file is not overwritten, we then just dont do anything.
	 * 
	 * @param projectFolder
	 * @throws IOException
	 */
	private void createGitIgnore(File projectFolder) throws IOException {
		GitIgnore gitIgnore = new GitIgnore(projectFolder);
		// TODO: merge existing .gitignore
		// (https://issues.jboss.org/browse/JBIDE-10391)
		if (gitIgnore.exists()) {
			return;
		}
		gitIgnore.add("target")
				.add(".settings")
				.add(".project")
				.add(".classpath")
				.add(".factorypath");
		gitIgnore.write(false);
	}

	/**
	 * Imports the project that the user has chosen into the workspace.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws OpenShiftException
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 * 
	 * @see #setProjectName(String)
	 * @see #getProjectName()
	 * @see #
	 */
	public void importProject(IProgressMonitor monitor)
			throws OpenShiftException, CoreException, InterruptedException, URISyntaxException,
			InvocationTargetException {
		File cloneDestination = getCloneDestination();
		if (cloneDestination != null
				&& cloneDestination.exists()) {
			throw new WontOverwriteException(
					NLS.bind(
							"There's already a folder at {0}. The new OpenShift project would overwrite it. " +
									"Please choose another destination to clone to.",
							cloneDestination.getAbsolutePath()));
		}
		File repositoryFolder = cloneRepository(getApplication(), getRemoteName(), getCloneDestination(), monitor);
		List<IProject> importedProjects = importProjectsFrom(repositoryFolder, monitor);
		if (importedProjects.size() == 0) {
			throw new MavenImportFailedException(
					"The maven import failed. One of the possible reasons is that there's already a project " +
							"in your workspace that matches the maven name of the OpenShift application. " +
							"Please rename your workspace project in that case and start over again.");
		}

		connectToGitRepo(importedProjects, repositoryFolder, monitor);
		if (isCreateServer()) {
			createServerAdapter(
					importedProjects, getServerType(), getRuntime(), getMode(), getApplication(), getUser(), monitor);
		}
	}

	/**
	 * Enables the user chosen project to be used on the chosen OpenShift
	 * application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), shares the
	 * user project with git and creates the server adapter.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
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
	public void addToExistingProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException {
		// File repositoryFile =
		// model.cloneRepository(monitor);
		// model.importProject(repositoryFile, monitor);
		// Repository repository =
		// model.shareProject(monitor);
		// model.mergeWithApplicationRepository(repository,
		// monitor);
		IApplication application = getApplication();
		File tmpFolder = FileUtils.getRandomTmpFolder();
		File repositoryFile = cloneRepository(application, getRemoteName(), tmpFolder, monitor);
		String projectName = getProjectName();
		IProject project = getProject(projectName);
		copyOpenshiftConfiguration(repositoryFile, project, monitor);
		FileUtil.safeDelete(tmpFolder);

		shareProject(project, monitor);
		if (isCreateServer()) {
			createServerAdapter(
					project, getServerType(), getRuntime(), getMode(), application, getUser(), monitor);
		}
	}

	@SuppressWarnings("unused")
	private void mergeWithApplicationRepository(Repository repository, IProgressMonitor monitor)
			throws MalformedURLException, URISyntaxException, IOException, OpenShiftException, CoreException,
			InvocationTargetException {
		String uri = getApplication().getGitUri();
		EGitUtils.addRemoteTo("openshift", new URIish(uri), repository);
		EGitUtils.mergeWithRemote(new URIish(uri), "refs/remotes/openshift/HEAD", repository, monitor);
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

	/**
	 * Clones the repository of the selected OpenShift application to the user
	 * provided path
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws URISyntaxException
	 * @throws OpenShiftException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * 
	 * @see ImportProjectWizardModel#getApplication()
	 * @see #getRepositoryPath()
	 */
	private File cloneRepository(IApplication application, String remoteName, File destination, IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning repository for application {0}...", application.getName()));
		cloneRepository(application.getGitUri(), remoteName, destination, monitor);
		return destination;
	}

	private void cloneRepository(String uri, String remoteName, File destination, IProgressMonitor monitor)
			throws OpenShiftException, URISyntaxException, InvocationTargetException, InterruptedException {
		ensureEgitUIIsStarted();
		URIish gitUri = new URIish(uri);
		CloneOperation cloneOperation =
				new CloneOperation(gitUri, true, null, destination, Constants.HEAD, remoteName, CLONE_TIMEOUT);
		cloneOperation.run(monitor);
		RepositoryUtil repositoryUtil = Activator.getDefault().getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(new File(destination, Constants.DOT_GIT));
	}

	/**
	 * Returns the workspace project with the given name if it exists. Returns
	 * <code>null</code> otherwise.
	 * 
	 * @param name
	 *            the project name
	 * @return the project with the given name
	 */
	public IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.isTrue(project != null && project.exists(),
				NLS.bind("Could not find project {0} in your workspace.", name));
		return project;
	}

	/**
	 * The EGit UI plugin initializes the ssh factory to present the user a
	 * passphrase prompt if the ssh key was not read yet. If this initialization
	 * is not executed, the ssh connection to the git repo would just fail with
	 * an authentication error. We therefore have to make sure that the EGit UI
	 * plugin is started and initializes the JSchConfigSessionFactory.
	 * <p>
	 * EGit initializes the SshSessionFactory with the EclipseSshSessionFactory.
	 * The EclipseSshSessionFactory overrides JschConfigSessionFactory#configure
	 * to present a UserInfoPrompter if the key passphrase was not entered
	 * before.
	 * 
	 * @see Activator#start(org.osgi.framework.BundleContext)
	 * @see Activator#setupSSH
	 * @see JschConfigSessionFactory#configure
	 * @see EclipseSshSessionFactory#configure
	 */
	private void ensureEgitUIIsStarted() {
		Activator.getDefault();
	}

	/**
	 * creates an OpenShift server adapter for the user chosen project.
	 * 
	 * @param monitor
	 *            the monitor to report progress to.
	 * @throws OpenShiftException
	 */
	private void createServerAdapter(IProject project, IServerType serverType, IRuntime runtime, String mode,
			IApplication application, IUser user, IProgressMonitor monitor) throws OpenShiftException {
		String name = project.getName();
		monitor.subTask(NLS.bind("Creating server adapter for project {0}", name));
		createServerAdapter(Collections.singletonList(project), serverType, runtime, mode, application, user,
				monitor);
	}

	private void createServerAdapter(List<IProject> importedProjects, IServerType serverType,
			IRuntime runtime, String mode, IApplication application, IUser user, IProgressMonitor monitor) {
		try {
			renameWebContextRoot(importedProjects);
			IServer server = doCreateServerAdapter(serverType, runtime, mode, application, user);
			addModules(getModules(importedProjects), server, monitor);
		} catch (CoreException ce) {
			OpenShiftUIActivator.getDefault().getLog().log(ce.getStatus());
		} catch (OpenShiftException ose) {
			IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Cannot create openshift server adapter", ose);
			OpenShiftUIActivator.getDefault().getLog().log(s);
		}
	}

	private void renameWebContextRoot(List<IProject> importedProjects) {
		for (IProject project : importedProjects) {
			ComponentUtilities.setServerContextRoot(project, "/");
		}
	}

	private IServer doCreateServerAdapter(IServerType serverType, IRuntime rt, String mode, IApplication application,
			IUser user) throws CoreException,
			OpenShiftException {
		Assert.isLegal(serverType != null);
		Assert.isLegal(rt != null);
		Assert.isLegal(mode != null);
		Assert.isLegal(application != null);
		Assert.isLegal(user != null);

		String serverNameBase = application.getName() + " OpenShift Server";
		String serverName = org.jboss.ide.eclipse.as.core.util.ServerUtil.getDefaultServerName(serverNameBase);

		IServer server = ExpressServerUtils.createServer(rt, serverType, serverName);
		ExpressServerUtils.fillServerWithOpenShiftDetails(server, application.getApplicationUrl(),
				user.getRhlogin(), user.getPassword(),
				user.getDomain().getNamespace(), application.getName(), application.getUUID(), mode);
		return server;
	}

	private void addModules(List<IModule> modules, IServer server, IProgressMonitor monitor) throws CoreException {
		if (modules == null
				|| modules.size() == 0) {
			return;
		}
		IServerWorkingCopy wc = server.createWorkingCopy();
		IModule[] add = modules.toArray(new IModule[modules.size()]);
		wc.modifyModules(add, new IModule[0], new NullProgressMonitor());
		server = wc.save(true, monitor);
		((Server) server).setModulePublishState(add, IServer.PUBLISH_STATE_NONE);
	}

	private List<IModule> getModules(List<IProject> importedProjects) {
		Iterator<IProject> i = importedProjects.iterator();
		ArrayList<IModule> toAdd = new ArrayList<IModule>();
		while (i.hasNext()) {
			IProject p = i.next();
			IModule[] m = ServerUtil.getModules(p);
			if (m != null && m.length > 0) {
				toAdd.addAll(Arrays.asList(m));
			}
		}
		return toAdd;
	}

	public Object setProperty(String key, Object value) {
		Object oldVal = dataModel.get(key);
		dataModel.put(key, value);
		firePropertyChange(key, oldVal, value);
		return value;
	}

	public Object getProperty(String key) {
		return dataModel.get(key);
	}

	public void setUser(IUser user) {
		setProperty(USER, user);
	}

	public IUser getUser() {
		return (IUser) getProperty(USER);
	}

	public IApplication getApplication() {
		return (IApplication) getProperty(APPLICATION);
	}

	public String getApplicationName() {
		String applicationName = null;
		IApplication application = getApplication();
		if (application != null) {
			applicationName = application.getName();
		}
		return applicationName;
	}

	public ICartridge getApplicationCartridge() {
		ICartridge cartridge = null;
		IApplication application = getApplication();
		if (application != null) {
			cartridge = application.getCartridge();
		}
		return cartridge;
	}

	public String getApplicationCartridgeName() {
		String cartridgeName = null;
		ICartridge cartridge = getApplicationCartridge();
		if (cartridge != null) {
			cartridgeName = cartridge.getName();
		}
		return cartridgeName;
	}

	public void setApplication(IApplication application) {
		setProperty(APPLICATION, application);
	}

	public String setRemoteName(String remoteName) {
		setProperty(REMOTE_NAME, remoteName);
		return remoteName;
	}

	public String getRemoteName() {
		return (String) getProperty(REMOTE_NAME);
	}

	public String setRepositoryPath(String repositoryPath) {
		return (String) setProperty(REPOSITORY_PATH, repositoryPath);
	}

	public String getRepositoryPath() {
		return (String) getProperty(REPOSITORY_PATH);
	}

	public boolean isNewProject() {
		return (Boolean) getProperty(NEW_PROJECT);
	}

	public Boolean setNewProject(boolean newProject) {
		return (Boolean) setProperty(NEW_PROJECT, newProject);
	}

	public String setProjectName(String projectName) {
		return (String) setProperty(PROJECT_NAME, projectName);
	}

	public String getProjectName() {
		return (String) getProperty(PROJECT_NAME);
	}

	public String setMergeUri(String mergeUri) {
		return (String) setProperty(MERGE_URI, mergeUri);
	}

	public String getMergeUri() {
		return (String) getProperty(MERGE_URI);
	}

	public IServerType getServerType() {
		return (IServerType) getProperty(AdapterWizardPageModel.SERVER_TYPE);
	}

	public IRuntime getRuntime() {
		return (IRuntime) getProperty(AdapterWizardPageModel.RUNTIME_DELEGATE);
	}

	public String getMode() {
		return (String) getProperty(AdapterWizardPageModel.MODE);
	}

	public boolean isCreateServer() {
		Boolean isCreateServer = (Boolean) getProperty(AdapterWizardPageModel.CREATE_SERVER);
		return isCreateServer != null
				&& isCreateServer.booleanValue();
	}
}
