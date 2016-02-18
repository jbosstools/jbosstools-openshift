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
package org.jboss.tools.openshift.internal.ui.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerEditorModel extends ServerSettingsViewModel {

	public static final String PROPERTY_CONNECTIONS = "connections";
	public static final String PROPERTY_OVERRIDE_PROJECT = "overrideProject";

	private boolean overrideProject = false;

	public OpenShiftServerEditorModel(IServerWorkingCopy server, Connection connection) {
		super(server, connection);
	}

  	private void update(boolean overrideProject, Connection connection, List<Connection> connections,  
  			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
  			String sourcePath, String podPath, Map<String, ProjectImageStreamTags> deploymentMapperByProjectName, 
  			IService service, List<ObservableTreeItem> serviceItems) {
  		update(connection, connections, deployProject, projects, sourcePath, podPath, deploymentMapperByProjectName, service, serviceItems);
	 	firePropertyChange(PROPERTY_OVERRIDE_PROJECT, this.overrideProject, this.overrideProject = overrideProject);
	}

	public boolean isOverrideProject() {
		return overrideProject;
	}

	public void setOverrideProject(boolean overrideProject) {
		update(overrideProject, getConnection(), getConnections(), getDeployProject(), getProjects(), 
				getSourcePath(), getPodPath(), getImageStreamTagsMap(), getService(), getServiceItems());
	}

	@Override
	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		// don't default to 1st element
		return project;
	}

	@Override
	protected IService getServiceOrDefault(IService service, List<ObservableTreeItem> services) {
		// don't default to 1st element
		return service;
	}

	@Override
	protected List<ObservableTreeItem> loadServices(Connection connection) {
		// don't load
		return Collections.emptyList();
	}	
}
