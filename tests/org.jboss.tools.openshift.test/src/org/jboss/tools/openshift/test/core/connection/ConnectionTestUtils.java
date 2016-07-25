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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationDetails;
import com.openshift.restclient.model.user.IUser;

/**
 * @author Andre Dietisheim
 */
public class ConnectionTestUtils {

	public static Connection createConnection(String username, String token, String host) throws MalformedURLException {
		IClient client = createClient(username, token, host);
		return new Connection(client, null);
	}
	
	public static IClient createClient(String username, String token, String host) throws MalformedURLException {
		IClient client = mock(IClient.class);
		when(client.getBaseURL()).thenReturn(new URL(host));
		doReturn(mockAuthorizationContext(username, token, true)).when(client).getAuthorizationContext();
		return client;
	}
	
	public static IAuthorizationContext mockAuthorizationContext(String username, String token, boolean isAuthorized) {
		TestableAuthorizationContext authorizationContext = spy(new TestableAuthorizationContext(username, token, isAuthorized));

		IUser user = mock(IUser.class);
		doReturn(username).when(user).getName();
		doReturn(user).when(authorizationContext).getUser();
		
		authorizationContext.setUser(user);

		return authorizationContext;
	}

	public static class TestableAuthorizationContext implements IAuthorizationContext{

		private String password;
		private String username;
		private String token;
		private String authscheme;
		private boolean authorized;
		private IUser user;

		public TestableAuthorizationContext(String username, String token, boolean isAuthorized) {
			this.authorized = isAuthorized;
			this.username = username;
			this.token = token;
		}

		public void setUser(IUser user) {
			this.user = user;
		}

		@Override
		public IUser getUser() {
			return user;
		}

		@Override
		public boolean isAuthorized() {
			return authorized;
		}

		@Override
		public String getAuthScheme() {
			return authscheme;
		}

		@Override
		public String getToken() {
			return token;
		}

		@Override
		public void setToken(String token) {
			this.token = token;
		}

		@Override
		public void setUserName(String userName) {
			this.username = userName;
		}

		@Override
		public String getUserName() {
			return username;
		}

		@Override
		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public String getExpiresIn() {
			return null;
		}

		@Override
		public IAuthorizationDetails getAuthorizationDetails() {
			return null;
		}
		
		
	}

}
