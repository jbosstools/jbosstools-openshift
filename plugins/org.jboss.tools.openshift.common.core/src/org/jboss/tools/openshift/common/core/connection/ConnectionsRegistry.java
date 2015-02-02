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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
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
	
	public ConnectionsRegistry() {
	}

	public void addListener(IConnectionsRegistryListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IConnectionsRegistryListener listener) {
		listeners.remove(listener);
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

	protected boolean add(ConnectionURL connectionUrl, IConnection connection) {
		if (connectionsByUrl.containsKey(connectionUrl)) {
			return false;
		}
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
		if (connection == null) {
			return;
		}
		fireChange(connection, CHANGED);
	}

	public boolean remove(IConnection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			if (!connectionsByUrl.containsKey(connectionUrl)) {
				return false;
			}
			connectionsByUrl.remove(connectionUrl);
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
		if (connection == null) {
			return;
		}
		Iterator<IConnectionsRegistryListener> i = listeners.iterator();
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
				l.connectionChanged(connection);
				break;

			default:
				break;
			}
		}
	}

	public IConnection getRecentConnection() {
		return recentConnection;
	}
	
	public <T extends IConnection> T getRecentConnection(Class<T> clazz) {
		if (recentConnection == null
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
	
	public <T extends IConnection> T getByUrl(ConnectionURL connectionUrl, Class<T> clazz) {
		IConnection connection = getByUrl(connectionUrl);
		if (connection != null
				&& !clazz.isAssignableFrom(connection.getClass())) {
			return null;
		}
		return (T) connection;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IConnection> Collection<T> get(Class<T> clazz) {
		List<T> connections = new ArrayList<T>();
		for (IConnection connection : connectionsByUrl.values()) {
			if (connection != null
					&& clazz.isAssignableFrom(connection.getClass())) {
				connections.add((T) connection);
			}
		}
		return connections;
	}

	public IConnection[] getAll() {
		Collection<IConnection> connection = connectionsByUrl.values();
		return (IConnection[]) connection.toArray(new IConnection[connection.size()]);
	}

	public int size() {
		return connectionsByUrl.size();
	}

	public IConnection setRecent(IConnection connection) {
		return this.recentConnection = connection;
	}
}
