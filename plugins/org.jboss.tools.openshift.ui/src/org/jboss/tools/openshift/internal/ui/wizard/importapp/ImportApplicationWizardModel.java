/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.BuildConfigTreeItems.ConnectionTreeItem;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.build.IGitBuildSource;

/**
 * @author Andre Dietisheim
 */
public class ImportApplicationWizardModel 
	extends ObservableUIPojo 
	implements IBuildConfigPageModel, IGitCloningPageModel {

	private Connection connection;
	private ConnectionTreeItem connectionItem;
	private Object selectedItem;
	private String repoPath;
	private boolean useDefaultRepoPath;
	private String projectName;
	private IProject project;
	
	private List<ObservableTreeItem> buildConfigs = new ArrayList<>();

	public ImportApplicationWizardModel() {
		this.useDefaultRepoPath = true;
		this.repoPath = getDefaultRepoPath();
	}

	private String getDefaultRepoPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
	}

	@Override
	public IBuildConfig getSelectedBuildConfig() {
		return getBuildConfig(selectedItem);
	}

	private IBuildConfig getBuildConfig(Object item) {
		if (!(item instanceof IBuildConfig)) {
			return null;
		}
		return (IBuildConfig) item;
	}

	public String getGitUrl() {
		IBuildConfig config = getSelectedBuildConfig();
		if (config == null) {
			return null;
		} else {
			return config.getSourceURI();
		}
	}
	
	public String getGitRef() {
		IBuildConfig config = getSelectedBuildConfig();
		if (config != null && config.getBuildSource() instanceof IGitBuildSource){
			return ((IGitBuildSource)config.getBuildSource()).getRef();
		}
		return null;
	}
	
	@Override
	public void setSelectedItem(Object selectedItem) {
		firePropertyChange(PROPERTY_SELECTED_ITEM, this.selectedItem, this.selectedItem = selectedItem);
		setProjectName(getBuildConfig(selectedItem));
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
		setProjectName(getProjectName(config));
	}

	private static String getProjectName(IBuildConfig config) {
		String projectName = (config == null)? null:extractProjectNameFromURI(config.getSourceURI());
		return projectName;
	}

	/**
	 * Extract the last segment of an URI, stripped from .git suffixes
	 *
	 * Made public for testing purposes.
	 */
	public static String extractProjectNameFromURI(String uri) {
		String projectName = null;
		if (uri != null) {
			uri = uri.trim();
			while(uri.endsWith("/")) {
				//Trailing slashes do not matter.
				uri = uri.substring(0, uri.length() - 1);
			}
			if(uri.endsWith(".git")) {
				uri = uri.substring(0, uri.length() - 4);
				if(uri.endsWith("/")) { 
					// '/' before .git is error
					return null;
				}
			}
			int b = uri.lastIndexOf("/");
			if(b >= 0) {
				projectName = uri.substring(b + 1);
			}
		}
		return projectName;
	}

	@Override
	public void setProjectName(String name) {
		if (name == null && getProject() != null) {
			name = getProject().getName();
		}
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
	public void setConnection(Connection connection) {
		this.connectionItem = new ConnectionTreeItem(connection);
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}

	@Override
	public List<ObservableTreeItem> getBuildConfigs() {
		return buildConfigs;
	}
	
	@Override
	public void loadBuildConfigs() {
		if (connectionItem == null) {
				return;
		}
		connectionItem.load();
		setBuildConfigs(filterBuildConfigs(connectionItem.getChildren()));
	}
	
	private void setBuildConfigs(List<ObservableTreeItem> newBuildConfigs) {
		if (newBuildConfigs == null) {
			return;
		}
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.buildConfigs);
		List<ObservableTreeItem> newItems = new ArrayList<>(newBuildConfigs);
		buildConfigs.clear();
		buildConfigs.addAll(newItems);
		firePropertyChange(PROPERTY_BUILDCONFIGS, oldItems, newItems);
	}

	private List<ObservableTreeItem> filterBuildConfigs(List<ObservableTreeItem> children) {
		List<ObservableTreeItem> result = children;
		if (project != null) {
			result = children.stream()
					.filter(c -> project.equals(c.getModel()))
					.map(p -> p.getChildren())
					.flatMap(l -> l.stream())
					.collect(Collectors.toList());
		}
		return result;
	}

	@Override
	public Object getContext() {
		return null;
	}

	public String getGitContextDir() {
		IBuildConfig config = getSelectedBuildConfig();
		if (config != null && config.getBuildSource() instanceof IGitBuildSource){
			return ((IGitBuildSource)config.getBuildSource()).getContextDir();
		}
		return null;
	}

	@Override
	public String getApplicationName() {
		String appName = null;
		IBuildConfig buildConfig = getSelectedBuildConfig();
		if (buildConfig != null) {
			appName = buildConfig.getName();
		}
		return appName;
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}
