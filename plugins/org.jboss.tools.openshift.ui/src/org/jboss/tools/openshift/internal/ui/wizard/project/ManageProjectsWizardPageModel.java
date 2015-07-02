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

import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * @author jeff.cantrill
 */
public class ManageProjectsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_PROJECT = "selectedProject";
	public static final String PROPERTY_PROJECTS = "projects";

	private Connection connection;
	private IProject selectedProject;
	private List<IProject> projects;
	private IConnectionsRegistryListener connectionChangeListener;

	public ManageProjectsWizardPageModel(IProject project, Connection connection) {
		this(connection);
		setSelectedProject(project);
	}

	public ManageProjectsWizardPageModel(Connection connection) {
		this.connection = connection;
		this.connectionChangeListener = onConnectionsChanged();
		ConnectionsRegistrySingleton.getInstance().addListener(connectionChangeListener );
	}

	private IConnectionsRegistryListener onConnectionsChanged() {
		return new IConnectionsRegistryListener() {
		
			@Override
			public void connectionRemoved(IConnection connection) {
				if(connection != null && connection.equals(ManageProjectsWizardPageModel.this.connection)){
					ManageProjectsWizardPageModel.this.connection = null;
					setProjects(Collections.<IProject>emptyList());
				}
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {

				if(connection != null && connection.equals(ManageProjectsWizardPageModel.this.connection) && ConnectionProperties.PROPERTY_PROJECTS.equals(property)){
					setProjects((List<IProject>) newValue); 
				}
			}
			
			@Override
			public void connectionAdded(IConnection connection) {
				if(connection != null && connection.equals(ManageProjectsWizardPageModel.this.connection)){
					ManageProjectsWizardPageModel.this.connection = (Connection) connection;
					loadProjects();
				}
			}
		};
	}

	public void loadProjects() {
		if (connection == null) {
			setProjects(Collections.<IProject>emptyList());
		} else {
			List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
			setProjects(projects);
		}
	}

	public void setProjects(List<IProject> projects) {
		firePropertyChange(PROPERTY_PROJECTS, null, this.projects = projects);
	}

	public List<IProject> getProjects() {
		return projects;
	}

	public void refresh() {
		connection.refresh();
		loadProjects();
	}

	public void setSelectedProject(IProject project) {
		firePropertyChange(PROPERTY_SELECTED_PROJECT, this.selectedProject, this.selectedProject = project);
	}

	public IProject getSelectedProject() {
		return selectedProject;
	}

	public Connection getConnection() {
		return connection;
	}
	
	@Override
	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(connectionChangeListener);
	}
}
