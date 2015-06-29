/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * Contributes OpenShift v3 specific content to the OpenShift explorer view
 * 
 * @author jeff.cantrill
 */
public class OpenShiftExplorerContentProvider extends BaseExplorerContentProvider{
	
	private static final String [] groupings = new String [] {
		ResourceKind.BUILD_CONFIG, 
		ResourceKind.DEPLOYMENT_CONFIG, 
		ResourceKind.SERVICE, 
		ResourceKind.POD,
		ResourceKind.REPLICATION_CONTROLLER, 
		ResourceKind.BUILD, 
		ResourceKind.IMAGE_STREAM, 
		ResourceKind.ROUTE
	};
	
	private Map<IProject, List<ResourceGrouping>> groupMap = new HashMap<IProject, List<ResourceGrouping>>();
	
	@Override
	protected void handleConnectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		if(!(connection instanceof Connection))return;
		if(ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
			if(oldValue == null && newValue != null) {
				//add
				IResource resource = (IResource)newValue;
				refreshGrouping(groupMap.get(resource.getProject()), resource.getKind());
			}else if(oldValue != null && newValue == null) {
				//delete
				IResource resource = (IResource)oldValue;
				refreshGrouping(groupMap.get(resource.getProject()), resource.getKind());
			}else {
				refreshViewer(newValue);
			}
		}else{
			super.handleConnectionChanged(connection, property, oldValue, newValue);
		}
	}
	
	@Override
	protected void handleConnectionRemoved(IConnection connection) {
		if(!(connection instanceof Connection))return;
		for (IProject project : groupMap.keySet()) {
			Connection conn = ConnectionsRegistryUtil.getConnectionFor(project);
			if(connection.equals(conn)) {
				groupMap.remove(project);
				break;
			}
		}
		super.handleConnectionRemoved(connection);
	}



	private void refreshGrouping(List<ResourceGrouping> groupings, String kind) {
		for (ResourceGrouping group : groupings) {
			if(kind.equals(group.getKind())){
				group.refresh();
				break;
			}
		}
	}

	/**
	 * Called to obtain the root elements of the tree viewer,
	 * which should be Connections
	 */
	@Override
	public Object[] getExplorerElements(final Object parentElement) {
		if(parentElement instanceof ConnectionsRegistry){
			ConnectionsRegistry registry = (ConnectionsRegistry) parentElement;
			return registry.getAll(Connection.class).toArray();
		} else if (parentElement instanceof Connection) {
			return ((Connection) parentElement).get(ResourceKind.PROJECT).toArray();
		} else {
			return new Object[0];
		}
	}
	
	/**
	 * Called to obtain the children of any element in the tree viewer
	 */
	@Override
	public Object[] getChildrenFor(Object parentElement) {
		try{
			if (parentElement instanceof Connection) {
				Connection connection = (Connection) parentElement;
				return connection.get(ResourceKind.PROJECT).toArray();
			} else if (parentElement instanceof IProject) {
				IProject project = (IProject) parentElement;
				List<ResourceGrouping> groups = new ArrayList<ResourceGrouping>(groupings.length);
				for (String kind : groupings) {
					final ResourceGrouping grouping = new ResourceGrouping(kind, project);
					grouping.setRefreshable(new IRefreshable() {
						@Override
						public void refresh() {
							refreshViewer(grouping);
						}
					});
					groups.add(grouping);
				}
				groupMap.put(project, groups);
				return groups.toArray();
			} else if (parentElement instanceof ResourceGrouping) {
				ResourceGrouping group = (ResourceGrouping) parentElement;
				return group.getProject().getResources(group.getKind()).toArray();
			}
		} catch (OpenShiftException e) {
			addException(parentElement, e);
		}
		return new Object[0];
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ConnectionsRegistry
				|| element instanceof IConnection
				|| element instanceof IProject
				|| element instanceof ResourceGrouping;
	}
}
