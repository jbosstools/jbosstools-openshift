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
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * Contributes OpenShift v3 specific content to the OpenShift explorer view
 */
public class OpenShiftExplorerContentProvider extends BaseExplorerContentProvider{
	
	private static final ResourceKind [] groupings = new ResourceKind [] {
		ResourceKind.BuildConfig, ResourceKind.DeploymentConfig, ResourceKind.Service, ResourceKind.Pod,
		ResourceKind.ReplicationController, ResourceKind.Build, ResourceKind.ImageRepository
	};

	/**
	 * Called to obtain the root elements of the tree viewer,
	 * which should be Connections
	 */
	@Override
	public Object[] getExplorerElements(final Object parentElement) {
		if(parentElement instanceof ConnectionsRegistry){
			ConnectionsRegistry registry = (ConnectionsRegistry) parentElement;
			return registry.getAll(Connection.class).toArray();
		}
		return null;
	}
	
	@Override
	public Object[] getChildren(Object parentElement){
		if(parentElement instanceof IProject){
			IProject project = (IProject) parentElement;
			List<ResourceGrouping> groups = new ArrayList<ResourceGrouping>(groupings.length);
			for (ResourceKind kind : groupings) {
				groups.add(new ResourceGrouping(kind, project));
			}
			return groups.toArray();
		}
		return super.getChildren(parentElement);
	}
	
	/**
	 * Called to obtain the children of any element in the tree viewer
	 */
	@Override
	public Object[] getChildrenFor(Object parentElement) {
		try{
			if (parentElement instanceof Connection) {
				Connection connection = (Connection) parentElement;
				return connection.get(ResourceKind.Project).toArray();
			}
			if(parentElement instanceof ResourceGrouping){
				return ((ResourceGrouping) parentElement).getResources();
			}
		}catch(OpenShiftException e){
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
