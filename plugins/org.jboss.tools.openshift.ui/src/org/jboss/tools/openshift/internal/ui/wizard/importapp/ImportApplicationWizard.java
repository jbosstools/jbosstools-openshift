/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import org.eclipse.core.runtime.IStatus;
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
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * The new application wizard that allows you to create an application given an
 * OpenShift template
 * 
 * @author jeff.cantrill
 */
public class ImportApplicationWizard extends Wizard implements IWorkbenchWizard, IConnectionAwareWizard<Connection> {

	private ImportApplicationWizardModel model;

	public ImportApplicationWizard() {
		setWindowTitle("Import OpenShift Application");
		setNeedsProgressMonitor(true);
		this.model = new ImportApplicationWizardModel();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
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
				} else {
					ResourceGrouping grouping = UIUtils.getFirstElement(selection, ResourceGrouping.class);
					if (grouping != null) {
						setModelConnection(ConnectionsRegistryUtil.safeGetConnectionFor(grouping.getProject()));
						model.setSelectedItem(grouping.getProject());
					}
				}
			}
		}
	}

	@Override
	public void addPages() {
		addPage(new BuildConfigWizardPage(this, model));
		addPage(new GitCloningWizardPage(this, model));
	}

	@Override
	public boolean performFinish() {
		boolean success = importProject();
		UsageStats.getInstance().importV3Application(model.getConnection().getHost(), success);
		return success;
	}

	private boolean importProject() {
		try {
			final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			ImportJob importJob = new ImportJob(model.getGitUrl(), model.getCloneDestination(), delegatingMonitor)
					.setGitRef(model.getGitRef());
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
	public Connection setConnection(Connection connection) {
		return model.setConnection(connection);
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
