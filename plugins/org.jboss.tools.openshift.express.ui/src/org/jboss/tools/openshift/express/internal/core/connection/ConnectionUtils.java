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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

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
			scheme = UrlUtils.ensureStartsWithSchemeOrHttps(UrlUtils.getScheme(host));
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
	 */
	public static String getUrlForConnection(Connection connection) throws UnsupportedEncodingException {
		String username = connection.getUsername();
		String host = getHostReplaceDefault(connection);
		String[] schemeAndHost = splitSchemeAndHostname(host);
		return UrlUtils.toUrlString(username, schemeAndHost[1], schemeAndHost[0]);
	}

	private static String getHostReplaceDefault(Connection connection) {
 		String host = connection.getHost();
 		if (isDefaultHost(host)) {
 			host = null;
 		}
		return host;
	}

	private static String[] splitSchemeAndHostname(String host) {
		if (StringUtils.isEmpty(host)) {
			return new String[] { UrlUtils.SCHEME_HTTPS, null };
		}

		String scheme = UrlUtils.getScheme(host);
		if (StringUtils.isEmpty(scheme)) {
			scheme = UrlUtils.SCHEME_HTTPS;
		} else {
			host = UrlUtils.cutScheme(host);
		}
		return new String[] { scheme, host };
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
		} catch (Exception e) {
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
		return isEmptyHost(host)
				|| getDefaultHostUrl().equals(UrlUtils.ensureStartsWithSchemeOrHttps(host));
	}

	/**
	 * Returns <code>true</code> if the given host is an empty string or is an
	 * url with an empty host portion.
	 * 
	 * @param host
	 *            the host to check whether it is empty
	 * @return true if empty string or url without a host portion
	 */
	public static boolean isEmptyHost(String host) {
		try {
			return StringUtils.isEmpty(host)
					|| new URL(UrlUtils.ensureStartsWithSchemeOrHttps(host)).getHost().isEmpty();
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
