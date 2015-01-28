/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressPreferences;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;
import com.openshift.client.configuration.IOpenShiftConfiguration;
import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ExpressConnectionUtils {

	private ExpressConnectionUtils() {
		// inhibit instantiation
	}
	
	/**
	 * Returns the connection for a given application (OpenShift REST resource).
	 * 
	 * @param application the openshift application that we want the connection for
	 * @return the connection that this applicaiton belongs to.
	 * 
	 * @throws OpenShiftCoreException
	 */
	public static ExpressConnection getByResource(IApplication application, ConnectionsRegistry connectionsRegistry) {
		if (application == null) {
			return null;
		}
		
		return getByResource(application.getDomain().getUser(), connectionsRegistry);
	}

	/**
	 * Returns the connection for a given user (OpenShift REST resource).
	 * 
	 * @param user the openshift user
	 * @return the connection that this user belongs to.
	 * 
	 * @throws OpenShiftCoreException
	 */
	public static ExpressConnection getByResource(IUser user, ConnectionsRegistry connectionsRegistry)
			throws OpenShiftCoreException {
		if (user == null) {
			return null;
		}
		try {
			ConnectionURL connectionUrl = ConnectionURL.forUsernameAndHost(user.getRhlogin(), user.getServer());
			ExpressConnection connection = connectionsRegistry.getByUrl(connectionUrl, ExpressConnection.class);
			String defaultHost = ExpressConnectionUtils.getDefaultHostUrl();
			if (connection == null 
					&& defaultHost.equals(user.getServer())) {
				connectionUrl = ConnectionURL.forUsername(user.getRhlogin());
				connection = connectionsRegistry.getByUrl(connectionUrl, ExpressConnection.class);
			}
			return connection;
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e, 
					NLS.bind(
					"Could not get connection for user resource {0} - {1}",
					user.getRhlogin(), user.getServer()));
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(e, 
					NLS.bind(
					"Could not get connection for user resource {0} - {1}",
					user.getRhlogin(), user.getServer()));
		}
	}

	public static ExpressConnection getByUrl(ConnectionURL connectionUrl, ConnectionsRegistry connectionsRegistry) {
		if (connectionUrl == null) {
			return null;
		}
		return connectionsRegistry.getByUrl(connectionUrl, ExpressConnection.class);
	}
	
	/**
	 * Returns the connection for the given username if it exists. The
	 * connection must use the default host to match the query by username.
	 * 
	 * @param username
	 *            the username that the connection must use
	 * @return the connection with the given username that uses the default host
	 * 
	 * @see ExpressConnectionUtils#getDefaultHostUrl()
	 */
	public static ExpressConnection getByUsername(String username, ConnectionsRegistry connectionsRegistry) {
		try {
			return getByUrl(ConnectionURL.forUsername(username), connectionsRegistry);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		}
	}
	
	/**
	 * Returns the default host from the preferences if present. If it's not it
	 * will return the host defined in the OpenShift configuration. The host
	 * that is returned will always have the scheme prefix.
	 * 
	 * @return the default host
	 * 
	 * @see ExpressPreferences#getDefaultHost()
	 * @see IOpenShiftConfiguration#getLibraServer()
	 */
	public static String getDefaultHostUrl() {
		try {
			String defaultHost = ExpressPreferences.INSTANCE.getDefaultHost();
			if (!StringUtils.isEmpty(defaultHost)) {
				return defaultHost;
			}
			return new OpenShiftConfiguration().getLibraServer();
		} catch (IOException e) {
			ExpressCoreActivator.pluginLog().logError("Could not load default server from OpenShift configuration.", e);
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if the given host is the default host. This
	 * method reports a given String is the default host if it is empty or if
	 * it's equal to the default host defined for this plugin. This plugin takes
	 * the default host from the preferences or the openshift configuration. If
	 * the given host has no scheme this method will assume it's https.
	 * 
	 * @param host
	 *            the host to check whether it is the default host
	 * @return true if it is equal to the default host
	 * 
	 * @see getDefaultHost()
	 */
	public static boolean isDefaultHost(String host) {
		return UrlUtils.isEmptyHost(host)
				|| getDefaultHostUrl().equals(
						UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS));
	}


}
