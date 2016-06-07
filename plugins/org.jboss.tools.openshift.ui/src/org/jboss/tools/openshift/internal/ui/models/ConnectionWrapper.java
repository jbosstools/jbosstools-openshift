package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class ConnectionWrapper extends AbstractOpenshiftUIElement<IOpenShiftConnection, OpenshiftUIModel> {
	public static final String[] RESOURCE_KINDS = { ResourceKind.BUILD, ResourceKind.BUILD_CONFIG,
			ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.IMAGE_STREAM, ResourceKind.IMAGE_STREAM_TAG, ResourceKind.POD,
			ResourceKind.ROUTE, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.SERVICE };

	private AtomicReference<LoadingState> state = new AtomicReference<LoadingState>(LoadingState.INIT);
	private Map<IProject, ProjectWrapper> projects = new HashMap<>();
	private ResourceCache resourceCache = new ResourceCache();

	public ConnectionWrapper(OpenshiftUIModel parent, IOpenShiftConnection wrapped) {
		super(parent, wrapped);
	}

	public IOpenShiftConnection getConnection() {
		return getWrapped();
	}

	public Collection<ProjectWrapper> getProjects() {
		synchronized (projects) {
			return new ArrayList<ProjectWrapper>(projects.values());
		}
	}

	public LoadingState getState() {
		return state.get();
	}
	
	void initWith(List<IResource> resources) {
		synchronized (projects) {
			resources.forEach(project -> {
				projects.put((IProject) project, new ProjectWrapper(this, (IProject) project));
			});
			state.set(LoadingState.LOADED);
		}
	}

	public boolean load(IExceptionHandler handler) {
		if (state.compareAndSet(LoadingState.INIT, LoadingState.LOADING)) {
			startLoadJob(handler);
			return true;
		}
		return false;
	}

	void startLoadJob(ProjectWrapper projectWrapper, IExceptionHandler handler) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					WatchManager.getInstance().startWatch(projectWrapper.getResource());
					Collection<IResource> resources = new HashSet<>();
					for (String kind : RESOURCE_KINDS) {
						resources.addAll(projectWrapper.getResource().getResources(kind));
					}
					resources.forEach(r -> resourceCache.add(r));
					projectWrapper.initWithResources(resources);
					projectWrapper.fireChanged();
				} catch (OpenShiftException e) {
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	void startLoadJob(IExceptionHandler handler) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IOpenShiftConnection connection = getConnection();
					List<IResource> projects = connection.getResources(ResourceKind.PROJECT);
					initWith(projects);
					state.compareAndSet(LoadingState.LOADING, LoadingState.LOADED);
					fireChanged();
				} catch (OpenShiftException e) {
					state.compareAndSet(LoadingState.LOADING, LoadingState.INIT);
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@SuppressWarnings("unchecked")
	public void connectionChanged(String property, Object oldValue, Object newValue) {
		if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
			if (newValue != null) {
				IResource newResource = (IResource) newValue;
				ProjectWrapper projectWrapper = findProjectWrapper(newResource);
				if (projectWrapper != null) {
					if (projectWrapper.getResource().equals(newResource)) {
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
						new RuntimeException("Warnign origing"));
			}
		} else if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) && (newValue instanceof List)) {
			handleProjectsChanged((List<IProject>)newValue);
		}
	}

	private void handleProjectsChanged(List<IProject> newValue) {
		Map<IProject, ProjectWrapper> updated = new HashMap<>();
		boolean changed = false;
		synchronized (projects) {
			HashMap<IProject, ProjectWrapper> oldWrappers = new HashMap<>(projects);
			projects.clear();
			for (IProject r : newValue) {
				ProjectWrapper existingWrapper = oldWrappers.remove(r);

				if (existingWrapper == null) {
					ProjectWrapper newWrapper = new ProjectWrapper(this, r);
					projects.put(r, newWrapper);
					changed = true;
				} else {
					projects.put(r, existingWrapper);
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
		Collection<IResource> resources = resourceCache.getResources(oldResource.getProject().getNamespace());
		projectWrapper.updateWithResources(resources);
	}

	private ProjectWrapper findProjectWrapper(IResource oldResource) {
		Collection<ProjectWrapper> projects = getProjects();
		if (projects != null) {
			IProject project = oldResource.getProject();
			for (ProjectWrapper projectWrapper : projects) {
				if (projectWrapper.getResource().equals(project)) {
					return projectWrapper;
				}
			}
		}
		return null;
	}

	protected void handleUpdate(ProjectWrapper projectWrapper, IResource newResource) {
		resourceCache.remove(newResource);
		resourceCache.add(newResource);
		Collection<IResource> resources = resourceCache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		projectWrapper.updateWithResources(resources);
	}

	@Override
	public void refresh() {
		handleProjectsChanged(loadProjects());
		for (ProjectWrapper project : projects.values()) {
			project.refresh();
		}

	}

	private List<IProject> loadProjects() {
		return getConnection().getResources(ResourceKind.PROJECT);
	}

	public void refresh(ProjectWrapper projectWrapper) {
		resourceCache.flush(projectWrapper.getResource().getNamespace());
		WatchManager.getInstance().stopWatch(projectWrapper.getResource());
		WatchManager.getInstance().startWatch(projectWrapper.getResource());
		Collection<IResource> resources = new HashSet<>();
		for (String kind : RESOURCE_KINDS) {
			resources.addAll(projectWrapper.getResource().getResources(kind));
		}
		resources.forEach(r -> resourceCache.add(r));
		projectWrapper.updateWithResources(resources);
	}

}
