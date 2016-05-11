/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractConnectionPersistency<C extends IConnection> {


	public Collection<C> load() {
		Map<String, C> connections = new HashMap<>();
		String[] persistedConnections = loadPersisted();
        for (String connectionUrl : persistedConnections) {
        	addConnection(connectionUrl, connections);
		}
        return new ArrayList<>(connections.values());
	}

	private void addConnection(String connectionUrl, Map<String, C> connections) {
		try {
			C connection = createConnection(createConnectionURL(connectionUrl));
			if (connection != null) {
				connections.put(connectionUrl, connection);
			}
		} catch (MalformedURLException e) {
			logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		} catch (UnsupportedEncodingException e) {
			logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		} catch (IllegalArgumentException e) {
			logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		}
	}

	private ConnectionURL createConnectionURL(String connectionUrl) throws UnsupportedEncodingException, MalformedURLException {
		if (UrlUtils.hasScheme(connectionUrl)) {
			// full url with username and host 
			return ConnectionURL.forURL(connectionUrl);
		} else {
			// username only
			return ConnectionURL.forUsername(connectionUrl);
		}
	}

	public void save(Collection<C> connections) {
		if (connections == null) {
			return;
		}

		Map<String, C> serializedConnections = new HashMap<>(connections.size());
		for (C connection : connections) {
			addConnection(connection, serializedConnections);
		}
		persist(serializedConnections);
	}

	private void addConnection(C connection, Map<String, C> serializedConnections) {
		try {
			ConnectionURL connectionURL = ConnectionURL.forConnection(connection);
			serializedConnections.put(connectionURL.toString(), connection);
		} catch (MalformedURLException e) {
			logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		} catch (UnsupportedEncodingException e) {
			logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		}
	}

	protected abstract String[] loadPersisted();
	
	/**
	 * Persist the connections using the given key as the
	 * connectionURL for the connection
	 * 
	 * @param connections    a map of connctionURL to connection
	 */
	protected abstract void persist(Map<String, C> connections);
	
	protected abstract void logError(String message, Exception e);
	
	protected abstract C createConnection(ConnectionURL connectionURL);
}
