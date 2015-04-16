/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionPersistency;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ConnectionPersistencyTest {

	private Connection connection1;
	private Connection connection2;

	@Before
	public void setup() throws Exception {
		this.connection1 = new Connection("https://localhost:8442", null, null, null);
		connection1.setUsername("foo");
		connection1.setToken("bar");
		this.connection2 = new Connection("https://localhost:8443", null, null, null);
		connection2.setUsername("bar");
		connection2.setToken("foo");
	}

	@Test
	public void shouldSaveConnections() {

		ConnectionPersistency persistency = new ConnectionPersistency() {

			@Override
			protected void persist(String[] connections) {
				// verification
				assertArrayEquals(new String[] {	"https://foo@localhost:8442", "https://bar@localhost:8443" }, connections);
			}
			
		};
		// pre-condition
		List<Connection> connections = new ArrayList<Connection>();
		connections.add(connection1);
		connections.add(connection2);
		
		// operations
		persistency.save(connections);
	}

	@Test
	public void shouldLoadConnections() {

		// pre-condition
		ConnectionPersistency persistency = new ConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"https://foo@localhost:8442",
						"https://bar@localhost:8443" };
				}
		};

		// operations
		Collection<Connection> connections = persistency.load();

		// verification
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
	}

	@Test
	public void shouldNotLoadMalformedUrl() {
		// pre-condition
		ConnectionPersistency persistency = new ConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"https://foo@localhost:8442",
						"@bingobongo",
						"https://bar@localhost:8443" };
				}
		};
		
		// operations
		Collection<Connection> connections = persistency.load();

		// verification
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
	}
	
	private void assertContainsConnection(Connection connection, Collection<Connection> connections) {
		for (Connection effectiveConnection : connections) {
			if (effectiveConnection.equals(connection)) {
				return;
			}
		}
		fail(String.format("Could not find connection %s in connections %s.", connection, connections));
	}}
