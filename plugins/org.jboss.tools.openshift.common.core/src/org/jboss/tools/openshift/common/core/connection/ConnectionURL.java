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
package org.jboss.tools.openshift.common.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils.UrlPortions;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;

/**
 * An url-alike connection identifier that holds all relevant part for an
 * OpenShift Connection. ConnectionURL is used when storing a connection to the
 * ConnectionUtils and in the preferences.
 * 
 * @author Andre Dietisheim
 * 
 * @see IConnection
 * @see ConnectionUtils
 */
public class ConnectionURL {

	private static final Pattern MALFORMED_URL_PATTERN = Pattern.compile("(https?://)?([^@]+)?@(https?://)?(.*)");
	
	private String username;
	private String host;
	private String scheme;
	private String url;

	private ConnectionURL(String username, String host, String scheme) throws UnsupportedEncodingException {
		this.username = username;
		this.host = host;
		this.scheme = scheme;
		this.url = UrlUtils.getUrlFor(username, host, scheme);
	}

	public String getUsername() {
		return username;
	}

	public String getHost() {
		return host;
	}

	public String getHostWithScheme() {
		if (isDefaultHost()) {
			return null;
		}
		return scheme + host;
	}
	
	public boolean isDefaultHost() {
		return StringUtils.isEmpty(host);
	}

	public String getScheme() {
		return scheme;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConnectionURL other = (ConnectionURL) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return url;
	}
	
	public String getUrl() {
		return url;
	}

	/**
	 * Returns an url for the given username. The host used to build this url is
	 * the default host. Returns <code>null</code> if the given username is
	 * empty or <code>null</code>.
	 * 
	 * @see #getDefaultHostUrl()
	 * 
	 */
	public static ConnectionURL forUsername(String username)
			throws UnsupportedEncodingException, MalformedURLException {
		if (StringUtils.isEmpty(username)) {
			throw new IllegalArgumentException("Username is empty");
		}
		return new ConnectionURL(username, null, UrlUtils.SCHEME_HTTPS);
	}

	public static ConnectionURL forHost(String host) {
		return forHost(host);
	}
	
	public static ConnectionURL forUsernameAndHost(String username, String host)
			throws UnsupportedEncodingException, MalformedURLException {
		if (StringUtils.isEmpty(username)) {
			throw new IllegalArgumentException("Username is empty");
		}
		if (StringUtils.isEmpty(host)) {
			throw new IllegalArgumentException("Host is empty");
		}
		UrlPortions portions = UrlUtils.toPortions(host);
		return new ConnectionURL(username, portions.getHost(), portions.getScheme());
	}
	
	public static ConnectionURL forConnection(IConnection connection) 
			throws UnsupportedEncodingException, MalformedURLException {
		if (connection.isDefaultHost()) {
			return forUsername(connection.getUsername());
		}
		String host = getHost(connection);
		String scheme = getScheme(connection);
		String username = connection.getUsername();
		return new ConnectionURL(username, host, scheme);
	}

	/**
	 * The ConnectionURL of the connection.
	 * For cases we want to ignore the exceptions;
	 * 
	 * @param connection
	 * @return the value or null if there is an exception
	 */
	public static ConnectionURL safeForConnection(IConnection connection) { 
		try {
			return forConnection(connection);
		} catch (MalformedURLException e) {
			OpenShiftCommonCoreActivator.pluginLog().logError(NLS.bind("Unable to getConnectionURL for connection {0}@{1}", connection.getUsername(), connection.getHost()), e);
		} catch (UnsupportedEncodingException e) {
			OpenShiftCommonCoreActivator.pluginLog().logError(NLS.bind("Unable to getConnectionURL for connection {0}@{1}", connection.getUsername(), connection.getHost()), e);
		}
		return null;
	}

	private static String getHost(IConnection connection) {
		if (connection.isDefaultHost()) {
			return null;
		} else {
			return UrlUtils.cutScheme(connection.getHost());
		}
	}

	private static String getScheme(IConnection connection) {
		String scheme = connection.getScheme();
		if (scheme == null) {
			scheme = UrlUtils.SCHEME_HTTPS;
		}
		return scheme;
	}

	public static ConnectionURL forURL(String url) throws UnsupportedEncodingException, MalformedURLException {
		return forURL(new URL(correctMalformedUrl(url)));
	}

	public static ConnectionURL safeForURL(String url) {
		try {
			return forURL(url);
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			OpenShiftCommonCoreActivator.pluginLog().logError(NLS.bind("Unable to getConnectionURL for connection url {0}", url), e);
			return null;
		}
	}

	public static ConnectionURL forURL(URL url) throws UnsupportedEncodingException {
		Assert.isLegal(url != null, "url is null");
		UrlPortions portions = UrlUtils.toPortions(url);
		String host = getHost(portions);
		return new ConnectionURL(portions.getUsername(), host, portions.getScheme());
	}

	private static String getHost(UrlPortions portions) {
		String host = portions.getHost();
		if (StringUtils.isEmpty(host)) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(portions.getHost());
		if (portions.getPort() > -1) {
			builder.append(UrlUtils.PORT_DELIMITER).append(portions.getPort());
		}
		return builder.toString();
	}

	private static String correctMalformedUrl(String url) {
		Matcher matcher = MALFORMED_URL_PATTERN.matcher(url);
		if (!matcher.matches()
				|| matcher.groupCount() != 4) {
			return url;
		}

		if (!StringUtils.isEmpty(matcher.group(1))) {
			// https://adietish%40redhat.com@openshift.redhat.com
			return url;
		}

		if (StringUtils.isEmpty(matcher.group(4))
				&& !StringUtils.isEmpty(matcher.group(3))) {
			// adietish%40redhat.com@http://
			return new StringBuilder(matcher.group(3))
					.append(matcher.group(2))
					.append('@')
					.toString();
		} else if (!StringUtils.isEmpty(matcher.group(3))) {
			// adietish%40redhat.com@https://openshift.redhat.com
			return new StringBuilder(matcher.group(3))
					.append(matcher.group(2))
					.append('@')
					.append(matcher.group(4))
					.toString();
		} else {
			return url;
		}
	}
}
