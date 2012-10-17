/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.CredentialsPrompter;

/**
 * @author Rob Stryker
 */
public class ConnectionsModel {

	/** event that a connection was added */
	private static final int ADDED = 0;
	/** event that a connection was removed */
	private static final int REMOVED = 1;
	/** event that a connection was changed */
	private static final int CHANGED = 2;

	private static ConnectionsModel model;

	public static ConnectionsModel getDefault() {
		if (model == null)
			model = new ConnectionsModel();
		return model;
	}

	/** The most recent user connected on OpenShift. */
	private Connection recentConnection = null;
	private HashMap<String, Connection> allConnections = new HashMap<String, Connection>();
	private List<IConnectionsModelListener> listeners = new ArrayList<IConnectionsModelListener>();

	private ConnectionsModel() {
		load();
	}

	public void addListener(IConnectionsModelListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IConnectionsModelListener listener) {
		listeners.remove(listener);
	}

	public void addConnection(Connection connection) {
		try {
			allConnections.put(ConnectionUtils.getUrlForConnection(connection), connection);
			this.recentConnection = connection;
			fireModelChange(connection, ADDED);
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(
					NLS.bind("Could not add connection {0}/{1}", connection.getUsername(), connection.getHost()),
					e);
		}
	}

	public boolean hasConnection(Connection connection) {
		try {
			String url = ConnectionUtils.getUrlForConnection(connection);
			return getConnectionByUrl(url) != null;
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(
					NLS.bind("Could not get url for connection {0} - {1}", connection.getUsername(),
							connection.getHost()), e);
			return false;
		}
	}

	public void fireConnectionChanged(Connection connection) {
		fireModelChange(connection, CHANGED);
	}

	public void removeConnection(Connection connection) {
		try {
			allConnections.remove(ConnectionUtils.getUrlForConnection(connection));
			if (this.recentConnection == connection)
				this.recentConnection = null;
			fireModelChange(connection, REMOVED);
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(
					NLS.bind("Could not remove connection {0} - {1}", connection.getUsername(), connection.getHost()),
					e);
		}
	}

	private void fireModelChange(Connection connection, int type) {
		Iterator<IConnectionsModelListener> i = listeners.iterator();
		while (i.hasNext()) {
			IConnectionsModelListener l = i.next();
			switch (type) {
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

	public Connection getRecentConnection() {
		return recentConnection;
	}

	public void setRecentUser(Connection connection) {
		this.recentConnection = connection;
	}

	public Connection getConnectionByUrl(String url) {
		if (url == null) {
			return null;
		}
		return allConnections.get(url);
	}

	public Connection getConnectionByUsernameAndHost(String username, String host) {
		try {
			return getConnectionByUrl(ConnectionUtils.getUrlForUsernameAndHost(username, host));
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get url for connection {0} - {1}", username, host), e);
			return null;
		}
	}

	public Connection getConnectionByUsername(String username) {
		try {
			return getConnectionByUrl(ConnectionUtils.getUrlForUsername(username));
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get url for connection {0}", username), e);
			return null;
		} catch (MalformedURLException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get url for connection {0}", username), e);
			return null;
		}
	}

	public Connection[] getConnections() {
		Collection<Connection> c = allConnections.values();
		Connection[] rets = (Connection[]) c.toArray(new Connection[c.size()]);
		return rets;
	}

	/**
	 * Load the user list from preferences and secure storage
	 */
	private void load() {
		String[] connections = OpenShiftPreferences.INSTANCE.getConnections();
		for (int i = 0; i < connections.length; i++) {
			Connection connection = null;
			try {
				URL connectionUrl = new URL(connections[i]);
				connection = new Connection(connectionUrl, new CredentialsPrompter());
				addConnection(connection);
			} catch (MalformedURLException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", connections[i]), e);
			} catch (UnsupportedEncodingException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", connections[i]), e);
			}
		}
	}

	/**
	 * Save the user list to preferences and secure storage
	 */
	public void save() {
		List<String> persistedConnections = new ArrayList<String>();
		for (Entry<String, Connection> entry : allConnections.entrySet()) {
			Connection connection = entry.getValue();
			connection.save();
			try {
				persistedConnections.add(ConnectionUtils.getUrlForConnection(connection));
			} catch (UnsupportedEncodingException e) {
				OpenShiftUIActivator.log(
						NLS.bind("Could not store connection {0}/{1}", connection.getUsername(), connection.getHost()),
						e);
			}
		}

		OpenShiftPreferences.INSTANCE.saveConnections(
				(String[]) persistedConnections.toArray(new String[persistedConnections.size()]));
	}

}