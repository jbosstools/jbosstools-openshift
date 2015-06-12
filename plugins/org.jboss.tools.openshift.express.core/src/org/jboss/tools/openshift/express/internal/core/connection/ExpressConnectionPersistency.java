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

import org.apache.commons.lang.ArrayUtils;
import org.jboss.tools.openshift.common.core.connection.AbstractConnectionPersistency;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.express.internal.core.LazySSLCertificateCallback;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionPersistency extends AbstractConnectionPersistency<ExpressConnection> {

	@Override
	protected String[] loadPersisted() {
		return (String[]) ArrayUtils.addAll(
				ExpressCorePreferences.INSTANCE.loadConnections(), 
				ExpressCorePreferences.INSTANCE.loadLegacyConnections());
	}

	@Override
	protected void persist(String[] connections) {
		ExpressCorePreferences.INSTANCE.saveConnections(connections);
	}

	@Override
	protected void logError(String message, Exception e) {
		ExpressCoreActivator.pluginLog().logError(message, e);
	}

	@Override
	protected ExpressConnection createConnection(ConnectionURL connectionURL) {
		return new ExpressConnection(
				connectionURL.getUsername(),
				connectionURL.getHostWithScheme(),
				new LazyCredentialsPrompter(
						ExpressCoreUIIntegration.getDefault().getCredentialPrompter()),
				new LazySSLCertificateCallback(
						ExpressCoreUIIntegration.getDefault().getSSLCertificateCallback()));
	}
}
