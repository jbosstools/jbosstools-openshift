package org.jboss.tools.openshift.express.test.core.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 * @author Matej 'Yin' Gagyi
 */
public class ExpressConnectionTest {
	@Test
	public void nullHostShouldBeDefaultHost() {
		// pre-conditions

		// operations
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", null);
		
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
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", host);
		
		// verifications
		assertFalse(connection.isDefaultHost());
		assertEquals(ExpressConnectionUtils.getDefaultHostUrl(), connection.getHost());
	}

	@Test
	public void setHostShouldResetIsDefaultHost() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", null);
		assertTrue(connection.isDefaultHost());

		// operations
		connection.setHost("http://www.redhat.com");
		
		// verifications
		assertFalse(connection.isDefaultHost());
	}

	@Test
	public void shouldExtractUrlPortions() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		String scheme = UrlUtils.SCHEME_HTTP;
		String username = "adietish@redhat.com";
		String password = "12345";
		String server = "openshift.redhat.com";
		
		// operations
		ConnectionURL connectionUrl = ConnectionURL.forURL(scheme + URLEncoder.encode(username, "UTF-8") + ":"
				+ password + "@" + server);
		ExpressConnectionFake connection = new ExpressConnectionFake(connectionUrl.getUsername(), connectionUrl.getScheme(),
				connectionUrl.getHost());
		
		// verifications
		assertEquals(scheme, connection.getScheme());
		assertEquals(username, connection.getUsername());
		assertEquals(scheme + server, connection.getHost());
	}

	@Test
	public void shouldAllowPortInUrl() throws UnsupportedEncodingException, MalformedURLException {
		// pre-conditions
		
		// operations
		ConnectionURL connectionUrl = ConnectionURL.forURL("http://adietish%40redhat.com@localhost:8081");
		ExpressConnectionFake connection = new ExpressConnectionFake(connectionUrl.getUsername(), connectionUrl.getScheme(),
				connectionUrl.getHost());

		// verifications
		assertEquals("http://localhost:8081", connection.getHost());
	}

	@Test
	public void shouldHaveHostWithScheme() {
		// pre-conditions
		
		// operations
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");
		
		// verifications
		assertNotNull(connection.getHost());
		assertTrue(connection.getHost().startsWith(UrlUtils.HTTP));
		assertNotNull(connection.getScheme());
		assertTrue(connection.getScheme().startsWith(UrlUtils.HTTP));
	}

	@Test
	public void shouldHaveHostWithSchemeAfterSetting() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");
		// operations
		connection.setHost("jboss.com");
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
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "scheme://openshift.redhat.com");
		
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
	public void setHostShouldDisconnect() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", "openshift.redhat.com");
		connection.setConnected(true);
		assertTrue(connection.isConnected());
		
		// operations
		connection.setHost("fakeHost");
		
		// verifications
		assertFalse(connection.isConnected());
	}

	@Test
	public void shouldUpdate() {
		// pre-conditions
		ExpressConnectionFake connection = new ExpressConnectionFake("fakeUser", null);
		connection.setRememberPassword(true);
		connection.setConnected(true);
		assertTrue(connection.isConnected());
		assertTrue(connection.isDefaultHost());
		String newUsername = "anotherUser";
		String newHost = "http://www.redhat.com";
		String newPassword = "1q2w3e";
		ExpressConnectionFake updatingConnection = new ExpressConnectionFake(newUsername, newHost);
		updatingConnection.setPassword(newPassword);
		updatingConnection.setRememberPassword(false);
		
		// operations
		connection.update(updatingConnection);
		
		// verifications
		assertEquals(newUsername, connection.getUsername());
		assertEquals(newHost, connection.getHost());
		assertFalse(newUsername, connection.isDefaultHost());
		assertEquals(newPassword, connection.getPassword());
		assertFalse(connection.isRememberPassword());
		assertFalse(connection.isConnected());
	}
}
