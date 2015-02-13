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

import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.AbstractConnection;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ICredentialsConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.client.ClientSystemProperties;
import org.jboss.tools.openshift.express.core.ICredentialsPrompter;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
import org.jboss.tools.openshift.express.internal.core.security.OpenShiftPasswordStorageKey;
import org.jboss.tools.openshift.express.internal.core.security.SecurePasswordStore;
import org.jboss.tools.openshift.express.internal.core.security.SecurePasswordStoreException;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IHttpClient.ISSLCertificateCallback;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.IQuickstart;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftConnectionFactory;
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
public class ExpressConnection extends AbstractConnection implements ICredentialsConnection {

	private String username;
	private String password;
	private IUser user;
	private boolean isDomainLoaded;
	private boolean rememberPassword;
	private boolean didPromptForPassword;
	private boolean passwordLoaded;
	private ICredentialsPrompter passwordPrompter;
	private ISSLCertificateCallback sslCallback;
	
	public ExpressConnection() {
		this(null, null, null, null, false, null, null);
	}

	protected ExpressConnection(String host) {
		this(null, null, UrlUtils.getScheme(host), UrlUtils.cutScheme(host), false, null, null);
	}

	public ExpressConnection(String username, String scheme, String host, ICredentialsPrompter prompter, ISSLCertificateCallback sslCallback) {
		this(username, null, scheme, host, false, null, sslCallback);
		this.passwordPrompter = prompter;
	}

	public ExpressConnection(String username, String host) {
		this(username, null, host, false, null);
	}

	public ExpressConnection(String username, String password, boolean rememberPassword, ISSLCertificateCallback sslCallback) {
		this(username, password, null, rememberPassword, sslCallback);
	}

	public ExpressConnection(String username, String password, String host, boolean rememberPassword, ISSLCertificateCallback sslCallback) {
		this(username, password, host, rememberPassword, null, sslCallback);
	}
	
	protected ExpressConnection(String username, String password, String host, boolean rememberPassword, IUser user, ISSLCertificateCallback sslCallback) {
		this(username, password, UrlUtils.getScheme(host), UrlUtils.cutScheme(host), rememberPassword, user, sslCallback);
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

	private IUser getUser() {
		return user;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
		clearUser();
	}

	public String getPassword() {
		loadPassword();
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public boolean isDefaultHost() {
		return isDefaultHost(super.getHost());
	}

	private boolean isDefaultHost(String host) {
		return host == null
				|| UrlUtils.cutScheme(host).isEmpty();
	}
	
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	public final void setRememberPassword(boolean rememberPassword) {
		this.rememberPassword = rememberPassword;
	}

	public boolean canPromptForPassword() {
		return this.didPromptForPassword == false;
	}

	/**
	 * Connects to OpenShift. Will do nothing if this user is already
	 * connected.
	 * 
	 * @return <code>true</code> if connect succeeed, <code>false</code>
	 *         otherwise
	 * @throws OpenShiftException
	 */
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
			IUser user = getUserForConnection();
			setUser(user);
			return true;
		}
	}
	
