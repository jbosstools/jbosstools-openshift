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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils.UrlPortions;
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
import com.openshift.client.configuration.OpenShiftConfiguration;

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
	private boolean alreadyPromptedForPassword;
	private boolean passwordLoaded;
	private OpenShiftConfiguration openShiftConfiguration;
	private ICredentialsPrompter prompter;

	public Connection() {
		this(null, null, null, false);
	}

	public Connection(String username, ICredentialsPrompter prompter) {
		this.username = username;
		this.prompter = prompter;
	}
	
	public Connection(URL url, ICredentialsPrompter prompter) throws MalformedURLException, UnsupportedEncodingException {
		UrlPortions portions = UrlUtils.toPortions(url);
		this.username = portions.getUsername();
		this.password = portions.getPassword();
		setHost(portions.getHost());
		this.prompter = prompter;
	}

	public Connection(Connection connection) {
		this(connection.getUsername(), connection.getPassword(), connection.getHost(),
				connection.isRememberPassword());
		setUser(connection.getUser());
	}

	private Connection(String username, String password, String host, boolean rememberPassword) {
		this.username = getUsername(username);
		this.password = password;
		setHost(host);
		this.rememberPassword = rememberPassword;
	}

	private String getUsername(String username) {
		if (!StringUtils.isEmpty(username)) {
			return username;
		}
		return getDefaultUsername();
	}

	private String getDefaultUsername() {
		String username = OpenShiftPreferences.INSTANCE.getLastUsername();
		if (StringUtils.isEmpty(username)) {
			try {
				username = getOpenShiftConfiguration().getRhlogin();
			} catch (Exception e) {
				Logger.error("Could not load default user name from OpenShift configuration.", e);
			}
		}
		return username;
	}
		
	private void setUser(IUser user) {
		if (user == null) {
			return;
		}
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
		setRememberPassword(!StringUtils.isEmpty(password));
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
			return UrlUtils.cutScheme(ConnectionUtils.getDefaultHostUrl());
		}

		return host;
	}
	
	public String setHost(String host) {
		if (isDefaultHost(host)) {
			this.host = null;
		} else {
			this.host = host;
		}
		clearUser();
		return host;
	}

	
	public boolean isDefaultHost() {
		return isDefaultHost(host);
	}

	private boolean isDefaultHost(String host) {
		try {
			return StringUtils.isEmpty(host)
					|| new URL(UrlUtils.ensureStartsWithSchemeOrHttps(host)).getHost().isEmpty();
		} catch (MalformedURLException e) {
			return true;
		}
	}
	
	public boolean isRememberPassword() {
		return rememberPassword;
	}

	public final boolean setRememberPassword(boolean rememberPassword) {
		return this.rememberPassword = rememberPassword;
	}

	public boolean canPromptForPassword() {
		return this.alreadyPromptedForPassword == false;
	}

	/**
	 * Prompts user for password if it was not given or retrieved from secure
	 * storage before.
	 * 
	 * @return true if user entered credentials, false otherwise.
	 */
	private boolean authenticate() {
		if (!hasUser()) {
			loadPassword();
			if (password != null) {
				if (createUser()) {
					return true;
				}
			}
			return promptForCredentials();
		}
		return true;
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

	private void clearUser() {
		this.user = null;
	}

	private boolean createUser() throws OpenShiftException {
		this.user = new OpenShiftConnectionFactory().getConnection(USER_ID, username, password, getHost()).getUser();
		// force domain loading so that there is no 'lazy domain loading' cost
		// after that.
		user.getDefaultDomain();
		setUser(user);
		return user != null;
	}

	public void update(Connection connection) {
		setUsername(connection.getUsername());
		setPassword(connection.getPassword());
		setRememberPassword(connection.isRememberPassword());
		setHost(connection.getHost());
		setUser(connection.getUser());
	}

	// TODO: extract UI related code from core package
	private boolean promptForCredentials() {
		if (prompter == null) {
			return false;
		}
		try {
			alreadyPromptedForPassword = true;
			prompter.promptAndAuthenticate(this);
		} catch (Exception e) {
			Logger.error("Failed to retrieve User's password", e);
		}
		return hasUser();
	}

	public IApplication createApplication(final String applicationName, final ICartridge applicationType,
			final ApplicationScale scale, final IGearProfile gearProfile)
			throws OpenShiftException {
		if (authenticate()) {
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
		if (authenticate()) {
			return user.createDomain(id);
		}
		return null;
	}

	public IApplication getApplicationByName(String name) throws OpenShiftException {
		if (authenticate()
				&& user.hasDomain()) {
			return user.getDefaultDomain().getApplicationByName(name);
		}
		return null;
	}

	public List<IApplication> getApplications() throws OpenShiftException {
		if (authenticate()
				&& user.hasDomain()) {
			return user.getDefaultDomain().getApplications();
		}
		return null;
	}

	public List<ICartridge> getStandaloneCartridgeNames() throws OpenShiftException {
		if (authenticate()) {
			return user.getConnection().getStandaloneCartridges();
		}
		return null;
	}

	public IDomain getDefaultDomain() throws OpenShiftException {
		if (authenticate()) {
			IDomain domain = user.getDefaultDomain();
			isDomainLoaded = true;
			return domain;
		}
		return null;
	}

	public boolean isDomainLoaded() throws OpenShiftException {
		return isDomainLoaded;
	}

	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException {
		if (authenticate()) {
			return user.getConnection().getEmbeddableCartridges();
		}
		return null;
	}

	public boolean hasApplication(String name) throws OpenShiftException {
		if (authenticate()) {
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

	public boolean connect() throws OpenShiftException {
		if (isConnected()) {
			return true;
		}
		if (authenticate()) {
			save();
			return true;
		} else {
			return false;
		}
	}

	public void refresh() throws OpenShiftException {
		isDomainLoaded = false;
		if (authenticate()) {
			user.refresh();
		}
	}

	public boolean isConnected() {
		return hasUser();
	}

	public List<IOpenShiftSSHKey> getSSHKeys() {
		if (authenticate()) {
			return user.getSSHKeys();
		}
		return null;
	}

	public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException,
			OpenShiftException {
		return user.getSSHKeyByPublicKey(publicKey);
	}

	public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
		return user.putSSHKey(name, key);
	}

	public boolean hasSSHKeyName(String name) {
		return user.hasSSHKeyName(name);
	}

	public boolean hasSSHPublicKey(String publicKey) {
		return user.hasSSHPublicKey(publicKey);
	}

	private OpenShiftConfiguration getOpenShiftConfiguration() throws FileNotFoundException, OpenShiftException,
			IOException {
		if (openShiftConfiguration == null) {
			this.openShiftConfiguration = new OpenShiftConfiguration();
		}
		return openShiftConfiguration;
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

	/**
	 * @return an url-alike string that always starts with a scheme but
	 *         eventually has no host where the default host shall be used.
	 * @throws UnsupportedEncodingException 
	 */
	public String toURLString() throws UnsupportedEncodingException {
		return UrlUtils.toUrlString(username, host);
	}
}
