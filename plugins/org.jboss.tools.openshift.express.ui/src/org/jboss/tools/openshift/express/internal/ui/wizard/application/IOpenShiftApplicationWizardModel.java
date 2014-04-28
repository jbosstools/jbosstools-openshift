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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author André Dietisheim
 */
public interface IOpenShiftApplicationWizardModel extends IConnectionAwareModel, IObservablePojo {

	public static final String PROP_APPLICATION = "application";
	public static final String PROP_APPLICATION_NAME = "applicationName";
	public static final String PROP_APPLICATION_GEAR_PROFILE = "applicationGearProfile";
	public static final String PROP_APPLICATION_SCALE = "applicationScale";
	public static final String PROP_AVAILABLE_EMBEDDABLE_CARTRIDGES = "allEmbeddableCartridges";
	public static final String PROP_AVAILABLE_STANDALONE_CARTRIDGES = "allStandaloneCartridges";
	public static final String PROP_CARTRIDGES = "cartridges";
	public static final String PROP_CREATE_SERVER_ADAPTER = "createServerAdapter";
	public static final String PROP_CONNECTION = "connection";
	public static final String PROP_DOMAIN = "domain";
	public static final String PROP_DOMAINS = "domains";
	public static final String PROP_EMBEDDED_CARTRIDGES = "embeddedCartridges";
	public static final String PROP_ENVIRONMENT_VARIABLES = "environmentVariables";
	public static final String PROP_INITIAL_GIT_URL = "initialGitUrl";
	public static final String PROP_MERGE_URI = "mergeUri";
	public static final String PROP_NEW_PROJECT = "newProject";
	public static final String PROP_PROJECT_NAME = "projectName";
	public static final String PROP_REMOTE_NAME = "remoteName";
	public static final String PROP_REPOSITORY_PATH = "repositoryPath";
	public static final String PROP_SKIP_MAVEN_BUILD = "skipMavenBuild";
	public static final String PROP_SERVER_ADAPTER = "serverAdapter";
	public static final String PROP_SELECTED_APPLICATION_TEMPLATE = "selectedApplicationTemplate";
	public static final String PROP_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROP_USE_INITIAL_GIT_URL = "useInitialGitUrl";
	
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

	public IDomain setDomain(IDomain domain);

	/**
	 * Sets the default to this wizard if there's no domain set to it yet.
	 * 
	 * @see Connection#getDefaultDomain()
	 * @see #setDomain(IDomain)
	 * @see #hasDomain()
	 */
	public void setDefaultDomainIfRequired();

	/**
	 * Returns <code>true</code> if this wizard has a domain.
	 * 
	 * @return true if this wizard has a domain
	 */
	public boolean hasDomain();

	/**
	 * Returns the domain that this wizard will create the application for
	 * 
	 * @return
	 */
	public IDomain getDomain();

	/**
	 * Returns the application used in this model. Returns the application
	 * that's being imported or the new application once it's created.
	 * 
	 * @return the (new or existing) application in this model
	 */
	public IApplication getApplication();

	public IApplication setApplication(IApplication application);

	public String setApplicationName(String name);

	/**
	 * Returns the name for the new application that this wizard will create
	 * upon completion.
	 * 
	 * @return
	 * 
	 * @see IDomain#createApplication(String, IStandaloneCartridge,
	 *      ApplicationScale, IGearProfile, String, int, Map,
	 *      com.openshift.client.cartridge.IEmbeddableCartridge...)
	 */
	public String getApplicationName();

	public List<IStandaloneCartridge> setAvailableStandaloneCartridges(List<IStandaloneCartridge> cartridges);

	/**
	 * Returns all standalone cartridges that are available in OpenShift
	 * 
	 * @return all available standalone cartridges
	 */
	public List<IStandaloneCartridge> getAvailableStandaloneCartridges();

	public String setRemoteName(String remoteName);

	/**
	 * Returns the git remote name we're using to store the OpenShift
	 * (fetch-/pull- uri) git repo to.
	 * 
	 * @return
	 */
	public String getRemoteName();

	public String setRepositoryPath(String repositoryPath);

