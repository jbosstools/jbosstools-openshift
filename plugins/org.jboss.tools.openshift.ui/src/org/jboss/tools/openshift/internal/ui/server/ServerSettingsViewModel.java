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
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsViewModel extends ObservablePojo {

	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTIONS = "connections";
	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_POD_PATH = "podPath";
	public static final String PROPERTY_SERVICE = "service";
	public static final String PROPERTY_SERVICE_ITEMS = "serviceItems";
	
	private Connection connection;
	private org.eclipse.core.resources.IProject deployProject;
	private List<org.eclipse.core.resources.IProject> projects = new ArrayList<>();
	private String podPath;
	private List<ObservableTreeItem> serviceItems = new ArrayList<>();
	private IService service;
	private IServerWorkingCopy server;

	public ServerSettingsViewModel(IServerWorkingCopy server, Connection connection) {
		this.server = server;
		this.connection = connection;
	}

	private void update(Connection connection, org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String podPath, IService service, List<ObservableTreeItem> serviceItems) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		updateProjects(projects);
		firePropertyChange(PROPERTY_DEPLOYPROJECT, this.deployProject, this.deployProject = getProjectOrDefault(deployProject, projects));
		updatePodPath(service);
		updateServiceItems(serviceItems);
		firePropertyChange(PROPERTY_SERVICE, this.service, this.service = getServiceOrDefault(service, serviceItems));
	}

	private void updateProjects(List<org.eclipse.core.resources.IProject> projects) {
		List<org.eclipse.core.resources.IProject> oldItems = new ArrayList<>(this.projects);
		// ensure we're not operating on the same list
		List<org.eclipse.core.resources.IProject> newProjects = new ArrayList<>();
		if (projects != null) {
			newProjects.addAll(projects);
		}
		this.projects.clear();
		this.projects.addAll(newProjects);
		firePropertyChange(PROPERTY_PROJECTS, oldItems, this.projects);
	}

	private void updateServiceItems(List<ObservableTreeItem> serviceItems) {
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.serviceItems);
		// ensure we're not operating on the same list
		List<ObservableTreeItem> newItems = new ArrayList<>();
		if (serviceItems != null) {
			newItems.addAll(serviceItems);
		}
		this.serviceItems.clear();
		this.serviceItems.addAll(newItems);
		firePropertyChange(PROPERTY_SERVICE_ITEMS, oldItems, this.serviceItems);
	}

	private void updatePodPath(IService service) {
		String podPath = "/opt/app-root/src";
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = podPath);
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void setConnection(Connection connection) {
		update(connection, this.deployProject, this.projects, this.podPath, this.service, this.serviceItems);
	}
	
	public Collection<ObservableTreeItem> getServiceItems() {
		return serviceItems;
	}

	private void setServiceItems(List<ObservableTreeItem> items) {
		update(this.connection, this.deployProject, this.projects, this.podPath, this.service, items);
	}

	public IService getService() {
		return service;
	}
	
	public void setService(IService service) {
		update(connection, this.deployProject, this.projects, this.podPath, service, this.serviceItems);
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(this.connection, project, this.projects, this.podPath, this.service, serviceItems);
	}

	public org.eclipse.core.resources.IProject getDeployProject() {
		return this.deployProject;
	}

	private void setProjects(List<org.eclipse.core.resources.IProject> projects) {
		update(this.connection, this.deployProject, projects, this.podPath, this.service, this.serviceItems);
	}

	public List<org.eclipse.core.resources.IProject> getProjects() {
		return projects;
	}
	
	public void setPodPath(String podPath) {
		update(this.connection, this.deployProject, this.projects, this.podPath, this.service, this.serviceItems);
	}

	public String getPodPath() {
		return podPath;
	}

	private IService getServiceOrDefault(IService service, List<ObservableTreeItem> services) {
		if (service == null) {
			service = getDefaultService(services);
		}
		return service;
	}

	private IService getDefaultService(List<ObservableTreeItem> items) {
		if (items == null 
				|| items.size() == 0) {
			return null;
		}
		
		for (ObservableTreeItem item : items) {
			if (item.getModel() instanceof IService) {
				return (IService) item.getModel();
			} else {
				return getDefaultService(item.getChildren());
			}
		}
		
		return null;
	}

	private org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		if (project == null) {
			project = getDefaultProject(projects);
		}
		return project;
	}

	private org.eclipse.core.resources.IProject getDefaultProject(List<org.eclipse.core.resources.IProject> projects) {
		if (projects == null 
				|| projects.size() == 0) {
			return null;
		}
		
		return projects.get(0);
	}

	public void loadResources() {
		setProjects(loadProjects());
		if (connection == null) {
			return;
		}
		setServiceItems(loadServices(connection));
	}

	private List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
	}

	private List<ObservableTreeItem> loadServices(Connection connection) {
		ObservableTreeItem connectionItem = ServiceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
	}

	public void updateServer() {
		updateServer(server);
	}
	
	private void updateServer(IServerWorkingCopy server) throws OpenShiftException {
		String connectionUrl = getConnectionUrl(connection);
		OpenShiftServerUtils.updateServer(
				OpenShiftServerUtils.getServerName(service, connection), connectionUrl, service, podPath, deployProject, server);
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, service, podPath, deployProject);
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
