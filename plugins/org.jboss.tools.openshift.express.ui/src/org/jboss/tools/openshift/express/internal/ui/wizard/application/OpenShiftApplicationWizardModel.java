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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ServerUserAdaptable;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.ImportNewProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.MergeIntoGitSharedProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.MergeIntoUnsharedProject;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
class OpenShiftApplicationWizardModel extends ObservablePojo implements IOpenShiftApplicationWizardModel {

	protected HashMap<String, Object> dataModel = new HashMap<String, Object>();

	public OpenShiftApplicationWizardModel(Connection connection, IDomain domain) {
		this(connection, domain, null, null, false);
	}

	public OpenShiftApplicationWizardModel(Connection connection, IDomain domain, IApplication application, IProject project, 
			boolean useExistingApplication) {
		setProject(project);
		setDomain(domain);
		setApplication(application);
		setUseExistingApplication(useExistingApplication);
		setConnection(connection);
		setEnvironmentVariables(new LinkedHashMap<String, String>());
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
		IProject project = new ImportNewProject(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, getRepositoryFile()
				, getMarkers()
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
				, getMarkers()
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
				, getMarkers()
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
			server = new OpenShiftServerAdapterFactory().create(project, getApplication(), getDomain(), monitor);
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
	public boolean hasDomain() {
		return getDomain() != null;
	}
	
	@Override
	public IDomain setDomain(IDomain domain) {
		return setProperty(PROP_DOMAIN, domain);
	}

	@Override
	public IDomain getDomain() {
		return getProperty(PROP_DOMAIN);
	}
	
	@Override
	public void ensureHasDomain() {
		Assert.isNotNull(getConnection());
		if (getDomain() == null) {
			setDomain(getConnection().getDefaultDomain());
		}
	}

	@Override
	public List<IDomain> setDomains(List<IDomain> domains) {
		return setProperty(PROP_DOMAINS, domains);
	}

	@Override
	public List<IDomain> getDomains() {
		return getProperty(PROP_DOMAINS);
	}

	@Override
	public IApplication getApplication() {
		return getProperty(PROP_APPLICATION);
	}

	@Override
	public IApplication setApplication(IApplication application) {
		setProperty(PROP_APPLICATION, application);
		setApplicationName(application);
		return application;
	}

	@Override
	public String setRemoteName(String remoteName) {
		return (String) setProperty(PROP_REMOTE_NAME, remoteName);
	}

	@Override
	public String getRemoteName() {
		return getProperty(PROP_REMOTE_NAME);
	}

	@Override
	public String setRepositoryPath(String repositoryPath) {
		return setProperty(PROP_REPOSITORY_PATH, repositoryPath);
	}

	@Override
	public String getRepositoryPath() {
		return getProperty(PROP_REPOSITORY_PATH);
	}

	@Override
	public boolean isNewProject() {
		return getBooleanProperty(PROP_NEW_PROJECT);
	}

	@Override
	public Boolean setNewProject(boolean newProject) {
		return setProperty(PROP_NEW_PROJECT, newProject);
	}

	@Override
	public String setProjectName(String projectName) {
		return setProperty(PROP_PROJECT_NAME, projectName);
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
		return setProperty(PROP_CREATE_SERVER_ADAPTER, createServerAdapter);
	}

	@Override
	public boolean isCreateServerAdapter() {
		return getBooleanProperty(PROP_CREATE_SERVER_ADAPTER);
	}

	@Override
	public Boolean setSkipMavenBuild(Boolean skipMavenBuild) {
		return setProperty(PROP_SKIP_MAVEN_BUILD, skipMavenBuild);
	}

	@Override
	public boolean isSkipMavenBuild() {
		return getBooleanProperty(PROP_SKIP_MAVEN_BUILD);
	}

	private List<IOpenShiftMarker> getMarkers() {
		List<IOpenShiftMarker> markers = new ArrayList<IOpenShiftMarker>();
		if (isSkipMavenBuild()) {
			markers.add(IOpenShiftMarker.SKIP_MAVEN_BUILD);
		}
		return markers;
	}
	
	@Override
	public String getProjectName() {
		return getProperty(PROP_PROJECT_NAME);
	}

	@Override
	public String setMergeUri(String mergeUri) {
		return setProperty(PROP_MERGE_URI, mergeUri);
	}

	@Override
	public String getMergeUri() {
		return getProperty(PROP_MERGE_URI);
	}

	@Override
	public boolean isUseExistingApplication() {
		return getBooleanProperty(PROP_USE_EXISTING_APPLICATION);
	}

	@Override
	public boolean setUseExistingApplication(boolean useExistingApplication) {
		return setProperty(PROP_USE_EXISTING_APPLICATION, useExistingApplication);
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return getProperty(PROP_APPLICATION_SCALE);
	}

	@Override
	public ApplicationScale setApplicationScale(final ApplicationScale scale) {
		return setProperty(PROP_APPLICATION_SCALE, scale);
	}

	protected void setApplicationScale(IApplication application) {
		ApplicationScale scale = null;
		if (application != null) {
			scale = application.getApplicationScale();
		}
		setApplicationScale(scale);
	}

	@Override
	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() {
		Set<IEmbeddableCartridge> selectedEmbeddableCartridges =
				getProperty(PROP_SELECTED_EMBEDDABLE_CARTRIDGES, Collections.<IEmbeddableCartridge> emptySet());
		return selectedEmbeddableCartridges;
	}

	@Override 
	public boolean hasEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		return getSelectedEmbeddableCartridges().contains(cartridge);
	}
	
	@Override
	public Set<IEmbeddableCartridge> setSelectedEmbeddableCartridges(
			Set<IEmbeddableCartridge> selectedEmbeddableCartridges) {
		return setProperty(PROP_SELECTED_EMBEDDABLE_CARTRIDGES, selectedEmbeddableCartridges);
	}

	@Override
	public void removeSelectedEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		Set<IEmbeddableCartridge> selectedEmbeddableCartridges = getSelectedEmbeddableCartridges();
		selectedEmbeddableCartridges .remove(cartridge);
		firePropertyChange(PROP_SELECTED_EMBEDDABLE_CARTRIDGES, null, selectedEmbeddableCartridges);
	}
	
