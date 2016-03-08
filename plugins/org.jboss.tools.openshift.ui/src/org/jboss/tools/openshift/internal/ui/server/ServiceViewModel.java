/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.utils.ObservableTreeItemUtils;

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

	private boolean isLoaded = false;
	private Connection connection;
	private List<Connection> connections = new ArrayList<>();
	private List<ObservableTreeItem> serviceItems = new ArrayList<>();
	protected IService service;

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

	protected IService updateService(final IService service, final List<ObservableTreeItem> serviceItems) {
		if (!isLoaded) {
			return service;
		}

		IService newService = getServiceOrDefault(service, serviceItems);
		firePropertyChange(PROPERTY_SERVICE, null, this.service = newService);
		return newService;
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
			update(connection, this.connections, null, Collections.emptyList());
		}
	}

	public List<ObservableTreeItem> getServiceItems() {
		return serviceItems;
	}

	protected void setServiceItems(List<ObservableTreeItem> items) {
		update(this.connection, this.connections, this.service, items);
	}

	public IService getService() {
		if (!isLoaded) {
			return null;
		}
		// reveal selected service only once model is loaded
		return service;
	}
	
	public void setService(IService service) {
		update(this.connection, this.connections, service, this.serviceItems);
	}
	
	protected IProject getOpenShiftProject(IService service) {
		if (service == null
				|| serviceItems.isEmpty()) {
			return null;
		}
		Optional<ObservableTreeItem> projectItem = serviceItems.stream()
				.filter(item -> ObservableTreeItemUtils.getItemFor(service, item.getChildren()) != null)
				.findFirst();
		if(projectItem.isPresent()) {
			return (IProject) projectItem.get().getModel();
		} else {
			return null;
		}
		
	}

	protected IService getServiceOrDefault(final IService service, final List<ObservableTreeItem> items) {
		if (service == null
				|| !ObservableTreeItemUtils.contains(service, items)) {
			return ObservableTreeItemUtils.getFirstModel(IService.class, items);
		}
		return service;
	}

	/**
	 * @param serviceName
	 *            the name of the {@link IService} to look-up
	 * @return the matching {@link IService} previously loaded or
	 *         <code>null</code> if the {@code serviceItems} were not loaded or
	 *         not match was found.
	 */
	public IService getService(final String serviceName) {
		if(this.serviceItems != null) {
			return this.serviceItems.stream().flatMap(ObservableTreeItemUtils::flatten)
					.filter(item -> item.getModel() instanceof IService).map(item -> (IService) item.getModel())
					.filter(service -> service.getName().equals(serviceName)).findFirst().orElseGet(() -> null);
		}
		return null;
	}
	
	/**
	 * Loads the resource from the current {@link Connection}.
	 */
	public void loadResources() {
		loadResources(getConnection());
	}

	/**
	 * On setting new connection externally, resources related to it must be reloaded.
	 * This method should be invoked in an ui job.
	 * @param connection the connection to use to load the resources.
	 */
	public void loadResources(final Connection connection) {
		this.isLoaded = false;
		setConnection(connection);
		setConnections(loadConnections());
		if (connection != null) {
			List<ObservableTreeItem> serviceItems = loadServices(connection);
			setServiceItems(serviceItems);
		}
		this.isLoaded = true;
		update(this.connection, this.connections, this.service, this.serviceItems);
	}

	private List<Connection> loadConnections() {
		return (List<Connection>) ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
	}

	protected List<ObservableTreeItem> loadServices(Connection connection) {
		if (connection == null) {
			return null;
		}
		ObservableTreeItem connectionItem = ServiceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
	}

	static class ServiceTreeItemsFactory implements IModelFactory {

		private static final ServiceTreeItemsFactory INSTANCE = new ServiceTreeItemsFactory();
			
		/**
		 * Creates a service items tree with the following structure:
		 * connection
		 * 	|_ project
		 *	|	|_ service
		 *	|	|_ service
		 *	|_project
		 *		|_service  
		 */
		@Override
		@SuppressWarnings("unchecked")
		public <T> List<T> createChildren(Object parent) {
			if (parent instanceof Connection) {
				return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
			} else if (parent instanceof IProject) {
				return (List<T>) ((IProject) parent).getResources(ResourceKind.SERVICE);
			}
			return Collections.emptyList();
		}

		@Override
		public ObservableTreeItem create(Object object) {
			return new ObservableTreeItem(object, this);
		}
	}

}
