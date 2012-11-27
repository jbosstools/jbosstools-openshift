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
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.test.core.connection.ConnectionsModelFake.ConnectionsChange;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ConnectionsModelTest {

	private ConnectionsModelFake connectionsModel;
	private Connection connection;

	@Before
	public void setUp() {
		this.connectionsModel = new ConnectionsModelFake();
		this.connection = new ConnectionFake();
	}

	@Test
	public void shouldAddConnection() {
		// pre-conditions
		int numberOfConnections = connectionsModel.size();

		// operations
		boolean added = connectionsModel.addConnection(connection);

		// verifications
		assertTrue(added);
		assertEquals(numberOfConnections + 1, connectionsModel.size());
	}

	@Test
	public void shouldNotAddIdenticalConnection() {
		// pre-conditions
		connectionsModel.addConnection(connection);
		int numberOfConnections = connectionsModel.size();

		// operations
		boolean added = connectionsModel.addConnection(connection);

		// verifications
		assertFalse(added);
		assertEquals(numberOfConnections, connectionsModel.size());
	}

	@Test
	public void shouldAddDefaultAndNonDefaultConnection() {
		// pre-conditions
		/* connection explicitly using default host */
		Connection defaultHostConnection = new ConnectionFake("fakeUser", null);
		/* connection pointing to default host */
		Connection connectionToDefaultHost = new ConnectionFake("fakeUser", ConnectionUtils.getDefaultHostUrl());
		int numberOfConnections = connectionsModel.size();

		// operations
		boolean added1 = connectionsModel.addConnection(defaultHostConnection);
		boolean added2 = connectionsModel.addConnection(connectionToDefaultHost);

		// verifications
		assertTrue(added1);
		assertTrue(added2);
		assertEquals(numberOfConnections + 2, connectionsModel.size());
	}

	@Test
	public void shouldNotifyAddition() {
		// pre-conditions

		// operations
		connectionsModel.addConnection(connection);

		// verifications
		ConnectionsChange change = connectionsModel.getChange();
		assertTrue(change.isAdditionNotified());
		assertEquals(connection, change.getConnection());
	}

	@Test
	public void shouldSetRecentConnectionToLatestAddition() {
		// pre-conditions
		Connection recentConnection = connectionsModel.getRecentConnection();

		// operations
		connectionsModel.addConnection(connection);

		// verifications
		assertTrue(recentConnection == null || !recentConnection.equals(connection));
		assertEquals(connection, connectionsModel.getRecentConnection());
	}

	@Test
	public void shouldRemoveConnection() {
		// pre-conditions
		connectionsModel.addConnection(connection);
		int numberOfConnections = connectionsModel.size();

		// operations
		boolean removed = connectionsModel.removeConnection(connection);

		// verifications
		assertTrue(removed);
		assertEquals(numberOfConnections - 1, connectionsModel.size());
	}

	@Test
	public void shouldNotRemoveConnectionThatWasNotAdded() {
		// pre-conditions
		int numberOfConnections = connectionsModel.size();

		// operations
		boolean removed = connectionsModel.removeConnection(connection);

		// verifications
		assertFalse(removed);
		assertEquals(numberOfConnections, connectionsModel.size());
	}

	@Test
	public void shouldResetRecentConnectionOnRemoval() {
		// pre-conditions
		connectionsModel.addConnection(connection);

		// operations
		connectionsModel.removeConnection(connection);

		// verifications
		Connection recentConnection = connectionsModel.getRecentConnection();
		assertEquals(null, recentConnection);
	}

	@Test
	public void shouldNotifyRemoval() {
		// pre-conditions
		connectionsModel.addConnection(connection);

		// operations
		connectionsModel.removeConnection(connection);

		// verifications
		ConnectionsChange change = connectionsModel.getChange();
		assertTrue(change.isRemovalNotified());
		assertEquals(connection, change.getConnection());
	}

	@Test
	public void shouldGetConnectionByUrl() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		connectionsModel.addConnection(connection);

		// operations
		ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
		Connection queriedConnection = connectionsModel.getConnectionByUrl(connectionUrl);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldGetConnectionByUsername() throws UnsupportedEncodingException {
		// pre-conditions
		String username = "adietisheim";
		Connection connection = new ConnectionFake(username);
		connectionsModel.addConnection(connection);

		// operations
		Connection queriedConnection = connectionsModel.getConnectionByUsername(username);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldNotGetConnectionByUsername() throws UnsupportedEncodingException {
		// pre-conditions
		String username = "adietisheim";
		String host = "fakeHost";
		Connection connection = new ConnectionFake(username, host);
		connectionsModel.addConnection(connection);

		// operations
		Connection queriedConnection = connectionsModel.getConnectionByUsername(username);

		// verifications
		assertEquals(null, queriedConnection);
	}

	@Test
	public void shouldHaveConnection() throws UnsupportedEncodingException {
		// pre-conditions
		connectionsModel.addConnection(connection);

		// operations
		boolean found = connectionsModel.hasConnection(connection);

		// verifications
		assertTrue(found);
	}

	@Test
	public void shouldReturnAllConnections() throws UnsupportedEncodingException {
		// pre-conditions
		int numOfConnections = connectionsModel.getConnections().length;

		connectionsModel.addConnection(connection);
		connectionsModel.addConnection(new ConnectionFake("jbtools", "http://www.jboss.org"));
		connectionsModel.addConnection(new ConnectionFake("openshift", "http://openshift.redhat.com"));

		// operations
		int numOfConnectionsAfterAddition = connectionsModel.getConnections().length;

		// verifications
		assertEquals(numOfConnections + 3, numOfConnectionsAfterAddition);
	}

	@Test
	public void shouldLoadDefaultHostConnectionsByUsername() throws UnsupportedEncodingException {
		// pre-conditions
		ConnectionsModel connectionsModel = new ConnectionsModelFake() {

			@Override
			protected String[] loadPersistedDefaultHosts() {
				return new String[] { "adietish@redhat.com" };
			}

			@Override
			protected String[] loadPersistedCustomHosts() {
				return new String[] {};
			}
		};

		// operations

		// verifications
		assertEquals(1, connectionsModel.getConnections().length);
		Connection connection = connectionsModel.getConnections()[0];
		assertTrue(connection.isDefaultHost());
		assertEquals(ConnectionUtils.getDefaultHostUrl(), connection.getHost());
		assertEquals("adietish@redhat.com", connection.getUsername());
	}

	@Test
	public void shouldSaveDefaultHostConnectionsByUsername() throws UnsupportedEncodingException {
		// pre-conditions
		ConnectionsModelFake connectionsModel = new ConnectionsModelFake();
		
		// operations
		/* custom host */
		connectionsModel.addConnection(new ConnectionFake("toolsjboss@gmail.com", "http://openshift.local"));
		/* default host */
		connectionsModel.addConnection(new ConnectionFake("adietish@redhat.com"));
		
		// verifications
		connectionsModel.save();
		List<String> defaultHosts = connectionsModel.getSavedDefaultHosts();
		assertEquals(1, defaultHosts.size());
		assertEquals("adietish@redhat.com", defaultHosts.get(0));
	}

	@Test
	public void shouldLoadCustomHostConnectionsByUrl() throws UnsupportedEncodingException {
		// pre-conditions
		ConnectionsModel connectionsModel = new ConnectionsModelFake() {

			@Override
			protected String[] loadPersistedDefaultHosts() {
				return new String[] {};
			}

			@Override
			protected String[] loadPersistedCustomHosts() {
				return new String[] { "http://adietish%40redhat.com@openshift.local" };
			}
		};

		// operations

		// verifications
		assertEquals(1, connectionsModel.getConnections().length);
		Connection connection = connectionsModel.getConnections()[0];
		assertFalse(connection.isDefaultHost());
		assertEquals("http://openshift.local", connection.getHost());
		assertEquals("adietish@redhat.com", connection.getUsername());
	}

	@Test
	public void shouldSaveCustomHostConnectionsByUrl() throws UnsupportedEncodingException {
		// pre-conditions
		ConnectionsModelFake connectionsModel = new ConnectionsModelFake();
		
		// operations
		/* custom host */
		connectionsModel.addConnection(new ConnectionFake("toolsjboss@gmail.com", "http://openshift.local"));
		/* default host */
		connectionsModel.addConnection(new ConnectionFake("adietish@redhat.com"));
		
		// verifications
		connectionsModel.save();
		List<String> customHosts = connectionsModel.getSavedCustomHosts();
		assertEquals(1, customHosts.size());
		assertEquals("http://toolsjboss%40gmail.com@openshift.local", customHosts.get(0));
	}

}
