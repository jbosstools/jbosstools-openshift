/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.auth.IAuthorizationClient;

public class Connection extends ObservablePojo implements IConnection, IRefreshable {

	private final IClient client;
	private IAuthorizationClient authorizer;
	private String userName;
	private String password;
	private String token;
	
	//TODO modify default client to take url and throw runtime exception
	public Connection(String url, IAuthorizationClient authorizer) throws MalformedURLException{
		this(new DefaultClient(new URL(url), null), authorizer);
	}
	
	public Connection(IClient client, IAuthorizationClient authorizer){
		this.client = client;
		this.authorizer = authorizer;
	}
	
	@Override
	public String getUsername() {
		return this.userName;
	}

	public void setUsername(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean connect() throws OpenShiftException {
		if (authorize()) {
			// initializeCapabilities();
			return true;
		}
		return false;
	}

	private boolean authorize() {
		token = authorizer.requestToken(client.getBaseURL().toString(), userName, password);
		if(token != null){
			client.setAuthorizationStrategy(new BearerTokenAuthorizationStrategy(token));
			return true;
		}
		return false;
	}

	@Override
	public String getHost() {
		return client.getBaseURL().getHost();
	}

	@Override
	public boolean isDefaultHost() {
		// TODO: implement
		return false;
	}

	@Override
	public String getScheme() {
		return client.getBaseURL().getProtocol();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((client == null) ? 0 : client.getBaseURL().hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.getBaseURL().toString().equals(other.client.getBaseURL().toString()))
			return false;
		return true;
	}

	@Override
	public void refresh() {
		this.connect();
	}

	@Override
	public ConnectionType getType() {
		return ConnectionType.Kubernetes;
	}

	@Override
	public String toString() {
		return client.getBaseURL().toString();
	}

	/**
	 * Get a list of resource types
	 * 
	 * @return List<IResource>
	 */
	public <T extends IResource> List<T> get(ResourceKind kind) {
		return client.list(kind);
	}

	@Override
	public boolean canConnect() throws IOException {
		client.getOpenShiftAPIVersion();
		return true;
	}

}
