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
package org.jboss.tools.openshift.express.test.core.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ConnectionURLTest {

	@Test
	public void shouldCorrectMisplacedSchemeWithDefaultHost() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		String username = "adietish@redhat.com";
		String schemeOnlyUrl = UrlUtils.SCHEME_HTTP;

		// operation
		ConnectionURL connectionURL = ConnectionURL.forURL(URLEncoder.encode(username, "UTF-8") + '@' + schemeOnlyUrl);

		// verifications
		assertEquals(username, connectionURL.getUsername());
		assertEquals(null, connectionURL.getHost());
		assertTrue(connectionURL.isDefaultHost());
		assertEquals(schemeOnlyUrl, connectionURL.getScheme());
		assertEquals(schemeOnlyUrl + URLEncoder.encode(username, "UTF-8") + '@', connectionURL.getUrl());
	}
	
	@Test
	public void shouldCorrectMisplacedScheme() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		String username = "adietish@redhat.com";
		String scheme = UrlUtils.SCHEME_HTTP;
		String host = "openshift.local";
		
		// operation
		ConnectionURL connectionUrl = ConnectionURL.forURL(URLEncoder.encode(username, "UTF-8") + '@' + scheme + host);

		// verifications
		assertEquals(host, connectionUrl.getHost());
		assertFalse(connectionUrl.isDefaultHost());
		assertEquals(username, connectionUrl.getUsername());
		assertEquals(scheme, connectionUrl.getScheme());
	}
	
	@Test
	public void shouldCorrectMisplacedSchemeWithDefaultServer() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		String username = "adietish@redhat.com";
		String scheme = UrlUtils.SCHEME_HTTP;
		
		// operation
		ConnectionURL connectionUrl = ConnectionURL.forURL(URLEncoder.encode(username, "UTF-8") + '@' + scheme);

		// verifications
		assertEquals(null, connectionUrl.getHost());
		assertTrue(connectionUrl.isDefaultHost());
		assertEquals(username, connectionUrl.getUsername());
		assertEquals(scheme, connectionUrl.getScheme());
	}

	@Test
	public void shouldGetForUsernameAndServer() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		String username = "adietish@redhat.com";
		String server = "https://openshift.redhat.com";
		
		// operation
		ConnectionURL connectionUrl = ConnectionURL.forUsernameAndServer(username, server);
		
		// verifications
		assertEquals("openshift.redhat.com", connectionUrl.getHost());
		assertFalse(connectionUrl.isDefaultHost());
		assertEquals(username, connectionUrl.getUsername());
		assertEquals("https://", connectionUrl.getScheme());
	}

}
