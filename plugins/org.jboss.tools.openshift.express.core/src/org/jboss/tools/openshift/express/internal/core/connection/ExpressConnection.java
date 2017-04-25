/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.AbstractConnection;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.client.ClientSystemProperties;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
import org.jboss.tools.openshift.express.internal.core.util.ExpressResourceLabelUtils;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;

import com.openshift.client.ApplicationScale;
import com.openshift.client.ConnectionBuilder;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IHttpClient.ISSLCertificateCallback;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.IQuickstart;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftUnknonwSSHKeyTypeException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.openshift.internal.client.utils.StreamUtils;

/**
 * @author Rob Stryker
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class ExpressConnection extends AbstractConnection implements Cloneable {

	private static final String SECURE_STORAGE_PASSWORD = "pass";

	/*
	 * Hard-code the openshift UI activator id, due to backwards compatability issues
	 */
	private static final String SECURE_STORAGE_BASEKEY = "org.jboss.tools.openshift.express.ui";
	
	private String username;
	private String password;
	private IUser user;
	private boolean isDomainLoaded;
	private boolean rememberPassword;
	private boolean promptPasswordEnabled = true;
	private boolean didPromptForPassword;
	private boolean passwordLoaded;
	private ICredentialsPrompter passwordPrompter;
	private ISSLCertificateCallback sslCallback;

	public ExpressConnection(String host, ISSLCertificateCallback callback) {
		this(null, null, UrlUtils.getScheme(host), UrlUtils.cutScheme(host), false, null, callback);
	}
	
	public ExpressConnection(String username, String host) {
		this(username, null, host, false, null);
	}

	public ExpressConnection(String username, String host, ICredentialsPrompter prompter, ISSLCertificateCallback sslCallback) {
		this(username, null, UrlUtils.getScheme(host), UrlUtils.cutScheme(host), false, null, sslCallback);
		this.passwordPrompter = prompter;
	}

	public ExpressConnection(String username, String password, String host, boolean rememberPassword, ISSLCertificateCallback sslCallback) {
		this(username, password, UrlUtils.getScheme(host), UrlUtils.cutScheme(host), rememberPassword, null, sslCallback);
	}

	protected ExpressConnection(String username, String password, String scheme, String host, boolean rememberPassword, IUser user, ISSLCertificateCallback sslCallback) {
		super(scheme, host);
		this.username = username;
		this.password = password;
		this.rememberPassword = rememberPassword;
		this.sslCallback = sslCallback;
		setUser(user);
	}

	protected void setUser(IUser user) {
		this.user = user;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		firePropertyChange(PROPERTY_USERNAME, this.username, this.username = username);
		clearUser();
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
		clearUser();
	}

	@Override
	public String getHost() {
		if (isDefaultHost()) {
			return ExpressConnectionUtils.getDefaultHostUrl();
		}
		return super.getHost();
	}
	
	@Override
	public String getScheme() {
		if (isDefaultHost()) {
			return UrlUtils.getScheme(ExpressConnectionUtils.getDefaultHostUrl());
		}

		return UrlUtils.getScheme(super.getHost());
	}

	@Override
	public boolean isDefaultHost() {
		return isDefaultHost(super.getHost());
	}

	private boolean isDefaultHost(String host) {
		return host == null
				|| UrlUtils.cutScheme(host).isEmpty();
	}
	
	@Override
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	@Override
	public final void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD, this.rememberPassword, this.rememberPassword = rememberPassword);
	}

	public boolean canPromptForPassword() {
		return this.didPromptForPassword == false
				&& promptPasswordEnabled;
	}

	@Override
	public void enablePromptCredentials(boolean enable) {
		this.promptPasswordEnabled = enable;
	}
	
	@Override
	public boolean isEnablePromptCredentials() {
		return promptPasswordEnabled;
	}
	
	public void setSSLCertificateCallback(ISSLCertificateCallback callback) {
		this.sslCallback = callback;
	}
	
	/**
	 * Connects to OpenShift. Will do nothing if this user is already
	 * connected.
	 * 
	 * @return <code>true</code> if connect succeeed, <code>false</code>
	 *         otherwise
	 * @throws OpenShiftException
	 */
	@Override
	public boolean connect() throws OpenShiftException {
		if (isConnected()) {
			save();
			return true;
		}
		if (createUser()) {
			/* JBIDE-15847: user may get rewritten in case of kerberos */
			updateUsername(user);
			save();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates an OpenShift user instance for this user. Prompts for
	 * credentials if needed.
	 * 
	 * @return <code>true</code> if user could get created, <code>false</code>
	 *         otherwise.
	 */
	protected boolean createUser() {
		loadPassword();
		if (password == null) {
			return promptForCredentials();
		} else {
			setClientTimeout();
			IUser user = doCreateUser();
			setUser(user);
			return true;
		}
	}
	
	protected IUser doCreateUser() {
		final String userId = ExpressCoreActivator.PLUGIN_ID + " " + ExpressCoreActivator.getDefault().getBundle().getVersion();

		try {
			return new ConnectionBuilder(getHost())
				.credentials(username, password)
				.clientId(userId)
				.sslCertificateCallback(sslCallback)
				.create()
				.getUser();
		} catch (IOException e) {
			throw new OpenShiftException("Could not connect for user {0} - {1}", username, getHost());
		}
	}

	private void setClientTimeout() {
		int timeout = ExpressCorePreferences.INSTANCE
				.getClientReadTimeout(ClientSystemProperties.getReadTimeoutSeconds());
		ClientSystemProperties.setReadTimeoutSeconds(timeout);
	}
	
	/**
	 * Attempts to load the password from the secure storage, only at first time
	 * it is called.
	 */
	private void loadPassword() {
		if (StringUtils.isEmpty(password)
				&& !passwordLoaded) {
			this.password = getPassword(getSecureStore(getHost(), getUsername()));
			this.passwordLoaded = true;
			this.rememberPassword = (password != null);
		}
	}

	private boolean hasUser() {
		return user != null;
	}

	protected void clearUser() {
		this.user = null;
	}

	private String updateUsername(IUser user) {
		if (!user.getRhlogin().equals(username)) {
			ExpressCoreActivator.getDefault().getLog().log(
					new Status(Status.WARNING, ExpressCoreActivator.PLUGIN_ID, 
							NLS.bind("User {0} was logged in as {1}", username, user.getRhlogin())));
		}
		this.username = user.getRhlogin();
		return username;
	}
	
	private boolean promptForCredentials() {
		if (passwordPrompter == null) {
			return false;
		}
		try {
			passwordPrompter.promptAndAuthenticate(this, null);
			didPromptForPassword = true;
		} catch (Exception e) {
			ExpressCoreActivator.pluginLog().logError("Failed to retrieve User's password", e);
		}
		return hasUser();
	}

	public IApplication createApplication(final String applicationName, final IStandaloneCartridge standaloneCartridge,
			final ApplicationScale scale, final IGearProfile gearProfile, final IDomain domain)
			throws OpenShiftException {
		if (connect()) {
			return domain.createApplication(applicationName, standaloneCartridge, scale, gearProfile);
		} else {
			return null;
		}
	}

	/**
	 * Creates a new domain with the given id
	 * 
	 * @param id
	 *            the domain id
	 * @return the created domain
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException
	 */
	public IDomain createDomain(String id) throws OpenShiftException {
		if (connect()) {
			return user.createDomain(id);
		} else {
			return null;
		}
	}

	public List<IStandaloneCartridge> getStandaloneCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getStandaloneCartridges();
		} else {
			return null;
		}
	}
	
	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getEmbeddableCartridges();
		} else {
			return null;
		}
	}

	public List<ICartridge> getCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getCartridges();
		} else {
			return null;
		}
	}
	
	public List<IQuickstart> getQuickstarts() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getQuickstarts();
		} else {
			return null;
		}
	}


	public void load() {
		getDomains();
	}

	public IApplication getApplication(String name, IDomain domain) throws OpenShiftException {
		if (domain == null) {
			return null;
		}
		return domain.getApplicationByName(name);
	}
	
	public boolean hasApplication(String name, IDomain domain) throws OpenShiftException {
		return getApplication(name, domain) != null;
	}

	public IDomain getDefaultDomain() throws OpenShiftException {
		if (connect()) {
			return user.getDefaultDomain();
		} else {
			return null;
		}
	}

	public IDomain getDomain(String id) throws OpenShiftException {
		if (StringUtils.isEmpty(id)) {
			return null;
		}
		// trigger authentication, domain loading
		getDomains();
		//if authentication failed, user is null
		if(!isConnected()) {
			return null;
		}
		return user.getDomain(id);
	}

	public IDomain getFirstDomain() throws OpenShiftException {
		if (!connect()) {
			return null;
		} else {
			List<IDomain> domains = getDomains();
			if (domains == null
					|| domains.isEmpty()) {
				return null;
			}
			return domains.get(0);
		}
	}
	
	public List<IDomain> getDomains() throws OpenShiftException {
		if (!connect()) {
			return Collections.emptyList();
		} else {
			List<IDomain> domains = user.getDomains();
			isDomainLoaded = true;
			return domains;
		}
	}

	public void destroy(IDomain domain, boolean force) {
		if (connect()) {
			domain.destroy(force);
		}
	}
	
	public boolean isLoaded() throws OpenShiftException {
		return isDomainLoaded;
	}

	public boolean hasApplicationOfType(IStandaloneCartridge type) throws OpenShiftException {
		if (hasDomain()) {
			return user.getDefaultDomain().hasApplicationByCartridge(type);
		}
		return false;
	}

	public boolean hasDomain() throws OpenShiftException {
		if (connect()) {
			return user.hasDomain();
		} else {
			return false;
		}
	}

	public boolean hasSSHKeys() throws OpenShiftException {
		if (connect()) {
			return !user.getSSHKeys().isEmpty();
		} else {
			return false;
		}
	}

	@Override
	public void refresh() throws OpenShiftException {
		isDomainLoaded = false;
		if (connect()) {
			user.refresh();
		}
	}

	@Override
	public boolean isConnected() {
		return hasUser();
	}

	public List<IOpenShiftSSHKey> getSSHKeys() throws OpenShiftException {
		if (connect()) {
			return user.getSSHKeys();
		} else {
			return Collections.emptyList();
		}
	}

	public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException,
			OpenShiftException {
		if (connect()) {
			return user.getSSHKeyByPublicKey(publicKey);
		} else {
			return null;
		}
	}

	public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
		if (connect()) {
			return user.putSSHKey(name, key);
		} else {
			return null;
		}
	}

	public boolean hasSSHKeyName(String name) throws OpenShiftException {
		if (connect()) {
			return user.hasSSHKeyName(name);
		} else {
			return false;
		}
	}

	public boolean hasSSHPublicKey(String publicKey) {
		if (connect()) {
			return user.hasSSHPublicKey(publicKey);
		} else {
			return false;
		}
	}

	public void save() {
		String username = getUsername();
		if (!StringUtils.isEmpty(username)) {
			ExpressCorePreferences.INSTANCE.saveLastUsername(username);
			saveOrClearPassword(username, getHost(), getPassword());
		}
	}

	private void saveOrClearPassword(String username, String host, String password) {
		SecureStore store = getSecureStore(host, username);
		if (store != null
				&& !StringUtils.isEmpty(username)) {
			try {
				if (isRememberPassword()
						&& !StringUtils.isEmpty(password)) {
					store.put(SECURE_STORAGE_PASSWORD, password);
				} else {
					store.remove(SECURE_STORAGE_PASSWORD);
				}
			} catch (SecureStoreException e) {
				firePropertyChange(SecureStoreException.ID, null, e);
				//ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
	}

	private String getPassword(SecureStore store) {
		String password = null;
		if (store != null
				&& !StringUtils.isEmpty(getUsername())) {
			try {
				password = store.get(SECURE_STORAGE_PASSWORD);
			} catch (SecureStoreException e) {
				ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
		return password;
	}

	/**
	 * Returns a secure store for the current host and username
	 */
	private SecureStore getSecureStore(final String host, final String username) {
		return new SecureStore(new OpenShiftSecureStorageKey(SECURE_STORAGE_BASEKEY, host, username));
	}
	
	public void removeSecureStoreData() {
		SecureStore store = getSecureStore(getHost(), getUsername());
		if (store != null) {
			try {
				store.removeNode();
			} catch (SecureStoreException e) {
				firePropertyChange(SecureStoreException.ID, null, e);
				ExpressCoreActivator.pluginLog().logWarning(e.getMessage(), e);
			}
		}
	}

	public String getId() {
		StringBuilder builder = new StringBuilder(username);
		builder
			.append(" at ")
			.append(getHost());
		if (isDefaultHost()) {
			builder.append(" (default)");
		}
		return builder.toString();
	}

	@Override
	public boolean canConnect() throws IOException {
		// TODO: move to client library
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(getHost() + "/broker/rest/api").openConnection();
			connection.setConnectTimeout(2 * 1000);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("Accept", "application/json");
			InputStream in = connection.getInputStream();
			StreamUtils.readToString(in);
			return connection.getResponseCode() == 200;
		} catch (MalformedURLException e) {
			return false;
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			if (connection != null
					// can throw IOException (ex. UnknownHostException)
					&& connection.getResponseCode() != -1) {
				return false;
			} else {
				throw e;
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	@Override
	public IConnection clone() {
		return new ExpressConnection(getUsername(), getPassword(), getScheme(), getHost(), isRememberPassword(), null, sslCallback);
	}

	@Override
	public void update(IConnection connection) {
		Assert.isLegal(connection instanceof ExpressConnection);
		
		ExpressConnection otherConnection = (ExpressConnection) connection; 
		setUsername(otherConnection.getUsername());
		setPassword(otherConnection.getPassword());
		setRememberPassword(otherConnection.isRememberPassword());
		setUser(otherConnection.user);
		this.sslCallback = otherConnection.sslCallback;
	}
	
	@Override
	public ConnectionType getType() {
		return ConnectionType.Express;
	}
	
	@Override
	public void notifyUsage() {
		UsageStats.getInstance().newV2Connection(getHost());
	}

	@Override
	public String toString() {
		return ExpressResourceLabelUtils.toString(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ExpressConnection))
			return false;
		ExpressConnection other = (ExpressConnection) obj;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}
