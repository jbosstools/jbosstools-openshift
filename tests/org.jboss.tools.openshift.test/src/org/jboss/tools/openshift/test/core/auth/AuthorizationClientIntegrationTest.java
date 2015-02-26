package org.jboss.tools.openshift.test.core.auth;

import static org.junit.Assert.*;

import org.jboss.tools.openshift.internal.core.auth.AuthorizationClient;
import org.junit.Test;

public class AuthorizationClientIntegrationTest {

	private static final String BASE_URL = "https://localhost:8443";
	private AuthorizationClient client = new AuthorizationClient();
	
	@Test
	public void testAuthorize() {
		String token = client.requestToken(BASE_URL, "foo", "bar");
		assertNotNull("Exp. to get a token", token);
	}

	@Test
	public void testBadAuthorization() {
		String token = client.requestToken(BASE_URL, "foo", "");
		assertNull("Exp. to not get a token", token);
	}

}
