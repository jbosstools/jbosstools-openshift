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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ServerUserAdaptable;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.ImportNewProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.MergeIntoGitSharedProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.MergeIntoUnsharedProject;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
class OpenShiftApplicationWizardModel extends ObservableUIPojo implements IOpenShiftWizardModel {

	private static final String KEY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	private static final String DEFAULT_APPLICATION = "default_application";
	private static final String DEFAULT_USE_EXISTING_APPLICATION = "default_useExistingApplication";
	
	protected HashMap<String, Object> dataModel = new HashMap<String, Object>();

	public OpenShiftApplicationWizardModel(Connection connection) {
		this(connection, null, null, false);
	}

	public OpenShiftApplicationWizardModel(Connection connection, IProject project, IApplication application,
			boolean useExistingApplication) {
		// default value(s)
		setProject(project);
		setDefaultApplication(application);
		setDefaultUseExistingApplication(useExistingApplication);
		setConnection(connection);
	}

	/**
	 * Imports the project that the user has chosen into the workspace.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @return 
	 * @throws OpenShiftException
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 * @throws IOException
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	@Override
	public IProject importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException,
			URISyntaxException, InvocationTargetException, IOException, NoWorkTreeException, GitAPIException {
		IProject project =
				new ImportNewProject(
						getProjectName()
						, getApplication()
						, getRemoteName()
						, getRepositoryFile()
						, getConnection())
						.execute(monitor);
		setProject(project);
		return project;
	}

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), shares the
	 * user project with git and creates the server adapter.
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
	 */
	@Override
	public IProject mergeIntoUnsharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException {
		IProject project = new MergeIntoUnsharedProject(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, getConnection())
				.execute(monitor);
		setProject(project);
		return project;
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
	public IProject mergeIntoGitSharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException, NoWorkTreeException, GitAPIException {
		IProject project = new MergeIntoGitSharedProject(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, getConnection())
				.execute(monitor);
		setProject(project);
		return project;
	}

	@Override
	public IServer createServerAdapter(IProgressMonitor monitor) throws OpenShiftException {
		IServer server = null;
		if (isCreateServerAdapter()) {
			IProject project = getProject();
			if (project == null) {
				throw new OpenShiftException(
						"Could not create a server adapter for your application {0}. No project was found when importing",
						getApplication().getName());
			}
			server = new OpenShiftServerAdapterFactory().create(project, this, monitor);
			setServerAdapter(server);
		}
		return server;
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
		return (IApplication) getProperty(PROP_APPLICATION);
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
		setProperty(PROP_APPLICATION, application);
		setUseExistingApplication(application);
		setApplicationCartridge(application);
		setApplicationName(application);
		setApplicationScaling(application);
		setApplicationGearProfile(application);

	}

	@Override
	public String setRemoteName(String remoteName) {
		setProperty(PROP_REMOTE_NAME, remoteName);
		return remoteName;
	}

	@Override
	public String getRemoteName() {
		return (String) getProperty(PROP_REMOTE_NAME);
	}

	@Override
	public String setRepositoryPath(String repositoryPath) {
		return (String) setProperty(PROP_REPOSITORY_PATH, repositoryPath);
	}

	@Override
	public String getRepositoryPath() {
		return (String) getProperty(PROP_REPOSITORY_PATH);
	}

	@Override
	public boolean isNewProject() {
		return (Boolean) getProperty(PROP_NEW_PROJECT);
	}

	@Override
	public Boolean setNewProject(boolean newProject) {
		return (Boolean) setProperty(PROP_NEW_PROJECT, newProject);
	}

	@Override
	public String setProjectName(String projectName) {
		return (String) setProperty(PROP_PROJECT_NAME, projectName);
	}

	@Override
	public IProject setProject(IProject project) {
		if (project != null) { 
			setProjectName(project.getName());
		} else {
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
		if (StringUtils.isEmpty(projectName)) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	@Override
	public Boolean setCreateServerAdapter(Boolean createServerAdapter) {
		return (Boolean) setProperty(PROP_CREATE_SERVER_ADAPTER, createServerAdapter);
	}

	@Override
	public String getProjectName() {
		return (String) getProperty(PROP_PROJECT_NAME);
	}

	@Override
	public String setMergeUri(String mergeUri) {
		return (String) setProperty(PROP_MERGE_URI, mergeUri);
	}

	@Override
	public String getMergeUri() {
		return (String) getProperty(PROP_MERGE_URI);
	}

	@Override
	public IRuntime getRuntime() {
		return (IRuntime) getProperty(PROP_RUNTIME_DELEGATE);
	}

	@Override
	public boolean isCreateServerAdapter() {
		Boolean isCreateServer = (Boolean) getProperty(PROP_CREATE_SERVER_ADAPTER);
		return isCreateServer != null && isCreateServer.booleanValue();
	}

	@Override
	public IServerType getServerType() {
		return (IServerType) getProperty(PROP_SERVER_TYPE);
	}

	@Override
	public IServerType setServerType(IServerType serverType) {
		return (IServerType) setProperty(PROP_SERVER_TYPE, serverType);
	}

	@Override
	public boolean isUseExistingApplication() {
		return (Boolean) getProperty(PROP_USE_EXISTING_APPLICATION);
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
		Boolean isUseExistingApplication = (Boolean) setProperty(PROP_USE_EXISTING_APPLICATION, useExistingApplication);
		return isUseExistingApplication != null && isUseExistingApplication;
	}

	protected void setUseExistingApplication(IApplication application) {
		setUseExistingApplication(application != null);
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return (ApplicationScale) getProperty(PROP_APPLICATION_SCALE);
	}

	@Override
	public ApplicationScale setApplicationScale(final ApplicationScale scale) {
		return (ApplicationScale) setProperty(PROP_APPLICATION_SCALE, scale);
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
	public IStandaloneCartridge setApplicationCartridge(IStandaloneCartridge cartridge) {
		return (IStandaloneCartridge) setProperty(PROP_APPLICATION_CARTRIDGE, cartridge);
	}

	protected void setApplicationCartridge(IApplication application) {
		if (application == null) {
			return;
		}
		setApplicationCartridge(application.getCartridge());
	}

	@Override
	public IGearProfile getApplicationGearProfile() {
		return (IGearProfile) getProperty(PROP_APPLICATION_GEAR_PROFILE);
	}

	@Override
	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile) {
		return (IGearProfile) setProperty(PROP_APPLICATION_GEAR_PROFILE, gearProfile);
	}

	protected void setApplicationGearProfile(IApplication application) {
		if (application != null) {
			setApplicationGearProfile(application.getGearProfile());
		}
	}

	@Override
	public IStandaloneCartridge getApplicationCartridge() {
		return (IStandaloneCartridge) getProperty(PROP_APPLICATION_CARTRIDGE);
	}

	@Override
	public String setApplicationName(String applicationName) {
		return (String) setProperty(PROP_APPLICATION_NAME, applicationName);
	}

	protected void setApplicationName(IApplication application) {
		if (application == null) {
			return;
		}
		setApplicationName(application.getName());
	}

	@Override
	public String getApplicationName() {
		return (String) getProperty(PROP_APPLICATION_NAME);
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

	@Override
	public Connection setConnection(Connection connection) {
		setProperty(PROP_CONNECTION, connection);
		resetWizardModel();
		return connection;
	}
	
	@Override
	public Connection getConnection() {
		return (Connection) getProperty(PROP_CONNECTION);
	}

	protected IServer setServerAdapter(IServer server) {
		return (IServer) setProperty(PROP_SERVER_ADAPTER, server);
	}

	protected IServer getServerAdapter() {
		return (IServer) getProperty(PROP_SERVER_ADAPTER);
	}
	
	protected boolean hasServerAdapter() {
		return getServerAdapter() != null;
	}

	@Override
	public IStatus publishServerAdapter(IProgressMonitor monitor) {
		if (!hasServerAdapter()) {
			return Status.OK_STATUS;
		}
				
		IServer server = getServerAdapter();
		server.publish(IServer.PUBLISH_FULL, null, new ServerUserAdaptable(), null);
		return Status.OK_STATUS;
	}
	
	public void resetWizardModel() {
		setApplication(getDefaultApplication());
		setUseExistingApplication(getDefaultUseExistingApplication());
		setSelectedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>());
		setNewProject(true);
		setCreateServerAdapter(true);
		setRepositoryPath(IOpenShiftWizardModel.DEFAULT_REPOSITORY_PATH);
		setRemoteName(IOpenShiftWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT);
		setServerType(ServerCore.findServerType(OpenShiftServerUtils.OPENSHIFT_SERVER_TYPE));
		setServerAdapter(null);
	}			

	public void fireConnectionChanged() {
		ConnectionsModelSingleton.getInstance().fireConnectionChanged(getConnection());
	}

	public void updateRecentConnection() {
		if (getConnection() == null) {
			return;
		}
		
		ConnectionsModelSingleton.getInstance().setRecent(getConnection());
		
	}

}