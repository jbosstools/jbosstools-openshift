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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationStrategy;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IResource;

/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionTest {
	
	private Connection connection;
	@Mock IClient client;
	
	@Before
	public void setup() throws Exception {
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8443"));
		doReturn(mock(IAuthorizationStrategy.class)).when(client).getAuthorizationStrategy();		
		when(client.getContext(anyString())).thenReturn(mock(IAuthorizationContext.class));
		IAuthorizationContext authContext = mock(IAuthorizationContext.class);
		doReturn(true).when(authContext).isAuthorized();

		connection = new Connection(client, null, null);
	}

	@Test
	public void ownsResourceShouldReturnTrueWhenClientsAreSame() {
		Map<String, Object> mocks = givenAResourceThatAcceptsAVisitorForIClientCapability(client);
		assertTrue(connection.ownsResource((IResource)mocks.get("resource")));
	}

	@Test
	public void ownsResourceShouldReturnFalseWhenClientsAreDifferent() {
		IClient diffClient = mock(IClient.class);
		Map<String, Object> mocks = givenAResourceThatAcceptsAVisitorForIClientCapability(diffClient);
		assertFalse(connection.ownsResource((IResource)mocks.get("resource")));
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> givenAResourceThatAcceptsAVisitorForIClientCapability(IClient client){
		final IClientCapability capability = mock(IClientCapability.class);
		when(capability.getClient()).thenReturn(client);
		IResource resource = mock(IResource.class);
		when(resource.accept(any(CapabilityVisitor.class), any())).thenAnswer(new Answer<IClient>() {
			@Override
			public IClient answer(InvocationOnMock invocation) throws Throwable {
				CapabilityVisitor<IClientCapability, IClient> visitor = (CapabilityVisitor<IClientCapability, IClient>)invocation.getArguments()[0];
				return visitor.visit(capability);
			}
		});
		Map<String, Object> mocks = new HashMap<>();
		mocks.put("capability", capability);
		mocks.put("resource", resource);
		return mocks;
	}
	
	@Test
	public void getResourceKindShouldCallClient() {
		// given
		// when
		connection.getResources(ResourceKind.PROJECT);
		// then
		verify(client).list(eq(ResourceKind.PROJECT), anyString());
	}

	@Test
	public void getHostShouldReturnHost() {
		assertEquals("https://localhost:8443", connection.getHost());
	}
	@Test
	public void getSchemeShouldReturnScheme() {
		assertEquals("https://", connection.getScheme());
	}
	
	@Test
	public void shouldNotEqualsIfDifferentUser() throws Exception {
		Connection two = new Connection("https://localhost:8443", null, null);
		two.setUsername("foo");

		assertNotEquals("Exp. connections not to be equal unless they have same url and user", connection, two);
	}

	@Test
	public void shouldNotEqualsIfDifferentScheme() throws Exception {
		Connection two = new Connection("http://localhost:8443", null, null);

		assertNotEquals("Exp. connections not to be equal unless they have same url and user", connection, two);
	}

	@Test
	public void shouldNotEqualsIfDifferentHost() throws Exception {
		Connection two = new Connection("https://openshift.redhat.com:8443", null, null);
		two.setUsername("foo");

		assertNotEquals("Exp. connections not to be equal unless they have same url and user", connection, two);
	}

	@Test
	public void shouldEqualsIfSameUrlAndUser() throws Exception {
		connection.setUsername("foo");
		Connection two = new Connection("https://localhost:8443", null, null);
		two.setUsername("foo");

		assertEquals("Exp. connections to be equal if they have same url and user", connection, two);
	}

	@Test
	public void cloneShouldCreateIdenticalConnection() throws Exception {
		// pre-conditions
		Connection connection = new Connection("https://localhost:8443", null, null);
		connection.setPassword("foo");
		connection.setPassword("bar");
		
		// operations
		IConnection clonedConnection = connection.clone();

		// verifications
		assertTrue(clonedConnection instanceof Connection);
		assertEquals(connection.getUsername(), clonedConnection.getUsername());
		assertEquals(connection.getPassword(), clonedConnection.getPassword());
		assertEquals(connection.isRememberPassword(), clonedConnection.isRememberPassword());
		assertEquals(connection.getHost(), clonedConnection.getHost());
	}

	@Test
	public void shouldNotCredentialsEqualIfDifferentToken() throws Exception {
		Connection two = (Connection)connection.clone();
		two.setToken("tokenTwo");
		assertEquals(connection, two);
		assertFalse(two.credentialsEqual(connection));
	}

	@Test
	public void shouldNotCredentialsEqualIfDifferentPassword() throws Exception {
		Connection two = (Connection)connection.clone();
		two.setPassword("passwordTwo");
		assertEquals(connection, two);
		assertFalse(two.credentialsEqual(connection));
	}

	@Test
	public void shouldNotCredentialsEqualIfDifferentUsername() throws Exception {
		Connection two = (Connection)connection.clone();
		two.setUsername("userTwo");
		assertNotEquals(connection, two);
		assertFalse(two.credentialsEqual(connection));
	}

	@Test
	public void shouldCredentialsEqualForClone() throws Exception {
		Connection two = (Connection)connection.clone();
		assertEquals(connection, two);
		assertTrue(two.credentialsEqual(connection));
	}

	@Test
	public void should_not_overwrite_client_authorization_strategy_on_isConnected() {
		// given
		// when
		connection.isConnected(new NullProgressMonitor());
		// then
		verify(client, never()).setAuthorizationStrategy(any(IAuthorizationStrategy.class));
	}

	@Test
	public void should_set_client_authorization_strategy_when_not_present_on_isConnected() {
		// given
		when(client.getAuthorizationStrategy()).thenReturn(null);
		// when
		connection.isConnected(new NullProgressMonitor());
		// then
		verify(client).setAuthorizationStrategy(any(IAuthorizationStrategy.class));
	}

	//	@Test
//	public void nullHostShouldBeDefaultHost() {
//		// pre-conditions
//
//		// operations
//		Connection connection = new ConnectionFake("fakeUser", null);
//
//		// verifications
//		assertTrue(connection.isDefaultHost());
//		assertEquals(ConnectionUtils.getDefaultHostUrl(), connection.getHost());
//	}
//
//	@Test
//	public void manuallySetDefaultHostShouldNotBeDefaultHost() {
//		// pre-conditions
//		String host = ConnectionUtils.getDefaultHostUrl();
//		assertNotNull(host);
//
//		// operations
//		Connection connection = new ConnectionFake("fakeUser", host);
//
//		// verifications
//		assertFalse(connection.isDefaultHost());
//		assertEquals(ConnectionUtils.getDefaultHostUrl(), connection.getHost());
//	}
//
//	@Test
//	public void setHostShouldResetIsDefaultHost() {
//		// pre-conditions
//		Connection connection = new ConnectionFake("fakeUser", null);
//		assertTrue(connection.isDefaultHost());
//
//		// operations
//		connection.setHost("http://www.redhat.com");
//
//		// verifications
//		assertFalse(connection.isDefaultHost());
//	}
//
//	@Test
//	public void shouldExtractUrlPortions() throws UnsupportedEncodingException, MalformedURLException {
//		// pre-conditions
//		String scheme = UrlUtils.SCHEME_HTTP;
//		String username = "adietish@redhat.com";
//		String password = "12345";
//		String server = "openshift.redhat.com";
//
//		// operations
//		ConnectionURL connectionUrl = ConnectionURL.forURL(scheme + URLEncoder.encode(username, "UTF-8") + ":"
//				+ password + "@" + server);
//		Connection connection = new ConnectionFake(connectionUrl.getUsername(), connectionUrl.getScheme(), connectionUrl.getHost());
//
//		// verifications
//		assertEquals(scheme, connection.getScheme());
//		assertEquals(username, connection.getUsername());
//		assertEquals(scheme + server, connection.getHost());
//	}
//
//	@Test
//	public void shouldAllowPortInUrl() throws UnsupportedEncodingException, MalformedURLException {
//		// pre-conditions
//
//		// operations
//		ConnectionURL connectionUrl = ConnectionURL.forURL("http://adietish%40redhat.com@localhost:8081");
//		Connection connection = new ConnectionFake(connectionUrl.getUsername(), connectionUrl.getScheme(), connectionUrl.getHost());
//
//		// verifications
//		assertEquals("http://localhost:8081", connection.getHost());
//	}
//
//	@Test
//	public void shouldHaveHostWithScheme() {
//		// pre-conditions
//
//		// operations
//		Connection connection = new ConnectionFake("fakeUser", "openshift.redhat.com");
//
//		// verifications
//		assertNotNull(connection.getHost());
//		assertTrue(connection.getHost().startsWith(UrlUtils.HTTP));
//		assertNotNull(connection.getScheme());
//		assertTrue(connection.getScheme().startsWith(UrlUtils.HTTP));
//	}
//
//	@Test
//	public void shouldHaveHostWithSchemeAfterSetting() {
//		// pre-conditions
//		Connection connection = new ConnectionFake("fakeUser", "openshift.redhat.com");
//
//		// operations
//		connection.setHost("jboss.com");
//
//		// verifications
//		assertNotNull(connection.getHost());
//		assertTrue(connection.getHost().startsWith(UrlUtils.HTTP));
//		assertNotNull(connection.getScheme());
//		assertTrue(connection.getScheme().startsWith(UrlUtils.HTTP));
//	}
//
//	@Test
//	public void shouldNotOverrideGivenScheme() {
//		// pre-conditions
//
//		// operations
//		Connection connection = new ConnectionFake("fakeUser", "scheme://openshift.redhat.com");
//
//		// verifications
//		assertNotNull(connection.getHost());
//		assertTrue(connection.getHost().startsWith("scheme://"));
//		assertNotNull(connection.getScheme());
//		assertEquals("scheme://", connection.getScheme());
//	}
//
//	@Test
//	public void setUsernameShouldDisconnect() {
//		// pre-conditions
//		ConnectionFake connection = new ConnectionFake("fakeUser", "openshift.redhat.com");
//		connection.setConnected(true);
//		assertTrue(connection.isConnected());
//
//		// operations
//		connection.setUsername("adietish");
//
//		// verifications
//		assertFalse(connection.isConnected());
//	}
//
//	@Test
//	public void setPasswordShouldDisconnect() {
//		// pre-conditions
//		ConnectionFake connection = new ConnectionFake("fakeUser", "openshift.redhat.com");
//		connection.setConnected(true);
//		assertTrue(connection.isConnected());
//
//		// operations
//		connection.setPassword("fakePassword");
//
//		// verifications
//		assertFalse(connection.isConnected());
//	}
//
//	@Test
//	public void setHostShouldDisconnect() {
//		// pre-conditions
//		ConnectionFake connection = new ConnectionFake("fakeUser", "openshift.redhat.com");
//		connection.setConnected(true);
//		assertTrue(connection.isConnected());
//
//		// operations
//		connection.setHost("fakeHost");
//
//		// verifications
//		assertFalse(connection.isConnected());
//	}
//
//	@Test
//	public void shouldUpdate() {
//		// pre-conditions
//		ConnectionFake connection = new ConnectionFake("fakeUser", null);
//		connection.setRememberPassword(true);
//		connection.setConnected(true);
//		assertTrue(connection.isConnected());
//		assertTrue(connection.isDefaultHost());
//
//		String newUsername = "anotherUser";
//		String newHost = "http://www.redhat.com";
//		String newPassword = "1q2w3e";
//		Connection updatingConnection = new ConnectionFake(newUsername, newHost);
//		updatingConnection.setPassword(newPassword);
//		updatingConnection.setRememberPassword(false);
//
//		// operations
//		connection.update(updatingConnection);
//
//		// verifications
//		assertEquals(newUsername, connection.getUsername());
//		assertEquals(newHost, connection.getHost());
//		assertFalse(newUsername, connection.isDefaultHost());
//		assertEquals(newPassword, connection.getPassword());
//		assertFalse(connection.isRememberPassword());
//		assertFalse(connection.isConnected());
//	}
}
