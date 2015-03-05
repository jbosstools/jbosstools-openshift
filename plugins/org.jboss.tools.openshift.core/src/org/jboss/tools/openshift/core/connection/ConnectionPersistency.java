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
package org.jboss.tools.openshift.core.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.core.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.client.OpenShiftException;

/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class ConnectionPersistency {

	private ConnectionSerializer serializer = new ConnectionSerializer();
	private OpenShiftPreferences preferences;

	public ConnectionPersistency(OpenShiftPreferences preferences) {
		this.preferences = preferences;
	}

	public Collection<Connection> load() {
		List<Connection> connections = new ArrayList<Connection>();
		String[] persistedConnections = preferences.loadConnections();
        for (String persistedConnection : persistedConnections) {
        	addConnection(persistedConnection, serializer, connections);
		}
        return connections;
	}
	
	private void addConnection(String serializedConnection, ConnectionSerializer serializer, List<Connection> connections) {
		try {
			connections.add(serializer.deserialize(serializedConnection));
		} catch (OpenShiftException e) {
			OpenShiftCoreActivator.pluginLog().logError(
					String.format("Could not deserialize the connection '%s'", serializedConnection), e);
		}
	}

	public void save(Collection<Connection> connections) {
		List<String> serializedConnections = new ArrayList<String>(connections.size());
		for (Connection connection : connections) {
			addConnection(connection, serializer, serializedConnections);
		}
		preferences.saveConnections(serializedConnections.toArray(new String[] {}));
	}
	
	private void addConnection(Connection connection, ConnectionSerializer serializer, Collection<String> serializedConnections) {
		try {
			serializedConnections.add(serializer.serialize(connection));
		} catch (OpenShiftException e) {
			OpenShiftCoreActivator.pluginLog().logError(
					String.format("Could not serialize the connection '%s'", connection), e);
		}
	}
	
}
