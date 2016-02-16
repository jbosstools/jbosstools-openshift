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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public class DeploymentResourceMapper extends ObservablePojo implements OpenShiftAPIAnnotations, IDeploymentResourceMapper{

	private IProjectAdapter projectAdapter;
	private IProject project;
	private IOpenShiftConnection conn;
	private IConnectionsRegistryListener connectionListener = new ConnectionListener();
	private AtomicReference<State> state = new AtomicReference<>(State.UNINITIALIZED);
	private Set<Deployment> deployments = Collections.synchronizedSet(new HashSet<>());
	private IResourceCache cache = new ObservableResourceCache();

	private static enum State {
		UNINITIALIZED, LOADING, LOADED
	}

	public DeploymentResourceMapper(IOpenShiftConnection conn, IProjectAdapter projectAdapter) {
		this.projectAdapter = projectAdapter;
		this.project = projectAdapter.getProject();
		this.conn = conn;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		 ConnectionsRegistrySingleton.getInstance().removeListener(connectionListener);	
		 cache.dispose();
		 deployments.clear();
	}

	@Override
	public synchronized void refresh() {
		deployments.forEach(d->cache.removeListener(d));
		deployments.clear();
		cache.flush();
		state.set(State.UNINITIALIZED);
		buildDeployments();
	}
	
	
	@Override
	public Collection<IResource> getImageStreamTagsFor(IService service) {
		Optional<Deployment> deployment = deployments.stream().filter(d->service.equals(d.getService())).findFirst();
		if(deployment.isPresent()) {
			Set<String> imageRefs = deployment.get()
				.getBuildConfigs()
				.stream()
				.map(bc->ResourceUtils.imageRef((IBuildConfig) bc.getResource())).collect(Collectors.toSet());
			final String projectName = service.getNamespace();
			return imageRefs.stream()
					.map(ref->conn.<IResource>getResource(ResourceKind.IMAGE_STREAM_TAG, projectName, ref))
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	@Override
	public Collection<Deployment> getDeployments() {
		buildDeployments();
		synchronized (deployments) {
			return new HashSet<>(deployments);
		}
	}
	
	private synchronized void setDeployments(Collection<Deployment> newDeployments) {
		Collection<Deployment> old = new HashSet<>(deployments);
		deployments = Collections.synchronizedSet(new HashSet<>(newDeployments));
		firePropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, old, deployments);
	}
	
	private void load(String kind) {
		List<IResource> resources = conn.getResources(kind, project.getName());
		resources.forEach(r->add(r));
	}


	private void buildDeployments() {
		if (state.compareAndSet(State.UNINITIALIZED, State.LOADING)) {
			for (String kind : WatchManager.KINDS) {
				load(kind);
			}
			WatchManager.getInstance().stopWatch(project);
			try {
				ConnectionsRegistrySingleton.getInstance().removeListener(connectionListener);
				Collection<IService> services = cache.getResourcesOf(ResourceKind.SERVICE);
				Collection<Deployment> deployments = new ArrayList<>();
				for (IService service : services) {
					Deployment d = newDeployment(service);
					deployments.add(d);
					synchronized (cache) {
						for (String kind : WatchManager.KINDS) {
							init(d, cache, kind);
						}
					}
				}
				setDeployments(deployments);
			} finally {
				ConnectionsRegistrySingleton.getInstance().addListener(connectionListener);
				state.set(State.LOADED);
				WatchManager.getInstance().startWatch(project);
			}
		}
	}

	public synchronized void add(IResource resource) {
		try {
			Trace.debug("Trying to add resource to deployment {0}", resource);
			if(!cache.add(resource)){
				return;
			}
			projectAdapter.add(resource);
			if(ResourceKind.SERVICE.equals(resource.getKind())) {
				Deployment d = newDeployment((IService) resource);
				if(addDeployment(d)) {
					synchronized (cache) {
						for (String kind : WatchManager.KINDS) {
							init(d, cache, kind);
						}
					}
				}
			}
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	private boolean addDeployment(Deployment deployment) {
		synchronized (deployments) {
			Collection<Deployment> old = new ArrayList<>(deployments);
			if(deployments.add(deployment)) {
				int index = deployments.size() - 1;
				fireIndexedPropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, index, old, new ArrayList<>(deployments));
				return true;
			}
			return false;
		}
	}
	
	private void init(Deployment d, IResourceCache cache, String kind) {
		if(ResourceKind.SERVICE.equals(kind)) return;
		for(IResource resource : cache.getResourcesOf(kind)) {
			d.handleAddToCache(cache, resource);
		}
	}
	
	private Deployment newDeployment(IService service) {
		Deployment d = new Deployment(service, projectAdapter);
		cache.addListener(d);
		return d;
	}
	
	public synchronized void remove(IResource resource) {
		try {
			Trace.debug("Trying to remove resource to deployment {0}", resource);
			if(!cache.remove(resource)) {
				return;
			}
			projectAdapter.remove(resource);
			if(ResourceKind.SERVICE.equals(resource.getKind())) {
				Collection<Deployment> old = new ArrayList<>(deployments);
				Collection<Deployment> clone = new ArrayList<>(deployments);
				int index = 0;
				for (Deployment deployment : clone) {
					if(deployment.getService().equals(resource)) {
						cache.removeListener(deployment);
						deployments.remove(deployment);
						fireIndexedPropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, index, old, new ArrayList<>(deployments));
					}
					++index;
				}
			}
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	public synchronized void update(IResource resource) {
		try {
			Trace.debug("Trying to update resource for a deployment {0}", resource);
			if(!cache.update(resource)) {
				return;
			}
			projectAdapter.update(resource);
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	private class ConnectionListener extends ConnectionsRegistryAdapter {

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (!conn.equals(connection))
				return;
			if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
				if (oldValue == null && newValue != null) {
					// add
					handleChange((IResource) newValue, mapper->add(mapper));

				} else if (oldValue != null && newValue == null) {
					// delete
					handleChange((IResource) oldValue, mapper->remove(mapper));
				} else {
					// update
					handleChange((IResource) newValue, mapper->update(mapper));
				}
			}
		}
		
		private void handleChange(IResource resource, Consumer<IResource> action) {
			if (!project.getName().equals(resource.getNamespace())) {
				return;
			}
			action.accept(resource);
		}
	}
}
