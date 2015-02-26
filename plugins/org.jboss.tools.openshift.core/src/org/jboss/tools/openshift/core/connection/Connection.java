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

import com.openshift.client.IHttpClient.ISSLCertificateCallback;
import com.openshift.client.IRefreshable;
import com.openshift.client.OpenShiftException;
import com.openshift3.client.IClient;
import com.openshift3.client.ResourceKind;
import com.openshift3.client.authorization.BearerTokenAuthorizationStrategy;
import com.openshift3.client.model.IResource;
import com.openshift3.internal.client.DefaultClient;

public class Connection extends ObservablePojo implements IConnection, IRefreshable {

	private final IClient client;
	private IAuthorizationClient authorizer;
	private String userName;
	private String password;
	private String token;
	
	//TODO modify default client to take url and throw runtime exception
	public Connection(String url, IAuthorizationClient authorizer, ISSLCertificateCallback sslCertCallback) throws MalformedURLException{
		this(new DefaultClient(new URL(url), sslCertCallback), authorizer, sslCertCallback);
	}
	
	public Connection(IClient client, IAuthorizationClient authorizer, ISSLCertificateCallback sslCertCallback){
		this.client = client;
		this.authorizer = authorizer;
		if(this.authorizer != null){
			authorizer.setSSLCertificateCallback(sslCertCallback);
		}
	}
	
	@Override
	public String getUsername(){
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
		setToken(authorizer.requestToken(client.getBaseURL().toString(), userName, password));
		return getToken() != null;
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
		result = prime * result + ((client  == null) ? 0 : client.getBaseURL().hashCode());
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
		if(userName == null){
			if(other.userName != null)
				return false;
		}else if(!userName.equals(other.userName))
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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
		if(token != null){
			client.setAuthorizationStrategy(new BearerTokenAuthorizationStrategy(token));
		}else{
			//TODO: NoAuthStrategy?
			client.setAuthorizationStrategy(null);
		}

	}
}