	/**
	 * Returns the path to the local git repository the wizard will clone the
	 * existing/new OpenShift application to.
	 * 
	 * @return
	 */
	public String getRepositoryPath();

	/**
	 * Returns <code>true</code> if this wizard is set to import (the new or
	 * existing application) into a new project. Returns <code>false</code> if
	 * the model is set to import into an existing application.
	 * 
	 * @return true if this wizard is about to import into a new project
	 */
	public boolean isNewProject();

	public Boolean setNewProject(boolean newProject);

	public String setProjectName(String projectName);

	/**
	 * Returns the name of the project that we're merging the new/existing application into.
	 * @return
	 */
	public String getProjectName();

	/**
	 * Sets the project that the new or existing application will get imported
	 * to.
	 * 
	 * @param project
	 *            the project that the application will get imported to.
	 * @return
	 */
	public IProject setProject(IProject project);

	/**
	 * Returns the project that this wizard is using to import the new or
	 * existing application to. The project may be new or existing.
	 * 
	 * @return
	 * @see #isNewProject()
	 */
	public IProject getProject();

	/**
	 * Returns <code>true</code> if the project that this wizard is importing to
	 * is shared with a git repo.
	 * 
	 * @return true if the project that we're importing to is shared with git.
	 */
	public boolean isGitSharedProject();

	public Boolean setCreateServerAdapter(Boolean createServerAdapter);

	/**
	 * Returns <code>true</code> if the wizard told to create a server adapter
	 * when importing the OpenShift application into the local workspace.
	 * 
	 * @return
	 */
	public boolean isCreateServerAdapter();

	/**
	 * Creates the server adapter for the new or existing application.
	 * 
	 * @param monitor
	 *            the monitor to report the progress to
	 * @return
	 * @throws OpenShiftException
	 */
	public IServer createServerAdapter(IProgressMonitor monitor) throws OpenShiftException;

	/**
	 * Sets the wizard to create the marker which prevents OpenShift from maven
	 * building when one pushed the (cloned git repo) of the application to
	 * OpenShift
	 * 
	 * @param skipMavenBuild
	 * @return
	 */
	public Boolean setSkipMavenBuild(Boolean skipMavenBuild);

	/**
	 * Returns <code>true</code> if we're setting the marker to skip the maven
	 * build when pushing (the cloned application git repo) to OpenShift
	 * 
	 * @return
	 */
	public boolean isSkipMavenBuild();

	public String setMergeUri(String mergeUri);

	public String getMergeUri();

	/**
	 * Returns <code>true</code> if the model is set to import an existing application
	 * 
	 * @return true if the wizard is set to import an existing application
	 */
	public boolean isUseExistingApplication();

	public boolean setUseExistingApplication(boolean useExistingApplication);

	/**
	 * Adds the given cartridges to the cartridges that shall be used for the new application
	 * @param cartridges
	 */
	public void addEmbeddedCartridges(List<ICartridge> cartridges);

	/**
	 * Removes the given cartridge from the cartridges that shall be used for the new application
	 * @param cartridge
	 */
	public void removeEmbeddedCartridge(ICartridge cartridge);

	/**
	 * Removes the given cartridges from the list of cartridges for the new application
	 *  
	 * @param cartridges
	 */
	public void removeEmbeddedCartridges(List<ICartridge> cartridges);

	/**
	 * Returns all embeddable cartridges that available in OpenShift
	 * 
	 * @return all available embeddable cartridges
	 */
	public List<ICartridge> getAvailableEmbeddableCartridges();

	public List<ICartridge> setAvailableEmbeddableCartridges(List<ICartridge> embeddableCartridges);

	/**
	 * Returns all the (standalone- and embedded-) cartridges that are set to be
	 * used (used as standalone or added as embedded) in the new application.
	 * 
	 * @return all cartridges for the new application
	 * 
	 * @see IDomain#createApplication(String, IStandaloneCartridge,
	 *      ApplicationScale, IGearProfile, String, int, Map,
	 *      com.openshift.client.cartridge.IEmbeddableCartridge...)
	 */
	public Set<ICartridge> getEmbeddedCartridges();
	
	public Set<ICartridge> setEmbeddedCartridges(Set<ICartridge> selectedEmbeddableCartridges);

