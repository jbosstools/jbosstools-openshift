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

import java.util.Map;
import java.util.Map.Entry;

import org.jboss.tools.openshift.common.core.connection.AbstractConnectionPersistency;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCorePreferences;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class ConnectionPersistency extends AbstractConnectionPersistency<Connection>{
	
	private final IOpenShiftCorePreferences preferences;
	
	public ConnectionPersistency() {
		this(OpenShiftCorePreferences.INSTANCE);
	}
	
	protected ConnectionPersistency(IOpenShiftCorePreferences preferences) {
		this.preferences = preferences;
	}
	
	@Override
	protected String[] loadPersisted() {
		return preferences.loadConnections();
	}

	@Override
	protected void persist(Map<String, Connection> connections) {
		preferences.saveConnections(connections.keySet().toArray(new String[] {}));
		for (Entry<String, Connection> entry : connections.entrySet()) {
			preferences.saveExtProperties(entry.getKey(), entry.getValue().getExtendedProperties());
		}
	}

	@Override
	protected void logError(String message, Exception e) {
		OpenShiftCoreActivator.pluginLog().logError(message, e);
	}

	@Override
	protected Connection createConnection(ConnectionURL connectionURL) {
		Connection connection = new ConnectionFactory().create(
				connectionURL.getHostWithScheme());
		if (connection == null) {
			return null;
		} else {
			connection.setUsername(connectionURL.getUsername());
			connection.setAuthScheme(preferences.loadScheme(connectionURL.toString()));
			connection.setExtendedProperties(preferences.loadExtProperties(connectionURL.toString()));
			return connection;
		}
	}
}
