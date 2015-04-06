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
import java.net.MalformedURLException;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.core.LazySSLCertificateCallback;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.authorization.AuthorizationClientFactory;


/**
 * @author Andre Dietisheim
 */
public class ConnectionFactory implements IConnectionFactory {

	public ConnectionFactory() {
	}

	@Override
	public String getName() {
		return "OpenShift 3";
	}

	@Override
	public String getId() {
		return "org.jboss.tools.openshift.core.ConnectionFactory";
	}
	
	@Override
	public Connection create(String url) {
		return create(url, null);
	}		
	
	public Connection create(String url, ICredentialsPrompter credentialsPrompter) {
		try {
			return new Connection(url, 
					new AuthorizationClientFactory().create(), 
					credentialsPrompter,
					new LazySSLCertificateCallback(
							OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback()));
		} catch (MalformedURLException e) {
			OpenShiftCoreActivator.pluginLog().logInfo(NLS.bind("Could not create OpenShift connection: Malformed url {0}", url), e);
			return null;
		}
	}

	@Override
	public String getDefaultHost() {
		return null;
	}

	@Override
	public boolean hasDefaultHost() {
		return false;
	}

	@Override
	public <T extends IConnection> boolean canCreate(Class<T> clazz) {
		return Connection.class.isAssignableFrom(clazz);
	}
}
