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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionTest {

	@Test
	public void nullHostShouldBeDefaultHost() {
		// pre-conditions

		// operations
		ExpressConnection connection = new ExpressConnectionFake("fakeUser", null);

		// verifications
		assertTrue(connection.isDefaultHost());
		assertEquals(ExpressConnectionUtils.getDefaultHostUrl(), connection.getHost());
	}

	@Test
	public void manuallySetDefaultHostShouldNotBeDefaultHost() {
		// pre-conditions
		String host = ExpressConnectionUtils.getDefaultHostUrl();
		assertNotNull(host);

		// operations
		ExpressConnection connection = new ExpressConnectionFake("fakeUser", host);

		// verifications
		assertFalse(connection.isDefaultHost());
		assertEquals(ExpressConnectionUtils.getDefaultHostUrl(), connection.getHost());
	}

	@Test
	public void shouldHaveHostWithScheme() {
		// pre-conditions

		// operations
		ExpressConnection connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");

		// verifications
		assertNotNull(connection.getHost());
		assertTrue(connection.getHost().startsWith(UrlUtils.HTTP));
		assertNotNull(connection.getScheme());
		assertTrue(connection.getScheme().startsWith(UrlUtils.HTTP));
	}

	@Test
	public void shouldNotOverrideGivenScheme() {
		// pre-conditions

		// operations
		ExpressConnection connection = new ExpressConnectionFake("fakeUser", "scheme://openshift.redhat.com");

		// verifications
		assertNotNull(connection.getHost());
		assertTrue(connection.getHost().startsWith("scheme://"));
		assertNotNull(connection.getScheme());
		assertEquals("scheme://", connection.getScheme());
	}

	@Test
	public void setUsernameShouldDisconnect() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");
		connection.setConnected(true);
		assertTrue(connection.isConnected());

		// operations
		connection.setUsername("adietish");

		// verifications
		assertFalse(connection.isConnected());
	}

	@Test
	public void setPasswordShouldDisconnect() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");
		connection.setConnected(true);
		assertTrue(connection.isConnected());

		// operations
		connection.setPassword("fakePassword");

		// verifications
		assertFalse(connection.isConnected());
	}

	@Test
	public void hasCodeShouldNotChangeUponUsernameChange() {
		// pre-conditions
		ExpressConnection connection = new ExpressConnection("fakeUser", "openshift.redhat.com");
		int hashCode = connection.hashCode();

		// operations
		connection.setUsername("foobar");

		// verifications
		assertEquals(hashCode, connection.hashCode());
	}

	@Test
	public void hasCodeShouldNotChangeUponPasswordChange() {
		// pre-conditions
		ExpressConnection connection = new ExpressConnection("fakeUser", "openshift.redhat.com");
		connection.setPassword("111111");
		int hashCode = connection.hashCode();

		// operations
		connection.setPassword("22222");

		// verifications
		assertEquals(hashCode, connection.hashCode());
	}

	@Test
	public void cloneShouldCreateIdenticalConnection() {
		// pre-conditions
		ExpressConnection connection = new ExpressConnection("foo", "https://openshift.redhat.com");
		connection.setPassword("bar");

		// operations
		IConnection clonedConnection = connection.clone();

		// verifications
		assertTrue(clonedConnection instanceof ExpressConnection);
		assertEquals(connection.getUsername(), clonedConnection.getUsername());
		assertEquals(connection.getPassword(), clonedConnection.getPassword());
		assertEquals(connection.isRememberPassword(), clonedConnection.isRememberPassword());
		assertEquals(connection.getHost(), clonedConnection.getHost());
	}

	@Test
	public void updateShouldUpdateConnection() {
		// pre-conditions
		ExpressConnectionFake updatedConnection = new ExpressConnectionFake("foo", "https://openshift.redhat.com");
		updatedConnection.setPassword("bar");
		updatedConnection.setRememberPassword(true);
		updatedConnection.setConnected(false);

		ExpressConnectionFake updatingConnection = new ExpressConnectionFake("bar", "http://localhost:8443");
		updatingConnection.setPassword("foo");
		updatingConnection.setRememberPassword(false);
		updatingConnection.setConnected(true);

		// operations
		updatedConnection.update(updatingConnection);

		// verifications
		assertEquals(updatingConnection.getUsername(), updatedConnection.getUsername());
		assertEquals(updatingConnection.getPassword(), updatedConnection.getPassword());
		assertEquals(updatingConnection.isRememberPassword(), updatedConnection.isRememberPassword());
		assertTrue(updatedConnection.isConnected());

		// host cannot be updated (is immutable)!
		assertEquals("https://openshift.redhat.com", updatedConnection.getHost());

	}
}