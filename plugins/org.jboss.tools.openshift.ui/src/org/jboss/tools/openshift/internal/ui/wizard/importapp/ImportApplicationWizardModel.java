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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.BuildConfigTreeItems.ConnectionTreeItem;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.build.IGitBuildSource;

/**
 * @author Andre Dietisheim
 * @author Fred Bricon
 * @author Jeff.Cantrill
 * @author Rob Stryker
 */
public class ImportApplicationWizardModel 
	extends ObservableUIPojo 
	implements IBuildConfigPageModel, IGitCloningPageModel {

	private Connection connection;
	private ConnectionTreeItem connectionItem;
	private Object selectedItem;
	private String repoPath;
	private boolean useDefaultRepoPath;
	private String repoName;
	private IProject project;
	private boolean reuseGitRepo = false;
	private String cloneDestination;
	
	private List<ObservableTreeItem> buildConfigs = new ArrayList<>();

	public ImportApplicationWizardModel() {
		this.useDefaultRepoPath = true;
		this.repoPath = getDefaultRepoPath();
	}

	private void update(Connection connection, Object selectedItem,
			String repoPath, boolean useDefaultRepoPath, IProject project, boolean reuseGitRepo) {
		this.connectionItem = updateConnection(connection);
		this.project = project;
		updateSelectedItem(selectedItem);
		updateUseDefaultRepositoryPath(useDefaultRepoPath);
		updateRepositoryPath(useDefaultRepoPath, repoPath);
		String repoName = updateRepoName(getBuildConfig(selectedItem));
		updateCloneDestination(repoPath, repoName);
		updateReuseGitRepo(reuseGitRepo);
	}
	
	private ConnectionTreeItem updateConnection(Connection connection) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		return new ConnectionTreeItem(connection);
	}

	protected void updateSelectedItem(Object selectedItem) {
		firePropertyChange(PROPERTY_SELECTED_ITEM, this.selectedItem, this.selectedItem = selectedItem);
	}

	private String updateRepoName(IBuildConfig config) {
		String name = getRepoName(config);
		if (name == null 
				&& getProject() != null) {
			name = getProject().getName();
		}
		firePropertyChange(PROPERTY_REPO_NAME, this.repoName, this.repoName = name);
		return name;
	}

	private void updateRepositoryPath(boolean useDefaultRepo, String repoPath) {
		if (useDefaultRepo) {
			repoPath = getDefaultRepoPath();
		}
		firePropertyChange(PROPERTY_REPOSITORY_PATH, this.repoPath, this.repoPath = repoPath);
	}

	private void updateCloneDestination(String repositoryPath, String repoName) {
		String cloneDestination = this.cloneDestination;
		if (StringUtils.isNotBlank(repositoryPath)) {
			IPath cloneDestinationPath = new Path(repositoryPath);
			if (StringUtils.isNotBlank(repoName)) {
				cloneDestinationPath = cloneDestinationPath.append(new Path(repoName));
			}
			cloneDestination = cloneDestinationPath.toOSString();
		}

		firePropertyChange(PROPERTY_CLONE_DESTINATION, this.cloneDestination, this.cloneDestination = cloneDestination);
	}

	private void updateReuseGitRepo(boolean reuseGitRepo) {
		firePropertyChange(PROPERTY_REUSE_GIT_REPOSITORY, this.reuseGitRepo, this.reuseGitRepo = reuseGitRepo);
	}

	private static String getRepoName(IBuildConfig config) {
		String repoName = null;
		if (config != null) {
			repoName = ResourceUtils.extractProjectNameFromURI(config.getSourceURI());
		}
		return repoName;
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
		String gitRef = null;
		IBuildConfig config = getSelectedBuildConfig();
		if (config != null 
				&& config.getBuildSource() instanceof IGitBuildSource) {
			gitRef = ((IGitBuildSource) config.getBuildSource()).getRef();
		}
		return gitRef;
	}
	
	@Override
	public void setSelectedItem(Object selectedItem) {
		update(this.connection, selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, this.reuseGitRepo);
	}

	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void setRepositoryPath(String repoPath) {
		update(this.connection, this.selectedItem, repoPath, this.useDefaultRepoPath, this.project, this.reuseGitRepo);
	}

	@Override
	public String getRepositoryPath() {
		return repoPath;
	}

	@Override
	public String getRepoName() {
		return repoName;
	}
	
	public File getCloneDestination() {
		return new Path(cloneDestination).toFile();
	}
	
	@Override
	public void setUseDefaultRepositoryPath(boolean useDefaultRepoPath) {
		update(connection, this.selectedItem, this.repoPath, useDefaultRepoPath, this.project, this.reuseGitRepo);
	}

	public void updateUseDefaultRepositoryPath(boolean useDefault) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REPOSITORY_PATH, this.useDefaultRepoPath, this.useDefaultRepoPath = useDefault);
	}

	@Override
	public boolean isUseDefaultRepositoryPath() {
		return useDefaultRepoPath;
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
		update(connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, this.reuseGitRepo);
	}

	@Override
	public List<ObservableTreeItem> getBuildConfigs() {
		return buildConfigs;
	}
	
	@Override
	public void loadBuildConfigs() {
		loadBuildConfigs(connectionItem);
	}

	public void loadBuildConfigs(ConnectionTreeItem connectionItem) {
		if (connectionItem == null) {
				return;
		}
		connectionItem.load();
		updateBuildConfigs(filterBuildConfigs(project, connectionItem.getChildren()));
	}

	private void updateBuildConfigs(List<ObservableTreeItem> newBuildConfigs) {
		if (newBuildConfigs == null
				|| ListUtils.isEqualList(newBuildConfigs, buildConfigs)) {
			return;
		}
		List<ObservableTreeItem> oldItems = new ArrayList<>(buildConfigs);
		List<ObservableTreeItem> newItems = new ArrayList<>(newBuildConfigs);
		buildConfigs.clear();
		buildConfigs.addAll(newItems);
		firePropertyChange(PROPERTY_BUILDCONFIGS, oldItems, newItems);
	}

	private List<ObservableTreeItem> filterBuildConfigs(IProject project, List<ObservableTreeItem> children) {
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
		String gitContextDir = null; 
		IBuildConfig config = getSelectedBuildConfig();
		if (config != null 
				&& config.getBuildSource() instanceof IGitBuildSource) {
			gitContextDir = ((IGitBuildSource) config.getBuildSource()).getContextDir();
		}
		return gitContextDir;
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
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, project, this.reuseGitRepo);
	}

	@Override
	public boolean isReuseGitRepository() {
		return reuseGitRepo;
	}

	@Override
	public void setReuseGitRepository(boolean reuseGitRepository) {
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, reuseGitRepository);
	}
}