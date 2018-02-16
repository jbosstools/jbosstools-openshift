/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.NoopSSLCertificateCallback;
import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * A requirement that makes sure a given connection exists. If it doesn't it
 * will get created.
 * 
 * @author adietish@redhat.com
 */
public class OpenShiftConnectionRequirement implements Requirement<RequiredBasicConnection> {
	
	private static final int CONNECT_TIMEOUT = 4;
	private static Logger LOGGER = new Logger(OpenShiftConnectionRequirement.class);

	private RequiredBasicConnection connectionSpec;
	private Connection connection;

	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RequiredBasicConnection {
		/**
		 * the host to connect to. If nothing is provided {@link DatastoreOS3#SERVER} is used
		 */
		String server() default StringUtils.EMPTY;

		/**
		 * the username to use when authenticating. If nothing is provided {@link DatastoreOS3#USERNAME} is used
		 */
		String username() default StringUtils.EMPTY;

		/**
		 * the password to use when authenticating. If nothing is provided {@link DatastoreOS3#PASSWORD} is used
		 */
		String password() default StringUtils.EMPTY;
		
		/**
		 * the token to use when authenticating. If nothing is provided {@link DatastoreOS3#TOKEN} is used
		 */
		String token() default StringUtils.EMPTY;
		
		/**
		 * whether the connection created by the requirement should be automatically deleted after test class, default false
		 */
		boolean cleanup() default false;
	}

	@Override
	public void fulfill() {
		String server = TestUtils.getValueOrDefault(connectionSpec.server(), DatastoreOS3.SERVER);
		String username = TestUtils.getValueOrDefault(connectionSpec.username(), DatastoreOS3.USERNAME);
		String password = TestUtils.getValueOrDefault(connectionSpec.password(), DatastoreOS3.PASSWORD);
		String token = TestUtils.getValueOrDefault(connectionSpec.token(), DatastoreOS3.TOKEN);

		try {
			ConnectionURL url;
			if (StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(username)) {
				url = getConnectionURL(username, server);
			} else {
				url = getConnectionURL(server);
			}
			Connection connection = ConnectionsRegistrySingleton.getInstance().getByUrl(url, Connection.class);
			if (connection == null) {
				LOGGER.debug(NLS.bind("No connection for {0} found. Creating a new one.", url));
				connection = createConnection(server, username, password, token);
				ConnectionsRegistrySingleton.getInstance().add(connection);
			}
			LOGGER.debug(NLS.bind("Connecting to OpenShift 3 server at {0}", url));
			connection.connect();
			this.connection = connection;
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not create connection for {0} : {1}", server, e));
		}
	}
	
	private ConnectionURL getConnectionURL(String server) throws UnsupportedEncodingException, MalformedURLException {
		return ConnectionURL.forURL(server);	
	}

	private ConnectionURL getConnectionURL(String username, String server) throws UnsupportedEncodingException, MalformedURLException {
		return ConnectionURL.forUsernameAndHost(username, server);
	}

	private Connection createConnection(String server, String username, String password, String token) {
		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
			return createConnectionWithCredentials(server,username,password);
		} else {
			return createConnectionWithToken(server,token);
		}
	}
	
	private Connection createConnectionWithCredentials(String server, String username, String password) {
		IClient client = createClient(server);
		Connection connection = new Connection(client, null);
		connection.setAuthScheme(IAuthorizationContext.AUTHSCHEME_BASIC);
		connection.setUsername(username);
		connection.setPassword(password);
		return connection;
	}
	
	private Connection createConnectionWithToken(String server, String token) {
		IClient client = createClient(server);
		Connection connection = new Connection(client, null);
		connection.setAuthScheme(IAuthorizationContext.AUTHSCHEME_OAUTH);
		connection.setUsername(connection.getUsername());
		connection.setToken(token);
		connection.refresh();
		return connection;
	}

	private IClient createClient(String server) {
		IClient client = new ClientBuilder(server)
			.sslCertificateCallback(new NoopSSLCertificateCallback())
			.withConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
			.build();
		return client;
	}

	@Override
	public void setDeclaration(RequiredBasicConnection connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	@Override
	public void cleanUp() {
		if (connectionSpec.cleanup()) {
			ConnectionsRegistrySingleton.getInstance().remove(connection);
			new WaitWhile(new ConnectionExists(connection.getHost(), connection.getUsername()));
		}
	}
	
	public Connection getConnection() {
		return connection;
	}

	public String getUsername() {
		if (connection == null) {
			return null;
		}
		return connection.getUsername();
	}
	
	public String getHost() {
		if (connection == null) {
			return null;
		}
		return connection.getHost();
	}
	
	private class ConnectionExists extends AbstractWaitCondition {
		
		private OpenShiftExplorerView view;
		private String server;
		private String user;

		public ConnectionExists(String server, String user) {
			view = new OpenShiftExplorerView();
			this.server = server;
			this.user = user;
		}
		
		@Override
		public boolean test() {
			return view.connectionExists(server, user);
		}
		
	}

	@Override
	public RequiredBasicConnection getDeclaration() {
		return this.connectionSpec;
	}
}
