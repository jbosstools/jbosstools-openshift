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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IApplication;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 */
public class GitCloningSettingsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_NEW_PROJECT = "newProject";
	public static final String PROPERTY_REPO_PATH = "repositoryPath";
	public static final String PROPERTY_REMOTE_NAME = "remoteName";
	public static final String PROPERTY_USE_DEFAULT_REPO_PATH = "useDefaultRepoPath";
	public static final String PROPERTY_USE_DEFAULT_REMOTE_NAME = "useDefaultRemoteName";
	public static final String PROPERTY_HAS_REMOTEKEYS = "hasRemoteKeys";

	private IOpenShiftApplicationWizardModel wizardModel;
	private boolean useDefaultRepoPath = true;
	private boolean useDefaultRemoteName = true;
	private boolean hasRemoteKeys;

	public GitCloningSettingsWizardPageModel(IOpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		wizardModel.addPropertyChangeListener(IOpenShiftApplicationWizardModel.PROP_APPLICATION_NAME, onWizardApplicationNameChanged());
		wizardModel.addPropertyChangeListener(IOpenShiftApplicationWizardModel.PROP_PROJECT_NAME, onWizardProjectNameChanged());
		wizardModel.addPropertyChangeListener(IOpenShiftApplicationWizardModel.PROP_NEW_PROJECT, onWizardProjectNameChanged());
		setRepositoryPath(getDefaultRepositoryPath());
		setDefaultRemoteName();
	}


	/**
	 * Listener to propagate the application name changes from the underlying WizardModel into this WizardPageModel, so that properties can be affected here, too.
	 * @return
	 */
	private PropertyChangeListener onWizardApplicationNameChanged() {
		return new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				firePropertyChange(PROPERTY_APPLICATION_NAME, evt.getOldValue(), evt.getNewValue());
				if(wizardModel.isNewProject() && isUseDefaultRepoPath()) {
					setRepositoryPath(IOpenShiftApplicationWizardModel.DEFAULT_REPOSITORY_PATH);
				} 
			}
		};
	}

	/**
	 * Listener to propagate the project name changes from the underlying WizardModel into this WizardPageModel, so that properties can be affected here, too.
	 * @return
	 */
	private PropertyChangeListener onWizardProjectNameChanged() {
		return new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(isUseDefaultRepoPath()) {
					final IProject project = wizardModel.getProject();
					if(project != null && project.exists()) {
						setRepositoryPath(project.getLocation().toOSString());
					} else {
						setRepositoryPath(IOpenShiftApplicationWizardModel.DEFAULT_REPOSITORY_PATH);
					}
				}
				setDefaultRemoteName();
			}
		};
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}
	
	public String getRepositoryPath() {
		return wizardModel.getRepositoryPath();
	}

	public void setRepositoryPath(String repositoryPath) {
		firePropertyChange(PROPERTY_REPO_PATH, wizardModel.getRepositoryPath(),
				wizardModel.setRepositoryPath(repositoryPath));
	}

	private void resetRemoteName() {
		setRemoteName(null);
	}

	private void setDefaultRemoteName() {
		if(wizardModel.isNewProject()) {
			setRemoteName(IOpenShiftApplicationWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT);
		} else {
			setRemoteName(IOpenShiftApplicationWizardModel.EXISTING_PROJECT_REMOTE_NAME_DEFAULT);
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
			setDefaultRemoteName();
		} else {
			resetRemoteName();
		}
	}

	public boolean isUseDefaultRemoteName() {
		return useDefaultRemoteName;
	}

	public boolean getHasRemoteKeys() {
		return hasRemoteKeys;
	}
	
	public void setHasRemoteKeys(boolean hasRemoteKeys) {
		firePropertyChange(PROPERTY_HAS_REMOTEKEYS, this.hasRemoteKeys, this.hasRemoteKeys = hasRemoteKeys);
	}

	public Connection getConnection() {
		return wizardModel.getConnection();
	}
	
	public boolean isConnected() {
		return getConnection() != null
				&& getConnection().isConnected();
	}
	
	public void reset() {
		setRemoteName(wizardModel.getRemoteName());
		setRepositoryPath(wizardModel.getRepositoryPath());
		setHasRemoteKeys(getHasRemoteKeys());
	}
}
