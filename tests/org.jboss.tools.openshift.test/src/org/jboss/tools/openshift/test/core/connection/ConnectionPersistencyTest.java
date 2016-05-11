/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionPersistency;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCorePreferences;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionPersistencyTest {

	private Connection connection1;
	private Connection connection2;
	
	@Mock
	private IOpenShiftCorePreferences preferences;
	private ConnectionPersistency persistency;

	@Before
	public void setup() throws Exception {
		this.connection1 = new Connection("https://localhost:8442", null, null);
		connection1.setUsername("foo");
		connection1.setToken("bar");
		this.connection2 = new Connection("https://localhost:8443", null, null);
		connection2.setUsername("bar");
		connection2.setToken("foo");
		
		persistency = new TestConnectionPersistency();
	}

	@Test
	public void shouldSaveConnections() {
		// pre-condition
		List<Connection> connections = new ArrayList<>();
		connections.add(connection1);
		connections.add(connection2);
		
		// operations
		persistency.save(connections);
		
		verify(preferences).saveConnections(eq(new String[] {"https://foo@localhost:8442", "https://bar@localhost:8443" }));
		verify(preferences, times(2)).saveExtProperties(anyString(), any());
	}

	@Test
	public void shouldLoadConnections() {

		// pre-condition
		when(preferences.loadConnections()).thenReturn(
				new String[] {
					"https://foo@localhost:8442",
					"https://bar@localhost:8443" 
				}
			);

		// operations
		Collection<Connection> connections = persistency.load();

		// verification
		verify(preferences, times(2)).loadExtProperties(anyString());
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
		
	}

	@Test
	public void shouldNotLoadMalformedUrl() {
		// pre-condition
		when(preferences.loadConnections()).thenReturn(
				new String[] {
						"https://foo@localhost:8442",
						"@bingobongo",
						"https://bar@localhost:8443" 
				}
			);

		// operations
		Collection<Connection> connections = persistency.load();

		// verification
		assertEquals(2, connections.size());
		assertContainsConnection(connection1, connections);
		assertContainsConnection(connection2, connections);
	}
	
	@Ignore("no default server for OpenShift 3 yet")
	@Test
	public void shouldLoadUsernamesAsDefaultHostConnection() {	
		// pre-condition
		ConnectionPersistency persistency = new ConnectionPersistency() {

			@Override
			protected String[] loadPersisted() {
				return new String[] {
						"bingobongo@redhat.com" };
				}
		};
		
		// operations
		Collection<Connection> connections = persistency.load();

		// verification
		assertEquals(1, connections.size());
		Connection connection = connections.iterator().next();
		assertTrue(connection.isDefaultHost());
		assertEquals("bingobongo@redhat.com", connection.getUsername());
	}

	private void assertContainsConnection(Connection connection, Collection<Connection> connections) {
		for (Connection effectiveConnection : connections) {
			if (effectiveConnection.equals(connection)) {
				assertNotNull("Exp. the extended properties to be loaded and not null", connection.getExtendedProperties());
				return;
			}
		}
		fail(String.format("Could not find connection %s in connections %s.", connection, connections));
	}
	
	private class TestConnectionPersistency extends ConnectionPersistency{
		

		TestConnectionPersistency(){
			super(preferences);
		}
	}
}	
