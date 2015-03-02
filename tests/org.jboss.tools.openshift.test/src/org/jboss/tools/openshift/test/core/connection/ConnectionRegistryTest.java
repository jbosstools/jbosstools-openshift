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
package org.jboss.tools.openshift.test.core.connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;

import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ConnectionRegistryTest {

	private ConnectionsRegistry registry;
	private IConnection connection;
	private ConnectionsChange change;
	
	@Before
	public void setUp() {
		this.registry = new ConnectionsRegistry();
		this.change = new ConnectionsChange(registry);
		this.connection = new OneConnectionImpl("http://localhost:8081", "http://");
	}
	
	@Test
	public void getForAConnectionTypeShouldReturnACollectionOfTheRightType(){
		// pre-condition
		OneConnectionImpl [] ones = new OneConnectionImpl[] { 
				new OneConnectionImpl("localhost:8080", "http://"), 
				new OneConnectionImpl("localhost:8081", "http://")};
		OtherConnectionImpl [] others = new OtherConnectionImpl[]{ 
				new OtherConnectionImpl("localhost:9080", "http://"), 
				new OtherConnectionImpl("localhost:9081", "http://")};
		registry.add(ones[0]);
		registry.add(others[0]);
		registry.add(ones[1]);
		registry.add(others[1]);
		
		// operation
		Collection<OneConnectionImpl> allOneConnections = registry.getAll(OneConnectionImpl.class);
		
		// verification
		assertEqualsNoOrdering(Arrays.<OneConnectionImpl>asList(ones), allOneConnections);
	}

	private <T extends IConnection> void assertEqualsNoOrdering(Collection<T> allExpected, Collection<T> allActual) {
		assertEquals(allExpected.size(), allExpected.size());
		
		for (IConnection expected : allExpected) {
			assertTrue(allActual.contains(expected));
		}
	}
	
	static class OneConnectionImpl implements IConnection {
		String host;
		String scheme;
		String username;

		OneConnectionImpl(String host, String scheme) {
			this.host = host;
			this.scheme = scheme;
		}

		@Override
		public boolean connect() {
			return false;
		}

		@Override
		public String getHost() {
			return host;
		}

		@Override
		public boolean isDefaultHost() {
			return false;
		}

		@Override
		public String getScheme() {
			return scheme;
		}

		@Override
		public ConnectionType getType() {
			return null;
		}

		@Override
		public void refresh() {
		}

		@Override
		public boolean canConnect() throws IOException {
			return false;
		}

	}

	static class OtherConnectionImpl implements IConnection {

		String host;
		String scheme;
		String username;

		OtherConnectionImpl(String host, String schem) {
			this.host = host;
			this.scheme = schem;
		}

		@Override
		public boolean connect() {
			return false;
		}

		@Override
		public String getHost() {
			return host;
		}

		@Override
		public boolean isDefaultHost() {
			return false;
		}

		@Override
		public String getScheme() {
			return scheme;
		}

		@Override
		public ConnectionType getType() {
			return null;
		}

		@Override
		public void refresh() {
		}

		@Override
		public boolean canConnect() throws IOException {
			return false;
		}
	}

	@Test
	public void shouldAddConnectionAndIncrementSize() {
		// pre-conditions
		int numberOfConnections = registry.size();

		// operations
		boolean added = registry.add(connection);

		// verifications
		assertTrue(added);
		assertEquals(numberOfConnections + 1, registry.size());
	}

	@Test
	public void shouldNotAddIdenticalConnection() {
		// pre-conditions
		registry.add(connection);
		int numberOfConnections = registry.size();

		// operations
		boolean added = registry.add(connection);

		// verifications
		assertFalse(added);
		assertEquals(numberOfConnections, registry.size());
	}

	@Test
	public void shouldNotifyAddition() {
		// pre-conditions

		// operations
		registry.add(connection);

		// verifications
		assertTrue(change.isAdditionNotified());
		assertEquals(connection, change.getConnection());
	}

	@Test
	public void shouldSetRecentConnectionToLatestAddition() {
		// pre-conditions
		IConnection recentConnection = registry.getRecentConnection();

		// operations
		registry.add(connection);

		// verifications
		assertTrue(recentConnection == null
				|| !recentConnection.equals(connection));
		assertEquals(connection, registry.getRecentConnection());
	}

	@Test
	public void shouldRemoveConnection() {
		// pre-conditions
		registry.add(connection);
		int numberOfConnections = registry.size();

		// operations
		boolean removed = registry.remove(connection);

		// verifications
		assertTrue(removed);
		assertEquals(numberOfConnections - 1, registry.size());
	}

	@Test
	public void shouldNotRemoveConnectionThatWasNotAdded() {
		// pre-conditions
		int numberOfConnections = registry.size();

		// operations
		boolean removed = registry.remove(connection);

		// verifications
		assertFalse(removed);
		assertEquals(numberOfConnections, registry.size());
	}

	@Test
	public void shouldResetRecentConnectionOnRemoval() {
		// pre-conditions
		registry.add(connection);

		// operations
		registry.remove(connection);

		// verifications
		IConnection recentConnection = registry.getRecentConnection();
		assertEquals(null, recentConnection);
	}

	@Test
	public void shouldNotifyRemoval() {
		// pre-conditions
		registry.add(connection);

		// operations
		registry.remove(connection);

		// verifications
		assertTrue(change.isRemovalNotified());
		assertEquals(connection, change.getConnection());
	}

	@Test
	public void shouldGetConnectionByUrl() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		registry.add(connection);

		// operations
		ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
		IConnection queriedConnection = registry.getByUrl(connectionUrl);

		// verifications
		assertEquals(connection, queriedConnection);
	}

	@Test
	public void shouldHaveConnection() throws UnsupportedEncodingException {
		// pre-conditions
		registry.add(connection);

		// operations
		boolean found = registry.has(connection);

		// verifications
		assertTrue(found);
	}

	@Test
	public void shouldReturnAllRegistrySize() throws UnsupportedEncodingException {
		// pre-conditions
		int numOfConnections = registry.getAll().size();

		registry.add(connection);
		registry.add(new OneConnectionImpl("http://www.jboss.org", "https://"));
		registry.add(new OtherConnectionImpl("http://openshift.redhat.com", "http://"));

		// operations
		int numOfConnectionsAfterAddition = registry.getAll().size();

		// verifications
		assertEquals(numOfConnections + 3, numOfConnectionsAfterAddition);
	}
}
