/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ConnectionsRegistry {

	/** event that a connection was added */
	private static final int ADDED = 0;
	/** event that a connection was removed */
	private static final int REMOVED = 1;
	/** event that a connection was changed */
	private static final int CHANGED = 2;

	/** The most recent user connected on OpenShift. */
	private IConnection recentConnection = null;
	private Map<ConnectionURL, IConnection> connectionsByUrl = new HashMap<ConnectionURL, IConnection>();
	private List<IConnectionsRegistryListener> listeners = new ArrayList<IConnectionsRegistryListener>();
	private PropertyChangeListener connectionListener = new ConnectionListener();
	
	public ConnectionsRegistry() {
	}

	public synchronized void addListener(IConnectionsRegistryListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(IConnectionsRegistryListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return independent copy of listeners to fire changes. 
	 */
	private synchronized List<IConnectionsRegistryListener> getListeners() {
		return new ArrayList<>(listeners);
	}

	public void clear() {
		IConnection[] connections = connectionsByUrl.values().toArray(new IConnection[connectionsByUrl.size()]);
		for (IConnection connection : connections) {
			remove(connection);
		}
	}

	public boolean add(IConnection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return add(connectionUrl, connection);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(
					e, "Could not add connection {0}", connection.getHost());
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(
					e, "Could not add connection {0}", connection.getHost());
		}
	}

	public void addAll(Collection<? extends IConnection> connections) {
		for (IConnection connection : connections) {
			add(connection);
		}
	}

	protected boolean add(ConnectionURL connectionUrl, IConnection connection) {
		if (connectionsByUrl.containsKey(connectionUrl)) {
			return false;
		}
		addPropertyChangeListener(connection);
		connectionsByUrl.put(connectionUrl, connection);
		this.recentConnection = connection;
		fireChange(connection, ADDED);
		return true;
	}

	public boolean has(IConnection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return getByUrl(connectionUrl) != null;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not get url for connection {0}", connection.getHost()));
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not get url for connection {0}", connection.getHost()));
		}
	}

	// TODO: dont allow/require external trigger to changer notification
	public void fireConnectionChanged(IConnection connection) {
		fireConnectionChanged(connection, null, null, null);
	}
	// TODO: dont allow/require external trigger to changer notification
	public void fireConnectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		if (connection == null) {
			return;
		}
		fireChange(connection, CHANGED, property, oldValue, newValue);
	}

	public boolean remove(IConnection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			if (!connectionsByUrl.containsKey(connectionUrl)) {
				return false;
			}
			connectionsByUrl.remove(connectionUrl);
			removePropertyChangeListener(connection);
			
			if (this.recentConnection == connection) {
				this.recentConnection = null;
			}
			fireChange(connection, REMOVED);
			return true;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not remove connection {0}", connection.getHost()));
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not remove connection {0}", connection.getHost()));
		}
	}

	private void fireChange(IConnection  connection, int event) {
		fireChange(connection, event, null, null, null);
	}
	private void fireChange(IConnection  connection, int event, String property, Object oldValue, Object newValue) {
		if (connection == null) {
			return;
		}
		Iterator<IConnectionsRegistryListener> i = getListeners().iterator();
		while (i.hasNext()) {
			IConnectionsRegistryListener l = i.next();
			switch (event) {
			case ADDED:
				l.connectionAdded(connection);
				break;
			case REMOVED:
				l.connectionRemoved(connection);
				break;
			case CHANGED:
				l.connectionChanged(connection, property, oldValue, newValue);
				break;

			default:
				break;
			}
		}
	}

	public IConnection getRecentConnection() {
		return recentConnection;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IConnection> T getRecentConnection(Class<T> clazz) {
		if (recentConnection == null
				|| clazz == null
				|| !clazz.isAssignableFrom(recentConnection.getClass())) {
			return null;
		}
		return (T) recentConnection;
	}

	public IConnection getByUrl(ConnectionURL connectionUrl) {
		if (connectionUrl == null) {
			return null;
		}
		return connectionsByUrl.get(connectionUrl);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IConnection> T getByUrl(ConnectionURL connectionUrl, Class<T> clazz) {
		IConnection connection = getByUrl(connectionUrl);
		if (connection != null
				&& !clazz.isAssignableFrom(connection.getClass())) {
			return null;
		}
		return (T) connection;
	}
	
	/**
	 * Return a list of connections that are of the given type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends IConnection> Collection<T> getAll(Class<T> clazz) {
		List<T> connections = new ArrayList<T>();
		for (IConnection connection : connectionsByUrl.values()) {
			if (connection != null
					&& clazz.isAssignableFrom(connection.getClass())) {
				connections.add((T) connection);
			}
		}
		return connections;
	}

	public Collection<IConnection> getAll() {
		return connectionsByUrl.values();
	}
	
	public int size() {
		return connectionsByUrl.size();
	}

	public IConnection setRecent(IConnection connection) {
		return this.recentConnection = connection;
	}
	
	private void addPropertyChangeListener(IConnection connection) {
		if (!(connection instanceof ObservablePojo)) {
			return;
		}
		
		((ObservablePojo) connection).addPropertyChangeListener(connectionListener);;
		
	}
	
	private void removePropertyChangeListener(IConnection connection) {
		if (!(connection instanceof ObservablePojo)) {
			return;
		}
		
		((ObservablePojo) connection).removePropertyChangeListener(connectionListener);;
		
	}

	private class ConnectionListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (!(event.getSource() instanceof IConnection)) {
				return;
			}
			fireConnectionChanged((IConnection) event.getSource(), event.getPropertyName(), event.getOldValue(), event.getNewValue());
		}
		
	}

	public void update(IConnection currentConnection, IConnection updatedConnection) {
		ConnectionURL updatedConnectionUrl = null;
		try {
			updatedConnectionUrl = ConnectionURL.forConnection(updatedConnection);
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not update connection {0}", updatedConnection.getHost()));
		}
		ConnectionURL oldConnectionUrl = null;
		try {
			oldConnectionUrl = ConnectionURL.forConnection(currentConnection);
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftCoreException(e, NLS.bind("Could not update connection {0}", currentConnection.getHost()));
		}
		if (!oldConnectionUrl.equals(updatedConnectionUrl)) {
			connectionsByUrl.remove(oldConnectionUrl);
		}
		
		//serious change = username changed
		boolean seriousChange = !updatedConnection.equals(currentConnection);
		//change requiring refresh = password or token changed
		boolean credentialsChange = !updatedConnection.credentialsEqual(currentConnection);
		
		//in case of a serious change, we perform remove+add instead of just updating+emitting change event
		// because the connection hashcode will change, refreshing it in the treeview will cause `widget is disposed` errors
		if (seriousChange) {
			remove(currentConnection);
		}
		currentConnection.update(updatedConnection);
		if (seriousChange) {
			add(currentConnection);
		} else if(credentialsChange) {
			//Property is defined in org.jboss.tools.openshift.core.connection.ConnectionProperties
			fireChange(currentConnection, CHANGED, "openshift.resource.refresh", currentConnection, currentConnection);
		}
		this.recentConnection = currentConnection;
	}
	
}
