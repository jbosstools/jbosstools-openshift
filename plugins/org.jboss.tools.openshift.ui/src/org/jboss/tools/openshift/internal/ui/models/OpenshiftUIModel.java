package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.model.IResource;

public class OpenshiftUIModel implements IOpenshiftUIElement<IOpenshiftUIElement<?>> {
	private Map<IOpenShiftConnection, ConnectionWrapper> connections = new HashMap<>();
	private List<IElementListener> listeners = new ArrayList<IElementListener>();

	private IConnectionsRegistryListener listener;
	
	public OpenshiftUIModel() {
		this(ConnectionsRegistrySingleton.getInstance());
	}

	public OpenshiftUIModel(ConnectionsRegistry registry) {
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
				connection.connectionChanged(property, oldValue, newValue);
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

	public Collection<ConnectionWrapper> getConnections() {
		synchronized (connections) {
			return new ArrayList<ConnectionWrapper>(connections.values());
		}
	}

	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(listener);
	}

	@Override
	public void refresh() {
			Map<IOpenShiftConnection, ConnectionWrapper> updated = new HashMap<>();
			boolean changed = false;
			synchronized (connections) {
				HashMap<IOpenShiftConnection, ConnectionWrapper> oldWrappers = new HashMap<>(connections);
				connections.clear();
				for (IConnection r : ConnectionsRegistrySingleton.getInstance().getAll()) {
					if (!(r instanceof IOpenShiftConnection)) {
						return;
					}
					IOpenShiftConnection connection= (IOpenShiftConnection) r;
					ConnectionWrapper existingWrapper = oldWrappers.remove(connection);

					if (existingWrapper == null) {
						ConnectionWrapper newWrapper = new ConnectionWrapper(this, connection);
						connections.put(connection, newWrapper);
						changed = true;
					} else {
						connections.put(connection, existingWrapper);
						updated.put(connection, existingWrapper);
					}
				}
				if (!oldWrappers.isEmpty()) {
					changed = true;
				}
			}

			if (changed) {
				fireChanged();
			}

			updated.keySet().forEach(r -> {
				ConnectionWrapper wrapper = updated.get(r);
				wrapper.updateWith(r);
			});
		for (ConnectionWrapper connection : connections.values()) {
			connection.refresh();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return super.equals(o);
	}
}
