/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils.UrlPortions;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.configuration.IOpenShiftConfiguration;
import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 */
public class ConnectionUtils {

	private ConnectionUtils() {
		// inhibit instantiation
	}

	/**
	 * Returns an url for the given username. The host used to build this url is
	 * the default host. Returns <code>null</code> if the given username is
	 * empty or <code>null</code>.
	 * 
	 * @see #getDefaultHostUrl()
	 * 
	 */
	public static String getUrlForUsername(String username) throws UnsupportedEncodingException, MalformedURLException {
		if (StringUtils.isEmpty(username)) {
			return null;
		}
		UrlPortions portions = UrlUtils.toPortions(getDefaultHostUrl());
		return UrlUtils.getUrlFor(username, null, portions.getProtocol() + UrlUtils.SCHEME_SEPARATOR);
	}

	public static String getUrlForUsernameAndHost(String username, String host) throws UnsupportedEncodingException {
		String scheme = UrlUtils.SCHEME_HTTPS;
		if (isDefaultHost(host)) {
			scheme = UrlUtils.ensureStartsWithScheme(UrlUtils.getScheme(host), UrlUtils.SCHEME_HTTPS);
			host = null;
		} else if (UrlUtils.hasScheme(host)) {
			scheme = UrlUtils.getScheme(host);
			host = UrlUtils.cutScheme(host);
		}
		return UrlUtils.getUrlFor(username, host, scheme);
	}

	/**
	 * @return an url-alike string that always starts with a scheme but
	 *         eventually has no host where the default host shall be used.
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 */
	public static String getUrlFor(Connection connection) throws UnsupportedEncodingException, MalformedURLException {
		String username = connection.getUsername();
		if (connection.isDefaultHost()) {
			return getUrlForUsername(username);
		}
		String host = UrlUtils.cutScheme(getHostOrDefault(connection));
		String scheme = connection.getScheme();
		if (scheme == null) {
			scheme = UrlUtils.SCHEME_HTTPS;
		}
		return UrlUtils.getUrlFor(username, host, scheme);
	}

	private static String getHostOrDefault(Connection connection) {
		if (connection.isDefaultHost()) {
			return null;
		} else {
			return connection.getHost();
		}
	}

	/**
	 * Returns the default host from the preferences if present. If it's not it
	 * will return the host defined in the OpenShift configuration. The host
	 * that is returned will always have the scheme prefix.
	 * 
	 * @return the default host
	 * 
	 * @see OpenShiftPreferences#getDefaultHost()
	 * @see IOpenShiftConfiguration#getLibraServer()
	 */
	public static String getDefaultHostUrl() {
		try {
			String defaultHost = OpenShiftPreferences.INSTANCE.getDefaultHost();
			if (!StringUtils.isEmpty(defaultHost)) {
				return defaultHost;
			}
			return new OpenShiftConfiguration().getLibraServer();
		} catch (IOException e) {
			Logger.error("Could not load default server from OpenShift configuration.", e);
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
