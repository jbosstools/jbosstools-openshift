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
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractConnectionPersistency<C extends IConnection> {


	public Collection<C> load() {
		List<C> connections = new ArrayList<C>();
		String[] persistedConnections = loadPersisted();
        for (String connectionUrl : persistedConnections) {
        	addConnection(connectionUrl, connections);
		}
        return connections;
	}

	private void addConnection(String connectionUrl, List<C> connections) {
		try {
			C connection = createConnection(createConnectionURL(connectionUrl));
			if (connection != null) {
				connections.add(connection);
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
			return ConnectionURL.forURL(connectionUrl);
		} else {
			return ConnectionURL.forUsername(connectionUrl);
		}
	}

	public void save(Collection<C> connections) {
		if (connections == null) {
			return;
		}

		List<String> serializedConnections = new ArrayList<String>(connections.size());
		for (C connection : connections) {
			addConnection(connection, serializedConnections);
		}
		persist(serializedConnections.toArray(new String[serializedConnections.size()]));
	}

	private void addConnection(C connection, List<String> serializedConnections) {
		try {
			ConnectionURL connectionURL = ConnectionURL.forConnection(connection);
			serializedConnections.add(connectionURL.toString());
		} catch (MalformedURLException e) {
			logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		} catch (UnsupportedEncodingException e) {
			logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		}
	}

	protected abstract String[] loadPersisted();
	
	protected abstract void persist(String[] connections);
	
	protected abstract void logError(String message, Exception e);
	
	protected abstract C createConnection(ConnectionURL connectionURL);
}
