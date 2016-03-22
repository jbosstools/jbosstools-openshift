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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.ClientFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.ISSLCertificateCallback;
import com.openshift.restclient.NotFoundException;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.authorization.BasicAuthorizationStrategy;
import com.openshift.restclient.authorization.IAuthorizationContext;
import com.openshift.restclient.authorization.IAuthorizationStrategy;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import com.openshift.restclient.authorization.UnauthorizedException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IResource;

public class Connection extends ObservablePojo implements IConnection, IRefreshable, IOpenShiftConnection {

	private static final String SECURE_STORAGE_BASEKEY = "org.jboss.tools.openshift.core";
	private static final String SECURE_STORAGE_PASSWORD = "password";
	private static final String SECURE_STORAGE_TOKEN = "token";
	private static final String SECURE_STORAGE_AUTHSCHEME = "authtype";
	public static final String PROPERTY_REMEMBER_TOKEN = "rememberToken";
	
	private IClient client;
	private String username;
	private String password;
	private String token;
	private boolean passwordLoaded;
	private boolean tokenLoaded;
	private boolean rememberPassword;
	private boolean rememberToken;
	private boolean promptCredentialsEnabled = true;
	private ICredentialsPrompter credentialsPrompter;
	private ISSLCertificateCallback sslCertificateCallback;
	private String authScheme;

	//TODO modify default client to take url and throw lib specific exception
	public Connection(String url, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback)
			throws MalformedURLException {
		this(new ClientFactory().create(url, sslCertCallback), credentialsPrompter, sslCertCallback);
	}
	
	public Connection(IClient client, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback) {
		this.client = client;
		this.client.setSSLCertificateCallback(sslCertCallback);
		this.credentialsPrompter = credentialsPrompter;
		this.sslCertificateCallback = sslCertCallback;
	}
	
