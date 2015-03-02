/*******************************************************************************
 * Copyright (c) 2012-2015 Red Hat, Inc.
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
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.test.core.NoopUserFake;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.IUser;


/**
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ExpressConnectionsRegistryTest {

	private ConnectionsRegistry registry;
	private IConnection connection;
	
	@Before
	public void setUp() {
		this.registry = new ConnectionsRegistry();
	}

	@Test
	public void shouldAddDefaultAndNonDefaultConnection() {
		// pre-conditions
		/* connection explicitly using default host */
		IConnection defaultHostConnection = new ExpressConnectionFake("fakeUser", null);
		/* connection pointing to default host */
		IConnection connectionToDefaultHost = 
				new ExpressConnectionFake("fakeUser", ExpressConnectionUtils.getDefaultHostUrl());
		int numberOfConnections = registry.size();

		// operations
		boolean added1 = registry.add(defaultHostConnection);
		boolean added2 = registry.add(connectionToDefaultHost);

		// verifications
		assertTrue(added1);
		assertTrue(added2);
		assertEquals(numberOfConnections + 2, registry.size());
	}

	@Test
	public void shouldGetConnectionByUsername() throws UnsupportedEncodingException {
		// pre-conditions
		String username = "adietisheim";
		ExpressConnection connection = new ExpressConnectionFake(username);
		registry.add(connection);

		// operations
		ExpressConnection queriedConnection = ExpressConnectionUtils.getByUsername(username, registry);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldNotGetConnectionByUsername() {
		// pre-conditions
		String username = "adietisheim";
		String host = "fakeHost";
		IConnection connection = new ExpressConnectionFake(username, host);
		registry.add(connection);

		// operations
		IConnection queriedConnection = ExpressConnectionUtils.getByUsername(username, registry);

		// verifications
		assertEquals(null, queriedConnection);
	}

	@Test
	public void shouldGetConnectionByUserResource() {
		// pre-conditions
		final String server = "http://localhost";
		final String username = "adietish@redhat.com";
		ExpressConnection connection = new ExpressConnectionFake(username, server);
		registry.add(connection);
		IUser user = new NoopUserFake() {

			@Override
			public String getServer() {
				return server;
			}

			@Override
			public String getRhlogin() {
				return username;
			}
		};

		// operations
		ExpressConnection queriedConnection = ExpressConnectionUtils.getByResource(user, registry);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldGetConnectionByUserResourceWithDefaultHost()
			throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		final String server = ExpressConnectionUtils.getDefaultHostUrl();
		final String username = "adietish@redhat.com";
		ExpressConnection connection = new ExpressConnectionFake(username, null);
		registry.add(connection);
		IUser user = new NoopUserFake() {

			@Override
			public String getServer() {
				return server;
			}

			@Override
			public String getRhlogin() {
				return username;
			}
		};

		// operations
		ExpressConnection queriedConnection = ExpressConnectionUtils.getByResource(user, registry);

		// verifications
		assertEquals(connection, queriedConnection);
	}
	
//	@Test
//	public void shouldLoadDefaultHostConnectionsByUsername()
//			throws UnsupportedEncodingException {
//		// pre-conditions
//		ConnectionsRegistry registry = new ConnectionsRegistry() {
//
//			@Override
//			protected String[] loadPersistedDefaultHosts() {
//				return new String[] { "adietish@redhat.com" };
//			}
//
//			@Override
//			protected String[] loadPersistedCustomHosts() {
//				return new String[] {};
//			}
//		};
//
//		// operations
//
//		// verifications
//		assertEquals(1, registry.getAll(ExpressConnection.class).size());
//		ExpressConnection connection = registry.getAll(ExpressConnection.class).iterator().next();
//		assertTrue(connection.isDefaultHost());
//		assertEquals(ExpressConnectionUtils.getDefaultHostUrl(), connection.getHost());
//		assertEquals("adietish@redhat.com", connection.getUsername());
//	}
//
//	@Test
//	public void shouldSaveDefaultHostConnectionsByUsername()
//			throws UnsupportedEncodingException {
//		// pre-conditions
//		ConnectionsRegistry registry = new ConnectionsRegistry();
//
//		// operations
//		/* custom host */
//		registry.add(new ExpressConnectionFake("toolsjboss@gmail.com", "http://openshift.local"));
//		/* default host */
//		registry.add(new ExpressConnectionFake("adietish@redhat.com"));
//
//		// verifications
//		registry.save();
//		List<String> defaultHosts = registry.getSavedDefaultHosts();
//		assertEquals(1, defaultHosts.size());
//		assertEquals("adietish@redhat.com", defaultHosts.get(0));
//	}
//
//	@Test
//	public void shouldLoadCustomHostConnectionsByUrl()
//			throws UnsupportedEncodingException {
//		// pre-conditions
//		ConnectionsRegistry registry = new ConnectionsRegistry() {
//
//			@Override
//			protected String[] loadPersistedDefaultHosts() {
//				return new String[] {};
//			}
//
//			@Override
//			protected String[] loadPersistedCustomHosts() {
//				return new String[] { "http://adietish%40redhat.com@openshift.local" };
//			}
//		};
//
//		// operations
//
//		// verifications
//		assertEquals(1, registry.getAll(ExpressConnection.class).size());
//		ExpressConnection connection = registry.getAll(ExpressConnection.class).iterator().next();
//		assertFalse(connection.isDefaultHost());
//		assertEquals("http://openshift.local", connection.getHost());
//		assertEquals("adietish@redhat.com", connection.getUsername());
//	}
//
//	@Test
//	public void shouldSaveCustomHostConnectionsByUrl()
//			throws UnsupportedEncodingException {
//		// pre-conditions
//		ConnectionsRegistry registry = new ConnectionsRegistry();
//
//		// operations
//		/* custom host */
//		registry.add(new ExpressConnectionFake("toolsjboss@gmail.com",
//				"http://openshift.local"));
//		/* default host */
//		registry.add(new ExpressConnectionFake("adietish@redhat.com"));
//
//		// verifications
//		registry.save();
//		List<String> customHosts = registry.getSavedCustomHosts();
//		assertEquals(1, customHosts.size());
//		assertEquals("http://toolsjboss%40gmail.com@openshift.local", customHosts.get(0));
//	}
//
//	@Test
//	public void shouldGetConnectionByResource()	throws UnsupportedEncodingException {
//		// pre-conditions
//		final String username = "adietisheim";
//		final String hostUrl = "http://fakeHost";
//		IUser user = new NoopUserFake() {
//
//			@Override
//			public String getServer() {
//				return hostUrl;
//			}
//
//			@Override
//			public String getRhlogin() {
//				return username;
//			}
//
//		};
//		ExpressConnection connection = new ExpressConnectionFake(username, hostUrl);
//		registry.add(connection);
//
//		// operations
//		ExpressConnection queriedConnection = ExpressConnectionUtils.getByResource(user, registry);
//
//		// verifications
//		assertEquals(connection, queriedConnection);
//	}
}
