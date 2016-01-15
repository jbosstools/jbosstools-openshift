/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.IAncestorable;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.models.OpenShiftProjectUIModel;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * Contributes OpenShift 3 specific content to the OpenShift explorer view
 * 
 * @author jeff.cantrill
 */
public class OpenShiftExplorerContentProvider extends BaseExplorerContentProvider implements PropertyChangeListener {
	
	private Map<String, IProjectAdapter> projectCache = new HashMap<>();
	
	@Override
	protected void handleConnectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		if (!(connection instanceof Connection)) {
			return;
		}
		if (ConnectionProperties.PROPERTY_PROJECTS.equals(property)) {
			handleProjectChanges((Connection) connection, oldValue, newValue);
		} else if (ConnectionProperties.PROPERTY_REFRESH.equals(property)) {
			refreshViewer(newValue);
		} 
	}
	
	//TODO: Handle updates to a project when needed.  Back-end doesnt support edit of resources(most?) now
	@SuppressWarnings("unchecked")
	private void handleProjectChanges(Connection connection, Object oldValue, Object newValue) {
		List<IProject> newProjects = (List<IProject>) newValue;
		List<IProject> oldProjects = (List<IProject>) oldValue;
		final List<IProjectAdapter> added = new ArrayList<>();
		final List<IProjectAdapter> removed = new ArrayList<>();
		ListDiff diffs = Diffs.computeListDiff(oldProjects, newProjects);
		diffs.accept(new ListDiffVisitor() {
			
			@Override
			public void handleRemove(int index, Object element) {
				IProject project = (IProject) element;
				if(project != null) {
					final String key = getCacheKey(connection, project);
					if(projectCache.containsKey(key)) {
						removed.add(projectCache.remove(key));
					}
				}
			}
			
			@Override
			public void handleAdd(int index, Object element) {
				added.add(newProjectAdapter(connection, (IProject) element));
			}
		});
		removeChildrenFromViewer(connection, removed.toArray());
		addChildrenToViewer(connection, added.toArray());
	}
	
	@Override
	protected void handleConnectionRemoved(IConnection connection) {
		super.handleConnectionRemoved(connection);
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
			return ((Connection) parentElement).getResources(ResourceKind.PROJECT).toArray();
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
				Collection<IProject> projects = connection.<IProject>getResources(ResourceKind.PROJECT);
				Collection<IProjectAdapter> children = new ArrayList<>();
				for (IProject project : projects) {
					children.add(newProjectAdapter(connection, project));
				}
				return children.toArray();
			} else if (parentElement instanceof IProjectAdapter) {
				IProjectAdapter adapter = (IProjectAdapter) parentElement;
				Connection conn = (Connection)adapter.getParent();
				IProject project = adapter.getProject();
				Collection<Deployment> deployments = new ArrayList<>(projectCache.get(getCacheKey(conn, project)).getDeployments());
				for (Deployment deployment : deployments) {
					deployment.addPropertyChangeListener(IProjectAdapter.PROP_PODS, this);
				}
				return deployments.toArray();
			} else if (parentElement instanceof Deployment) {
				Collection<IResourceUIModel> pods = ((Deployment) parentElement).getPods();
				return pods.toArray();
			}
		
		} catch (OpenShiftException e) {
			addException(parentElement, e);
		}
		return new Object[0];
	}
	
	private IProjectAdapter newProjectAdapter(Connection connection, IProject project) {
		OpenShiftProjectUIModel model = new OpenShiftProjectUIModel(connection, project);
		model.addPropertyChangeListener(IProjectAdapter.PROP_DEPLOYMENTS, this);
		projectCache.put(getCacheKey(connection, project), model);
		return model;
	}
	
	private String getCacheKey(Connection connection, IProject project) {
		return connection.toString() + "/" + project.getName();
	}
	
	@Override
	public Object getParent(Object element) {
		if(element instanceof IAncestorable) {
			return ((IAncestorable) element).getParent();
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();
		if(oldValue instanceof List && newValue instanceof List) {
			List oldList = (List)oldValue;
			List newList = (List)newValue;
			ListDiff diffs = Diffs.computeListDiff(oldList, newList);
			List removed = new ArrayList();
			List added = new ArrayList();
			diffs.accept(new ListDiffVisitor() {
				
				@Override
				public void handleRemove(int index, Object element) {
					removed.add(element);
				}
				
				@Override
				public void handleAdd(int index, Object element) {
					added.add(element);
				}
				
				
			});
			for (Object child : removed) {
				Object parent = getParent(child);
				Trace.debug("Explorer remove: parent: {0} / child: {1}", parent, child);
				removeChildrenFromViewer(parent, child);
				if(child instanceof Deployment) {
					((Deployment)child).removePropertyChangeListener(IProjectAdapter.PROP_PODS, this);
				}
			}
			for (Object child : added) {
				Object parent = getParent(child);
				Trace.debug("Explorer add: parent: {0} / child: {1}", parent, child);
				if(child instanceof Deployment) {
					((Deployment)child).addPropertyChangeListener(IProjectAdapter.PROP_PODS, this);
				}
				addChildrenToViewer(parent, child);
			}
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ConnectionsRegistry
				|| element instanceof IConnection
				|| element instanceof IProjectAdapter
				|| element instanceof Deployment;
	}
}
