/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * UI Model for an OpenShift Project
 * @author jeff.cantrill
 *
 */
public class OpenShiftProjectUIModel extends ObservableUIPojo
		implements IResourcesUIModel, IResourceUIModel, IProjectAdapter, IRefreshable, PropertyChangeListener {

	private final IOpenShiftConnection connection;
	private final IProject project;
	private final IDeploymentResourceMapper mapper;
	private AtomicBoolean deleting = new AtomicBoolean(false);

	/**
	 * Constructor.
	 * @param connection the OpenShift connection
	 * @param project the OpenShift project
	 */
	public OpenShiftProjectUIModel(final IOpenShiftConnection connection, final IProject project) {
		this.connection = connection;
		this.project = project;
		this.mapper = new DeploymentResourceMapper(connection, this);
		this.mapper.addPropertyChangeListener(PROP_DEPLOYMENTS, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		mapper.dispose();
	}
	
	@Override
	public void setDeleting(boolean deleting) {
		this.deleting.set(deleting);
	}

	@Override
	public boolean isDeleting() {
		return deleting.get();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (event instanceof IndexedPropertyChangeEvent) {
			fireIndexedPropertyChange(event.getPropertyName(), ((IndexedPropertyChangeEvent) event).getIndex(),
					event.getOldValue(), event.getNewValue());
		} else {
			firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
		}
	}

	@Override
	public Collection<Deployment> getDeployments() {
		return mapper.getDeployments();
	}

	@Override
	public void refresh() {
		mapper.refresh();
	}
	
	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public IResource getResource() {
		return getProject();
	}

	@Override
	public IOpenShiftConnection getConnection() {
		return this.connection;
	}
	
	@Override
	public String toString() {
		return this.project.getName();
	}

	@Override
	public Object getParent() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		if(adapter.equals(IProject.class)) {
			return (T) this.project;
		} else if(adapter.equals(IResource.class)) {
				return (T) this.project;
		} else if(adapter.equals(Connection.class)) {
			return (T) this.connection;
		}
		return null;
	}

	@Override
	public Collection<IResourceUIModel> getBuilds() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getBuilds().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getImageStreams() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getImageStreams().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getDeploymentConfigs() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getDeploymentConfigs().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getPods() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getPods().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getRoutes() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getRoutes().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getReplicationControllers() {
		return this.mapper.getDeployments().stream()
				.flatMap(deployment -> deployment.getReplicationControllers().stream()).collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getBuildConfigs() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getBuildConfigs().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<IResourceUIModel> getServices() {
		return this.mapper.getDeployments().stream().flatMap(deployment -> deployment.getServices().stream())
				.collect(Collectors.toList());
	}
	
}
