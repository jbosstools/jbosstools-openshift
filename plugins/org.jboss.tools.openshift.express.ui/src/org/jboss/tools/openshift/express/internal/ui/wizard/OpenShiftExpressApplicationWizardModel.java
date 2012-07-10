package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.appimport.ConfigureGitSharedProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.appimport.ConfigureUnsharedProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.appimport.ImportNewProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.appimport.ServerAdapterFactory;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;

public class OpenShiftExpressApplicationWizardModel extends ObservableUIPojo implements IOpenShiftExpressWizardModel {

	protected HashMap<String, Object> dataModel = new HashMap<String, Object>();

	private static final String KEY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	private static final String DEFAULT_APPLICATION = "default_application";
	private static final String DEFAULT_PROJECT = "default_project";
	private static final String DEFAULT_USE_EXISTING_APPLICATION = "default_useExistingApplication";
	
	public OpenShiftExpressApplicationWizardModel(UserDelegate user) {
		this(user, null, null, false);
	}

	public OpenShiftExpressApplicationWizardModel(UserDelegate user, IProject project, IApplication application,
			boolean useExistingApplication) {
		// default value(s)
		setDefaultProject(project);
		setDefaultApplication(application);
		setDefaultUseExistingApplication(useExistingApplication);
		setUser(user);
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
	 * @throws IOException
	 */
	@Override
	public void importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException,
			URISyntaxException, InvocationTargetException, IOException {
		IProject importedProject =
				new ImportNewProject(
						getProjectName()
						, getApplication()
						, getRemoteName()
						, getRepositoryFile()
						, getUser())
						.execute(monitor);
		createServerAdapter(monitor, importedProject);
	}

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
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
	 */
	@Override
	public void configureUnsharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException {
		IProject importedProject = new ConfigureUnsharedProject(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, getUser())
				.execute(monitor);
		createServerAdapter(monitor, importedProject);
	}

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), adds the
	 * appication git repo as remote and creates the server adapter.
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
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	@Override
	public void configureGitSharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException, NoWorkTreeException, GitAPIException {
		IProject project = new ConfigureGitSharedProject(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, getUser())
				.execute(monitor);
		createServerAdapter(monitor, project);
	}

	private void createServerAdapter(IProgressMonitor monitor, IProject project)
			throws OpenShiftException {
		if (isCreateServerAdapter()) {
			if (project == null) {
				throw new OpenShiftException(
						"Could not create a server adapter for your application {0}. No project was found when importing",
						getApplication().getName());
			}
			new ServerAdapterFactory().create(project, this, monitor);
		}
	}

	@Override
	public File getRepositoryFile() {
		String repositoryPath = getRepositoryPath();
		if (repositoryPath == null || repositoryPath.length() == 0) {
			return null;
		}
		return new File(repositoryPath, getApplicationName());
	}

	@Override
	public Object setProperty(String key, Object value) {
		Object oldVal = dataModel.get(key);
		dataModel.put(key, value);
		firePropertyChange(key, oldVal, value);
		return value;
	}

	@Override
	public Object getProperty(String key) {
		return dataModel.get(key);
	}

	@Override
	public IApplication getApplication() {
		return (IApplication) getProperty(APPLICATION);
	}

	public void setDefaultApplication(IApplication application) {
		setProperty(DEFAULT_APPLICATION, application);
		setApplication(application);
	}

	public IApplication getDefaultApplication() {
		return (IApplication) getProperty(DEFAULT_APPLICATION);
	}		

	@Override
	public void setApplication(IApplication application) {
		setProperty(APPLICATION, application);
		setUseExistingApplication(application);
		setApplicationCartridge(application);
		setApplicationName(application);
		setApplicationScaling(application);
		setApplicationGearProfile(application);

	}

	@Override
	public String setRemoteName(String remoteName) {
		setProperty(REMOTE_NAME, remoteName);
		return remoteName;
	}

	@Override
	public String getRemoteName() {
		return (String) getProperty(REMOTE_NAME);
	}

	@Override
	public String setRepositoryPath(String repositoryPath) {
		return (String) setProperty(REPOSITORY_PATH, repositoryPath);
	}

	@Override
	public String getRepositoryPath() {
		return (String) getProperty(REPOSITORY_PATH);
	}

	@Override
	public boolean isNewProject() {
		return (Boolean) getProperty(NEW_PROJECT);
	}

	@Override
	public boolean isExistingProject() {
		return !((Boolean) getProperty(NEW_PROJECT));
	}

	@Override
	public Boolean setNewProject(boolean newProject) {
		return (Boolean) setProperty(NEW_PROJECT, newProject);
	}

	@Override
	public Boolean setExistingProject(boolean existingProject) {
		return (Boolean) setProperty(NEW_PROJECT, !existingProject);
	}

	@Override
	public String setProjectName(String projectName) {
		return (String) setProperty(PROJECT_NAME, projectName);
	}

	public IProject setDefaultProject(IProject project) {
		setProperty(DEFAULT_PROJECT, project);
		setProject(project);
		return project;
	}

	public IProject getDefaultProject() {
		return (IProject) getProperty(DEFAULT_PROJECT);
	}		
		
	@Override
	public IProject setProject(IProject project) {
		if (project != null && project.exists()) {
			setExistingProject(false);
			setProjectName(project.getName());
		} else {
			setExistingProject(true);
			setProjectName(null);
		}
		return project;
	}

	@Override
	public boolean isGitSharedProject() {
		return EGitUtils.isSharedWithGit(getProject());
	}

	@Override
	public IProject getProject() {
		String projectName = getProjectName();
		if (projectName == null || projectName.isEmpty()) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
	}

