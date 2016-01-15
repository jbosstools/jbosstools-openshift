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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServiceViewModel extends ObservablePojo {

	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTIONS = "connections";
	public static final String PROPERTY_SERVICE = "service";
	public static final String PROPERTY_SERVICE_ITEMS = "serviceItems";
	
	private Connection connection;
	private List<Connection> connections = new ArrayList<>();
	private List<ObservableTreeItem> serviceItems = new ArrayList<>();
	private IService service;

	public ServiceViewModel(Connection connection) {
		this(null, connection);
	}

	public ServiceViewModel(IService service, Connection connection) {
		this.connection = connection;
		this.service = service;
	}

	protected void update(Connection connection, List<Connection> connections, IService service, List<ObservableTreeItem> serviceItems) {
		updateConnections(connections);
		updateConnection(connection);
		updateServiceItems(serviceItems);
		updateService(service, serviceItems);
	}

	protected void updateConnection(Connection connection) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}

	private void updateConnections(List<Connection> newConnections) {
		List<Connection> oldItems = new ArrayList<>(this.connections);
		// ensure we're not operating on the same list
		if (newConnections != this.connections) {
			this.connections.clear();
			if (newConnections != null) {
				this.connections.addAll(newConnections);
			}
			firePropertyChange(PROPERTY_CONNECTIONS, oldItems, this.connections);
		}
	}

	private void updateServiceItems(List<ObservableTreeItem> newServiceItems) {
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.serviceItems);
		// ensure we're not operating on the same list
		if (newServiceItems != this.serviceItems) {
			this.serviceItems.clear();
			if (newServiceItems != null) {
				this.serviceItems.addAll(newServiceItems);
			}
			firePropertyChange(PROPERTY_SERVICE_ITEMS, oldItems, this.serviceItems);
		}
	}

	protected void updateService(IService service, List<ObservableTreeItem> serviceItems) {
		firePropertyChange(PROPERTY_SERVICE, null, this.service = getServiceOrDefault(service, serviceItems));
	}

	public void setConnections(List<Connection> connections) {
		update(this.connection, connections, this.service, this.serviceItems);
	}

	public Connection getConnection() {
		return connection;
	}
	
	public List<Connection> getConnections() {
		return this.connections;
	}

	public void setConnection(Connection connection) {
		if(this.connection != connection) {
			//Clean service items immediately, they should be reloaded later in an ui job.
			List<ObservableTreeItem> newServiceItems = this.serviceItems.isEmpty() ? this.serviceItems : new ArrayList<>();
			update(connection, this.connections, null, newServiceItems);
		}
	}

	public List<ObservableTreeItem> getServiceItems() {
		return serviceItems;
	}

	public void setServiceItems(List<ObservableTreeItem> items) {
		IService newService = containsService(items, this.service) ? this.service : null;
		update(this.connection, this.connections, newService, items);
	}

	public IService getService() {
		return service;
	}
	
	public void setService(IService service) {
		update(this.connection, this.connections, service, this.serviceItems);
	}

	protected IService getServiceOrDefault(IService service, List<ObservableTreeItem> items) {
		if (service == null || !containsService(items, service)) {
			service = getDefaultService(items);
		}
		return service;
	}

	private boolean containsService(List<ObservableTreeItem> items, IService service) {
		if (service == null || items == null || items.size() == 0) {
			return false;
		}
		for (ObservableTreeItem item : items) {
			if (item.getModel() instanceof IService) {
				if(item.getModel() == service) {
					return true;
				};
			} else if(containsService(item.getChildren(), service)) {
				return true;
			}
		}
		return false;
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

	public void loadResources() {
		setConnections(loadConnections());
		if (connection == null) {
			return;
		}
		setServiceItems(loadServices(connection));
	}

	/**
	 * On setting new connection externally, resources related to it must be reloaded.
	 * This method should be invoked in an ui job.
	 * @param newConnection
	 */
	public void loadResources(Connection newConnection) {
		setConnection(newConnection);
		if (newConnection == null) {
			return;
		}
		setServiceItems(loadServices(newConnection));
	}

	private List<Connection> loadConnections() {
		return (List<Connection>) ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
	}

	protected List<ObservableTreeItem> loadServices(Connection connection) {
		ObservableTreeItem connectionItem = ServiceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
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
