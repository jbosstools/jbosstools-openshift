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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.utils.PojoEventBridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ProjectAndServerAdapterSettingsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION_NAME = "applicationName";

	/** whether this is a new project or not. */
	public static final String PROPERTY_IS_NEW_PROJECT = "newProject";

	/** the project name */
	public static final String PROPERTY_PROJECT_NAME = "projectName";

	/** whether this a server adapter should be created, or not. */
	public static final String PROPERTY_CREATE_SERVER_ADAPTER = "createServerAdapter";

	/** whether we create a skip maven build marker */
	public static final String PROPERTY_SKIP_MAVEN_BUILD = "skipMavenBuild";

	private IOpenShiftApplicationWizardModel wizardModel;

	public ProjectAndServerAdapterSettingsWizardPageModel(IOpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setNewProject(wizardModel.getProject() == null);
		setCreateServerAdapter(true);
		setupWizardModelListeners(wizardModel);
	}

	private void setupWizardModelListeners(IOpenShiftApplicationWizardModel wizardModel) {
		new PojoEventBridge().listenTo(IOpenShiftApplicationWizardModel.PROP_APPLICATION_NAME, wizardModel)
				.forwardTo(PROPERTY_APPLICATION_NAME, this);

	}

	public void setNewProject(boolean newProject) {
		firePropertyChange(PROPERTY_IS_NEW_PROJECT, wizardModel.isNewProject(), wizardModel.setNewProject(newProject));
		if (wizardModel.isNewProject()) {
			setProjectName(null);
		}
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}

	public void setProjectName(String projectName) {
		firePropertyChange(PROPERTY_PROJECT_NAME, wizardModel.getProjectName(),
				wizardModel.setProjectName(projectName));
	}

	public String getProjectName() {
		return wizardModel.getProjectName();
	}

	public void setCreateServerAdapter(boolean createServerAdapter) {
		firePropertyChange(PROPERTY_CREATE_SERVER_ADAPTER, wizardModel.isCreateServerAdapter(),
				wizardModel.setCreateServerAdapter(createServerAdapter));
	}

	public boolean isCreateServerAdapter() {
		return wizardModel.isCreateServerAdapter();
	}

	public void setSkipMavenBuild(boolean skipMavenBuild) {
		firePropertyChange(PROPERTY_SKIP_MAVEN_BUILD, wizardModel.isSkipMavenBuild(),
				wizardModel.setSkipMavenBuild(skipMavenBuild));
	}

	public boolean isSkipMavenBuild() {
		return wizardModel.isSkipMavenBuild();
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	public void reset() {
		setNewProject(wizardModel.isNewProject());
		setProjectName(wizardModel.getProjectName());
		setCreateServerAdapter(wizardModel.isCreateServerAdapter());
	}
}
