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
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author Andre Dietisheim
 */
public class UrlUtils {

	private static final String HTTPS = "https";
	private static final String HTTP = "http";
	private static final String SCHEME_SEPARATOR = "://";
	public static final String SCHEME_HTTPS = HTTPS + SCHEME_SEPARATOR;
	public static final String SCHEME_HTTP = HTTP + SCHEME_SEPARATOR;

	private static final char CREDENTIALS_HOST_DELIMITER = '@';

	private UrlUtils() {
		// inhibit instantiation
	}

	public static UrlPortions toPortions(URL url) throws UnsupportedEncodingException {
		return new UrlPortions(url);
	}

	public static class UrlPortions {

		private String username;
		private String password;
		private String host;

		private UrlPortions(URL url) throws UnsupportedEncodingException {
			String[] userInfo = url.getUserInfo().split(":");
			if (userInfo.length >= 1) {
				this.username = URLDecoder.decode(userInfo[0], "UTF-8");
			}
			if (userInfo.length >= 2) {
				this.password = userInfo[1];
			}
			this.host = url.getHost();
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
	}

	public static String toUrlString(String username, String host) throws UnsupportedEncodingException {
		host = cutScheme(host);
		StringBuilder builder = new StringBuilder(URLEncoder.encode(username, "UTF-8"))
				.append(CREDENTIALS_HOST_DELIMITER);
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
		int schemeDelimiterIndex = host.indexOf(SCHEME_SEPARATOR);
		if (schemeDelimiterIndex > -1) {
			return host.substring(schemeDelimiterIndex + SCHEME_SEPARATOR.length());
		}
		return host;
	}
	
	private static boolean isEmpty(String string) {
		return string == null
				|| string.isEmpty();
	}
}
