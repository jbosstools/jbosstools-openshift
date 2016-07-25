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

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.properties.IPropertiesProvider;
import org.jboss.tools.foundation.core.properties.PropertiesHelper;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.core.LazySSLCertificateCallback;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;


/**
 * @author Andre Dietisheim
 */
public class ConnectionFactory implements IConnectionFactory {

	public static final String OPENSHIFT_USERDOC = "openshift.userdoc.url"; //$NON-NLS-1$

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
		try {
			LazySSLCertificateCallback sslCertCallback = new LazySSLCertificateCallback();
			IClient client = new ClientBuilder(url)
					.sslCertificateCallback(sslCertCallback)
					.build();
			return new Connection(client, new LazyCredentialsPrompter());
		} catch (OpenShiftException e) {
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

	@Override
	public String getSignupUrl(String host) {
		return null;
	}

	@Override
	public String getUserDocUrl() {
		// use commandline override -Dopenshift.userdoc.url
		String userdoc = System.getProperty(OPENSHIFT_USERDOC, null);
		if (userdoc == null) {
			IPropertiesProvider pp = PropertiesHelper.getPropertiesProvider();
			// fall back to hard-coded default, if not found in ide-config.properties file
			userdoc = pp.getValue(OPENSHIFT_USERDOC, "http://tools.jboss.org/documentation/howto/openshift3_getting_started.html"); //$NON-NLS-1$
		}
		return userdoc;
	}
}
