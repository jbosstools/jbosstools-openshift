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
package org.jboss.tools.openshift.internal.ui.wizard.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.project.IProjectRequest;

/**
 * @author jeff.cantrill
 */
public class NewProjectWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_PROJECT_NAME = "projectName";
	public static final String PROPERTY_DESCRIPTION = "description";
	public static final String PROPERTY_DISPLAY_NAME = "displayName";
	
	private String description;
	private String displayName;
	private String projectName;
	private Connection connection;
	private List<IProject> projects;
	private IProject project;

	protected NewProjectWizardModel(Connection connection, List<IProject> projects) {
		this.connection = connection;
		this.projects = projects;
	}

	
	public IProject createProject() {
		if (connection == null) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Could not create project, missing connection.");
			return null;
		}
		IProjectRequest request = connection.getResourceFactory().stub(ResourceKind.PROJECT_REQUEST, getProjectName());
		request.setDescription(getDescription());
		request.setDisplayName(getDisplayName());
		IProject project = (IProject)connection.createResource(request);
		List<IProject> newProjects = new ArrayList<IProject>(projects);
		newProjects.add((IProject)connection.getResource(project));
		
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(
				connection, 
				ConnectionProperties.PROPERTY_PROJECTS, 
				projects, 
				Collections.unmodifiableList(newProjects));
		return this.project = project;
	}
	public String getProjectName() {
		return this.projectName;
	}

	public void setProjectName(String projectName) {
		firePropertyChange(PROPERTY_PROJECT_NAME,
				this.projectName, this.projectName = projectName);
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		firePropertyChange(PROPERTY_DESCRIPTION, this.description, this.description = description);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		firePropertyChange(PROPERTY_DISPLAY_NAME, this.displayName, this.displayName = displayName);
	}
	
	public IProject getProject() {
		return project;
	}
}
