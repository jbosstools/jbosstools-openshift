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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIException;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.CredentialsPrompter;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 */
public class ConnectionsModel {

	/** event that a connection was added */
	private static final int ADDED = 0;
	/** event that a connection was removed */
	private static final int REMOVED = 1;
	/** event that a connection was changed */
	private static final int CHANGED = 2;

	/** The most recent user connected on OpenShift. */
	private Connection recentConnection = null;
	private HashMap<ConnectionURL, Connection> connectionsByUrl = new HashMap<ConnectionURL, Connection>();
	private List<IConnectionsModelListener> listeners = new ArrayList<IConnectionsModelListener>();

	protected ConnectionsModel() {
		load();
	}

	public void addListener(IConnectionsModelListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IConnectionsModelListener listener) {
		listeners.remove(listener);
	}

	public void clear() {
		Connection[] connections = connectionsByUrl.values().toArray(new Connection[connectionsByUrl.size()]);
		for (Connection connection : connections) {
			removeConnection(connection);
		}
	}

	public boolean addConnection(Connection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return addConnection(connectionUrl, connection);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftUIException(
					e, "Could not add connection {0}/{1}", connection.getUsername(), connection.getHost());
		} catch (MalformedURLException e) {
			throw new OpenShiftUIException(
					e, "Could not add connection {0}/{1}", connection.getUsername(), connection.getHost());
		}
	}

	protected boolean addConnection(ConnectionURL connectionUrl, Connection connection) {
		if (connectionsByUrl.containsKey(connectionUrl)) {
			return false;
		}
		connectionsByUrl.put(connectionUrl, connection);
		this.recentConnection = connection;
		fireModelChange(connection, ADDED);
		return true;
	}

	protected boolean addConnection(ConnectionURL connectionUrl) {
		Connection connection =
				new Connection(connectionUrl.getUsername(), connectionUrl.getScheme(), connectionUrl.getHost(), new CredentialsPrompter());
		return addConnection(connectionUrl, connection);
	}

	public boolean hasConnection(Connection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return getConnectionByUrl(connectionUrl) != null;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftUIException(e,
					NLS.bind("Could not get url for connection {0} - {1}", connection.getUsername(),
							connection.getHost()));
		} catch (MalformedURLException e) {
			throw new OpenShiftUIException(e,
					NLS.bind("Could not get url for connection {0} - {1}", connection.getUsername(),
							connection.getHost()));
		}
	}

	// TODO: dont allow/require external trigger to changer notification
	public void fireConnectionChanged(Connection connection) {
		fireModelChange(connection, CHANGED);
	}

	public boolean removeConnection(Connection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			if (!connectionsByUrl.containsKey(connectionUrl)) {
				return false;
			}
			connectionsByUrl.remove(connectionUrl);
			if (this.recentConnection == connection) {
				this.recentConnection = null;
			}
			fireModelChange(connection, REMOVED);
			return true;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftUIException(e,
					NLS.bind("Could not remove connection {0} - {1}", connection.getUsername(), connection.getHost()));
		} catch (MalformedURLException e) {
			throw new OpenShiftUIException(e,
					NLS.bind("Could not remove connection {0} - {1}", connection.getUsername(), connection.getHost()));
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

	public Connection getConnectionByUrl(ConnectionURL connectionUrl) {
		if (connectionUrl == null) {
			return null;
		}
		return connectionsByUrl.get(connectionUrl);
	}
	
	/**
	 * Returns the connection for the given username if it exists. The
	 * connection must use the default host to match the query by username.
	 * 
	 * @param username
	 *            the username that the connection must use
	 * @return the connection with the given username that uses the default host
	 * 
	 * @see ConnectionUtils#getDefaultHostUrl()
	 */
	public Connection getConnectionByUsername(String username) {
		try {
			return getConnectionByUrl(ConnectionURL.forUsername(username));
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftUIException(NLS.bind("Could not get url for connection {0}", username), e);
		} catch (MalformedURLException e) {
			throw new OpenShiftUIException(NLS.bind("Could not get url for connection {0}", username), e);
		}
	}

	public Connection[] getConnections() {
		Collection<Connection> c = connectionsByUrl.values();
		Connection[] rets = (Connection[]) c.toArray(new Connection[c.size()]);
		return rets;
	}

	/**
	 * Load the user list from preferences and secure storage
	 */
	protected void load() {
		addDefaultHostConnections(loadPersistedDefaultHosts());
		addCustomHostConnections(loadPersistedCustomHosts());
	}

	private void addDefaultHostConnections(String[] usernames) {
		for (String username : usernames) {
			try {
				ConnectionURL connectionUrl = ConnectionURL.forUsername(username);
				addConnection(connectionUrl);
			} catch (MalformedURLException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", username), e);
			} catch (UnsupportedEncodingException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", username), e);
			} catch (IllegalArgumentException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", username), e);
			}
		}
	}
	protected String[] loadPersistedDefaultHosts() {
		return OpenShiftPreferences.INSTANCE.getLegacyConnections();
	}

	private void addCustomHostConnections(String[] connectionUrls) {
		for (String connectionUrlString : connectionUrls) {
			try {
				ConnectionURL connectionUrl = ConnectionURL.forURL(connectionUrlString);
				addConnection(connectionUrl);
			} catch (MalformedURLException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			} catch (UnsupportedEncodingException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			} catch (IllegalArgumentException e) {
				OpenShiftUIActivator.log(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			}
		}
	}

	protected String[] loadPersistedCustomHosts() {
		return OpenShiftPreferences.INSTANCE.getConnections();
	}

	public int size() {
		return connectionsByUrl.size();
	}

	public Connection setRecent(Connection connection) {
		return this.recentConnection = connection;
	}
	
	/**
	 * Save the user list to preferences and secure storage
	 */
	public void save() {
		List<String> customHostConnections = new ArrayList<String>();
		List<String> defaultHostConnections = new ArrayList<String>();
		for (Entry<ConnectionURL, Connection> entry : connectionsByUrl.entrySet()) {
			Connection connection = entry.getValue();
			connection.save();
			ConnectionURL connectionUrl = entry.getKey();
			
			if (connection.isDefaultHost()) {
				defaultHostConnections.add(connection.getUsername());
			} else {
				customHostConnections.add(connectionUrl.toString());
			}
		}

		saveCustomHostConnections(customHostConnections);
		saveDefaultHostConnections(defaultHostConnections);
	}
	
	protected void saveDefaultHostConnections(List<String> usernames) {
		OpenShiftPreferences.INSTANCE.saveLegacyConnections(
				(String[]) usernames.toArray(new String[usernames.size()]));
	}

	protected void saveCustomHostConnections(List<String> connectionUrls) {
		OpenShiftPreferences.INSTANCE.saveConnections(
				(String[]) connectionUrls.toArray(new String[connectionUrls.size()]));
	}
	
}
