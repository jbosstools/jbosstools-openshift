/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import static org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIConstants.IMPORT_APPLICATION_DIALOG_SETTINGS_KEY;
import static org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIConstants.REPO_PATH_KEY;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * The new application wizard that allows you to create an application given an
 * OpenShift template
 * 
 * @author jeff.cantrill
 * @author Jeff Maury
 */
public class ImportApplicationWizard extends Wizard implements IWorkbenchWizard, IConnectionAwareWizard<Connection> {

	private ImportApplicationWizardModel model;

	public ImportApplicationWizard() {
		setWindowTitle("Import OpenShift Application");
		setNeedsProgressMonitor(true);
		setDialogSettings(
				DialogSettings.getOrCreateSection(
						OpenShiftCommonUIActivator.getDefault().getDialogSettings(), IMPORT_APPLICATION_DIALOG_SETTINGS_KEY));
		this.model = new ImportApplicationWizardModel();
		String repoPath = loadRepoPath();
		if (StringUtils.isNotBlank(repoPath)) {
		    model.setRepositoryPath(repoPath);
		    model.setUseDefaultRepositoryPath(false);
		}
	}

	public ImportApplicationWizard(Map<IProject, Collection<IBuildConfig>> projectsAndBuildConfigs) {
		this();
		if (projectsAndBuildConfigs != null
				&& projectsAndBuildConfigs.size() == 1) {
			Map.Entry<IProject, Collection<IBuildConfig>> entry = projectsAndBuildConfigs.entrySet().iterator().next();
			IProject project = entry.getKey();
			setConnection(project);
			model.setProject(project);
			setSelectedItem(entry, project);
		}
	}

	/**
	 * Get the default Git import path. Check for a section in the openshift.common.ui
	 * plugin (to share with Openshift v2) and if not found, check for a section in
	 * openshift.ui (initial implementation that was not shared with Openshift v2).
	 * 
	 * @return the found default git path
	 */
	private String loadRepoPath() {
	    String path = getDialogSettings().get(REPO_PATH_KEY);
	    if (path == null) {
	        IDialogSettings settings = DialogSettings.getOrCreateSection(
	        		OpenShiftUIActivator.getDefault().getDialogSettings(), IMPORT_APPLICATION_DIALOG_SETTINGS_KEY);
	        path = settings.get(REPO_PATH_KEY);
	    }
	    return path;
	}
	
	private void setConnection(IProject project) {
		Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(project);
		setModelConnection(connection);
	}

	private void setSelectedItem(Map.Entry<IProject, Collection<IBuildConfig>> entry, IProject project) {
		Collection<IBuildConfig> buildConfigs = entry.getValue();
		if (buildConfigs != null 
				&& buildConfigs.size() == 1) {
			model.setSelectedItem(buildConfigs.iterator().next());
		} else {
			model.setSelectedItem(project);
		}
	}
	
	public ImportApplicationWizardModel getModel() {
	    return model;
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (model.getConnection() != null 
				&& model.getSelectedItem() != null) {
			return;
		}
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if (connection != null) {
			model.setConnection(connection);
		} else {
			IResource resource = UIUtils.getFirstElement(selection, IResource.class);
			if (resource != null) {
				setModelConnection(ConnectionsRegistryUtil.safeGetConnectionFor(resource));
				model.setSelectedItem(resource);
			} else {
				IProject project = UIUtils.getFirstElement(selection, IProject.class);
				if (project != null) {
					setModelConnection(ConnectionsRegistryUtil.safeGetConnectionFor(project));
					model.setSelectedItem(project);
				}
			}
		}
	}

	@Override
	public void addPages() {
		//Skip build config selection page if it's already set
		if (model.getSelectedBuildConfig() == null) {
			addPage(new BuildConfigWizardPage(this, model));
		}
		addPage(new GitCloningWizardPage(this, model));
	}

	@Override
	public boolean performFinish() {
		Job importJob = createImportJob(model.isReuseGitRepository());
		importJob.setUser(true);
		importJob.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				boolean success = JobUtils.isOk(importJob.getResult());
				if (success) {
					saveRepoPath();
				}
				UsageStats.getInstance().importV3Application(model.getConnection().getHost(), success);
			}});
		importJob.schedule();
		return true;
	}

	private ImportJob createImportJob(boolean reuseGitRepository) {
		ImportJob importJob = null;
		if (reuseGitRepository) {
			importJob = new ImportJob(model.getGitUrl(), model.getGitRef(), model.getCloneDestination(),
					model.isCheckoutBranchReusedRepo());
		} else {
			importJob = new ImportJob(model.getGitUrl(), model.getGitRef(), model.getCloneDestination());
		}
		String gitContextDir = model.getGitContextDir();
		if (StringUtils.isNotEmpty(gitContextDir)) {
			importJob.setFilters(Collections.singleton(gitContextDir));
		}
		return importJob;
	}

	private void saveRepoPath() {
		if(!model.isUseDefaultRepositoryPath()) {
			getDialogSettings().put(REPO_PATH_KEY, model.getRepositoryPath());
		} else {
			getDialogSettings().put(REPO_PATH_KEY, ""); //clear the value
		}
	}

	@Override
	public Connection getConnection() {
		return model.getConnection();
	}

	@Override
	public boolean hasConnection() {
		return model.hasConnection();
	}

	@Override
	public void setConnection(Connection connection) {
		model.setConnection(connection);
	}

	@Override
	public Object getContext() {
		return null;
	}

	private void setModelConnection(Connection connection) {
		if (connection != null) {
			model.setConnection(connection);
		}
	}
	
}
