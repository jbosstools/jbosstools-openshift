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

import org.jboss.tools.openshift.common.core.connection.AbstractConnectionPersistency;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public class ConnectionPersistency extends AbstractConnectionPersistency<Connection>{

	@Override
	protected String[] loadPersisted() {
		return OpenShiftCorePreferences.INSTANCE.loadConnections();
	}

	@Override
	protected void persist(String[] connections) {
		OpenShiftCorePreferences.INSTANCE.saveConnections(connections);
	}

	@Override
	protected void logError(String message, Exception e) {
		OpenShiftCoreActivator.pluginLog().logError(message, e);
	}

	@Override
	protected Connection createConnection(ConnectionURL connectionURL) {
		Connection connection = new ConnectionFactory().create(
				connectionURL.getHostWithScheme(),
				new LazyCredentialsPrompter(OpenShiftCoreUIIntegration.getInstance().getCredentialPrompter()));
		if (connection == null) {
			return null;
		} else {
			connection.setUsername(connectionURL.getUsername());
			connection.setAuthScheme(OpenShiftCorePreferences.INSTANCE.loadScheme(connectionURL.toString()));
			return connection;
		}
	}
}
