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
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.internal.restclient.http.NotFoundException;
import com.openshift.internal.restclient.http.UnauthorizedException;
import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ISSLCertificateCallback;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.authorization.BearerTokenAuthorizationStrategy;
import com.openshift.restclient.authorization.IAuthorizationClient;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.model.IResource;

public class Connection extends ObservablePojo implements IConnection, IRefreshable {

	private static final String SECURE_STORAGE_BASEKEY = "org.jboss.tools.openshift.core";
	private static final String SECURE_STORAGE_PASSWORD = "password";
	private static final String SECURE_STORAGE_TOKEN = "token";
	
	private IClient client;
	private IAuthorizationClient authorizer;
	private String username;
	private String password;
	private boolean passwordLoaded;
	private boolean rememberPassword;
	private String token;
	private boolean tokenLoaded;
	private ICredentialsPrompter credentialsPrompter;
	private ISSLCertificateCallback sslCertificateCallback;

	//TODO modify default client to take url and throw lib specific exception
	public Connection(String url, IAuthorizationClient authorizer, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback) throws MalformedURLException{
		this(new ClientFactory().create(url, sslCertCallback), authorizer, credentialsPrompter, sslCertCallback);
	}
	
	public Connection(IClient client, IAuthorizationClient authorizer,  ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback){
		this.client = client;
		this.authorizer = authorizer;
		this.credentialsPrompter = credentialsPrompter;
		// TODO: how can authorizer not be null at this point?
		if(this.authorizer != null){
			authorizer.setSSLCertificateCallback(sslCertCallback);
		}
		this.sslCertificateCallback = sslCertCallback;
	}
	
	@Override
	public String getUsername(){
		return username;
	}

	@Override
	public void setUsername(String userName) {
		firePropertyChange(PROPERTY_USERNAME, this.username, this.username = userName);
	}

	@Override
	public String getPassword() {
		loadPassword();
		return password;
	}

	/**
	 * Attempts to load the password from the secure storage, only at first time
	 * it is called.
	 */
	private void loadPassword() {
		if (StringUtils.isEmpty(password)
				&& !passwordLoaded) {
			this.password = load(SECURE_STORAGE_PASSWORD, getSecureStore(getHost(), getUsername()));
			this.passwordLoaded = true;
			this.rememberPassword = (password != null);
		}
	}

	@Override
	public void setPassword(String password) {
		firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
		this.passwordLoaded = true;
	}

	@Override
	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD, this.rememberPassword, this.rememberPassword = rememberPassword);
	}
	
	@Override
	public boolean isRememberPassword() {
		return rememberPassword;
	}
	
	public void save() {
		saveOrClear(SECURE_STORAGE_PASSWORD, getPassword(), isRememberPassword(), getSecureStore(getHost(), getUsername()));
		saveOrClear(SECURE_STORAGE_TOKEN, getToken(), isRememberPassword(), getSecureStore(getHost(), getUsername()));
	}

	private String load(String id, SecureStore store) {
		String value = null;
		if (store != null) {
			try {
				value = store.get(id);
			} catch (SecureStoreException e) {
				OpenShiftCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
		return value;
	}

	private void saveOrClear(String id, String value, boolean saveOrClear, SecureStore store) {
		if (store != null) {
			try {
				if (saveOrClear
						&& !StringUtils.isEmpty(value)) {
					store.put(id, value);
				} else {
					store.remove(id);
				}
			} catch (SecureStoreException e) {
				//ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Returns a secure store for the current host and username
	 */
	private SecureStore getSecureStore(final String host, final String username) {
		return new SecureStore(new OpenShiftSecureStorageKey(SECURE_STORAGE_BASEKEY, host, username));
	}


	@Override
	public boolean connect() throws OpenShiftException {
		if (getToken() != null) {
			save();
			return true;
		} else if (authorize()) {
			save();
			return true;
		} else {
			return false;
		}
	}
	
	private boolean authorize() {
		IAuthorizationContext context = authorizer.getContext(client.getBaseURL().toString(), username, password);
		if (!context.isAuthorized() && credentialsPrompter != null) {
			credentialsPrompter.promptAndAuthenticate(this);
		} else {
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
		return client.getBaseURL().getProtocol() + UrlUtils.SCHEME_SEPARATOR;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((client  == null) ? 0 : client.getBaseURL().hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public IConnection clone() {
		Connection connection = new Connection(client, authorizer, credentialsPrompter, sslCertificateCallback);
		connection.setUsername(username);
		connection.setPassword(password);
		connection.setRememberPassword(rememberPassword);
		connection.setToken(token);
		return connection;
	}
	
	@Override
	public void update(IConnection connection) {
		Assert.isLegal(connection instanceof Connection);
		
		Connection otherConnection = (Connection) connection;
		this.client = otherConnection.client; 
		this.authorizer = otherConnection.authorizer;
		this.credentialsPrompter = otherConnection.credentialsPrompter;
		this.sslCertificateCallback = otherConnection.sslCertificateCallback;
		this.username = otherConnection.username;
		this.password = otherConnection.password;
		this.rememberPassword = otherConnection.rememberPassword;
		this.token = otherConnection.token;
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
		loadToken();
		return token;
	}

	/**
	 * Loads the token from the secure storage if it's not been loaded nor set yet. 
	 */
	private void loadToken() {
		if (StringUtils.isEmpty(token)
				&& !tokenLoaded) {
			this.token = load(SECURE_STORAGE_TOKEN, getSecureStore(getHost(), getUsername()));
			this.tokenLoaded = true;
		}
	}
	
	public void setToken(String token) {
		this.token = token;
		this.tokenLoaded = true;
		if (!StringUtils.isEmpty(token)) {
			client.setAuthorizationStrategy(new BearerTokenAuthorizationStrategy(token));
		} else {
			// TODO: NoAuthStrategy?
			client.setAuthorizationStrategy(null);
		}
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
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.getBaseURL().toString().equals(other.client.getBaseURL().toString()))
			return false;
		return true;
	}

}
