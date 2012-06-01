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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ProjectAndServerAdapterSettingsWizardPageModel extends ObservableUIPojo {

	/** whether this is a new project or not. */
	public static final String PROPERTY_IS_NEW_PROJECT = "newProject";

	/** The project name, whether it is a new one or not. */
	public static final String PROPERTY_PROJECT_NAME = "projectName";

	/** whether this a server adapter should be created, or not. */
	public static final String PROPERTY_CREATE_SERVER_ADAPTER = "createServerAdapter";

	/** Whether the existing project is a valid one or not. */
	public static final String PROPERTY_EXISTING_PROJECT_VALIDITY = "existingProjectValidity";

	private IOpenShiftExpressWizardModel wizardModel;

	private IStatus existingProjectValidity;

	public ProjectAndServerAdapterSettingsWizardPageModel(IOpenShiftExpressWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setNewProject(true);
	}

	public void setNewProject(boolean newProject) {
		firePropertyChange(PROPERTY_IS_NEW_PROJECT, wizardModel.isNewProject(), wizardModel.setNewProject(newProject));
		if(wizardModel.isNewProject()) {
			setProjectName(null);
		}
		validateExistingProject();
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}

	public void setCreateServerAdapter(boolean createServerAdapter) {
		firePropertyChange(PROPERTY_CREATE_SERVER_ADAPTER, wizardModel.isCreateServerAdapter(),
				wizardModel.setCreateServerAdapter(createServerAdapter));
	}

	public boolean isCreateServerAdapter() {
		return wizardModel.isCreateServerAdapter();
	}

	public void setProjectName(String projectName) {
		firePropertyChange(PROPERTY_PROJECT_NAME, wizardModel.getProjectName(), wizardModel.setProjectName(projectName));
		validateExistingProject();
	}

	public String getProjectName() {
		return wizardModel.getProjectName();
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	public IStatus validateExistingProject() {
		IStatus status = Status.OK_STATUS;
		final String applicationName = getApplicationName();
		if (isNewProject()) {
			if (applicationName == null) {
				status = OpenShiftUIActivator.createErrorStatus("You have to choose an application name");
			} else {
				final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(applicationName);
				if(project.exists()) {
					status = OpenShiftUIActivator.createErrorStatus(
							NLS.bind("A project named {0} already exists in the workspace.", applicationName));
				}
			}
		} else {
			final String projectName = wizardModel.getProjectName();
			if (projectName == null || projectName.isEmpty()) {
				status = OpenShiftUIActivator.createErrorStatus("Select an open project in the workspace.");
			} else {
				final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (!project.exists()) {
					status = OpenShiftUIActivator.createErrorStatus(
							NLS.bind("The project {0} does not exist in your workspace.", projectName));
				} else if (!project.isOpen()) {
					status = OpenShiftUIActivator.createErrorStatus(
							NLS.bind("The project {0} is not open.", projectName));
				}
			}
		}
		setExistingProjectValidity(status);
		return status;
	}

	public void setExistingProjectValidity(IStatus status) {
		firePropertyChange(PROPERTY_EXISTING_PROJECT_VALIDITY, this.existingProjectValidity,
				this.existingProjectValidity = status);
	}

	public IStatus getExistingProjectValidity() {
		return this.existingProjectValidity;
	}

}
