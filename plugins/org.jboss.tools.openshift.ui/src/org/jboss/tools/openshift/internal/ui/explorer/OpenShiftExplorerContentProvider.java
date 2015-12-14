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

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;
import org.jboss.tools.openshift.internal.core.WatchManager;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * Contributes OpenShift 3 specific content to the OpenShift explorer view
 * 
 * @author jeff.cantrill
 */
public class OpenShiftExplorerContentProvider extends BaseExplorerContentProvider {
	
	private Map<IProject, List<Deployment>> deploymentsByProject = new HashMap<>();
	private Map<IService, Deployment> deploymentByService = new HashMap<>();
	
	@Override
	protected void handleConnectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
       
		if (!(connection instanceof Connection)) {
			return;
		}
		if(ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
			if (oldValue == null && newValue != null) {
				// add
				handleAddResource((IResource) newValue);
				
			} else if (oldValue != null && newValue == null) {
				// delete
				handleRemoveResource((IResource) oldValue);
			} else {
				//update
				updateChildrenFromViewer(newValue);
			}
		} else if (ConnectionProperties.PROPERTY_PROJECTS.equals(property)) {
			handleProjectChanges((Connection) connection, oldValue, newValue);
		} else {
			super.handleConnectionChanged(connection, property, oldValue, newValue);
		}
	}
	private void handleAddResource(IResource resource) {
		switch(resource.getKind()) {
		case ResourceKind.SERVICE:
			IService service = (IService) resource;
			Deployment deployment = DeploymentResourceMapper.getDeployment(service);
			deploymentByService.put(service, deployment);
			addChildrenToViewer(resource.getProject(), deployment);
			break;
		case ResourceKind.POD:
			IPod pod = (IPod)resource;
			Map<String, String> labels = pod.getLabels();
			for (IService depService : deploymentByService.keySet()) {
				Map<String, String> selector = depService.getSelector();
				if(selectorsOverlap(selector, labels)) {
					Deployment deploymentForPod = deploymentByService.get(depService);
					deploymentForPod.add(pod);
					addChildrenToViewer(depService, deploymentForPod);
				}
			}
		default:
		}
	}
	private void handleRemoveResource(IResource resource) {
		switch(resource.getKind()) {
		case ResourceKind.SERVICE:
			IService service = (IService)resource;
			deploymentByService.remove(service);
			removeChildrenFromViewer(resource.getProject(), deploymentByService.get(service));
			break;
		case ResourceKind.POD:
			IPod pod = (IPod)resource;
			for (IService deployedService : deploymentByService.keySet()) {
				if(selectorsOverlap(deployedService.getSelector(), pod.getLabels())){
					deploymentByService.get(deployedService).remove(pod);
					removeChildrenFromViewer(deploymentByService, pod);
				}
			}
		default:
		}
	}
	
	private boolean selectorsOverlap(Map<String, String> source, Map<String, String> target) {
		if(!target.keySet().containsAll(source.keySet())) {
			return false;
		}
		for (String key : source.keySet()) {
			if(!target.get(key).equals(source.get(key))) {
				return false;
			}
		}
		return true;
	}
	
	//TODO: Handle updates to a project when needed.  Back-end doesnt support edit of resources(most?) now
	@SuppressWarnings("unchecked")
	private void handleProjectChanges(Connection connection, Object oldValue, Object newValue) {
		List<IProject> newProjects = (List<IProject>) newValue;
		List<IProject> oldProjects = (List<IProject>) oldValue;
		final List<IProject> added = new ArrayList<IProject>();
		final List<IProject> removed = new ArrayList<IProject>();
		ListDiff diffs = Diffs.computeListDiff(oldProjects, newProjects);
		diffs.accept(new ListDiffVisitor() {
			
			@Override
			public void handleRemove(int index, Object element) {
				IProject project = (IProject) element;
				removed.add(project);
				deploymentsByProject.remove(project);
			}
			
			@Override
			public void handleAdd(int index, Object element) {
				IProject project = (IProject) element;
				added.add(project);
			}
		});
		removeChildrenFromViewer(connection, removed.toArray());
		addChildrenToViewer(connection, added.toArray());
	}
	
	@Override
	protected void handleConnectionRemoved(IConnection connection) {
		if(!(connection instanceof Connection))return;
		for (IProject project : deploymentsByProject.keySet()) {
			Connection conn = ConnectionsRegistryUtil.getConnectionFor(project);
			if(connection.equals(conn)) {
				deploymentsByProject.remove(project);
				break;
			}
		}
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
				return connection.getResources(ResourceKind.PROJECT).toArray();
			} else if (parentElement instanceof IProject) {
				IProject project = (IProject) parentElement;
				DeploymentResourceMapper mapper = new DeploymentResourceMapper(ConnectionsRegistryUtil.getConnectionFor(project), project);
				deploymentsByProject.put(project, mapper.getDeployments());
				for (Deployment deployment : mapper.getDeployments()) {
					deploymentByService.put(deployment.getService(),deployment);
				}
				WatchManager.getInstance().startWatch(project);
				return mapper.getDeployments().toArray();

			} else if (parentElement instanceof Deployment) {
				return ((Deployment) parentElement).getPods().toArray();
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
				|| element instanceof Deployment;
	}
}
