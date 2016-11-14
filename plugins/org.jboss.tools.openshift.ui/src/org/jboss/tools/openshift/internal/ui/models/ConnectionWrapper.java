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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

class ConnectionWrapper extends AbstractOpenshiftUIElement<IOpenShiftConnection, OpenshiftUIModel> implements IConnectionWrapper {
	public static final String[] RESOURCE_KINDS = { 
			ResourceKind.BUILD, 
			ResourceKind.BUILD_CONFIG,
			ResourceKind.DEPLOYMENT_CONFIG, 
			ResourceKind.EVENT, 
			ResourceKind.IMAGE_STREAM, 
			ResourceKind.IMAGE_STREAM_TAG, 
			ResourceKind.POD,
			ResourceKind.ROUTE, 
			ResourceKind.REPLICATION_CONTROLLER, 
			ResourceKind.SERVICE, 
			ResourceKind.TEMPLATE,
			ResourceKind.PVC,
			ResourceKind.PROJECT
		};

	private AtomicReference<LoadingState> state = new AtomicReference<LoadingState>(LoadingState.INIT);
	private Map<String, ProjectWrapper> projects = new HashMap<>();
	private ResourceCache resourceCache = new ResourceCache();

	public ConnectionWrapper(OpenshiftUIModel parent, IOpenShiftConnection wrapped) {
		super(parent, wrapped);
	}

	public Collection<IResourceWrapper<?, ?>> getResources() {
		synchronized (projects) {
			return new ArrayList<IResourceWrapper<?, ?>>(projects.values());
		}
	}

