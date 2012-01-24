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
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
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

	private AbstractOpenShiftApplicationWizardModel wizardModel;

	private IStatus existingProjectValidity;

	public ProjectAndServerAdapterSettingsWizardPageModel(AbstractOpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setNewProject(true);
	}

	public void setNewProject(boolean newProject) {
		firePropertyChange(PROPERTY_IS_NEW_PROJECT, wizardModel.isNewProject(), wizardModel.setNewProject(newProject));
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
		// skip the validation if the user wants to create a new project. The name and state of the existing project do
		// not matter...
		final String applicationName = getApplicationName();
		if (isNewProject() && applicationName != null) {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(applicationName);
			if(project.exists()) {
				status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "A project named '" + applicationName + "' already exists in the workspace.");
			}
		} else {
			final String projectName = wizardModel.getProjectName();
			if (projectName == null || projectName.isEmpty()) {
				status = new Status(IStatus.CANCEL, OpenShiftUIActivator.PLUGIN_ID,
						"Select an open project in the workspace.");
			} else {
				final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (!project.exists()) {
					status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "The project does not exist.");
				} else if (!project.isOpen()) {
					status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "The project is not open.");
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
