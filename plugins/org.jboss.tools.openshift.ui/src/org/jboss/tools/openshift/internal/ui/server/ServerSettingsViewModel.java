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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsViewModel extends ServiceViewModel {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";
	
	protected org.eclipse.core.resources.IProject deployProject;
	protected List<org.eclipse.core.resources.IProject> projects = new ArrayList<>();
	private String sourcePath;
	protected String podPath;
	private IServerWorkingCopy server;

	public ServerSettingsViewModel(IServerWorkingCopy server, Connection connection) {
		super(connection);
		this.server = server;
	}

	protected void update(Connection connection, List<Connection> connections, org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, IService service, List<ObservableTreeItem> serviceItems) {
		update(connection, connections, service, serviceItems);
		updateProjects(projects);
		if (this.deployProject != deployProject) {
			//project changed, reset default sourcePath
			sourcePath = null;
		}
		deployProject = updateDeployProject(deployProject, projects);
		updateSourcePath(sourcePath, deployProject);
		updatePodPath(podPath);
	}

	private void updateProjects(List<org.eclipse.core.resources.IProject> projects) {
		List<org.eclipse.core.resources.IProject> oldProjects = new ArrayList<>(this.projects);
		// ensure we're not operating on the same list
		List<org.eclipse.core.resources.IProject> newProjects = new ArrayList<>();
		if (projects != null) {
			newProjects.addAll(projects);
		}
		this.projects.clear();
		this.projects.addAll(newProjects);
		firePropertyChange(PROPERTY_PROJECTS, oldProjects, this.projects);
	}

	protected org.eclipse.core.resources.IProject updateDeployProject(org.eclipse.core.resources.IProject deployProject,
			List<org.eclipse.core.resources.IProject> projects) {
		firePropertyChange(PROPERTY_DEPLOYPROJECT, this.deployProject, this.deployProject = getProjectOrDefault(deployProject, projects));
		return this.deployProject;
	}

	protected void updateSourcePath(String sourcePath, org.eclipse.core.resources.IProject deployProject) {
		if (StringUtils.isEmpty(sourcePath)
				&& ProjectUtils.isAccessible(deployProject)) {
			String projectPath = deployProject.getFullPath().toString();
			sourcePath = VariablesHelper.addWorkspacePrefix(projectPath);
		}
		firePropertyChange(PROPERTY_SOURCE_PATH, this.sourcePath, this.sourcePath = sourcePath);
	}

	protected void updatePodPath(String newPodPath) {
		if (newPodPath == null) {
			newPodPath = "/opt/app-root/src";
		}
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = newPodPath);
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), project, this.projects, this.sourcePath, this.podPath, getService(), getServiceItems());
	}

	public org.eclipse.core.resources.IProject getDeployProject() {
		return this.deployProject;
	}

	protected void setProjects(List<org.eclipse.core.resources.IProject> projects) {
		update(getConnection(), getConnections(), this.deployProject, projects, this.sourcePath, this.podPath, getService(), getServiceItems());
	}

	public List<org.eclipse.core.resources.IProject> getProjects() {
		return projects;
	}
	
	public void setSourcePath(String sourcePath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, sourcePath, this.podPath, getService(), getServiceItems());
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setPodPath(String podPath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, podPath, getService(), getServiceItems());
	}

	public String getPodPath() {
		return podPath;
	}

	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		if (project == null) {
			project = CollectionUtils.getFirstElement(projects);
		}
		return project;
	}

	@Override
	public void loadResources() {
		setProjects(loadProjects());
		super.loadResources();
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
	}

	protected List<ObservableTreeItem> loadServices(Connection connection) {
		ObservableTreeItem connectionItem = ServiceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
	}

	public void updateServer() {
		updateServer(server);
	}
	
	private void updateServer(IServerWorkingCopy server) throws OpenShiftException {
		String connectionUrl = getConnectionUrl(getConnection());
		OpenShiftServerUtils.updateServer(
				OpenShiftServerUtils.getServerName(getService(), getConnection()), connectionUrl, getService(), sourcePath, podPath, deployProject, server);
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, getService(), sourcePath, podPath, deployProject);
	}

	private String getConnectionUrl(Connection connection) {
		ConnectionURL connectionUrl;
		try {
			connectionUrl = ConnectionURL.forConnection(connection);
			return connectionUrl.toString();
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftException(e, "Could not get url for connection {0}", connection.getHost());
		}
	}
	
	protected IServerWorkingCopy getServer() {
		return server;
	}

	static class ServiceTreeItemsFactory implements IModelFactory {

		private static final ServiceTreeItemsFactory INSTANCE = new ServiceTreeItemsFactory();
			
		@SuppressWarnings("unchecked")
		public <T> List<T> createChildren(Object parent) {
			if (parent instanceof Connection) {
				return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
			} else if (parent instanceof IProject) {
				return (List<T>) ((IProject) parent).getResources(ResourceKind.SERVICE);
			}
			return Collections.emptyList();
		}

		public List<ObservableTreeItem> create(Collection<?> openShiftObjects) {
			if (openShiftObjects == null) {
				return Collections.emptyList();
			}
			List<ObservableTreeItem> items = new ArrayList<>();
			for (Object openShiftObject : openShiftObjects) {
				ObservableTreeItem item = create(openShiftObject);
				if (item != null) {
					items.add(item);
				}
			}
			return items;
		}

		public ObservableTreeItem create(Object object) {
			return new ObservableTreeItem(object, this);
		}
	}

}
