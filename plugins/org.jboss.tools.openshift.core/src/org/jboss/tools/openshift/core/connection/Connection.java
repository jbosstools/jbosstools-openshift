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
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.auth.IAuthorizationClient;
import org.jboss.tools.openshift.core.auth.IAuthorizationContext;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.client.IHttpClient.ISSLCertificateCallback;
import com.openshift.client.IRefreshable;
import com.openshift.client.OpenShiftException;
import com.openshift.internal.client.httpclient.NotFoundException;
import com.openshift.internal.client.httpclient.UnauthorizedException;
import com.openshift3.client.IClient;
import com.openshift3.client.ResourceKind;
import com.openshift3.client.authorization.BearerTokenAuthorizationStrategy;
import com.openshift3.client.model.IResource;
import com.openshift3.internal.client.DefaultClient;

public class Connection extends ObservablePojo implements IConnection, IRefreshable {

	private IClient client;
	private IAuthorizationClient authorizer;
	private String userName;
	private String password;
	private boolean rememberPassword;
	private String token;
	private ICredentialsPrompter credentialsPrompter;
	private ISSLCertificateCallback sslCertificateCallback;

	//TODO modify default client to take url and throw runtime exception
	public Connection(String url, IAuthorizationClient authorizer, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback) throws MalformedURLException{
		this(new DefaultClient(new URL(url), sslCertCallback), authorizer, credentialsPrompter, sslCertCallback);
	}
	
	public Connection(IClient client, IAuthorizationClient authorizer,  ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback){
		this.client = client;
		this.authorizer = authorizer;
		this.credentialsPrompter = credentialsPrompter;
		// TODO: how can authorizer not be bull at this point?
		if(this.authorizer != null){
			authorizer.setSSLCertificateCallback(sslCertCallback);
		}
		this.sslCertificateCallback = sslCertCallback;
	}
	
	@Override
	public String getUsername(){
		return this.userName;
	}

	@Override
	public void setUsername(String userName) {
		firePropertyChange(PROPERTY_USERNAME, this.userName, this.userName = userName);
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public void setPassword(String password) {
		firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
	}

	@Override
	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD, this.rememberPassword, this.rememberPassword = rememberPassword);
	}
	
	@Override
	public boolean isRememberPassword() {
		return rememberPassword;
	}
	
	@Override
	public boolean connect() throws OpenShiftException {
		if(getToken()  != null) return true;
		if(authorize()){
//			initializeCapabilities();
			return true;
		}
		return false;
	}
	
	private boolean authorize() {
		IAuthorizationContext context = authorizer.getContext(client.getBaseURL().toString(), userName, password);
		if(!context.isAuthorized() && credentialsPrompter != null){
			credentialsPrompter.promptAndAuthenticate(this);
		}else{
			setToken(context.getToken());
		}
		return getToken() != null;
	}

	@Override
	public String getHost() {
		return client.getBaseURL().toString();
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
	public IConnection clone() {
		Connection connection = new Connection(client, authorizer, credentialsPrompter, sslCertificateCallback);
		connection.setUsername(userName);
		connection.setPassword(password);
		connection.setRememberPassword(rememberPassword);
		connection.setToken(token);
		return connection;
	}
	
	@Override
	public void update(IConnection connection) {
		if (!(connection instanceof Connection)) {
			throw new UnsupportedOperationException();
		}
		Connection otherConnection = (Connection) connection;
		this.client = otherConnection.client; 
		this.authorizer = otherConnection.authorizer;
		this.credentialsPrompter = otherConnection.credentialsPrompter;
		this.sslCertificateCallback = otherConnection.sslCertificateCallback;
		this.userName = otherConnection.userName;
		this.password = otherConnection.password;
		this.rememberPassword = otherConnection.rememberPassword;
		this.token = otherConnection.token;
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
		connect();
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
		try {
			return client.list(kind);
		} catch (UnauthorizedException e) {
			OpenShiftCoreActivator.pluginLog().logInfo("Unauthorized.  Trying again to reauthenticate");
			setToken(null);// token must be invalid, make sure not to try with
							// cache
			if (connect()) {
				return client.list(kind);
			}
			throw e;
		}
	}

	@Override
	public boolean canConnect() throws IOException {
		try {
			client.getOpenShiftAPIVersion();
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
		if (token != null) {
			client.setAuthorizationStrategy(new BearerTokenAuthorizationStrategy(token));
		} else {
			// TODO: NoAuthStrategy?
			client.setAuthorizationStrategy(null);
		}
	}
}
