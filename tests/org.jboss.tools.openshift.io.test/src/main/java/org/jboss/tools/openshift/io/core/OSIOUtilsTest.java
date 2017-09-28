/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import io.jsonwebtoken.Jwts;

public class OSIOUtilsTest {
	private static final String LOGIN_RESPONSE = "{\"access_token\":\"a\",\"expires_in\":1,\"refresh_token\":\"b\",\"refresh_expires_in\":2}";
	
	private static final String REFRESH_RESPONSE = "{\r\n" + 
			"  \"token\": {\r\n" + 
			"    \"access_token\": \"Beatae fuga enim suscipit sapiente vitae eligendi.\",\r\n" + 
			"    \"expires_in\": 5476746541684266001,\r\n" + 
			"    \"not-before-policy\": \"0f968573-530f-4287-a500-ddb1ac80e7eb\",\r\n" + 
			"    \"refresh_expires_in\": 5476746541684266000,\r\n" + 
			"    \"refresh_token\": \"Eum sed nobis provident aut quae occaecati.\",\r\n" + 
			"    \"token_type\": \"Consequatur quasi voluptatem non accusamus.\"\r\n" + 
			"  }\r\n" + 
			"}";
	
	@Test
	public void checkThatAccessTokenIsReturned() throws IOException {
		LoginResponse info = OSIOUtils.decodeLoginResponse(LOGIN_RESPONSE);
		assertEquals("a", info.getAccessToken());
		
	}


	@Test
	public void checkThatRefreshTokenIsReturned() throws IOException {
		LoginResponse info = OSIOUtils.decodeLoginResponse(LOGIN_RESPONSE);
		assertEquals("b", info.getRefreshToken());
		
	}

	@Test
	public void checkRefreshResponse() throws IOException {
		RefreshResponse response = OSIOUtils.decodeRefreshResponse(REFRESH_RESPONSE);
		assertNotNull(response.getLoginResponse());
		assertEquals("Beatae fuga enim suscipit sapiente vitae eligendi.", response.getLoginResponse().getAccessToken());
		assertEquals("Eum sed nobis provident aut quae occaecati.", response.getLoginResponse().getRefreshToken());
	}
	
	public void checkEmailIsExtracted() {
		String token = Jwts.builder().claim("email", "info@jboss.org").compact();
		String email = OSIOUtils.decodeEmailFromToken(token);
		assertEquals("info@jboss.org", email);
	}
}
