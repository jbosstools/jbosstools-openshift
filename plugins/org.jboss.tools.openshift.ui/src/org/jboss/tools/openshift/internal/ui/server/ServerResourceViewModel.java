/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
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
import java.util.stream.Collectors;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.utils.ObservableTreeItemUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerResourceViewModel extends ObservablePojo {

	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTIONS = "connections";
	public static final String PROPERTY_RESOURCE = "resource";
	public static final String PROPERTY_RESOURCE_ITEMS = "resourceItems";

	private boolean isLoaded = false;
	private IConnection connection;
	private List<IConnection> connections = new ArrayList<>();
	private List<ObservableTreeItem> resourceItems = new ArrayList<>();
	protected IResource resource;

	public ServerResourceViewModel(IConnection connection) {
		this(null, connection);
	}

	public ServerResourceViewModel(IResource resource, IConnection connection) {
		this.connection = connection;
		this.resource = resource;
	}

	protected void update(IConnection connection, List<IConnection> connections, IResource resource, List<ObservableTreeItem> resourceItems) {
		updateConnections(connections);
		updateConnection(connection);
		updateResourceItems(resourceItems);
		updateResource(resource, resourceItems);
	}

	protected void updateConnection(IConnection connection) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}

	private void updateConnections(List<IConnection> newConnections) {
		List<IConnection> oldItems = new ArrayList<>(this.connections);
		// ensure we're not operating on the same list
		if (newConnections != this.connections) {
			this.connections.clear();
			if (newConnections != null) {
				this.connections.addAll(newConnections);
			}
			firePropertyChange(PROPERTY_CONNECTIONS, oldItems, this.connections);
		}
	}

	private void updateResourceItems(List<ObservableTreeItem> newResourceItems) {
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.resourceItems);
		// ensure we're not operating on the same list
		if (newResourceItems != this.resourceItems) {
			this.resourceItems.clear();
			if (newResourceItems != null) {
				this.resourceItems.addAll(newResourceItems);
			}
			firePropertyChange(PROPERTY_RESOURCE_ITEMS, oldItems, this.resourceItems);
		}
	}

	protected IResource updateResource(final IResource resource, final List<ObservableTreeItem> resourceItems) {
		if (!isLoaded) {
			return resource;
		}

		IResource newResource = getResourceOrDefault(resource, resourceItems);
		firePropertyChange(PROPERTY_RESOURCE, null, this.resource = newResource);
		return newResource;
	}

	public void setConnections(List<IConnection> connections) {
		update(this.connection, connections, this.resource, this.resourceItems);
	}

	public IConnection getConnection() {
		return connection;
	}
	
	public List<IConnection> getConnections() {
		return this.connections;
	}

	public void setConnection(IConnection connection) {
		if(this.connection != connection) {
			update(connection, this.connections, null, Collections.emptyList());
		}
	}

	public List<ObservableTreeItem> getResourceItems() {
		return resourceItems;
	}

	protected void setResourceItems(List<ObservableTreeItem> items) {
		update(this.connection, this.connections, this.resource, items);
	}

	public IResource getResource() {
		if (!isLoaded) {
			return null;
		}
		// reveal selected resource only once model is loaded
		return resource;
	}
	
	public void setResource(IResource resource) {
		update(this.connection, this.connections, resource, this.resourceItems);
	}
	
	protected IProject getOpenShiftProject(IResource resource) {
	    return resource.getProject();
	}

    protected IResource getResourceOrDefault(IResource resource, List<ObservableTreeItem> items) {
        if (resource == null
                || !ObservableTreeItemUtils.contains(resource, items)) {
            IResource newResource = ObservableTreeItemUtils.getFirstModel(IService.class, items);
            if (newResource == null) {
                newResource = ObservableTreeItemUtils.getFirstModel(IDeploymentConfig.class, items);
                if (newResource == null) {
                    newResource = ObservableTreeItemUtils.getFirstModel(IReplicationController.class, items);
                }
            }
            return newResource;
        }
        return resource;
    }

	/**
	 * @param serviceName
	 *            the name of the {@link IService} to look-up
	 * @return the matching {@link IService} previously loaded or
	 *         <code>null</code> if the {@code resourceItems} were not loaded or
	 *         not match was found.
	 */
	public IService getService(final String serviceName) {
		if(this.resourceItems != null) {
			return this.resourceItems.stream().flatMap(ObservableTreeItemUtils::flatten)
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
	public void loadResources(final IConnection connection) {
		this.isLoaded = false;
		setConnections(loadConnections());
		setConnection(connection);
		if (connection != null) {
			List<ObservableTreeItem> serviceItems = loadServices(connection);
			setResourceItems(serviceItems);
		}
		this.isLoaded = true;
		update(this.connection, this.connections, this.resource, this.resourceItems);
	}

	private List<IConnection> loadConnections() {
		return new ArrayList<IConnection>(ConnectionsRegistrySingleton.getInstance().getAll());
	}

	protected List<ObservableTreeItem> loadServices(IConnection connection) {
		if (connection == null) {
			return null;
		}
		ObservableTreeItem connectionItem = ResourceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
	}

	static class ResourceTreeItemsFactory implements IModelFactory {

		private static final ResourceTreeItemsFactory INSTANCE = new ResourceTreeItemsFactory();
			
		/**
		 * Creates a resource items tree with the following structure:
		 * connection
		 * 	|_ project
		 *	|	|_ resource
		 *	|	|_ resource
		 *	|_project
		 *		|_service  
		 */
		@Override
		@SuppressWarnings("unchecked")
		public <T> List<T> createChildren(Object parent) {
			if (parent instanceof Connection) {
				return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
			} else if (parent instanceof IProject) {
				return (List<T>) getProjectResources((IProject) parent);
			}
			return Collections.emptyList();
		}

		private List<IResource> getProjectResources(IProject project) {
		    List<IResource> services = project.getResources(ResourceKind.SERVICE);
		    /*
		     * add DeploymentConfig resources not linked to the services
		     */
		    List<IDeploymentConfig> dcConfigs = project.getResources(ResourceKind.DEPLOYMENT_CONFIG);
		    List<IDeploymentConfig> nonLinkedDcConfigs = new ArrayList<>();
		    dcConfigs.stream().filter(dc -> !services.stream().anyMatch(service -> ResourceUtils.areRelated((IService) service, dc)))
		                      .forEach(dc -> nonLinkedDcConfigs.add(dc));
		    services.addAll(nonLinkedDcConfigs);
		    /* 
		     * add ReplicationController resources not linked to DeploymentConfig
		     */
		    List<IReplicationController> replicationControllers = project.getResources(ResourceKind.REPLICATION_CONTROLLER);
		    List<IReplicationController> nonLinkedReplicationControllers = 
		            replicationControllers.stream().filter(rep -> !dcConfigs.stream().anyMatch(dc -> dc.getName().equals(rep.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME))))
		                                            .collect(Collectors.toList());
		    services.addAll(nonLinkedReplicationControllers);
		    return services;
        }

        @Override
		public ObservableTreeItem create(Object object) {
			return new ObservableTreeItem(object, this);
		}
	}

}
