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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public interface IOpenShiftWizardModel extends IConnectionAwareModel, IObservablePojo {

	public static final String PROP_APPLICATION = "application";
	public static final String PROP_APPLICATION_NAME = "applicationName";
	public static final String PROP_APPLICATION_CARTRIDGE = "applicationCartridge";
	public static final String PROP_APPLICATION_GEAR_PROFILE = "applicationGearProfile";
	public static final String PROP_APPLICATION_SCALE = "applicationScale";
	public static final String PROP_CREATE_SERVER_ADAPTER = "createServerAdapter";
	public static final String PROP_CONNECTION = "connection";
	public static final String PROP_DEFAULT_APPLICATION = "default_application";
	public static final String PROP_DEFAULT_USE_EXISTING_APPLICATION = "default_useExistingApplication";
	public static final String PROP_DOMAIN = "domain";
	public static final String PROP_DOMAINS = "domains";
	public static final String PROP_INITIAL_GITURL = "initialGitUrl";
	public static final String PROP_KEY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	public static final String PROP_MERGE_URI = "mergeUri";
	public static final String PROP_NEW_PROJECT = "newProject";
	public static final String PROP_PROJECT_NAME = "projectName";
	public static final String PROP_REMOTE_NAME = "remoteName";
	public static final String PROP_REPOSITORY_PATH = "repositoryPath";
	public static final String PROP_RUNTIME_DELEGATE = "runtimeDelegate";
	public static final String PROP_SKIP_MAVEN_BUILD = "skipMavenBuild";
	public static final String PROP_SERVER_TYPE = "serverType";
	public static final String PROP_SERVER_ADAPTER = "serverAdapter";
	public static final String PROP_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROP_ENVIRONMENT_VARIABLES = "environmentVariables";

	public static final String NEW_PROJECT_REMOTE_NAME_DEFAULT = "origin";
	public static final String EXISTING_PROJECT_REMOTE_NAME_DEFAULT = "openshift";
	public static final String DEFAULT_REPOSITORY_PATH = EGitUIUtils.getEGitDefaultRepositoryPath();

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
	public IProject importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException,
			URISyntaxException, InvocationTargetException, IOException, NoWorkTreeException, GitAPIException;

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
	public IProject mergeIntoUnsharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException;

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), adds the
	 * appication git repo as remote and creates the server adapter.
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
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	public IProject mergeIntoGitSharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException, NoWorkTreeException, GitAPIException;

	public File getRepositoryFile();

	public Object setProperty(String key, Object value);

	public Object getProperty(String key);

	public IDomain setDomain(IDomain domain);

	public boolean hasDomain();

	public IDomain getDomain();

	public IApplication getApplication();

	public String setApplicationName(String name);

	public String getApplicationName();

	public ICartridge setApplicationCartridge(IStandaloneCartridge cartridge);

	public IStandaloneCartridge getApplicationCartridge();

	public void setApplication(IApplication application);

	public String setRemoteName(String remoteName);

	public String getRemoteName();

	public String setRepositoryPath(String repositoryPath);

	public String getRepositoryPath();

	public boolean isNewProject();

	public Boolean setNewProject(boolean newProject);

	public String setProjectName(String projectName);

	public IProject setProject(IProject project);

	public boolean isGitSharedProject();

	public Boolean setCreateServerAdapter(Boolean createServerAdapter);

	public boolean isCreateServerAdapter();

	public Boolean setSkipMavenBuild(Boolean binaryDeploy);

	public boolean isSkipMavenBuild();

	public String getProjectName();

	public String setMergeUri(String mergeUri);

	public String getMergeUri();

	public IRuntime getRuntime();

	public IServerType getServerType();

	public IServerType setServerType(IServerType serverType);

	public boolean isUseExistingApplication();

	public boolean setUseExistingApplication(boolean useExistingApplication);

	public Set<IEmbeddableCartridge> setSelectedEmbeddableCartridges(
			Set<IEmbeddableCartridge> selectedEmbeddableCartridges);

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges();

	public IGearProfile getApplicationGearProfile();

	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile);

	public ApplicationScale getApplicationScale();

	public ApplicationScale setApplicationScale(ApplicationScale scale);

	public String getInitialGitUrl();
	
	public String setInitialGitUrl(String initialGitUrl);
	
	public IProject getProject();

	public IServer createServerAdapter(IProgressMonitor monitor) throws OpenShiftException;

	public IStatus publishServerAdapter(IProgressMonitor monitor);

	public List<IDomain> setDomains(List<IDomain> domains);

	public List<IDomain> getDomains();
	
	public Map<String, String> getEnvironmentVariables();
	
	public Map<String, String> setEnvironmentVariables(Map<String, String> environmentVariables);
}