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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * Deployment Resource mapper for a given OpenShift project 
 */
public class DeploymentResourceMapper extends ObservablePojo
		implements OpenShiftAPIAnnotations, IDeploymentResourceMapper {

	private IProjectAdapter projectAdapter;
	private IOpenShiftConnection connection;
	private IConnectionsRegistryListener connectionListener = new ConnectionListener();
	private AtomicReference<State> state = new AtomicReference<>(State.UNINITIALIZED);
	private Set<Deployment> deployments = Collections.synchronizedSet(new HashSet<>());
	private IResourceCache resourceCache = new ObservableResourceCache();

	private static enum State {
		UNINITIALIZED, LOADING, LOADED
	}

	/**
	 * Constructor.
	 * @param connection the OpenShift connection
	 * @param projectAdapter the UI adapter to the OpenShift project 
	 */
	public DeploymentResourceMapper(final IOpenShiftConnection connection, final IProjectAdapter projectAdapter) {
		this.projectAdapter = projectAdapter;
		this.connection = connection;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		 ConnectionsRegistrySingleton.getInstance().removeListener(connectionListener);	
		 resourceCache.dispose();
		 deployments.clear();
	}

	@Override
	public synchronized void refresh() {
		// FIXME: refresh by computing the diff, only !
		deployments.forEach(d->resourceCache.removeListener(d));
		deployments.clear();
		resourceCache.flush();
		state.set(State.UNINITIALIZED);
		buildDeployments();
	}
	
	
	@Override
	public Map<IService, Collection<IResource>> getAllImageStreamTags() {
		return deployments.stream()
				.collect(Collectors.toMap(
						deployment -> deployment.getService(), 
						deployment -> getImageStreamTagsFor(deployment)));
	}

	@Override
	public Collection<IResource> getImageStreamTagsFor(IService service) {
		Optional<Deployment> deployment = deployments.stream().filter(d->service.equals(d.getService())).findFirst();
		if(deployment.isPresent()) {
			return getImageStreamTagsFor(deployment.get());
		}
		return Collections.emptySet();
	}

	private Collection<IResource> getImageStreamTagsFor(Deployment deployment) {
			Set<String> imageRefs = deployment
				.getBuildConfigs()
				.stream()
				.map(bc->ResourceUtils.imageRef((IBuildConfig) bc.getResource())).collect(Collectors.toSet());
			final String projectName = deployment.getService().getNamespace();
			return imageRefs.stream()
					.map(ref->connection.<IResource>getResource(ResourceKind.IMAGE_STREAM_TAG, projectName, ref))
					.collect(Collectors.toSet());
	}

	/**
	 * @return an immutable set of {@link Deployment}
	 */
	@Override
	public Collection<Deployment> getDeployments() {
		buildDeployments();
		synchronized (this.deployments) {
			return Collections.unmodifiableSet(deployments);
		}
	}
	
	@Override
	public <T extends IResource> Collection<T> getResourcesOf(String kind) {
		return this.resourceCache.getResourcesOf(kind);
	}
	
	private void buildDeployments() {
		if (state.compareAndSet(State.UNINITIALIZED, State.LOADING)) {
			WatchManager.getInstance().stopWatch(projectAdapter.getProject());
			ConnectionsRegistrySingleton.getInstance().removeListener(connectionListener);
			try {
				// retrieve all resources of all watched kinds from the current connection and add them to the cache
				Stream.of(WatchManager.KINDS).flatMap(
						kind -> connection.getResources(kind, this.projectAdapter.getProject().getName()).stream())
						.forEach(resource -> resourceCache.add(resource));
				// create deployments from the Service resources
				final Set<Deployment> newDeployments = new HashSet<>(this.deployments);
				resourceCache.getResourcesOf(ResourceKind.SERVICE).stream().map(resource -> (IService) resource)
						.forEach(service -> newDeployments
								.add(createDeployment(this.connection, this.projectAdapter, service, this.resourceCache)));
				firePropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, this.deployments, this.deployments = newDeployments);
			} catch (Exception e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
			} finally {
				ConnectionsRegistrySingleton.getInstance().addListener(connectionListener);
				state.set(State.LOADED);
				WatchManager.getInstance().startWatch(this.projectAdapter.getProject());
			}
		}
	}

	private synchronized void add(final IResource resource) {
		try {
			Trace.debug("Trying to add resource to deployment {0}", resource);
			if(!resourceCache.add(resource)){
				return;
			}
			if(ResourceKind.SERVICE.equals(resource.getKind())) {
				final Deployment deployment = createDeployment(this.connection, this.projectAdapter, (IService) resource, this.resourceCache);
				addDeployment(deployment);
			}
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	private void addDeployment(final Deployment deployment) {
		synchronized (this.deployments) {
			final Collection<Deployment> old = new HashSet<>(deployments);
			if(this.deployments.add(deployment)) {
				// we can't use the fireIndexedPropertyChange method on a Set
				firePropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, old, this.deployments);
			}
		}
	}
	
	/**
	 * Create a {@link Deployment} from the given {@code service}, adds all
	 * underlying {@link IResource} to the given {@code cache} and registers the
	 * given {@code cache} to listen for further changes.
	 * 
	 * @param connection
	 *            the OpenShift connection
	 * @param projectAdapter
	 *            the OpenShift project
	 * @param service
	 *            the OpenShift service
	 * @param cache
	 *            the cache of all {@link IResource}
	 * @return the created deployment
	 */
	private static Deployment createDeployment(final IOpenShiftConnection connection, final IProjectAdapter projectAdapter, final IService service, final IResourceCache cache) {
		final Deployment deployment = new Deployment(connection, projectAdapter, service);
		synchronized (cache) {
			Stream.of(WatchManager.KINDS).filter(kind -> !ResourceKind.SERVICE.equals(kind))
					.flatMap(kind -> cache.getResourcesOf(kind).stream())
					.forEach(resource -> deployment.handleAddToCache(cache, resource));
			cache.addListener(deployment);
		}
		return deployment;
	}
	
	private synchronized void remove(final IResource resource) {
		try {
			Trace.debug("Trying to remove resource to deployment {0}", resource);
			if(!resourceCache.remove(resource)) {
				return;
			}
			if(ResourceKind.SERVICE.equals(resource.getKind())) {
				Collection<Deployment> old = new ArrayList<>(deployments);
				Collection<Deployment> clone = new ArrayList<>(deployments);
				int index = 0;
				for (Deployment deployment : clone) {
					if(deployment.getService().equals(resource)) {
						resourceCache.removeListener(deployment);
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
	
	private synchronized void update(IResource resource) {
		try {
			Trace.debug("Trying to update resource for a deployment {0}", resource);
			if(!resourceCache.update(resource)) {
				return;
			}
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	private class ConnectionListener extends ConnectionsRegistryAdapter {

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (!connection.equals(connection))
				return;
			if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
				if (oldValue == null && newValue != null) {
					// add resource
					applyChange(DeploymentResourceMapper.this::add, (IResource) newValue);
				} else if (oldValue != null && newValue == null) {
					// delete resource
					applyChange(DeploymentResourceMapper.this::remove, (IResource) oldValue);
				} else {
					// update resource
					applyChange(DeploymentResourceMapper.this::update, (IResource) newValue);
				}
			}
		}
		
		/**
		 * Applies the given {@code action} on the given {@code resource} if its namespace matches the current project.
		 * @param action the action to perform
		 * @param resource the resource on which the action should be performed
		 */
		private void applyChange(final Consumer<IResource> action, final IResource resource) {
			if (!DeploymentResourceMapper.this.projectAdapter.getProject().getName().equals(resource.getNamespace())) {
				return;
			}
			action.accept(resource);
		}
	}
}