	@Override
	public List<IEmbeddableCartridge> getEmbeddableCartridges() {
		return getProperty(PROP_EMBEDDABLE_CARTRIDGES, Collections.<IEmbeddableCartridge> emptyList());
	}

	@Override
	public List<IEmbeddableCartridge> setEmbeddableCartridges(List<IEmbeddableCartridge> embeddableCartridges) {
		return setProperty(PROP_EMBEDDABLE_CARTRIDGES, embeddableCartridges);
	}

	protected void setDomain(IApplication application) {
		IDomain domain = null;
		if (application != null) {
			domain = application.getDomain();
		} 
		setDomain(domain);
	}

	@Override
	public IStandaloneCartridge getStandaloneCartridge() {
		IStandaloneCartridge cartridge = null;
		IApplicationTemplate template = getSelectedApplicationTemplate();
		if (template instanceof ICartridgeApplicationTemplate) {
			cartridge = ((ICartridgeApplicationTemplate) template).getCartridge();
		}
		return cartridge;
	}

	@Override
	public IGearProfile getApplicationGearProfile() {
		return getProperty(PROP_APPLICATION_GEAR_PROFILE);
	}

	@Override
	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile) {
		return setProperty(PROP_APPLICATION_GEAR_PROFILE, gearProfile);
	}

	protected void setApplicationGearProfile(IApplication application) {
		IGearProfile profile = null;
		if (application != null) {
			profile = application.getGearProfile();
		}
		setApplicationGearProfile(profile);
	}

	@Override
	public List<IStandaloneCartridge> setStandaloneCartridges(List<IStandaloneCartridge> cartridges) {
		return setProperty(PROP_STANDALONE_CARTRIDGES, cartridges);
	}

	@Override
	public List<IStandaloneCartridge> getStandaloneCartridges() {
		return getProperty(PROP_STANDALONE_CARTRIDGES);
	}
	
	@Override
	public String setApplicationName(String applicationName) {
		return (String) setProperty(PROP_APPLICATION_NAME, applicationName);
	}

	protected void setApplicationName(IApplication application) {
		String applicationName = null;
		if (application != null) {
			applicationName = application.getName();
		}
		setApplicationName(applicationName);
	}

	@Override
	public String getApplicationName() {
		return getProperty(PROP_APPLICATION_NAME);
	}

	@Override
	public String getInitialGitUrl() {
		return getProperty(PROP_INITIAL_GITURL);
	}
	
	@Override
	public String setInitialGitUrl(String initialGitUrl) {
		return setProperty(PROP_INITIAL_GITURL, initialGitUrl);
	}

	@Override
	public boolean hasConnection() {
		return getConnection() != null;
	}

	@Override
	public Connection setConnection(Connection connection) {
		update(connection);
		setProperty(PROP_CONNECTION, connection);
		return connection;
	}
	
	@Override
	public Connection getConnection() {
		return getProperty(PROP_CONNECTION);
	}

	protected IServer setServerAdapter(IServer server) {
		return setProperty(PROP_SERVER_ADAPTER, server);
	}

	protected IServer getServerAdapter() {
		return getProperty(PROP_SERVER_ADAPTER);
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
	
	/**
	 * Updates this wizard model for the given connection. All settings are
	 * either used as is (if still valid for the given connection) or resetted.
	 * 
	 * @param connection
	 */
	public void update(Connection connection) {
		if (!connection.isConnected()) {
			return;
		}
		
		if (!connection.hasDomain()) {
			IDomain domain = getDomain();
			domain = connection.getFirstDomain();
			setDomain(domain);
		}
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
	
	@Override
	public IApplicationTemplate getSelectedApplicationTemplate() {
		return getProperty(PROP_SELECTED_APPLICATION_TEMPLATE);
	}

	@Override
	public IApplicationTemplate setSelectedApplicationTemplate(IApplicationTemplate template) {
		setProperty(PROP_SELECTED_APPLICATION_TEMPLATE, template);
		setUseExistingApplication(false);
		return template;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return getProperty(PROP_ENVIRONMENT_VARIABLES);
	}

	@Override
	public Map<String, String> setEnvironmentVariables(Map<String, String> environmentVariables) {
		return (Map<String, String>) setProperty(PROP_ENVIRONMENT_VARIABLES, environmentVariables);
	}


	public boolean isValid(Connection connection) {
		return connection != null
				&& connection.isConnected();
	}

	private <V> V setProperty(String key, V value) {
		Object oldVal = dataModel.get(key);
		dataModel.put(key, value);
		firePropertyChange(key, oldVal, value);
		return value;
	}

	private <E> E getProperty(String key) {
		return getProperty(key, null);
	}

	private <V> V getProperty(String key, V defaultValue) {
		@SuppressWarnings("unchecked")
		V value = (V) dataModel.get(key);
		if (value == null) {
			return value = defaultValue;
		}
		return value;
	}

	private boolean getBooleanProperty(String name) {
		Boolean binaryValue = (Boolean) getProperty(name);
		return binaryValue != null 
				&& binaryValue.booleanValue();
	}
}
