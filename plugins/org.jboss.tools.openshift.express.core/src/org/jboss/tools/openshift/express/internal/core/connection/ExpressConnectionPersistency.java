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
package org.jboss.tools.openshift.express.internal.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.express.internal.core.LazySSLCertificateCallback;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionPersistency {

	private ExpressCorePreferences preferences;

	public ExpressConnectionPersistency(ExpressCorePreferences preferences) {
		this.preferences = preferences;
	}

	public Collection<ExpressConnection> load() {
		List<ExpressConnection> connections = new ArrayList<ExpressConnection>();
		String[] persistedConnections = preferences.loadConnections();
        for (String connectionUrl : persistedConnections) {
        	addConnection(connectionUrl, connections);
		}
        return connections;
	}

	private void addConnection(String connectionUrl, List<ExpressConnection> connections) {
		try {
			ConnectionURL connectionURL = ConnectionURL.forURL(connectionUrl);
			ExpressConnection connection =
					new ExpressConnection(
							connectionURL.getUsername(),
							connectionURL.getScheme(),
							connectionURL.getHost(),
							new LazyCredentialsPrompter(ExpressCoreUIIntegration.getDefault().getCredentialPrompter()),
							new LazySSLCertificateCallback(
									ExpressCoreUIIntegration.getDefault()
									.getSSLCertificateCallback()));
			connections.add(connection);
		} catch (MalformedURLException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		} catch (UnsupportedEncodingException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		} catch (IllegalArgumentException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", connectionUrl), e);
		}
	}

	public void save(Collection<ExpressConnection> connections) {
		if (connections == null
				|| connections.size() == 0) {
			return;
		}

		List<String> serializedConnections = new ArrayList<String>(connections.size());
		for (ExpressConnection connection : connections) {
			addConnection(connection, serializedConnections);
		}
		preferences.saveConnections(serializedConnections.toArray(new String[serializedConnections.size()]));
	}

	private void addConnection(ExpressConnection connection, List<String> serializedConnections) {
		try {
			ConnectionURL connectionURL = ConnectionURL.forConnection(connection);
			serializedConnections.add(connectionURL.toString());
		} catch (MalformedURLException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		} catch (UnsupportedEncodingException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}@{1}.", connection.getUsername(), connection.getHost()), e);
		}
	}
}
