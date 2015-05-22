/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Andre Dietisheim
 */
public class ImportApplicationWizardModel 
	extends ObservableUIPojo 
	implements IBuildConfigPageModel, IGitCloningPageModel {

	private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("([^\\/\\.git]+)(\\.git)?$");
	
	private Connection connection;
	private Object selectedItem;
	private String repoPath;
	private boolean useDefaultRepoPath;
	private String projectName;
	
	ImportApplicationWizardModel() {
		this.useDefaultRepoPath = true;
		this.repoPath = getDefaultRepoPath();
	}

	private String getDefaultRepoPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
	}

	@Override
	public IBuildConfig getSelectedBuildConfig() {
		if (!(selectedItem instanceof IBuildConfig)) {
			return null;
		}
		return (IBuildConfig) selectedItem;
	}

	public String getGitUrl() {
		IBuildConfig config = getSelectedBuildConfig();
		if (config == null) {
			return null;
		} else {
			return config.getSourceURI();
		}
	}
	
	@Override
	public void setSelectedItem(Object selectedItem) {
		firePropertyChange(PROPERTY_SELECTED_ITEM, this.selectedItem, this.selectedItem = selectedItem);
		setProjectName(getSelectedBuildConfig());
	}
	
	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void setRepositoryPath(String path) {
		firePropertyChange(PROPERTY_REPOSITORY_PATH, this.repoPath, this.repoPath = path);
	}

	@Override
	public String getRepositoryPath() {
		return repoPath;
	}

	public File getCloneDestination() {
		if (StringUtils.isEmpty(repoPath)
				|| StringUtils.isEmpty(projectName)) {
			return null;
		}
		return new Path(repoPath).append(projectName).toFile();
	}
	
	@Override
	public void setUseDefaultRepositoryPath(boolean useDefault) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REPOSITORY_PATH, this.useDefaultRepoPath, this.useDefaultRepoPath = useDefault);
		if (useDefaultRepoPath) {
			setRepositoryPath(getDefaultRepoPath());
		}
	}

	@Override
	public boolean isUseDefaultRepositoryPath() {
		return useDefaultRepoPath;
	}

	private void setProjectName(IBuildConfig config) {
		String projectName = null;
		if (config != null) {
			Matcher matcher = PROJECT_NAME_PATTERN.matcher(config.getSourceURI());
			if (matcher.find()
					&& matcher.group(1) != null) {
				projectName = matcher.group(1);
			}
		}
		setProjectName(projectName);
	}

	@Override
	public void setProjectName(String name) {
		firePropertyChange(PROPERTY_PROJECT_NAME, this.projectName, this.projectName = name);
	}
	
	@Override
	public String getProjectName() {
		return projectName;
	}
	
	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean hasConnection() {
		return connection != null;
	}

	@Override
	public Connection setConnection(Connection connection) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		return this.connection;
	}

	@Override
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
