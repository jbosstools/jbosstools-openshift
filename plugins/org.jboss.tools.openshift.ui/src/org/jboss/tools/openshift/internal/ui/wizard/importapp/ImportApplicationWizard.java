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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
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

    /** The dialog settings key */
    private static final String DIALOG_SETTINGS_KEY = "ImportApplicationWizard";
    
    private static final String REPO_PATH_KEY = "repoPath";
    
	private ImportApplicationWizardModel model;

	public ImportApplicationWizard() {
		setWindowTitle("Import OpenShift Application");
		setNeedsProgressMonitor(true);
		setDialogSettings(DialogSettings.getOrCreateSection(OpenShiftUIActivator.getDefault().getDialogSettings(), DIALOG_SETTINGS_KEY));
		this.model = new ImportApplicationWizardModel();
		String repoPath = getDialogSettings().get(REPO_PATH_KEY);
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
			Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(project);
			setModelConnection(connection);
			Collection<IBuildConfig> buildConfigs = entry.getValue();
			model.setProject(project);
			if (buildConfigs != null && buildConfigs.size() == 1) {
				model.setSelectedItem(buildConfigs.iterator().next());
			} else {
				model.setSelectedItem(project);
			}
		}
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
		boolean success = false;
		if( model.getSkipClone()) {
			success = importProjectSkipClone();
		} else {
			success = importProject();
		}
		if (success && !model.isUseDefaultRepositoryPath()) {
		    getDialogSettings().put(REPO_PATH_KEY, model.getRepositoryPath());
		}
		UsageStats.getInstance().importV3Application(model.getConnection().getHost(), success);
		return success;
	}

	private boolean importProjectSkipClone() {
		final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
		ImportJob importJob = new ImportJob(model.getCloneDestination(), delegatingMonitor)
				.setGitRef(model.getGitRef());
		return importProject(importJob, delegatingMonitor);
	}

	private boolean importProject() {
		final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
		ImportJob importJob = new ImportJob(model.getGitUrl(), model.getCloneDestination(), delegatingMonitor)
				.setGitRef(model.getGitRef());
		return importProject(importJob, delegatingMonitor);
	}

	private boolean importProject(ImportJob importJob, DelegatingProgressMonitor delegatingMonitor) {
		try {
			String gitContextDir = model.getGitContextDir();
			if (StringUtils.isNotEmpty(gitContextDir)) {
				importJob.setFilters(Collections.singleton(gitContextDir));
			}
			IStatus jobResult = WizardUtils.runInWizard(importJob, delegatingMonitor, getContainer());
			return JobUtils.isOk(jobResult);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.", 
					OpenShiftUIActivator.statusFactory().errorStatus("An exception occurred while creating local git repository.", e));
			return false;
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
		// TODO Auto-generated method stub
		return null;
	}

	private void setModelConnection(Connection connection) {
		if (connection != null) {
			model.setConnection(connection);
		}
	}
	
}