	public IUser getUserForConnection(){
		final String userId = ExpressCoreActivator.PLUGIN_ID + " " + ExpressCoreActivator.getDefault().getBundle().getVersion();

		return new OpenShiftConnectionFactory()
		.getConnection(userId, username, password, getHost(), sslCallback).getUser();
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

	public void update(ExpressConnection connection) {
		if (connection == null) {
			return;
		}
		setUsername(connection.getUsername());
		setPassword(connection.getPassword());
		setRememberPassword(connection.isRememberPassword());
		if (connection.isDefaultHost()) {
			setHost(null);
		} else {
			setHost(connection.getHost());
		}
		setUser(connection.getUser());
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
			passwordPrompter.promptAndAuthenticate(this);
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
		}
		return null;
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
		}
		return null;
	}

	public List<IStandaloneCartridge> getStandaloneCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getStandaloneCartridges();
		}
		return null;
	}
	
	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getEmbeddableCartridges();
		}
		return null;
	}

	public List<ICartridge> getCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getCartridges();
		}
		return null;
	}
	
	public List<IQuickstart> getQuickstarts() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getQuickstarts();
		}
		return null;
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
		}
		return null;
	}

	public IDomain getDomain(String id) throws OpenShiftException {
		if (StringUtils.isEmpty(id)) {
			return null;
		}
		// trigger authentication, domain loading
		getDomains(); 
		return user.getDomain(id);
	}

	public IDomain getFirstDomain() throws OpenShiftException {
		if (!connect()) {
			return null;
		}
		List<IDomain> domains = getDomains();
		if (domains == null
				|| domains.isEmpty()) {
			return null;
		}
		return domains.get(0);
	}
	
	public List<IDomain> getDomains() throws OpenShiftException {
		if (!connect()) {
			return Collections.emptyList();
		}
		List<IDomain> domains = user.getDomains();
		isDomainLoaded = true;
		return domains;
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
		}
		return false;
	}

	public boolean hasSSHKeys() throws OpenShiftException {
		if (connect()) {
			return !user.getSSHKeys().isEmpty();
		}
		return false;
	}

	public void refresh() throws OpenShiftException {
		isDomainLoaded = false;
		if (connect()) {
			user.refresh();
		}
	}

	public boolean isConnected() {
		return hasUser();
	}

	public List<IOpenShiftSSHKey> getSSHKeys() throws OpenShiftException{
		if (connect()) {
			return user.getSSHKeys();
		}
		return Collections.emptyList();
	}

	public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException,
			OpenShiftException {
		if (connect()) {
			return user.getSSHKeyByPublicKey(publicKey);
		}
		return null;
	}

	public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
		if (connect()) {
			return user.putSSHKey(name, key);
		}
		return null;
	}

	public boolean hasSSHKeyName(String name) throws OpenShiftException {
		if (connect()) {
			return user.hasSSHKeyName(name);
		}
		return false;
	}

	public boolean hasSSHPublicKey(String publicKey) {
		if (connect()) {
			return user.hasSSHPublicKey(publicKey);
		}
		return false;
	}

	public void save() {
		String username = getUsername();
		if (!StringUtils.isEmpty(username)) {
			ExpressCorePreferences.INSTANCE.saveLastUsername(username);
			saveOrClearPassword(username, getHost(), getPassword());
		}
	}

	private void saveOrClearPassword(String username, String host, String password) {
		SecurePasswordStore store = getSecureStore(host, username);
		if (store != null
				&& !StringUtils.isEmpty(username)) {
			try {
				if (isRememberPassword()
						&& !StringUtils.isEmpty(password)) {
					store.setPassword(password);
				} else {
					store.remove();
				}
			} catch (SecurePasswordStoreException e) {
				//ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
	}

	private String getPassword(SecurePasswordStore store) {
		String password = null;
		if (store != null
				&& !StringUtils.isEmpty(getUsername())) {
			try {
				password = store.getPassword();
			} catch (SecurePasswordStoreException e) {
				ExpressCoreActivator.pluginLog().logError(e.getMessage(), e);
			}
		}
		return password;
	}

	/**
	 * Return a secure store or <code>null</code> if platform is not found
	 */
	private SecurePasswordStore getSecureStore(final String platform, final String username) {
		if (platform == null) {
			return null;
		}
		final OpenShiftPasswordStorageKey key = new OpenShiftPasswordStorageKey(platform, username);
		SecurePasswordStore store = new SecurePasswordStore(key);
		return store;
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
	public ConnectionType getType() {
		return ConnectionType.Express;
	}

	@Override
	public String toString() {
		return "ExpressConnection ["
				+ "username=" + username 
				+ ", password=" + password 
				+ ", host=" + getHost() 
				+ ", user="	+ user 
				+ ", isDomainLoaded=" + isDomainLoaded  
				+ ", rememberPassword=" + rememberPassword
				+ ", didPromptForPassword=" + didPromptForPassword 
				+ ", passwordLoaded=" + passwordLoaded + "]";
	}
}
