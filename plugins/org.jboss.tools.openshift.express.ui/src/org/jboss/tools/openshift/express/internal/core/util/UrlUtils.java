/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class UrlUtils {

	public static final String HTTP = "http";
	public static final String HTTPS = "https";
	public static final String SCHEME_SEPARATOR = "://";
	public static final String SCHEME_HTTPS = HTTPS + SCHEME_SEPARATOR;
	public static final String SCHEME_HTTP = HTTP + SCHEME_SEPARATOR;
	public static final char CREDENTIALS_HOST_SEPARATOR = '@';
	public static final char PORT_DELIMITER = ':';

	private UrlUtils() {
		// inhibit instantiation
	}

	public static UrlPortions toPortions(String url) throws UnsupportedEncodingException, MalformedURLException {
		return new UrlPortions(new URL(url));
	}

	public static UrlPortions toPortions(URL url) throws UnsupportedEncodingException {
		return new UrlPortions(url);
	}

	public static class UrlPortions {

		private String protocol;
		private String username;
		private String password;
		private String host;
		private int port;

		private UrlPortions(URL url) throws UnsupportedEncodingException {
			String userInfo = url.getUserInfo();
			if (userInfo != null) {
				String[] userInfos = url.getUserInfo().split(":");
				if (userInfos.length >= 1) {
					this.username = URLDecoder.decode(userInfos[0], "UTF-8");
				}
				if (userInfos.length >= 2) {
					this.password = userInfos[1];
				}
			}
			this.host = url.getHost();
			this.protocol = url.getProtocol();
			this.port = url.getPort();
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getHost() {
			return host;
		}

		public String getScheme() {
			return protocol + SCHEME_SEPARATOR;
		}
		
		public int getPort() {
			return port;
		}
	}

	/**
	 * Ensures the given host url starts with a scheme. The given default-scheme
	 * is appended otherwise.
	 * 
	 * @param hostUrl
	 *            the host url that shall start with a scheme
	 * @param defaultScheme
	 *            the default scheme that shall get appended if the host url has
	 *            no scheme
	 * @return the host url with a scheme
	 * @throws IllegalArgumentException
	 */
	public static String ensureStartsWithScheme(String hostUrl, String defaultScheme) throws IllegalArgumentException {
		Assert.isLegal(!isEmpty(defaultScheme), "Default scheme is empty");
		if (isEmpty(hostUrl)) {
			return hostUrl;
		}
		if (hostUrl.indexOf(SCHEME_SEPARATOR) == -1) {
			return defaultScheme + hostUrl;
		}
		return hostUrl;
	}

	public static String cutScheme(String host) {
		if (isEmpty(host)) {
			return host;
		}
		int hostIndex = getHostIndex(host);
		if (hostIndex > -1) {
			return host.substring(hostIndex);
		}
		return host;
	}

	public static String getScheme(String url) {
		if (isEmpty(url)) {
			return null;
		}

		int hostIndex = getHostIndex(url);
		if (hostIndex == -1) {
			return null;
		}

		return url.substring(0, hostIndex);
	}

	public static boolean hasScheme(String host) {
		if (isEmpty(host)) {
			return false;
		}
		return host.indexOf(SCHEME_SEPARATOR) > -1;
	}

	private static int getHostIndex(String url) {
		int schemeSeparatorIndex = url.indexOf(SCHEME_SEPARATOR);
		if (schemeSeparatorIndex == -1) {
			return schemeSeparatorIndex;
		}
		return schemeSeparatorIndex + SCHEME_SEPARATOR.length();
	}

	private static boolean isEmpty(String string) {
		return string == null
				|| string.isEmpty();
	}

	/**
	 * Returns an url for the given username, host and scheme. If the given host
	 * already has a scheme, the scheme wont get prepended.
	 * 
	 * @param username
	 *            the username for the url
	 * @param host
	 *            the host for the url
	 * @param scheme
	 *            the scheme to prepend
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String getUrlFor(String username, String host, String scheme) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		if (!hasScheme(host)) {
			builder.append(scheme);
		}
		if (!isEmpty(username)) {
			builder.append(URLEncoder.encode(username, "UTF-8"))
					.append(UrlUtils.CREDENTIALS_HOST_SEPARATOR);
		}
		if (!isEmpty(host)) {
			builder.append(host);
		}
		return builder.toString();
	}

	/**
	 * Returns an url for the given username and host. The host is required to
	 * have a scheme. An illegalArgumentException is thrown otherwise.
	 * 
	 * @param username
	 *            the username to use in the url
	 * @param host
	 *            the host with a scheme to use in the url
	 * @return an url for the username and host
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 *             if the host has no scheme
	 */
	public static String getUrlFor(String username, String host) throws UnsupportedEncodingException,
			IllegalArgumentException {
		String scheme = getScheme(host);
		Assert.isLegal(!isEmpty(scheme),
				MessageFormat.format("Could not extract scheme. Host {0} has no scheme", host));
		return getUrlFor(username, host, scheme);
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
					|| new URL(UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS)).getHost().isEmpty();
		} catch (MalformedURLException e) {
			return false;
		}
	}

}