	/**
	 * Returns the standalone cartridge used to create the new application.
	 * <p>
	 * Warning: There's no standalone cartridge if we're set to use a
	 * quickstart. Quickstarts unfortunately only use non-specified cartridges
	 * (no distinction btw. IEmbeddableCartridge and IStandaloneCartridge,
	 * there's just a collection of ICartridge).
	 * </p>
	 * 
	 * @return the standalone cartridge used to create the new application or
	 *         null (in case of a quickstart)
	 *         
	 * @see #getSelectedApplicationTemplate()
	 * @see ICartridgeApplicationTemplate
	 * @see IQuickstartApplicationTemplate
	 */
	public IStandaloneCartridge getStandaloneCartridge();

	/**
	 * Returns all (standalone and embedded) cartridges.
	 * 
	 * @return
	 */
	public Set<ICartridge> getCartridges();

	/**
	 * Sets all (embedded and standalone) cartridges.
	 * 
	 * @param cartridges
	 * @return
	 */
	public Set<ICartridge> setCartridges(Set<ICartridge> cartridges);

	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile);

	/**
	 * Returns the gear profile that the new application will get created with.
	 * 
	 * @return
	 * 
	 * @see IGearProfile
	 * @see IDomain#createApplication(String, IStandaloneCartridge,
	 *      ApplicationScale, IGearProfile, String, int, Map,
	 *      com.openshift.client.cartridge.IEmbeddableCartridge...)
	 */
	public IGearProfile getApplicationGearProfile();

	public ApplicationScale setApplicationScale(ApplicationScale scale);

	/**
	 * Returns the application scale that the new application will get created
	 * with.
	 * 
	 * @return
	 * 
	 * @see ApplicationScale
	 * @see IDomain#createApplication(String, IStandaloneCartridge,
	 *      ApplicationScale, IGearProfile, String, int, Map,
	 *      com.openshift.client.cartridge.IEmbeddableCartridge...)
	 */
	public ApplicationScale getApplicationScale();

	public IApplicationTemplate setSelectedApplicationTemplate(IApplicationTemplate template);

	/**
	 * Returns the application template that this wizard is using when creating
	 * the new application
	 * 
	 * @return the application template that the wizard is using
	 * 
	 * @see IApplicationTemplate
	 * @see ICartridgeApplicationTemplate
	 * @see IQuickstartApplicationTemplate
	 */
	public IApplicationTemplate getSelectedApplicationTemplate();

	/**
	 * Returns the initial git url that this wizard is using when creating the
	 * new application.
	 * 
	 * @return
	 * 
	 * @see IDomain#createApplication(String, IStandaloneCartridge,
	 *      ApplicationScale, IGearProfile, String, int, Map,
	 *      com.openshift.client.cartridge.IEmbeddableCartridge...)
	 */
	public String getInitialGitUrl();
	
	public String setInitialGitUrl(String initialGitUrl);
	
	/**
	 * Returns <code>true</code> if we're using an initial git url when creating
	 * the new application.
	 * 
	 * @return
	 */
	public boolean isUseInitialGitUrl();
	
	public boolean setUseInitialGitUrl(boolean useInitialGitUrl);

	/**
	 * Publishes the project (within this wizard) to OpenShift using the server
	 * adapter (that was possibly created when importing the new or existing
	 * project).
	 * 
	 * @param monitor the monitor to report progress to.
	 * @return
	 */
	public IStatus publishServerAdapter(IProgressMonitor monitor);

	public List<IDomain> setDomains(List<IDomain> domains);

	/**
	 * Returns all domains that exist in the connection that's used in this
	 * wizard.
	 * 
	 * @return all domains that exist in the connection used in this wizard.
	 * 
	 * @see #getConnection()
	 * @see Connection#getDomains()
	 */
	public List<IDomain> getDomains();
	
	/**
	 * Returns the environment variabels that this wizard will set to the new
	 * application.
	 * 
	 * @return
	 */
	public Map<String, String> getEnvironmentVariables();
	
	public Map<String, String> setEnvironmentVariables(Map<String, String> environmentVariables);
}