	@Override
	public Boolean setCreateServerAdapter(Boolean createServerAdapter) {
		return (Boolean) setProperty(CREATE_SERVER_ADAPTER, createServerAdapter);
	}

	@Override
	public String getProjectName() {
		return (String) getProperty(PROJECT_NAME);
	}

	@Override
	public String setMergeUri(String mergeUri) {
		return (String) setProperty(MERGE_URI, mergeUri);
	}

	@Override
	public String getMergeUri() {
		return (String) getProperty(MERGE_URI);
	}

	@Override
	public IRuntime getRuntime() {
		return (IRuntime) getProperty(RUNTIME_DELEGATE);
	}

	@Override
	public boolean isCreateServerAdapter() {
		Boolean isCreateServer = (Boolean) getProperty(CREATE_SERVER_ADAPTER);
		return isCreateServer != null && isCreateServer.booleanValue();
	}

	@Override
	public IServerType getServerType() {
		return (IServerType) getProperty(SERVER_TYPE);
	}

	@Override
	public IServerType setServerType(IServerType serverType) {
		return (IServerType) setProperty(SERVER_TYPE, serverType);
	}

	@Override
	public boolean isUseExistingApplication() {
		return (Boolean) getProperty(USE_EXISTING_APPLICATION);
	}

	public boolean setDefaultUseExistingApplication(boolean useExistingApplication) {
		setProperty(DEFAULT_USE_EXISTING_APPLICATION, useExistingApplication);
		setUseExistingApplication(useExistingApplication);
		return useExistingApplication;
	}

	public boolean getDefaultUseExistingApplication() {
		Object useExistingApp = getProperty(DEFAULT_USE_EXISTING_APPLICATION);
		if (useExistingApp != null) {
			return (Boolean) useExistingApp;
		}
		return false;
	}

	@Override
	public boolean setUseExistingApplication(boolean useExistingApplication) {
		Boolean isUseExistingApplication = (Boolean) setProperty(USE_EXISTING_APPLICATION, useExistingApplication);
		return isUseExistingApplication != null && isUseExistingApplication;
	}

	protected void setUseExistingApplication(IApplication application) {
		setUseExistingApplication(application != null);
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return (ApplicationScale) getProperty(APPLICATION_SCALE);
	}

	@Override
	public ApplicationScale setApplicationScale(final ApplicationScale scale) {
		return (ApplicationScale) setProperty(APPLICATION_SCALE, scale);
	}

	protected void setApplicationScaling(IApplication application) {
		if (application != null) {
			setApplicationScale(application.getApplicationScale());
		}
	}

	@Override
	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() {
		@SuppressWarnings("unchecked")
		Set<IEmbeddableCartridge> selectedEmbeddableCartridges =
				(Set<IEmbeddableCartridge>) getProperty(KEY_SELECTED_EMBEDDABLE_CARTRIDGES);
		if (selectedEmbeddableCartridges == null) {
			selectedEmbeddableCartridges = new HashSet<IEmbeddableCartridge>();
			setSelectedEmbeddableCartridges(selectedEmbeddableCartridges);
		}
		return selectedEmbeddableCartridges;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<IEmbeddableCartridge> setSelectedEmbeddableCartridges(
			Set<IEmbeddableCartridge> selectedEmbeddableCartridges) {
		return (Set<IEmbeddableCartridge>) setProperty(KEY_SELECTED_EMBEDDABLE_CARTRIDGES, selectedEmbeddableCartridges);
	}

	@Override
	public ICartridge setApplicationCartridge(ICartridge cartridge) {
		return (ICartridge) setProperty(APPLICATION_CARTRIDGE, cartridge);
	}

	protected void setApplicationCartridge(IApplication application) {
		if (application == null) {
			return;
		}
		setApplicationCartridge(application.getCartridge());
	}

	@Override
	public IGearProfile getApplicationGearProfile() {
		return (IGearProfile) getProperty(APPLICATION_GEAR_PROFILE);
	}

	@Override
	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile) {
		return (IGearProfile) setProperty(APPLICATION_GEAR_PROFILE, gearProfile);
	}

	protected void setApplicationGearProfile(IApplication application) {
		if (application != null) {
			setApplicationGearProfile(application.getGearProfile());
		}
	}

	@Override
	public ICartridge getApplicationCartridge() {
		return (ICartridge) getProperty(APPLICATION_CARTRIDGE);
	}

	@Override
	public String setApplicationName(String applicationName) {
		return (String) setProperty(APPLICATION_NAME, applicationName);
	}

	protected void setApplicationName(IApplication application) {
		if (application == null) {
			return;
		}
		setApplicationName(application.getName());
	}

	@Override
	public String getApplicationName() {
		return (String) getProperty(APPLICATION_NAME);
	}

	@Override
	public UserDelegate getUser() {
		return (UserDelegate) getProperty(USER);
	}

	@Override
	public UserDelegate setUser(UserDelegate user) {
		setProperty(USER, user);
		resetWizardModel();
		return user;
	}

	public void resetWizardModel() {
		setApplication(getDefaultApplication());
		setUseExistingApplication(getDefaultUseExistingApplication());
		setSelectedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>());
		setNewProject(true);
		setCreateServerAdapter(true);
		setRepositoryPath(IOpenShiftExpressWizardModel.DEFAULT_REPOSITORY_PATH);
		setRemoteName(IOpenShiftExpressWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT);
		setServerType(ServerCore.findServerType(ExpressServerUtils.OPENSHIFT_SERVER_TYPE));
	}			

	@Override
	public void addUserToModel() {
		UserDelegate user = getUser();
		Assert.isNotNull(user);
		UserModel.getDefault().addUser(user);
	}

}