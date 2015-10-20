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
package org.jboss.tools.openshift.common.core.utils;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.core.runtime.Assert;

/**
 * @author Andre Dietisheim
 */
public class UrlUtils {

	public static final String HTTP = "http";
	public static final String HTTPS = "https";
	public static final String SCHEME_TERMINATOR = ":";
	public static final String SCHEME_SEPARATOR = "://";
	public static final String SCHEME_HTTPS = HTTPS + SCHEME_SEPARATOR;
	public static final String SCHEME_HTTP = HTTP + SCHEME_SEPARATOR;
	public static final char CREDENTIALS_HOST_SEPARATOR = '@';
	public static final char PORT_DELIMITER = ':';
	
	private static final Pattern SIMPLE_URL_PATTERN =
			Pattern.compile("(\\w+://)(.+@)*([\\w\\d\\.]+)(:[\\d]+){0,1}/*(.*)");
	private static final String PROPERTY_BASIC = "Basic";
	private static final String PROPERTY_AUTHORIZATION = "Authorization";
	
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
		if (!isEmpty(scheme)) {
			builder.append(scheme);
		}
		if (!isEmpty(username)) {
			builder.append(URLEncoder.encode(username, "UTF-8"))
					.append(UrlUtils.CREDENTIALS_HOST_SEPARATOR);
		}
		host = cutScheme(host);
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
					|| new URL(ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS)).getHost().isEmpty();
		} catch (MalformedURLException e) {
			return false;
		}
	}

	public static boolean isValid(String url) {
		// Test via regex first. If passes then check via new URL(url) and URI(url) which are slower
		if(SIMPLE_URL_PATTERN.matcher(url).matches()) {
			try {
				new URI(url);
				new URL(url);
			} catch (MalformedURLException | URISyntaxException e) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Sets blindly accepting trustmanager and hostname verifiers to the given
	 * connection.
	 * 
	 * @param connection
	 *            the connection that the permissive trustmanager and hostname
	 *            verifiers are set to
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static void setupPermissiveSSLHandlers(HttpsURLConnection connection) throws KeyManagementException, NoSuchAlgorithmException {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[] { new PermissiveTrustManager() }, null);
		SSLSocketFactory socketFactory = sslContext.getSocketFactory();
		connection.setSSLSocketFactory(socketFactory);

		connection.setHostnameVerifier(new PermissiveHostnameVerifier());
	}

	private static class PermissiveTrustManager implements X509TrustManager {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}		
	}
	
	private static class PermissiveHostnameVerifier implements HostnameVerifier {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}		
	}
	
	public static void addBasicAuthorization(String username, String password, HttpURLConnection connection) {
		String credentials = toBase64Encoded(
				new StringBuilder().append(username).append(':').append(password).toString());
		connection.setRequestProperty(PROPERTY_AUTHORIZATION,
				new StringBuilder().append(PROPERTY_BASIC).append(' ').append(credentials).toString());
	}
	
	public static String toBase64Encoded(String unencoded) {
			if (unencoded == null) {
				return null;
			} else if (unencoded.getBytes().length == 0) {
				return new String();
			}
			return DatatypeConverter.printBase64Binary(unencoded.getBytes());
	}
}
