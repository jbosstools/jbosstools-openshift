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

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
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

	private static final String SIGNUP_URL = "https://manage.openshift.com/openshiftio";
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
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		try {
			LazySSLCertificateCallback sslCertCallback = new LazySSLCertificateCallback();
			IClient client = new ClientBuilder(url)
					.sslCertificateCallback(sslCertCallback)
					.withMaxRequests(ConnectionProperties.MAX_REQUESTS)
					.withMaxRequestsPerHost(ConnectionProperties.MAX_REQUESTS)
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
	public String getUserDocUrl() {
		return getSignupUrl();
	}

	private String getSignupUrl() {
		return SIGNUP_URL;
	}	

	@Override
	public String getUserDocText() {
		return "Want to try OpenShift 3 online? You can sign up for an account <a>here</a>";
	}
}
