/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core.connection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnectionPersistency;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionPersistencyTest {

	private ExpressConnection connection1;
	private ExpressConnection connection2;

	@Before
	public void setup() throws Exception {
		this.connection1 = new ExpressConnection("foo", "https://localhost:8442");
		this.connection2 = new ExpressConnection("bar", "https://localhost:8443");
	}

	@Test
	public void shouldSaveConnections() {
		ExpressConnectionPersistency persistency = new ExpressConnectionPersistency() {

			@Override
			protected void persist(String[] connections) {
				// verification
				assertArrayEquals(new String[] {	"https://foo@localhost:8442", "https://bar@localhost:8443" }, connections);
			}
			
		};
		// pre-condition
		List<ExpressConnection> connections = new ArrayList<ExpressConnection>();
		connections.add(connection1);
		connections.add(connection2);
		
		// operations
		persistency.save(connections);
	}

	@Test
	public void shouldLoadConnections() {
		// pre-condition
		ExpressConnectionPersistency persistency = new ExpressConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"https://foo@localhost:8442",
						"https://bar@localhost:8443" };
				}
		};
		// operations
		Collection<ExpressConnection> connections = persistency.load();

		// verification
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
	}

	@Test
	public void shouldNotLoadMalformedUrl() {	
		// pre-condition
		ExpressConnectionPersistency persistency = new ExpressConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"https://foo@localhost:8442",
						"htp://bingobongo",
						"https://bar@localhost:8443" };
				}
		};
		
		// operations
		Collection<ExpressConnection> connections = persistency.load();

		// verification
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
	}
	
	@Test
	public void shouldLoadUsernamesAsDefaultHostConnection() {	
		// pre-condition
		ExpressConnectionPersistency persistency = new ExpressConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"bingobongo@redhat.com" };
				}
		};
		
		// operations
		Collection<ExpressConnection> connections = persistency.load();

		// verification
		assertEquals(1, connections.size());
		ExpressConnection connection = connections.iterator().next();
		assertTrue(connection.isDefaultHost());
		assertEquals("bingobongo@redhat.com", connection.getUsername());
	}

	private void assertContainsConnection(ExpressConnection connection, Collection<ExpressConnection> connections) {
		for (ExpressConnection effectiveConnection : connections) {
			if (effectiveConnection.equals(connection)) {
				return;
			}
		}
		fail(String.format("Could not find connection %s in connections %s.", connection, connections));
	}
}
