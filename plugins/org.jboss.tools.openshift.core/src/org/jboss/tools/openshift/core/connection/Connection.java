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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;
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

import com.openshift.restclient.ClientBuilder;
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
import com.openshift.restclient.capability.ICapability;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IResourceBuilder;

public class Connection extends ObservablePojo implements IRefreshable, IOpenShiftConnection {

	public static final String SECURE_STORAGE_BASEKEY = "org.jboss.tools.openshift.core";
	public static final String SECURE_STORAGE_PASSWORD_KEY = "password";
	public static final String SECURE_STORAGE_TOKEN_KEY = "token";

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
	private Map<String, Object> extendedProperties = new HashMap<>();

	//TODO modify default client to take url and throw lib specific exception
	public Connection(String url, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback)
			throws MalformedURLException {
		this(new ClientBuilder(url).sslCertificateCallback(sslCertCallback).build(), credentialsPrompter, sslCertCallback);
	}
	
	public Connection(IClient client, ICredentialsPrompter credentialsPrompter, ISSLCertificateCallback sslCertCallback) {
		this.client = client;
		this.client.setSSLCertificateCallback(sslCertCallback);
		this.credentialsPrompter = credentialsPrompter;
		this.sslCertificateCallback = sslCertCallback;
	}
	
	@Override
	public Map<String, Object> getExtendedProperties() {
		return new HashMap<>(extendedProperties);
	}

	@Override
	public void setExtendedProperty(String name, Object value) {
		Map<String, Object> oldExt = new HashMap<>(extendedProperties);
		extendedProperties.put(name, value);
		firePropertyChange(PROPERTY_EXTENDED_PROPERTIES, oldExt, this.extendedProperties);
	}

	@Override
	public void setExtendedProperties(Map<String, Object> ext) {
		firePropertyChange(PROPERTY_EXTENDED_PROPERTIES, this.extendedProperties, this.extendedProperties = ext);
	}

