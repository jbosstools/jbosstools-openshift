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
package org.jboss.tools.openshift.common.test.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.*;

import static org.mockito.Matchers.eq;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionsRegistryTest {

	
	private ConnectionsRegistry connectionsRegistry;
	@Mock
	private IConnection connection;

	@Before
	public void setUp() {
		this.connectionsRegistry = new ConnectionsRegistry();
	}

	@Test
	public void shouldAddConnection() {
		// pre-conditions
		int numberOfConnections = connectionsRegistry.size();

		// operations
		boolean added = connectionsRegistry.add(connection);

		// verifications
		assertTrue(added);
		assertEquals(numberOfConnections + 1, connectionsRegistry.size());
	}

	@Test
	public void shouldNotAddIdenticalConnection() {
		// pre-conditions
		connectionsRegistry.add(connection);
		int numberOfConnections = connectionsRegistry.size();

		// operations
		boolean added = connectionsRegistry.add(connection);

		// verifications
		assertFalse(added);
		assertEquals(numberOfConnections, connectionsRegistry.size());
	}

	@Test
	public void shouldNotifyAddition() {
		// pre-conditions
		IConnectionsRegistryListener mockListener =
				mock(IConnectionsRegistryListener.class);
		
		// operations
		connectionsRegistry.addListener(mockListener);
		connectionsRegistry.add(connection);

		// verifications
		verify(mockListener).connectionAdded(eq(connection));
		verifyNoMoreInteractions(mockListener);
	}

	@Test
	public void shouldSetRecentConnectionToLatestAddition() {
		// pre-conditions
		IConnection recentConnection = connectionsRegistry.getRecentConnection();

		// operations
		connectionsRegistry.add(connection);

		// verifications
		assertTrue(recentConnection == null || !recentConnection.equals(connection));
		assertEquals(connection, connectionsRegistry.getRecentConnection());
	}

	@Test
	public void shouldRemoveConnection() {
		// pre-conditions
		connectionsRegistry.add(connection);
		int numberOfConnections = connectionsRegistry.size();

		// operations
		boolean removed = connectionsRegistry.remove(connection);

		// verifications
		assertTrue(removed);
		assertEquals(numberOfConnections - 1, connectionsRegistry.size());
	}

	@Test
	public void shouldNotRemoveConnectionThatWasNotAdded() {
		// pre-conditions
		int numberOfConnections = connectionsRegistry.size();

		// operations
		boolean removed = connectionsRegistry.remove(connection);

		// verifications
		assertFalse(removed);
		assertEquals(numberOfConnections, connectionsRegistry.size());
	}

	@Test
	public void shouldResetRecentConnectionOnRemoval() {
		// pre-conditions
		connectionsRegistry.add(connection);

		// operations
		connectionsRegistry.remove(connection);

		// verifications
		IConnection recentConnection = connectionsRegistry.getRecentConnection();
		assertEquals(null, recentConnection);
	}

	@Test
	public void shouldNotifyRemoval() {
		// pre-conditions
		IConnectionsRegistryListener mockListener =
				mock(IConnectionsRegistryListener.class);
		connectionsRegistry.add(connection);
		connectionsRegistry.addListener(mockListener);
		
		// operations
		connectionsRegistry.remove(connection);

		// verifications
		verify(mockListener).connectionRemoved(eq(connection));
		verifyNoMoreInteractions(mockListener);
	}

	@Test
	public void shouldGetConnectionByUrl() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		connectionsRegistry.add(connection);

		// operations
		ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
		IConnection queriedConnection = connectionsRegistry.getByUrl(connectionUrl);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldHaveConnection() throws UnsupportedEncodingException {
		// pre-conditions
		connectionsRegistry.add(connection);

		// operations
		boolean found = connectionsRegistry.has(connection);

		// verifications
		assertTrue(found);
	}

	@Test
	public void shouldReturnAllConnections() throws UnsupportedEncodingException {
		// pre-conditions
		int numOfConnections = connectionsRegistry.getAll().length;
		IConnection mockConnection2 = mock(IConnection.class);
		IConnection mockConnection3 = mock(IConnection.class);
		when(mockConnection2.getHost()).thenReturn("http://www.jboss.org");
		when(mockConnection3.getHost()).thenReturn("http://openshift.redhat.com");

		connectionsRegistry.add(connection);
		connectionsRegistry.add(mockConnection2);
		connectionsRegistry.add(mockConnection3);

		// operations
		int numOfConnectionsAfterAddition = connectionsRegistry.getAll().length;

		// verifications
		assertEquals(numOfConnections + 3, numOfConnectionsAfterAddition);
	}
}
