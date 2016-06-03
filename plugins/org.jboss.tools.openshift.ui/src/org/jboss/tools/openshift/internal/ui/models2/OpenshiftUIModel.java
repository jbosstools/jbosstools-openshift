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
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class OpenshiftUIModel implements IOpenshiftUIElement<IOpenshiftUIElement<?>> {
	public static final String[] RESOURCE_KINDS = { ResourceKind.BUILD, ResourceKind.BUILD_CONFIG,
			ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.IMAGE_STREAM, ResourceKind.IMAGE_STREAM_TAG, ResourceKind.POD,
			ResourceKind.ROUTE, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.SERVICE };

	private Map<IOpenShiftConnection, ConnectionWrapper> connections = new HashMap<>();
	private Map<ConnectionWrapper, ResourceCache> caches = new HashMap<>();
	private List<IElementListener> listeners = new ArrayList<IElementListener>();

	private IConnectionsRegistryListener listener;

	public OpenshiftUIModel() {
		listener = new IConnectionsRegistryListener() {

			@Override
			public void connectionRemoved(IConnection connection) {
				synchronized (connections) {
					connections.remove(connection);
				}
				fireChanged();
			}

			@Override
			public void connectionChanged(IConnection c, String property, Object oldValue, Object newValue) {
				ConnectionWrapper connection = connections.get(c);
				if (connection == null) {
					return;
				}
				ResourceCache cache = getCache(connection);
				if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
					if (newValue != null) {
						IResource newResource = (IResource) newValue;
						ProjectWrapper projectWrapper = findProjectWrapper(connection, newResource);
						if (projectWrapper != null) {
							if (projectWrapper.getResource().equals(newResource)) {
								projectWrapper.updateWith((IProject) newResource);
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
						if (oldResource != null) {
							ProjectWrapper projectWrapper = findProjectWrapper(connection, oldResource);
							// it's a remove
							handleRemove(projectWrapper, cache, oldResource);
						}
					} else {
						// old value == null, new value == null, ignore
						OpenShiftUIActivator.log(IStatus.WARNING, "old and new value are null", new RuntimeException("Warnign origing"));
					}
				} else if (ConnectionProperties.PROPERTY_PROJECTS.equals(property) && (oldValue instanceof List)
						&& (newValue instanceof List)) {
					// TODO: handle project changes
				}
			}

			@Override
			public void connectionAdded(IConnection connection) {
				if (!(connection instanceof IOpenShiftConnection)) {
					return;
				}
				synchronized (connections) {
					if (connections.containsKey(connection)) {
						return;
					}
					connections.put((IOpenShiftConnection) connection,
							new ConnectionWrapper(OpenshiftUIModel.this, (IOpenShiftConnection) connection));
				}
				fireChanged();
			}
		};
		ConnectionsRegistry registry = ConnectionsRegistrySingleton.getInstance();
		Collection<IConnection> allConnections = registry.getAll();
		synchronized (connections) {
			for (IConnection connection : allConnections) {
				if (connection instanceof IOpenShiftConnection) {
					connections.put((IOpenShiftConnection) connection,
							new ConnectionWrapper(OpenshiftUIModel.this, (IOpenShiftConnection) connection));
				}
			}
		}
		registry.addListener(listener);
	}

	protected void handleRemove(ProjectWrapper projectWrapper, ResourceCache cache, IResource oldResource) {
		cache.remove(oldResource);
		Collection<IResource> resources = cache.getResources(oldResource.getProject().getNamespace());
		projectWrapper.updateWithResources(resources);
	}

	private ProjectWrapper findProjectWrapper(ConnectionWrapper connection, IResource oldResource) {
		Collection<ProjectWrapper> projects = connection.getProjects();
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
		cache.remove(newResource);
		cache.add(newResource);
		Collection<IResource> resources = cache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		projectWrapper.updateWithResources(resources);
	}

	protected void handleAdd(ProjectWrapper projectWrapper, ResourceCache cache, IResource newResource) {
		cache.add(newResource);
		Collection<IResource> resources = cache.getResources(newResource.getProject().getNamespace());
		// relying in IResource#equals() definition
		projectWrapper.updateWithResources(resources);
	}

	private ResourceCache getCache(ConnectionWrapper connection) {
		synchronized (caches) {
			ResourceCache cache = caches.get(connection);
			if (cache == null) {
				cache= new ResourceCache();
				caches.put(connection, cache);
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
		Collection<IElementListener> copy = new ArrayList<>();
		synchronized (listeners) {
			copy.addAll(listeners);
		}
		copy.forEach(l -> l.elementChanged(source));
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

	public void startLoadJob(ProjectWrapper projectWrapper, IExceptionHandler handler) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					WatchManager.getInstance().startWatch(projectWrapper.getResource());
					ResourceCache cache = getCache(projectWrapper.getParent());
					Collection<IResource> resources = new HashSet<>();
					for (String kind : RESOURCE_KINDS) {
						resources.addAll(projectWrapper.getResource().getResources(kind));
					}
					resources.forEach(r -> cache.add(r));
					projectWrapper.initWithResources(resources);
					projectWrapper.fireChanged();
				} catch (OpenShiftException e) {
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	void startLoadJob(ConnectionWrapper wrapper, IExceptionHandler handler) {
		new Job("load project") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IOpenShiftConnection connection = wrapper.getConnection();
					List<IResource> projects = connection.getResources(ResourceKind.PROJECT);
					wrapper.initWith(projects);
					wrapper.fireChanged();
				} catch (OpenShiftException e) {
					handler.handleException(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public Collection<ConnectionWrapper> getConnections() {
		synchronized (connections) {
			return new ArrayList<ConnectionWrapper>(connections.values());
		}
	}

	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(listener);
	}
}