	/**
	 * Retrieve the resoruce factory associated with this connection
	 * for stubbing versioned resources supported by th server
	 * @return an {@link IResourceFactory}
	 */
	public IResourceFactory getResourceFactory() {
		return client.getResourceFactory();
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

	public boolean isRememberToken() {
		return rememberToken;
	}
	
	public void setRememberToken(boolean rememberToken) {
		firePropertyChange(PROPERTY_REMEMBER_TOKEN, this.rememberToken, this.rememberToken = rememberToken);
	}
	
	@Override
	public void enablePromptCredentials(boolean enable) {
		this.promptCredentialsEnabled = enable;
	}
	
	public void save() {
		//not using getters here because for save there should be no reason
		//to trigger a load from storage.
		if(!IAuthorizationContext.AUTHSCHEME_OAUTH.equals(getAuthScheme())) {
			boolean success = saveOrClear(SECURE_STORAGE_PASSWORD, this.password, isRememberPassword(), getSecureStore(getHost(), getUsername()));
			if(success) { //Avoid second secure storage prompt.
				//Password is stored, token should be cleared.
				setRememberToken(false);
				token = null;
				saveOrClear(SECURE_STORAGE_TOKEN, null, false, getSecureStore(getHost(), getUsername()));
			}
		} else {
			boolean success = saveOrClear(SECURE_STORAGE_TOKEN, this.token, isRememberToken(), getSecureStore(getHost(), getUsername()));
			if(success) { //Avoid second secure storage prompt.
				//Token is stored, password should be cleared.
				setRememberPassword(false);
				password = null;
				saveOrClear(SECURE_STORAGE_PASSWORD, null, false, getSecureStore(getHost(), getUsername()));
			}
		}
		ConnectionURL url = ConnectionURL.safeForConnection(this);
		if(url != null) {
			OpenShiftCorePreferences.INSTANCE.saveAuthScheme(url.toString(), getAuthScheme());
		}
	}

	public String getAuthScheme() {
		return org.apache.commons.lang.StringUtils.defaultIfBlank(this.authScheme, IAuthorizationContext.AUTHSCHEME_OAUTH);
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

	private boolean saveOrClear(String id, String value, boolean saveOrClear, SecureStore store) {
		if (store != null) {
			try {
				if (saveOrClear
						&& !StringUtils.isEmpty(value)) {
					store.put(id, value);
				} else {
					store.remove(id);
				}
			} catch (SecureStoreException e) {
				firePropertyChange(SecureStoreException.ID, null, e);
				OpenShiftCoreActivator.logError("Exception saving connection property", e);
				if(e.getCause() instanceof StorageException) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns a secure store for the current host and username
	 */
	private SecureStore getSecureStore(final String host, final String username) {
		return new SecureStore(new OpenShiftSecureStorageKey(SECURE_STORAGE_BASEKEY, host, username));
	}

	@Override
	public boolean connect() throws OpenShiftException {
		if(authorize()) {
			save();
			return true;
		}
		return false;
	}

	private boolean authorize() {
		client.setAuthorizationStrategy(getAuthorizationStrategy());
		try {
			IAuthorizationContext context = client.getContext(client.getBaseURL().toString());
			if (!context.isAuthorized() 
					&& credentialsPrompter != null
					&& promptCredentialsEnabled){
				credentialsPrompter.promptAndAuthenticate(this, null);
			} else {
				setToken(context.getToken());
				// TODO: move this logic to openshift-restclient-java
				TokenAuthorizationStrategy tokenStrategy = new TokenAuthorizationStrategy(getToken(), context.getUser().getName());
				client.setAuthorizationStrategy(tokenStrategy);
				updateCredentials(context);
			}
		} catch (UnauthorizedException e) {
			if (!promptCredentialsEnabled
					|| credentialsPrompter == null) {
				throw e;
			} else {
				credentialsPrompter.promptAndAuthenticate(this, e.getAuthorizationDetails());
			}
		}
		return getToken() != null;
	}
	
	
	private void updateCredentials(IAuthorizationContext context) {
		if (IAuthorizationContext.AUTHSCHEME_OAUTH.equalsIgnoreCase(this.getAuthScheme())) {
			setUsername(context.getUser().getName());
		}
	}
	
	private IAuthorizationStrategy getAuthorizationStrategy() {
		final String scheme = getAuthScheme();
		final String token = getToken();
		if (org.apache.commons.lang.StringUtils.isNotEmpty(token)
				|| IAuthorizationContext.AUTHSCHEME_OAUTH.equalsIgnoreCase(scheme)) {
			return new TokenAuthorizationStrategy(token); //always use the token if you have one?
		}
		if (IAuthorizationContext.AUTHSCHEME_BASIC.equalsIgnoreCase(scheme)) {
			return new BasicAuthorizationStrategy(getUsername(), getPassword(), getToken());
		}
		throw new OpenShiftException("Authscheme '%s' is not supported.", scheme);
	}

	public void setAuthScheme(String scheme) {
		firePropertyChange(SECURE_STORAGE_AUTHSCHEME, this.authScheme, this.authScheme = scheme);
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
		Connection connection = new Connection(client, credentialsPrompter, sslCertificateCallback);
		connection.setUsername(username);
		connection.setPassword(password);
		connection.setRememberPassword(rememberPassword);
		connection.setRememberToken(rememberToken);
		connection.setToken(token);
		connection.setAuthScheme(authScheme);
		connection.promptCredentialsEnabled = promptCredentialsEnabled;
		return connection;
	}
	
	@Override
	public void update(IConnection connection) {
		Assert.isLegal(connection instanceof Connection);
		
		Connection otherConnection = (Connection) connection;
		this.client = otherConnection.client; 
		this.credentialsPrompter = otherConnection.credentialsPrompter;
		this.sslCertificateCallback = otherConnection.sslCertificateCallback;
		this.username = otherConnection.username;
		this.password = otherConnection.password;
		this.rememberPassword = otherConnection.rememberPassword;
		this.token = otherConnection.token;
		this.rememberToken = otherConnection.rememberToken;
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
	 * 
	 * @param resource
	 * @return
	 * @throws UnauthorizedException 
	 */
	public <T extends IResource> T createResource(T resource) {
		try {
			if(client.getAuthorizationStrategy() == null) {
				client.setAuthorizationStrategy(getAuthorizationStrategy());
			}
			return client.create(resource);
		} catch (UnauthorizedException e) {
			return retryCreate("Unauthorized.  Trying to reauthenticate", e, resource);
		}
	}

	/**
	 * Get a list of resource types in the default namespace
	 * 
	 * @return List<IResource>
	 * @throws OpenShiftException
	 */
	@Override
	public <T extends IResource> List<T> getResources(String kind) {
		return getResources(kind,"");
	}
	
	@Override
	public <T extends IResource> List<T> getResources(String kind, String namespace) {
		try {
			if(client.getAuthorizationStrategy() == null) {
				client.setAuthorizationStrategy(getAuthorizationStrategy());
			}

			client.setSSLCertificateCallback(OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback());
			return client.list(kind, namespace);
		} catch (UnauthorizedException e) {
			return retryList("Unauthorized.  Trying to reauthenticate", e, kind, namespace);
		}
	}

	@Override
	public <T extends IResource> T getResource(String kind, String namespace, String name) {
		try {
			if(client.getAuthorizationStrategy() == null) {
				client.setAuthorizationStrategy(getAuthorizationStrategy());
			}
			return client.get(kind, name, namespace);
		} catch (UnauthorizedException e) {
			return retryGet("Unauthorized.  Trying to reauthenticate", e, kind, name, namespace);
		}
	}
	/**
	 * Get or refresh a resource
	 * 
	 * @return List<IResource>
	 * @throws OpenShiftException
	 */
	public <T extends IResource> T getResource(IResource resource) {
		try {
			if(client.getAuthorizationStrategy() == null) {
				client.setAuthorizationStrategy(getAuthorizationStrategy());
			}
			return client.get(resource.getKind(), resource.getName(), resource.getNamespace());
		} catch (UnauthorizedException e) {
			return retryGet("Unauthorized.  Trying to reauthenticate", e, resource.getKind(), resource.getName(), resource.getNamespace());
		}
	}
	
	private <T extends IResource> T retryGet(String message, OpenShiftException e, String kind, String name, String namespace){
		OpenShiftCoreActivator.pluginLog().logInfo(message);
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.get(kind, name, namespace);
		}
		throw e;
	}

	private <T extends IResource>  T retryCreate(String message, OpenShiftException e, T resource){
		OpenShiftCoreActivator.pluginLog().logInfo(message);
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.create(resource);
		}
		throw e;
	}

	private <T extends IResource> List<T> retryList(String message, OpenShiftException e, String kind, String namespace){
		OpenShiftCoreActivator.pluginLog().logInfo(message);
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.list(kind, namespace);
		}
		throw e;
	}
	
	/**
	 * Delete the resource from the namespace it is associated with.  The delete operation 
	 * return silently regardless if successful or not
	 * 
	 * @param resource
	 * @throws OpenShiftException
	 */
	public void deleteResource(IResource resource) {
		client.delete(resource);
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
	private synchronized void loadToken() {
		if (StringUtils.isEmpty(token) && !tokenLoaded) {
			tokenLoaded = true;
			setToken(load(SECURE_STORAGE_TOKEN, getSecureStore(getHost(), getUsername())));
			this.rememberToken = isNotBlank(token); //potential conflict with load password?
		}
	}
	
	public void setToken(String token) {
		firePropertyChange(SECURE_STORAGE_TOKEN, token, this.token = token);
		this.tokenLoaded = true;
	}

	@Override
	public void notifyUsage() {
		UsageStats.getInstance().newV3Connection(getHost());
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
		} else if (other.client == null
				|| !client.getBaseURL().toString().equals(other.client.getBaseURL().toString()))
			return false;
		return true;
	}

	@Override
	public boolean credentialsEqual(IConnection connection) {
		if(!equals(connection)) {
			return false;
		}
		//It is safe to cast now.
		Connection other = (Connection)connection;
		//User name is already compared
		if(!Objects.equals(password, other.password)) {
			return false;
		}
		if(!Objects.equals(token, other.token)) {
			return false;
		}
		return true;
	}

	public boolean ownsResource(IResource resource) {
		if (resource == null) {
			return false;
		}
		IClient client =  resource.accept(new CapabilityVisitor<IClientCapability, IClient>() {

			@Override
			public IClient visit(IClientCapability capability) {
				return capability.getClient();
			}
		}, null);
		return ObjectUtils.equals(this.client, client);
	}
}
