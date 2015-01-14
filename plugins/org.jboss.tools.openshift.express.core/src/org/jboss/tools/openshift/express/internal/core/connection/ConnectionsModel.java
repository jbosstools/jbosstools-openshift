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
import org.jboss.tools.openshift.core.ConnectionType;
import org.jboss.tools.openshift.core.ConnectionVisitor;
import org.jboss.tools.openshift.core.internal.ConnectionRegistry;
import org.jboss.tools.openshift.core.internal.KubernetesConnection;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.core.ICredentialsPrompter;
import org.jboss.tools.openshift.express.core.OpenShiftCoreException;
import org.jboss.tools.openshift.express.core.OpenshiftCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;
import com.openshift.client.IHttpClient.ISSLCertificateCallback;

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

	/** Kubernetes resources */
	private final ConnectionRegistry kubeConnectionRegistry = new ConnectionRegistry();
	private org.jboss.tools.openshift.core.Connection recentKubeConnection = null;
	
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
	
	public boolean addConnection(org.jboss.tools.openshift.core.Connection connection){
		class AddConnectionVisitor implements ConnectionVisitor {
			boolean added = false;

			@Override
			public void visit(Connection connection) {
				try {
					ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
					added = addConnection(connectionUrl, connection);
				} catch (UnsupportedEncodingException e) {
					throw new OpenShiftCoreException(
							e, "Could not add connection {0}/{1}", connection.getUsername(), connection.getHost());
				} catch (MalformedURLException e) {
					throw new OpenShiftCoreException(
							e, "Could not add connection {0}/{1}", connection.getUsername(), connection.getHost());
				}
			}

			@Override
			public void visit(KubernetesConnection connection) {
				if(kubeConnectionRegistry.add(connection)){
					fireModelChange(connection, ADDED, ConnectionType.Kubernetes);
					added = true;
				}
			};
		}
		AddConnectionVisitor visitor = new AddConnectionVisitor();
		connection.accept(visitor);
		return visitor.added;
	}

	protected boolean addConnection(ConnectionURL connectionUrl, Connection connection) {
		if (connectionsByUrl.containsKey(connectionUrl)) {
			return false;
		}
		connectionsByUrl.put(connectionUrl, connection);
		this.recentConnection = connection;
		fireModelChange(connection, ADDED, ConnectionType.Legacy);
		return true;
	}

	protected boolean addConnection(ConnectionURL connectionUrl) {
		ICredentialsPrompter credentialsPrompter = OpenshiftCoreUIIntegration.getDefault().getCredentialPrompter();
		ISSLCertificateCallback sslAuthorization = OpenshiftCoreUIIntegration.getDefault().getSSLCertificateCallback();
		
		Connection connection =
				new Connection(connectionUrl.getUsername(), connectionUrl.getScheme(), connectionUrl.getHost(), 
						credentialsPrompter, sslAuthorization);
		return addConnection(connectionUrl, connection);
	}

	public boolean hasConnection(Connection connection) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return getConnectionByUrl(connectionUrl) != null;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e,
					NLS.bind("Could not get url for connection {0} - {1}", connection.getUsername(),
							connection.getHost()));
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(e,
					NLS.bind("Could not get url for connection {0} - {1}", connection.getUsername(),
							connection.getHost()));
		}
	}

	// TODO: dont allow/require external trigger to changer notification
	public void fireConnectionChanged(org.jboss.tools.openshift.core.Connection connection) {
		if (connection == null) {
			return;
		}
		ConnectionType type = ConnectionType.Kubernetes;
		if(connection instanceof Connection)
			type = ConnectionType.Legacy;
		fireModelChange(connection, CHANGED, type);
	}

	// TODO: dont allow/require external trigger to changer notification
	public void fireConnectionChanged(IUser user) {
		if (user == null) {
			return;
		}
		Connection connection = getConnectionByResource(user);
		fireConnectionChanged(connection);
	}

	public boolean removeConnection(org.jboss.tools.openshift.core.Connection connection){
		
		class RemoveConnectionVisitor implements ConnectionVisitor {
			boolean removed = false;
			@Override
			public void visit(KubernetesConnection connection) {
				if(!kubeConnectionRegistry.remove(connection)) return;
				setRecent(null);
				fireModelChange(connection, REMOVED, ConnectionType.Kubernetes);
				removed = true;
			}
			
			@Override
			public void visit(Connection connection) {
				try {
					ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
					if (!connectionsByUrl.containsKey(connectionUrl)) {
						removed = false;
						return;
					}
					connectionsByUrl.remove(connectionUrl);
					if (getRecentConnection() == connection) {
						setRecent(null);
					}
					fireModelChange(connection, REMOVED, ConnectionType.Legacy);
					removed = true;
				} catch (UnsupportedEncodingException e) {
					throw new OpenShiftCoreException(e,
							NLS.bind("Could not remove connection {0} - {1}", connection.getUsername(), connection.getHost()));
				} catch (MalformedURLException e) {
					throw new OpenShiftCoreException(e,
							NLS.bind("Could not remove connection {0} - {1}", connection.getUsername(), connection.getHost()));
				}
			}
		};
		RemoveConnectionVisitor visitor = new RemoveConnectionVisitor();
		connection.accept(visitor);
		return visitor.removed;
	}

	private void fireModelChange(org.jboss.tools.openshift.core.Connection  connection, int event, ConnectionType type) {
		if (connection == null) {
			return;
		}
		Iterator<IConnectionsModelListener> i = listeners.iterator();
		while (i.hasNext()) {
			IConnectionsModelListener l = i.next();
			switch (event) {
			case ADDED:
				l.connectionAdded(connection, type);
				break;
			case REMOVED:
				l.connectionRemoved(connection, type);
				break;
			case CHANGED:
				l.connectionChanged(connection, type);
				break;

			default:
				break;
			}
		}
	}

	public Connection getRecentConnection() {
		return recentConnection;
	}
	
	/**
	 * Get the most recently used connection to OpenShift that uses
	 * Kubernetes
	 */
	public org.jboss.tools.openshift.core.Connection getRecentKubeConnection(){
		return recentKubeConnection;
	}

	/**
	 * Returns the connection for a given application (OpenShift REST resource).
	 * 
	 * @param application the openshift application that we want the connection for
	 * @return the connection that this applicaiton belongs to.
	 * 
	 * @throws OpenShiftCoreException
	 */
	public Connection getConnectionByResource(IApplication application) {
		if (application == null) {
			return null;
		}
		
		return getConnectionByResource(application.getDomain().getUser());
	}

	/**
	 * Returns the connection for a given user (OpenShift REST resource).
	 * 
	 * @param user the openshift user
	 * @return the connection that this user belongs to.
	 * 
	 * @throws OpenShiftCoreException
	 */
	public Connection getConnectionByResource(IUser user) throws OpenShiftCoreException {
		if (user == null) {
			return null;
		}
		try {
			ConnectionURL connectionUrl = ConnectionURL.forUsernameAndServer(user.getRhlogin(), user.getServer());
			Connection c = connectionsByUrl.get(connectionUrl);
			String defHost = ConnectionUtils.getDefaultHostUrl();
			if (c == null && defHost.equals(user.getServer())) {
				connectionUrl = ConnectionURL.forUsername(user.getRhlogin());
				c = connectionsByUrl.get(connectionUrl);
			}
			return c;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e, 
					NLS.bind(
					"Could not get connection for user resource {0} - {1}",
					user.getRhlogin(), user.getServer()));
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(e, 
					NLS.bind(
					"Could not get connection for user resource {0} - {1}",
					user.getRhlogin(), user.getServer()));
		}
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
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		}
	}

	/**
	 * Returns all the connections to OpenShift servers that do not
	 * utilize Kubernetes
	 * @return
	 */
	public Connection[] getConnections() {
		Collection<Connection> c = connectionsByUrl.values();
		Connection[] rets = (Connection[]) c.toArray(new Connection[c.size()]);
		return rets;
	}
	
	/**
	 * Return all known connections to OpenShift servers
	 * @return
	 */
	public org.jboss.tools.openshift.core.Connection[] getAllConnections(){
		Collection<org.jboss.tools.openshift.core.Connection> all = new ArrayList<org.jboss.tools.openshift.core.Connection>(connectionsByUrl.values());
		all.addAll(kubeConnectionRegistry.getConnections());
		return all.toArray(new org.jboss.tools.openshift.core.Connection[all.size()]);
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
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", username), e);
			} catch (UnsupportedEncodingException e) {
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", username), e);
			} catch (IllegalArgumentException e) {
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", username), e);
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
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			} catch (UnsupportedEncodingException e) {
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			} catch (IllegalArgumentException e) {
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrlString), e);
			}
		}
	}

	protected String[] loadPersistedCustomHosts() {
		return OpenShiftPreferences.INSTANCE.getConnections();
	}

	public int size() {
		return connectionsByUrl.size();
	}

	public void setRecent(org.jboss.tools.openshift.core.Connection connection) {
		if(connection == null){
			recentConnection = null;
			recentKubeConnection = null;
			return;
		}
		connection.accept(new ConnectionVisitor() {
			
			@Override
			public void visit(KubernetesConnection connection) {
				recentKubeConnection = connection;
			}
			
			@Override
			public void visit(Connection connection) {
				recentConnection = connection;
			}
		});
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

		kubeConnectionRegistry.save();
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
