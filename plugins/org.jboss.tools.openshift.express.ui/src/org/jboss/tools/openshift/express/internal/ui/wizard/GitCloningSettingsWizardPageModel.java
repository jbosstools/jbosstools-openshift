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

import static org.jboss.tools.openshift.express.internal.ui.wizard.IOpenShiftExpressWizardModel.EXISTING_PROJECT_REMOTE_NAME_DEFAULT;
import static org.jboss.tools.openshift.express.internal.ui.wizard.IOpenShiftExpressWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 */
public class GitCloningSettingsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_NEW_PROJECT = "newProject";
	public static final String PROPERTY_CLONE_URI = "cloneUri";
	public static final String PROPERTY_APPLICATION_URL = "applicationUrl";
	public static final String PROPERTY_REPO_PATH = "repositoryPath";
	public static final String PROPERTY_REMOTE_NAME = "remoteName";
	public static final String PROPERTY_LOADING = "loading";
	public static final String PROPERTY_USE_DEFAULT_REPO_PATH = "useDefaultRepoPath";
	public static final String PROPERTY_USE_DEFAULT_REMOTE_NAME = "useDefaultRemoteName";

	private IOpenShiftExpressWizardModel wizardModel;
	private boolean loading;
	private boolean useDefaultRepoPath = true;
	private boolean useDefaultRemoteName = true;

	public GitCloningSettingsWizardPageModel(IOpenShiftExpressWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setRepositoryPath(getDefaultRepositoryPath());
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}

	public void loadGitUri() throws OpenShiftException {
		setLoading(true);
		setCloneUri("Loading...");
		setCloneUri(getCloneUri());
		setLoading(false);
	}

	private void setCloneUri(String gitUri) {
		firePropertyChange(PROPERTY_CLONE_URI, null, gitUri);
	}

	public String getCloneUri() throws OpenShiftException {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return null;
		}
		return application.getGitUri();
	}

	public void loadApplicationUrl() throws OpenShiftException {
		setLoading(true);
		setApplicationUrl("Loading...");
		setApplicationUrl(getApplicationUrl());
		setLoading(false);
	}

	public String getApplicationUrl() throws OpenShiftException {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return null;
		}
		return application.getApplicationUrl();
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	public boolean isJBossAS7Application() {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return false;
		}
		return ICartridge.JBOSSAS_7.equals(application.getCartridge());
	}

	public void setApplicationUrl(String applicationUrl) {
		firePropertyChange(PROPERTY_APPLICATION_URL, null, applicationUrl);
	}

	public String getRepositoryPath() {
		return wizardModel.getRepositoryPath();
	}

	public void setRepositoryPath(String repositoryPath) {
		firePropertyChange(PROPERTY_REPO_PATH, wizardModel.getRepositoryPath(),
				wizardModel.setRepositoryPath(repositoryPath));
	}

	// public void resetRepositoryPath() {
	// if (wizardModel.isNewProject()
	// || getRepositoryPath() == null) {
	// setRepositoryPath(getDefaultRepositoryPath());
	// }
	// }

	public void resetRemoteName() {
		if (!wizardModel.isNewProject()) {
			// if existing project and remote name is still 'origin'
			// -> switch to 'openshift' (leave as is if existing project and
			// remote name != 'origin')
			if (NEW_PROJECT_REMOTE_NAME_DEFAULT.equals(getRemoteName())) {
				setRemoteName(EXISTING_PROJECT_REMOTE_NAME_DEFAULT);
			}
		} else {
			// if new project and remote name is not 'origin'
			// -> restore 'origin'
			if (!NEW_PROJECT_REMOTE_NAME_DEFAULT.equals(getRemoteName())) {
				setUseDefaultRemoteName(true);
				setRemoteName(NEW_PROJECT_REMOTE_NAME_DEFAULT);
			}
		}
	}

	private String getDefaultRepositoryPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
	}

	public String getRemoteName() {
		return wizardModel.getRemoteName();
	}

	public void setRemoteName(String remoteName) {
		firePropertyChange(PROPERTY_REMOTE_NAME, wizardModel.getRemoteName(), wizardModel.setRemoteName(remoteName));
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		firePropertyChange(PROPERTY_LOADING, this.loading, this.loading = loading);
	}

	public boolean isCompatibleToApplicationCartridge(ICartridge cartridge) {
		IApplication application = wizardModel.getApplication();
		return application != null && application.getCartridge() != null
				&& application.getCartridge().equals(cartridge);
	}

	public void setUseDefaultRepoPath(boolean useDefaultRepoPath) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REPO_PATH
				, this.useDefaultRepoPath
				, this.useDefaultRepoPath = useDefaultRepoPath);
		if (useDefaultRepoPath) {
			setRepositoryPath(getDefaultRepositoryPath());
		}
	}

	public boolean isUseDefaultRepoPath() {
		return useDefaultRepoPath;
	}

	public void setUseDefaultRemoteName(boolean useDefaultRemoteName) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REMOTE_NAME, useDefaultRemoteName,
				this.useDefaultRemoteName = useDefaultRemoteName);
		if (useDefaultRemoteName) {
			// setRemoteName(isNewProject() ? NEW_PROJECT_REMOTE_NAME_DEFAULT :
			// EXISTING_PROJECT_REMOTE_NAME_DEFAULT);
			resetRemoteName();
		}
	}

	public boolean isUseDefaultRemoteName() {
		return useDefaultRemoteName;
	}

	public IProject getProject() {
		String projectName = wizardModel.getProjectName();
		if (projectName == null) {
			return null;
		}

		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

}
