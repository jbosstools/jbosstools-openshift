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
package org.jboss.tools.openshift.express.internal.core.console;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftPasswordStorageKey;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStore;
import org.jboss.tools.openshift.express.internal.ui.utils.SecurePasswordStoreException;

import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.client.User;
import com.openshift.express.client.configuration.OpenShiftConfiguration;

public class UserModel {
	private static UserModel model;
	public static UserModel getDefault() {
		if( model == null )
			model = new UserModel();
		return model;
	}
	
	
	/** The most recent user connected on OpenShift. */
	private IUser recentUser = null;
	private HashMap<String, IUser> allUsers = new HashMap<String,IUser>();
	
	/**
	 * Create a user for temporary external use
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws OpenShiftException
	 * @throws IOException
	 */
	public IUser createUser(String username, String password) throws OpenShiftException, IOException {
		IUser u = new User(username, password, OpenShiftUIActivator.PLUGIN_ID + " " + 
				OpenShiftUIActivator.getDefault().getBundle().getVersion());
		return u;
	}

	public void addUser(IUser user) {
		try {
			allUsers.put(user.getRhlogin(), user);
			this.recentUser = user;
		} catch(OpenShiftException ose ) {
			// TODO 
		}
	}
	
	public IUser getRecentUser() {
		return recentUser;
	}
	
	public void setRecentUser(IUser user) {
		this.recentUser = user;
	}
	
	public IUser findUser(String username) {
		try {
			for( int i = 0; i < allUsers.size(); i++ ) {
				if( allUsers.get(i).getUUID().equals(username))
					return allUsers.get(i);
			}
		} catch(OpenShiftException ose) {
			
		}
		return null;
	}
	
	public IUser[] getUsers() {
		Collection<IUser> c = allUsers.values();
		IUser[] rets = (IUser[]) c.toArray(new IUser[c.size()]);
		return rets;
	}
	
	/**
	 * Load the user list from preferences and secure storage
	 */
	public void load() {
		// TODO
	}
	
	/**
	 * Save the user list to preferences and secure storage
	 */
	public void save() {
		// TODO
		// save the passwords in secure storage, save the username list somewhere else
	}
	
	private static final String RHLOGIN_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN";

	/**
	 * Get the username stored in preferences as most recently used
	 * @return
	 */
	public String getDefaultUsername() {
		StringPreferenceValue pref = new StringPreferenceValue(RHLOGIN_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		String rhLogin = null;
		rhLogin = pref.get();
		if (rhLogin == null || rhLogin.length() == 0) {
			String configuredUsername = null;
			try {
				configuredUsername = new OpenShiftConfiguration().getRhlogin();
			} catch (Exception e) {
				// do nothing
			}
			rhLogin = configuredUsername;
		}
		return rhLogin;
	}
	
	/*
	 * Return true if the value is updated, false otherwise
	 */
	public boolean setDefaultUsername(String rhLogin) {
		String prev = getDefaultUsername();
		StringPreferenceValue preference = new StringPreferenceValue(RHLOGIN_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		if (rhLogin != null && !rhLogin.equals(prev)) {
			preference.store(rhLogin);
			return true;
		}
		return false;
	}
	
	/*
	 * Return a password from secure storage, or 
	 * null if platform not found, or password not stored
	 */
	public String getPasswordFromSecureStorage(final String rhLogin) {
		SecurePasswordStore store = getSecureStore(rhLogin);
		if( store != null && rhLogin != null && !rhLogin.isEmpty() ) {
			try {
				return store.getPassword();
			} catch (SecurePasswordStoreException e) {
				Logger.error("Failed to retrieve OpenShift user's password from Secured Store", e);
			}
		}
		return null;
	}
	
	public void setPasswordInSecureStorage(final String rhLogin, String password) {
		SecurePasswordStore store = getSecureStore(rhLogin);
		if( store != null && rhLogin != null && !rhLogin.isEmpty() ) {
			try {
				store.setPassword(password);
			} catch (SecurePasswordStoreException e) {
				Logger.error(e.getMessage(), e);
			}
		}
	}

	
	private SecurePasswordStore getSecureStore(final String rhLogin) {
		return getSecureStore(initLibraServer(), rhLogin);
	}

	/*
	 * Return a secure store or null if platform is not found
	 */
	private SecurePasswordStore getSecureStore(final String platform, final String username) {
		if( platform == null )
			return null;
		final OpenShiftPasswordStorageKey key = new OpenShiftPasswordStorageKey(platform, username);
		SecurePasswordStore store = new SecurePasswordStore(key);
		return store;
	}

	private String initLibraServer() {
		try {
			return new OpenShiftConfiguration().getLibraServer();
		} catch (Exception e) {
			Logger.error("Failed to load OpenShift configuration from client library", e);
		}
		return null;
	}

	
}