	/**
	 * Retrieve the resource factory associated with this connection
	 * for stubbing versioned resources supported by th server
	 * @return an {@link IResourceFactory}
	 */
	public IResourceFactory getResourceFactory() {
		return client.getResourceFactory();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <B extends IResourceBuilder> B getResourceBuilder(Class<? extends ICapability> klass){
		if(client.supports(klass)) {
			ICapability cap = (ICapability) client.getCapability(klass);
			if(cap instanceof IResourceBuilder) {
				return (B) cap;
			}
		}
		return null;
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

	@Override
	public void setPassword(String password) {
		firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
		this.passwordLoaded = true;
	}

	/**
	 * Attempts to load the password from the secure storage, only at first time
	 * it is called.
	 */
	private void loadPassword() {
		if (StringUtils.isEmpty(password)
				&& !passwordLoaded) {
			this.password = load(SECURE_STORAGE_PASSWORD_KEY);
			this.passwordLoaded = true;
			setRememberPassword(password != null);
		}
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

	@Override
	public boolean isEnablePromptCredentials() {
		return promptCredentialsEnabled;
	}

	public String getAuthScheme() {
		return org.apache.commons.lang.StringUtils.defaultIfBlank(this.authScheme, IAuthorizationContext.AUTHSCHEME_OAUTH);
	}

	protected String load(String id) {
		String value = null;
		SecureStore store = getSecureStore(getHost(), getUsername());
		if (store != null) {
			try {
				value = store.get(id);
			} catch (SecureStoreException e) {
				OpenShiftCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
		return value;
	}

	private boolean saveOrClear(String id, String value, boolean saveOrClear) {
		SecureStore store = getSecureStore(getHost(), getUsername());
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
				OpenShiftCoreActivator.logError(NLS.bind("Exception saving {0} for connection to {1}",id, getHost()), e);
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
	protected SecureStore getSecureStore(String host, String username) {
		return new SecureStore(new OpenShiftSecureStorageKey(SECURE_STORAGE_BASEKEY, host, username));
	}

	@Override
	public boolean connect() throws OpenShiftException {
		if(authorize()) {
			savePasswordOrToken();
			saveAuthSchemePreference();
			return true;
		}
		return false;
	}

	protected boolean authorize() {
		client.setAuthorizationStrategy(createAuthorizationStrategy());
		try {
			IAuthorizationContext context = client.getContext(client.getBaseURL().toString());
			if (context.isAuthorized()) {
				String username = context.getUser().getName();
				String token = context.getToken();
				updateAuthorized(username, token);
			} else {
				if (isEnablePromptCredentials()
						&& credentialsPrompter != null) {
					credentialsPrompter.promptAndAuthenticate(this, null);
				}
			}
		} catch (UnauthorizedException e) {
			if (isEnablePromptCredentials()
					&& credentialsPrompter != null) {
				credentialsPrompter.promptAndAuthenticate(this, e.getAuthorizationDetails());
			} else {
				throw e;
			}
		}
		return getToken() != null;
	}

	private void updateAuthorized(String username, String token) {
		setToken(token);
		if (IAuthorizationContext.AUTHSCHEME_OAUTH.equalsIgnoreCase(getAuthScheme())) {
			setUsername(username);
		}
		// force auth strategy to token if authorized
		TokenAuthorizationStrategy tokenStrategy = new TokenAuthorizationStrategy(token, username);
		client.setAuthorizationStrategy(tokenStrategy);
	}

	private void savePasswordOrToken() {
		// not using getters here because for save there should be no reason
		// to trigger a load from storage.
		if (IAuthorizationContext.AUTHSCHEME_BASIC.equals(getAuthScheme())) {
			boolean success = saveOrClear(SECURE_STORAGE_PASSWORD_KEY, this.password, isRememberPassword());
			if (success) {
				//Avoid second secure storage prompt.
				// Password is stored, token should be cleared.
				clearToken();
			}
		} else if (IAuthorizationContext.AUTHSCHEME_OAUTH.equals(getAuthScheme())){
			boolean success = saveOrClear(SECURE_STORAGE_TOKEN_KEY, this.token, isRememberToken());
			if(success) { 
				//Avoid second secure storage prompt.
				//Token is stored, password should be cleared.
				clearPassword();
			}
		}
	}

	private void clearPassword() {
		setRememberPassword(false);
		setPassword(null);
		saveOrClear(SECURE_STORAGE_PASSWORD_KEY, null, false);
	}

	private void clearToken() {
		// dont clear the token instance var: JBIDE-22594
		setRememberToken(false);
		saveOrClear(SECURE_STORAGE_TOKEN_KEY, null, false);
	}

	protected void saveAuthSchemePreference() {
		ConnectionURL url = ConnectionURL.safeForConnection(this);
		if(!StringUtils.isEmpty(url)) {
			OpenShiftCorePreferences.INSTANCE.saveAuthScheme(url.toString(), getAuthScheme());
		}
	}

	private IAuthorizationStrategy createAuthorizationStrategy() {
		if (org.apache.commons.lang.StringUtils.isNotEmpty(getToken())
				|| IAuthorizationContext.AUTHSCHEME_OAUTH.equalsIgnoreCase(getAuthScheme())) {
			return new TokenAuthorizationStrategy(getToken(), getUsername()); //always use the token if you have one?
		} else if (IAuthorizationContext.AUTHSCHEME_BASIC.equalsIgnoreCase(getAuthScheme())) {
			return new BasicAuthorizationStrategy(getUsername(), getPassword(), getToken());
		}

		throw new OpenShiftException("Authscheme '%s' is not supported.", getAuthScheme());
	}

	/**
	 * Computes authorization state of connection. May be a long running operation.
	 * @return
	 */
	public boolean isAuthorized(IProgressMonitor monitor) {
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			IAuthorizationContext context = client.getContext(client.getBaseURL().toString());
			boolean result = context.isAuthorized();
			if(result && needStrategy) {
				//Call connect() to set the correct strategy instance to the client
				//in the case when no strategy has been set yet, and as we do it,
				//we can discard the current result, which nevertheless 
				//being true, promises that connect will go smoothly.
				return connect();
			}
			return result;
		} catch (UnauthorizedException e) {
			return false;
		}
	}

	/**
	 * Return true if the client had no strategy instance and it was set to the value provided by 
	 * method createAuthorizationStrategy()
	 * 
	 * @return
	 */
	private boolean initClientAuthorizationStrategy() {
		boolean needStrategy = client.getAuthorizationStrategy() == null;
		if(needStrategy) {
			client.setAuthorizationStrategy(createAuthorizationStrategy());
		}
		return needStrategy;
	}
	
	public void setAuthScheme(String scheme) {
		firePropertyChange(PROPERTY_AUTHSCHEME, this.authScheme, this.authScheme = scheme);
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
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			return client.create(resource);
		} catch (UnauthorizedException e) {
			needStrategy = false;
			return retryCreate(e, resource);
		} finally {
			if(needStrategy) {
				connect();
			}
		}
	}

	/**
	 * 
	 * @param resource
	 * @return
	 * @throws UnauthorizedException 
	 */
	public <T extends IResource> T updateResource(T resource) {
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			return client.update(resource);
		} catch (UnauthorizedException e) {
			needStrategy = false;
			return retryUpdate(e, resource);
		} finally {
			if(needStrategy) {
				connect();
			}
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
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			client.setSSLCertificateCallback(OpenShiftCoreUIIntegration.getInstance().getSSLCertificateCallback());
			return client.list(kind, namespace);
		} catch (UnauthorizedException e) {
			needStrategy = false;
			return retryList(e, kind, namespace);
		} finally {
			if(needStrategy) {
				connect();
			}
		}
	}

	@Override
	public <T extends IResource> T getResource(String kind, String namespace, String name) {
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			return client.get(kind, name, namespace);
		} catch (UnauthorizedException e) {
			needStrategy = false;
			return retryGet(e, kind, name, namespace);
		} finally {
			if(needStrategy) {
				connect();
			}
		}
	}
	/**
	 * Get or refresh a resource
	 * 
	 * @return List<IResource>
	 * @throws OpenShiftException
	 */
	public <T extends IResource> T getResource(IResource resource) {
		boolean needStrategy = initClientAuthorizationStrategy();
		try {
			return client.get(resource.getKind(), resource.getName(), resource.getNamespace());
		} catch (UnauthorizedException e) {
			needStrategy = false;
			return retryGet(e, resource.getKind(), resource.getName(), resource.getNamespace());
		} finally {
			if(needStrategy) {
				connect();
			}
		}
	}
	
	private <T extends IResource> T retryGet(OpenShiftException e, String kind, String name, String namespace){
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.get(kind, name, namespace);
		}
		throw e;
	}

	private <T extends IResource>  T retryCreate(OpenShiftException e, T resource){
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.create(resource);
		}
		throw e;
	}

	private <T extends IResource>  T retryUpdate(OpenShiftException e, T resource){
		setToken(null);// token must be invalid, make sure not to try with
		// cache
		if (connect()) {
			return client.update(resource);
		}
		throw e;
	}

	private <T extends IResource> List<T> retryList(OpenShiftException e, String kind, String namespace){
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
			setToken(load(SECURE_STORAGE_TOKEN_KEY));
			tokenLoaded = true;
			this.rememberToken = isNotBlank(token); //potential conflict with load password?
		}
	}
	
	public void setToken(String token) {
		firePropertyChange(SECURE_STORAGE_TOKEN_KEY, token, this.token = token);
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