	@Override
	public Collection<IResourceWrapper<?, ?>> getResourcesOfKind(String kind) {
		if (!ResourceKind.PROJECT.equals(kind)) {
			return Collections.emptyList();
		}
		return getResources();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResourceWrapper<?, ?>> Collection<T> getResourcesOfType(Class<T> clazz) {
		ArrayList<T> result= new ArrayList<>();
		for (IResourceWrapper<?, ?> r : getResources()) {
			if (clazz.isInstance(r)) {
				result.add((T) r);
			}
		}
		return result;
	}
	
	public LoadingState getState() {
		return state.get();
	}
	
	void initWith(List<IProject> resources) {
		synchronized (projects) {
			resources.forEach(project -> {
				projects.put(project.getName(), new ProjectWrapper(this, project));
			});
		}
		state.set(LoadingState.LOADED);
	}

	public boolean load(IExceptionHandler handler) {
		if (state.compareAndSet(LoadingState.INIT, LoadingState.LOADING)) {
			startLoadJob(handler);
			return true;
		}
		return false;
	}

	void startLoadJob(ProjectWrapper projectWrapper, IExceptionHandler handler) {
		new Job("Load project contents") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject project = projectWrapper.getWrapped();
					IOpenShiftConnection connection = projectWrapper.getParent().getWrapped();
					WatchManager.getInstance().startWatch(project, connection);
					Collection<IResource> resources = new HashSet<>();
					for (String kind : RESOURCE_KINDS) {
						resources.addAll(getWrapped().getResources(kind, project.getNamespace()));
					}
					resources.forEach(r -> resourceCache.add(r));
					projectWrapper.initWithResources(resources);
					projectWrapper.fireChanged();
				} catch (OperationCanceledException e) {
					projectWrapper.setLoadingState(LoadingState.LOAD_STOPPED);
				} catch (Throwable e) {
					projectWrapper.setLoadingState(LoadingState.LOAD_STOPPED);
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private void startLoadJob(IExceptionHandler handler) {
		new Job("Load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IOpenShiftConnection connection = getWrapped();
					List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
					initWith(projects);
					state.compareAndSet(LoadingState.LOADING, LoadingState.LOADED);
					fireChanged();
				} catch (OperationCanceledException e) {
					state.compareAndSet(LoadingState.LOADING, LoadingState.LOAD_STOPPED);
				} catch (Throwable e) {
					state.compareAndSet(LoadingState.LOADING, LoadingState.LOAD_STOPPED);
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@SuppressWarnings("unchecked")
	void connectionChanged(String property, Object oldValue, Object newValue) {
		if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
			if (newValue != null) {
				IResource newResource = (IResource) newValue;
				ProjectWrapper projectWrapper = findProjectWrapper(newResource);
				if (projectWrapper != null) {
					if (projectWrapper.getWrapped().equals(newResource)) {
						projectWrapper.updateWith((IProject) newResource);
					} else {
						IResource oldVersion = resourceCache.getCachedVersion(newResource);
						if (oldVersion == null) {
							// it's an add
							handleAdd(projectWrapper, newResource);
						} else if (OpenshiftUIModel.isOlder(oldVersion, newResource)) {
							// it's an update
							handleUpdate(projectWrapper, newResource);
						}
					}
				} else if (oldValue != null) { 
					// for Pods, which were marked for deletion and whose projects are already deleted
					resourceCache.remove((IResource)oldValue);
				}
			} else if (oldValue != null) {
				IResource oldResource = resourceCache.getCachedVersion((IResource) oldValue);
				if (oldResource != null) {
					ProjectWrapper projectWrapper = findProjectWrapper(oldResource);
					// it's a remove
					handleRemove(projectWrapper, oldResource);
				}
			} else {
				// old value == null, new value == null, ignore
				OpenShiftUIActivator.log(IStatus.WARNING, "old and new value are null",
						new RuntimeException("Warning origing"));
			}
		} else if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) && (newValue instanceof List)) {
			updateWithResources((List<IProject>)newValue);
		}
	}

	private ProjectWrapper findProjectWrapper(IResource resource) {
		synchronized (projects) {
			return projects.get(resource.getNamespace());
		}
	}

	private void updateWithResources(List<IProject> newValue) {
		Map<IProject, ProjectWrapper> updated = new HashMap<>();
		boolean changed = false;
		synchronized (projects) {
			HashMap<String, ProjectWrapper> oldWrappers = new HashMap<>(projects);
			projects.clear();
			for (IProject r : newValue) {
				ProjectWrapper existingWrapper = oldWrappers.remove(r.getName());
				if (existingWrapper == null) {
					ProjectWrapper newWrapper = new ProjectWrapper(this, r);
					resourceCache.add(r);
					projects.put(r.getName(), newWrapper);
					changed = true;
				} else {
					projects.put(r.getName(), existingWrapper);
					updated.put(r, existingWrapper);
				}
			}
			if (!oldWrappers.isEmpty()) {
				changed = true;
			}
		}
		updated.keySet().forEach(r -> {
			ProjectWrapper wrapper = updated.get(r);
			wrapper.updateWith(r);
		});		

		if (changed) {
			fireChanged();
		}
	}

	protected void handleAdd(ProjectWrapper projectWrapper, IResource newResource) {
		resourceCache.add(newResource);
		Collection<IResource> resources = resourceCache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		projectWrapper.updateWithResources(resources);
	}

	protected void handleRemove(ProjectWrapper projectWrapper, IResource oldResource) {
		resourceCache.remove(oldResource);
		if (oldResource instanceof IProject) {
			synchronized(projects) {
				projects.remove(oldResource.getName());
				resourceCache.flush(oldResource.getName());
				fireChanged();
			}
		} else if (projectWrapper != null) {
			Collection<IResource> resources = resourceCache.getResources(oldResource.getNamespace());
			projectWrapper.updateWithResources(resources);
		}
	}

	protected void handleUpdate(ProjectWrapper projectWrapper, IResource newResource) {
		resourceCache.remove(newResource);
		resourceCache.add(newResource);
		Collection<IResource> resources = resourceCache.getResources(newResource.getNamespace());
		// relying in IResource#equals() definition
		projectWrapper.updateWithResources(resources);
	}

	@Override
	public void refresh() {
		updateWithResources(loadProjects());
		state.set(LoadingState.LOADED);
		fireChanged();
		for (ProjectWrapper project : projects.values()) {
			project.refresh();
		}
	}

	private List<IProject> loadProjects() {
		return getWrapped().getResources(ResourceKind.PROJECT);
	}

	void refresh(ProjectWrapper projectWrapper) {
		resourceCache.flush(projectWrapper.getWrapped().getNamespace());
		IProject project = projectWrapper.getWrapped();
		IOpenShiftConnection connection = projectWrapper.getParent().getWrapped();
		WatchManager.getInstance().stopWatch(project, connection);
		WatchManager.getInstance().startWatch(project, connection);
		Collection<IResource> resources = new HashSet<>();
		for (String kind : RESOURCE_KINDS) {
			resources.addAll(getWrapped().getResources(kind, project.getNamespace()));
		}
		resources.forEach(r -> resourceCache.add(r));
		projectWrapper.updateWithResources(resources);
	}

}
