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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;

import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift3.client.model.IProject;

public class ExplorerContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * Called to obtain the root elements of the tree viewer,
	 * which should be Connections
	 */
	@Override
	public Object[] getElements(final Object parentElement) {
		if(parentElement instanceof ConnectionsRegistry){
			ConnectionsRegistry registry = (ConnectionsRegistry) parentElement;
			IConnection[] all = registry.getAll();
			List<IConnection> connections = new ArrayList<IConnection>(all.length);
			for (IConnection conn : all) {
				if(conn instanceof Connection){
					connections.add(conn);
				}
			}
			return connections.toArray();
		}
		return null;
	}

	/**
	 * Called to obtain the children of any element in the tree viewer
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Connection) {
			Connection connection = (Connection) parentElement;
			return connection.getProjects().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ConnectionsRegistry
				|| element instanceof IConnection
				|| element instanceof IProject;
	}

//	static class ResourceGrouping {
//		private List<Resource> resources;
//		private String title;
//		private ResourceKind kind;
//
//		ResourceGrouping(String title, List<Resource> resources, ResourceKind kind) {
//			this.title = title;
//			this.resources = resources;
//			this.kind = kind;
//		}
//
//		public ResourceKind getKind() {
//			return kind;
//		}
//
//		public Object[] getResources() {
//			return resources.toArray();
//		}
//
//		@Override
//		public String toString() {
//			return title;
//		}
//	}

}
