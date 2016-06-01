package org.jboss.tools.openshift.internal.ui.models2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class OpenshiftUIModel implements IOpenshiftUIElement<IOpenshiftUIElement<?>> {
	public static final String[] RESOURCE_KINDS = { ResourceKind.BUILD, ResourceKind.BUILD_CONFIG,
			ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.IMAGE_STREAM, ResourceKind.IMAGE_STREAM_TAG, ResourceKind.POD,
			ResourceKind.ROUTE, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.SERVICE };

	private Map<ConnectionWrapper, Collection<ProjectWrapper>> projectsByConnection = new HashMap<>();
	private Map<IOpenShiftConnection, ResourceCache> caches = new HashMap<>();
	private List<IElementListener> listeners= new ArrayList<IElementListener>();
	
	public OpenshiftUIModel() {
		ConnectionsRegistrySingleton.getInstance().addListener(new IConnectionsRegistryListener() {

			@Override
			public void connectionRemoved(IConnection connection) {
				// remove all projects
			}

			@Override
			public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
				if (!(connection instanceof IOpenShiftConnection)) {
					return;
				}
				ResourceCache cache = getCache((IOpenShiftConnection) connection);
				if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
					if (newValue != null) {
						IResource newResource = (IResource) newValue;
						ProjectWrapper projectWrapper = findProjectWrapper(connection, newResource);
						if (projectWrapper != null) {
							if (projectWrapper.getResource().equals(newResource)) {
								projectWrapper.updateWith(newResource);
							} else {
								IResource oldVersion = cache.getCachedVersion(newResource);
								if (oldVersion == null) {
									// it's an add
									handleAdd(projectWrapper, cache, newResource);
								} else if (isOlder(oldVersion, newResource)) {
									// it's an update
									handleUpdate(projectWrapper, cache, newResource);
								}
							}
						}
					} else if (oldValue != null) {
						IResource oldResource = cache.getCachedVersion((IResource) oldValue);
						ProjectWrapper projectWrapper = findProjectWrapper(connection, oldResource);
						if (oldResource != null) {
							// it's a remove
							handleRemove(projectWrapper, cache, oldResource);
						}
					}
				} else if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) && (oldValue instanceof List)
						&& (newValue instanceof List)) {

				}
			}

			@Override
			public void connectionAdded(IConnection connection) {
				// don't do anything, will be handled lazily
			}
		});
	}

	protected void handleRemove(ProjectWrapper projectWrapper, ResourceCache cache, IResource oldResource) {
		Collection<IResource> resources = cache.getResources(oldResource.getProject().getNamespace());
		resources.remove(oldResource);
		projectWrapper.updateWithResources(resources);
	}

	private ProjectWrapper findProjectWrapper(IConnection connection, IResource oldResource) {
		Collection<ProjectWrapper> projects = projectsByConnection.get(connection);
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

	protected void handleUpdate(ProjectWrapper projectWrapper, ResourceCache cache, IResource newResource) {
		Collection<IResource> resources = cache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		resources.remove(newResource);
		resources.add(newResource);
		projectWrapper.updateWithResources(resources);
	}

	protected void handleAdd(ProjectWrapper projectWrapper, ResourceCache cache, IResource newResource) {
		Collection<IResource> resources = cache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		resources.add(newResource);
		projectWrapper.updateWithResources(resources);
	}

	private ResourceCache getCache(IOpenShiftConnection connection) {
		synchronized (caches) {
			ResourceCache cache = caches.get(connection);
			if (cache == null) {
				caches.put(connection, new ResourceCache());
			}
			return cache;
		}
	}

	public static boolean isOlder(IResource oldResource, IResource newResource) {
		try {
			int oldVersion = Integer.valueOf(oldResource.getResourceVersion());
			int newVersion = Integer.valueOf(newResource.getResourceVersion());
			return oldVersion < newVersion;
		} catch (NumberFormatException e) {
			// treat this as an update
			return true;
		}
	}

	@Override
	public IOpenshiftUIElement<?> getParent() {
		return null;
	}

	@Override
	public void fireChanged(IOpenshiftUIElement<?> source) {
		if (Display.getCurrent() != null) {
			dispatchChange(source);
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					dispatchChange(source);
				}
			});
		}
	}

	protected void dispatchChange(IOpenshiftUIElement<?> source) {
		Collection<IElementListener> copy= new ArrayList<>();
		synchronized (listeners) {
			copy.addAll(listeners);
		}
		copy.forEach(l->l.elementChanged(source));
	}
	
	public void addListener(IElementListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}
	
	public void removeListener(IElementListener l) {
		synchronized (listeners) {
			int lastIndex = listeners.lastIndexOf(l);
			if (lastIndex >= 0) {
				listeners.remove(lastIndex);
			}
		}
	}


	@Override
	public OpenshiftUIModel getRoot() {
		return this;
	}

	public void startLoadJob(ProjectWrapper projectWrapper) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IOpenShiftConnection connection = projectWrapper.getParent().getConnection();
				ResourceCache cache = getCache(connection);
				Collection<IResource> resources = new HashSet<>();
				for (String kind : RESOURCE_KINDS) {
					resources.addAll(connection.getResources(kind));
				}
				resources.forEach(r -> cache.add(r));
				projectWrapper.initWithResources(resources);
				return Status.OK_STATUS;
			}
		}.schedule();
	}
	
	public void startLoadJob(ConnectionWrapper wrapper) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IOpenShiftConnection connection = wrapper.getConnection();
				List<IResource> projects = connection.getResources(ResourceKind.PROJECT);
				wrapper.initWith(projects);
				return Status.OK_STATUS;
			}
		}.schedule();
	}

}
