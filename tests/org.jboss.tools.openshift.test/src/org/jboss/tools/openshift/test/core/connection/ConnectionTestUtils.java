/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationStrategy;
import com.openshift.restclient.model.user.IUser;

/**
 * @author Andre Dietisheim
 */
public class ConnectionTestUtils {

	public static Connection createConnection(String username, String token, String host) throws MalformedURLException {
		IClient client = createClient(username, token, host);
		return new Connection(client, null, null);
	}
	
	public static IClient createClient(String username, String token, String host) throws MalformedURLException {
		IClient client = mock(IClient.class);
		when(client.getBaseURL()).thenReturn(new URL(host));
		doReturn(mock(IAuthorizationStrategy.class)).when(client).getAuthorizationStrategy();	
		doReturn(mockAuthorizationContext(username, token, true)).when(client).getContext(anyString());
		return client;
	}
	
	public static IAuthorizationContext mockAuthorizationContext(String username, String token, boolean isAuthorized) {
		IAuthorizationContext authorizationContext = mock(IAuthorizationContext.class);
		doReturn(isAuthorized).when(authorizationContext).isAuthorized();
		doReturn(token).when(authorizationContext).getToken();

		IUser user = mock(IUser.class);
		doReturn(username).when(user).getName();
		doReturn(user).when(authorizationContext).getUser();

		return authorizationContext;
	}


}
