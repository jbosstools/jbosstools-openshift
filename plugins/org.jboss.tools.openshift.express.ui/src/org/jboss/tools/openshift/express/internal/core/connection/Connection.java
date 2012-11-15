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

import java.net.SocketTimeoutException;
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftPasswordStorageKey;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStore;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStoreException;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftConnectionFactory;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftUnknonwSSHKeyTypeException;

/**
 * @author Rob Stryker
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class Connection {

	private static final String USER_ID =
			OpenShiftUIActivator.PLUGIN_ID + " " + OpenShiftUIActivator.getDefault().getBundle().getVersion();

	private String username;
	private String password;
	private String host;
	private IUser user;
	private boolean isDomainLoaded;
	private boolean rememberPassword;
	private boolean didPromptForPassword;
	private boolean passwordLoaded;
	private ICredentialsPrompter prompter;

	public Connection() {
		this(null, null, null, false, null);
	}

	protected Connection(String username) {
		this.username = username;
	}

	public Connection(String username, String host, ICredentialsPrompter prompter) {
		this(username, null, host, false);
		this.prompter = prompter;
	}

	public Connection(String username, String password, boolean rememberPassword) {
		this(username, password, null, rememberPassword);
	}

	public Connection(String username, String password, String host, boolean rememberPassword) {
		this(username, password, host, rememberPassword, null);
	}
	
	protected Connection(String username, String password, String host, boolean rememberPassword, IUser user) {
		this.username = username;
		this.password = password;
		this.host = getHost(host);
		this.rememberPassword = rememberPassword;
		setUser(user);
	}

	private String getHost(String host) {
		if (StringUtils.isEmpty(host)) {
			return host;
		}
		return UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS);
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

	public String setUsername(String username) {
		this.username = username;
		clearUser();
		// TODO: replace default name by userinput
		return username;
	}

	public String getPassword() {
		loadPassword();
		return password;
	}

	public String setPassword(String password) {
		this.password = password;
		// setRememberPassword(!StringUtils.isEmpty(password));
		this.passwordLoaded = true;
		clearUser();
		return password;
	}

	/**
	 * Returns the host this connection is bound to.
	 * 
	 * @return
	 */
	public String getHost() {
		if (isDefaultHost()) {
			return ConnectionUtils.getDefaultHostUrl();
		}
		return host;
	}

	public String getScheme() {
		if (isDefaultHost()) {
			return UrlUtils.getScheme(ConnectionUtils.getDefaultHostUrl());
		}

		return UrlUtils.getScheme(host);
	}

	public String setHost(String host) {
		this.host = UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS);
		clearUser();
		return host;
	}

	public boolean isDefaultHost() {
		return isDefaultHost(host);
	}

	private boolean isDefaultHost(String host) {
		return host == null
				|| UrlUtils.cutScheme(host).isEmpty();
	}
	
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	public final boolean setRememberPassword(boolean rememberPassword) {
		return this.rememberPassword = rememberPassword;
	}

	public boolean canPromptForPassword() {
		return this.didPromptForPassword == false;
	}

	/**
	 * Connects to OpenShift. Will do nothing if this connection is already
	 * connected.
	 * 
	 * @return <code>true</code> if connect succeeed, <code>false</code>
	 *         otherwise
	 * @throws OpenShiftException
	 */
	public boolean connect() throws OpenShiftException {
		if (isConnected()) {
			return true;
		}
		if (createUser()) {
			save();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates an OpenShift user instance for this connection. Prompts for
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
			IUser user = new OpenShiftConnectionFactory().getConnection(USER_ID, username, password, getHost())
					.getUser();
			// force domain loading so that there is no 'lazy domain
			// loading' cost
			// after that.
			user.getDefaultDomain();
			setUser(user);
			return true;
		}
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

	public void update(Connection connection) {
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

	private boolean promptForCredentials() {
		if (prompter == null) {
			return false;
		}
		try {
			didPromptForPassword = true;
			prompter.promptAndAuthenticate(this);
		} catch (Exception e) {
			Logger.error("Failed to retrieve User's password", e);
		}
		return hasUser();
	}

	public IApplication createApplication(final String applicationName, final ICartridge applicationType,
			final ApplicationScale scale, final IGearProfile gearProfile)
			throws OpenShiftException {
		if (connect()) {
			return user.getDefaultDomain().createApplication(applicationName, applicationType, scale, gearProfile);
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

	public IApplication getApplicationByName(String name) throws OpenShiftException {
		if (connect()
				&& user.hasDomain()) {
			return user.getDefaultDomain().getApplicationByName(name);
		}
		return null;
	}

	public List<IApplication> getApplications() throws OpenShiftException {
		if (connect()
				&& user.hasDomain()) {
			return user.getDefaultDomain().getApplications();
		}
		return null;
	}

	public List<ICartridge> getStandaloneCartridgeNames() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getStandaloneCartridges();
		}
		return null;
	}

	public void load() {
		getDefaultDomain();
	}

	public IDomain getDefaultDomain() throws OpenShiftException {
		if (connect()) {
			IDomain domain = user.getDefaultDomain();
			isDomainLoaded = true;
			return domain;
		}
		return null;
	}

	public boolean isLoaded() throws OpenShiftException {
		return isDomainLoaded;
	}

	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException {
		if (connect()) {
			return user.getConnection().getEmbeddableCartridges();
		}
		return null;
	}

	public boolean hasApplication(String name) throws OpenShiftException {
		if (connect()) {
			return user.getDefaultDomain().hasApplicationByName(name);
		}
		return false;
	}

	public boolean hasApplicationOfType(ICartridge type) throws OpenShiftException {
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

	public void refresh() throws OpenShiftException {
		isDomainLoaded = false;
		if (connect()) {
			user.refresh();
		}
	}

	public boolean isConnected() {
		return hasUser();
	}

	public List<IOpenShiftSSHKey> getSSHKeys() {
		if (connect()) {
			return user.getSSHKeys();
		}
		return null;
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

	public boolean hasSSHKeyName(String name) {
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
			OpenShiftPreferences.INSTANCE.saveLastUsername(username);
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
				Logger.error(e.getMessage(), e);
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
				Logger.error(e.getMessage(), e);
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
}
