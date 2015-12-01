/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.core.connection.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyCredentialsPrompterTest {

	private Connection connection;
	@Mock
	private IClient client;
	private LazyCredentialsPrompter lazyPrompter;
	@Mock
	private ICredentialsPrompter permissiveExtensionPrompter;
	@Mock
	private ICredentialsPrompter denyingExtensionPrompter;


	@Before
	public void setup() throws MalformedURLException {
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8443"));
		this.connection = new Connection(client, lazyPrompter, null);
		when(permissiveExtensionPrompter.promptAndAuthenticate(connection, null)).thenReturn(true);
		when(denyingExtensionPrompter.promptAndAuthenticate(connection, null)).thenReturn(true);
		this.lazyPrompter = spy(new LazyCredentialsPrompter());
	}

	@Test
	public void testAuthenticatesOKWithPrompter() {
		when(lazyPrompter.getExtension()).thenReturn(permissiveExtensionPrompter);

		assertTrue("Exp. to prompt for creds", lazyPrompter.promptAndAuthenticate(connection, null));
		verify(permissiveExtensionPrompter, times(1)).promptAndAuthenticate(any(Connection.class), any());
	}

	@Test
	public void testAuthenticatesNOTOKWithPrompter() {
		when(lazyPrompter.getExtension()).thenReturn(null);

		assertFalse("Exp. to prompt for creds", lazyPrompter.promptAndAuthenticate(connection, null));
		verify(denyingExtensionPrompter, never()).promptAndAuthenticate(any(Connection.class), any());
	}

	@Test
	public void testAuthenticatesNOTOKWithoutPrompter() {
		when(lazyPrompter.getExtension()).thenReturn(null);

		assertFalse("Exp. to prompt for creds", lazyPrompter.promptAndAuthenticate(connection, null));
	}
}
