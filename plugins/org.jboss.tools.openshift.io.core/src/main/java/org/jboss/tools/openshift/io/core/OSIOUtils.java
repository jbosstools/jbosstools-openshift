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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class OSIOUtils {

	private static final ObjectMapper mapper = new ObjectMapper();

	private OSIOUtils() {
	}

	/**
	 * Decode the login JSON as an object.
	 * 
	 * @param jsonString the login JSON string
	 * @return the decoded object.
	 * @throws IOException if error occurs
	 */
	public static LoginResponse decodeLoginResponse(String jsonString) throws IOException {
		return mapper.readValue(jsonString, LoginResponse.class);
	}

	/**
	 * Decode the refresh JSON as an object.
	 * 
	 * @param response the response JSON string
	 * @return the decoded object.
	 * @throws IOException if error occurs
	 */
	public static RefreshResponse decodeRefreshResponse(String response) throws IOException {
		return mapper.readValue(response, RefreshResponse.class);
	}

	/**
	 * Extract the email from the OpenShift.io access token.
	 * 
	 * @param token the token
	 * @return the email address
	 */
	public static String decodeEmailFromToken(String token) {
		String payloads[] = token.split("\\.");
		Claims claims = (Claims) Jwts.parser().parse(payloads[0] + '.' + payloads[1] + '.').getBody();
		return (String) claims.get("email");
	}

	/**
	 * Extract the expiry time from the OpenShift.io token.
	 * 
	 * @param token the token
	 * @return the expiry time
	 */
	public static long decodeExpiryFromToken(String token) {
		String payloads[] = token.split("\\.");
		Claims claims = (Claims) Jwts.parser().parse(payloads[0] + '.' + payloads[1] + '.').getBody();
		return claims.get("exp", Date.class).getTime();
	}

	public static String computeLandingURL(String endpointURL, String devstudioOsioLandingPageSuffix) {
		try {
			URI uri = new URI(endpointURL);
			URI landingURI = new URI(uri.getScheme(), null, uri.getAuthority(), uri.getPort(),
					devstudioOsioLandingPageSuffix, null, null);
			return landingURI.toString();
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
