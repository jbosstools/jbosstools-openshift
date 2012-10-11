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

		public String getProtocol() {
			return protocol;
		}

	}

	public static String toUrlString(String username, String host) throws UnsupportedEncodingException {
		host = cutScheme(host);
		StringBuilder builder = new StringBuilder();
		if (!isEmpty(username)) {
			builder.append(URLEncoder.encode(username, "UTF-8"))
					.append(CREDENTIALS_HOST_SEPARATOR);
		}
		if (!isEmpty(host)) {
			builder.append(host);
		}
		return ensureStartsWithSchemeOrHttps(builder.toString());
	}

	public static String ensureStartsWithSchemeOrHttps(String host) {
		if (isEmpty(host)) {
			return SCHEME_HTTPS;
		}
		if (host.indexOf(SCHEME_SEPARATOR) > -1) {
			return host;
		}
		return SCHEME_HTTPS + host;
	}

	public static String cutScheme(String host) {
		if (isEmpty(host)) {
			return host;
		}
		int schemeDelimiterIndex = getSchemeIndex(host);
		if (schemeDelimiterIndex > -1) {
			return host.substring(schemeDelimiterIndex + SCHEME_SEPARATOR.length());
		}
		return host;
	}

	private static boolean hasScheme(String host) {
		return host.indexOf(SCHEME_SEPARATOR) > -1;
	}

	private static int getSchemeIndex(String host) {
		int schemeDelimiterIndex = host.indexOf(SCHEME_SEPARATOR);
		return schemeDelimiterIndex;
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
	 */
	public static String getUrlFor(String username, String host, String scheme) {
		StringBuilder builder = new StringBuilder();
		if (StringUtils.isEmpty(host)
				|| !hasScheme(host)) {
			builder.append(scheme).append(SCHEME_SEPARATOR);
		}
		if (!StringUtils.isEmpty(username)) {
			builder.append(username)
					.append(UrlUtils.CREDENTIALS_HOST_SEPARATOR);
		}
		return builder.append(host)
				.toString();

	}
}
