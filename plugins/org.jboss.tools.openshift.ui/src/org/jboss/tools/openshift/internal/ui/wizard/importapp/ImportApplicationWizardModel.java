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
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.Repository;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.core.EGitUtils;
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
	private boolean checkoutBranchReusedRepo;
	private String cloneDestination;
	private Repository cloneDestinationRepository;
	private String gitContextDir;
	
	private List<ObservableTreeItem> buildConfigs = new ArrayList<>();
	private boolean isCloneDestinationAtGitRef;

	public ImportApplicationWizardModel() {
		this.useDefaultRepoPath = true;
		this.repoPath = getDefaultRepoPath();
	}

	private void update(Connection connection, Object selectedItem,
			String repoPath, boolean useDefaultRepoPath, IProject project, boolean reuseGitRepo, boolean checkoutBranchReusedRepo, String gitContextDir) {
		this.connectionItem = updateConnection(connection);
		this.project = project;
		Object oldSelectedItem = this.selectedItem;
		updateSelectedItem(selectedItem);
		updateUseDefaultRepositoryPath(useDefaultRepoPath);
		updateRepositoryPath(useDefaultRepoPath, repoPath);
		IBuildConfig buildConfig = getBuildConfig(selectedItem);
		String repoName = updateRepoName(buildConfig);
		String oldCloneDestination = this.cloneDestination;
		String newCloneDestination = updateCloneDestination(repoPath, repoName);
		reuseGitRepo = updateReuseGitRepo(reuseGitRepo, newCloneDestination, oldCloneDestination);
		String gitRef = updateGitRef(getGitRef(buildConfig));
		Repository cloneDestinationRepository = updateCloneDestinationRepository(newCloneDestination, oldCloneDestination);
		boolean isCloneDestinationAtGitRef = updateIsCloneDestinationAtGitRef(gitRef, cloneDestinationRepository);
		updateCheckoutBranchReusedRepo(checkoutBranchReusedRepo, isCloneDestinationAtGitRef);
		updateGitContextDir(gitContextDir, buildConfig, getBuildConfig(oldSelectedItem));
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

	private static String getRepoName(IBuildConfig config) {
		String repoName = null;
		if (config != null) {
			repoName = ResourceUtils.extractProjectNameFromURI(config.getSourceURI());
		}
		return repoName;
	}

	private void updateRepositoryPath(boolean useDefaultRepo, String repoPath) {
		firePropertyChange(PROPERTY_REPOSITORY_PATH, this.repoPath, this.repoPath = repoPath);
	}

	private String updateCloneDestination(String repositoryPath, String repoName) {
		String cloneDestination = this.cloneDestination;
		if (StringUtils.isNotBlank(repositoryPath)) {
			IPath cloneDestinationPath = new Path(repositoryPath);
			if (StringUtils.isNotBlank(repoName)) {
				cloneDestinationPath = cloneDestinationPath.append(new Path(repoName));
			}
			cloneDestination = cloneDestinationPath.toOSString();
		}

		firePropertyChange(PROPERTY_CLONE_DESTINATION, this.cloneDestination, this.cloneDestination = cloneDestination);
		return cloneDestination;
	}

	private Repository updateCloneDestinationRepository(String newCloneDestination, String oldCloneDestination) {
		Repository repository = this.cloneDestinationRepository;
		if (!StringUtils.equals(newCloneDestination, oldCloneDestination)) {
			if (repository != null) {
				repository.close();
			}
			repository = EGitUtils.getRepository(getCloneDestination(newCloneDestination));
		}
		return this.cloneDestinationRepository = repository;
	}

	private boolean updateIsCloneDestinationAtGitRef(String gitRef, Repository cloneDestination) {
		boolean isCloneDestinationAtGitRef = this.isCloneDestinationAtGitRef;
		String cloneDestinationBranch = null;
		if (cloneDestination == null) {
			return false;
		}
		try {
			cloneDestinationBranch = EGitUtils.getCurrentBranch(cloneDestination);
		} catch (CoreException e) {
		}
		isCloneDestinationAtGitRef = StringUtils.equals(gitRef, cloneDestinationBranch);
		firePropertyChange(PROPERTY_IS_CLONE_DESTINATION_AT_GITREF, this.isCloneDestinationAtGitRef, 
				this.isCloneDestinationAtGitRef = isCloneDestinationAtGitRef);
		return isCloneDestinationAtGitRef;
	}

	private boolean updateReuseGitRepo(boolean reuseGitRepo, String newCloneDestination, String oldCloneloneDestination) {
		if (!Objects.equals(newCloneDestination, oldCloneloneDestination)
				&& !FileUtils.exists(new File(newCloneDestination))
				&& reuseGitRepo) {
			// reset reuseGitRepo if new cloneDestination does not exist
			reuseGitRepo = false;
		}
		firePropertyChange(PROPERTY_REUSE_GIT_REPOSITORY, this.reuseGitRepo, this.reuseGitRepo = reuseGitRepo);
		return reuseGitRepo;
	}

	private void updateCheckoutBranchReusedRepo(boolean checkoutBranchReusedRepo, boolean isCloneDestinationAtGitRef) {
		if (isCloneDestinationAtGitRef) {
			// reset checkout if we're already at the branch that's required
			checkoutBranchReusedRepo = false;
		}
		firePropertyChange(PROPERTY_CHECKOUT_BRANCH_REUSED_REPO, this.checkoutBranchReusedRepo, this.checkoutBranchReusedRepo = checkoutBranchReusedRepo);
	}

	private void updateGitContextDir(String newGitContextDir, IBuildConfig newBuildConfig, IBuildConfig oldBuildConfig) {
		if (Objects.equals(newBuildConfig, oldBuildConfig)) {
			if (Objects.equals(newGitContextDir, this.gitContextDir)) {
				return;
			}
		} else {
			newGitContextDir = getGitContextDir(newBuildConfig);
		}

		firePropertyChange(PROPERTY_GIT_CONTEXT_DIR, this.gitContextDir, this.gitContextDir = newGitContextDir);
	}

	private String updateGitRef(String gitRef) {
		firePropertyChange(PROPERTY_GIT_REF, null, gitRef);
		return gitRef;
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

	@Override
	public String getGitUrl() {
		IBuildConfig config = getSelectedBuildConfig();
		if (config == null) {
			return null;
		} else {
			return config.getSourceURI();
		}
	}

	@Override
	public String getGitRef() {
		return getGitRef(getSelectedBuildConfig());
	}

	protected String getGitRef(IBuildConfig config) {
		String gitRef = null;
		if (config != null 
				&& config.getBuildSource() instanceof IGitBuildSource) {
			gitRef = ((IGitBuildSource) config.getBuildSource()).getRef();
		}
		return gitRef;
	}
	
	@Override
	public void setSelectedItem(Object selectedItem) {
		update(this.connection, selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, 
				this.reuseGitRepo, this.checkoutBranchReusedRepo, this.gitContextDir);
	}

	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void setRepositoryPath(String repoPath) {
		update(this.connection, this.selectedItem, repoPath, false, this.project, 
				this.reuseGitRepo, this.checkoutBranchReusedRepo, this.gitContextDir);
	}

	@Override
	public String getRepositoryPath() {
		return repoPath;
	}

	@Override
	public String getRepoName() {
		return repoName;
	}
	
	@Override
	public File getCloneDestination() {
		return getCloneDestination(cloneDestination);
	}
	
	private File getCloneDestination(String cloneDestination) {
		return new Path(cloneDestination).toFile();
	}

	@Override
	public Repository getCloneDestinationRepository() {
		return this.cloneDestinationRepository;
	}

	@Override
	public boolean isCloneDestinationAtGitRef() {
		return this.isCloneDestinationAtGitRef;
	}
	
	@Override
	public void setUseDefaultRepositoryPath(boolean useDefaultRepoPath) {
		update(this.connection, this.selectedItem, useDefaultRepoPath? getDefaultRepoPath() : this.repoPath, 
				useDefaultRepoPath, this.project, this.reuseGitRepo, this.checkoutBranchReusedRepo, this.gitContextDir);
	}

	private String getDefaultRepoPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
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
		update(connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, 
				this.reuseGitRepo, this.checkoutBranchReusedRepo, this.gitContextDir);
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

	@Override
	public String getGitContextDir() {
		return this.gitContextDir;
	}

	protected String getGitContextDir(IBuildConfig bc) {
		String gitContextDir = null; 
		if (bc != null 
				&& bc.getBuildSource() instanceof IGitBuildSource) {
			gitContextDir = ((IGitBuildSource) bc.getBuildSource()).getContextDir();
		}
		return gitContextDir;
	}

	@Override
	public void setGitContextDir(String gitContextDir) {
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, 
				this.reuseGitRepo, this.checkoutBranchReusedRepo, gitContextDir);
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
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, project, 
				this.reuseGitRepo, this.checkoutBranchReusedRepo, this.gitContextDir);
	}

	@Override
	public boolean isReuseGitRepository() {
		return reuseGitRepo;
	}

	@Override
	public void setReuseGitRepository(boolean reuseGitRepo) {
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, 
				reuseGitRepo, reuseGitRepo, this.gitContextDir);
	}
	
	@Override
	public boolean isCheckoutBranchReusedRepo() {
		return checkoutBranchReusedRepo;
	}

	@Override
	public void setCheckoutBranchReusedRepo(boolean checkout) {
		update(this.connection, this.selectedItem, this.repoPath, this.useDefaultRepoPath, this.project, 
				this.reuseGitRepo, checkout, this.gitContextDir);
	}
}