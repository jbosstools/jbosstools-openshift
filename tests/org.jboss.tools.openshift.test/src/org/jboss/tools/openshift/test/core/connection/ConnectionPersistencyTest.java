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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionPersistency;
import org.jboss.tools.openshift.core.preferences.OpenShiftPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionPersistencyTest {

	@Mock
	private OpenShiftPreferences preferences;
	private ConnectionPersistency persistency;
	private ArrayList<Connection> connections;
	private Connection connection1;
	private Connection connection2;

	@Before
	public void setup() throws Exception {
		this.persistency = new ConnectionPersistency(preferences);

		this.connection1 = new Connection("https://localhost:8442", null, null, null);
		connection1.setUsername("foo");
		connection1.setToken("bar");
		this.connection2 = new Connection("https://localhost:8442", null, null, null);
		connection2.setUsername("kung");
		connection2.setToken("foo");

		this.connections = new ArrayList<Connection>();
		connections.add(connection1);
		connections.add(connection2);
	}

	@Test
	public void shouldSaveConnections() {
		// pre-condition

		// operations
		persistency.save(connections);

		// verification
		verify(preferences).saveConnections(new String[] {
				"{\"url\" : \"https://localhost:8442\", \"username\" : \"foo\", \"token\" : \"bar\"}",
				"{\"url\" : \"https://localhost:8442\", \"username\" : \"kung\", \"token\" : \"foo\"}" });
	}

	@Test
	public void shouldLoadConnections() {
		// pre-condition
		when(preferences.loadConnections()).thenReturn(new String[] {
				"{\"url\" : \"https://localhost:8442\", \"username\" : \"foo\", \"token\" : \"bar\"}",
				"{\"url\" : \"https://localhost:8442\", \"username\" : \"kung\", \"token\" : \"foo\"}" });
		
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
	}
}
