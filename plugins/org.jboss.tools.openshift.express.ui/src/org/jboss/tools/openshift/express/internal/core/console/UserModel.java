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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.common.ui.preferencevalue.StringsPreferenceValue;
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
	private static final String USER_ID = OpenShiftUIActivator.PLUGIN_ID + " " +
			OpenShiftUIActivator.getDefault().getBundle().getVersion();
	private static UserModel model;

	public static UserModel getDefault() {
		if (model == null)
			model = new UserModel();
		return model;
	}

	/** The most recent user connected on OpenShift. */
	private IUser recentUser = null;
	private HashMap<String, IUser> allUsers = new HashMap<String, IUser>();
	private ArrayList<IUserModelListener> listeners = new ArrayList<IUserModelListener>();

	public UserModel() {
		load();
	}

	public void addListener(IUserModelListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IUserModelListener listener) {
		listeners.remove(listener);
	}

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
		IUser u = new User(username, password, USER_ID);
		return u;
	}

	private static final int ADDED = 0;
	private static final int REMOVED = 1;
	private static final int CHANGED = 2;

	public void addUser(IUser user) {
		allUsers.put(user.getRhlogin(), user);
		this.recentUser = user;
		fireModelChange(user, ADDED);
	}

	public void removeUser(IUser user) {
		allUsers.remove(user.getRhlogin());
		if (this.recentUser == user)
			this.recentUser = null;
		fireModelChange(user, REMOVED);
	}

	private void fireModelChange(IUser user, int type) {
		Iterator<IUserModelListener> i = listeners.iterator();
		while (i.hasNext()) {
			IUserModelListener l = i.next();
			switch (type) {
			case ADDED:
				l.userAdded(user);
				break;
			case REMOVED:
				l.userRemoved(user);
				break;
			case CHANGED:
				l.userChanged(user);
				break;

			default:
				break;
			}
		}
	}

	public IUser getRecentUser() {
		return recentUser;
	}

	public void setRecentUser(IUser user) {
		this.recentUser = user;
	}

	public IUser findUser(String username) {
		return allUsers.get(username);
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
		StringsPreferenceValue pref = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY,
				OpenShiftUIActivator.PLUGIN_ID);
		String[] users = pref.get();
		for (int i = 0; i < users.length; i++) {
			try {
				String password = getPasswordFromSecureStorage(users[i]);
				IUser u = createUser(users[i], password);
				addUser(u);
			} catch (OpenShiftException ose) {
				// TODO
			} catch (IOException ioe) {
				// TODO
			}
		}
	}

	/**
	 * Save the user list to preferences and secure storage
	 */
	public void save() {
		// passwords are already in secure storage, save the username list
		// somewhere else
		Set<String> set = allUsers.keySet();
		String[] userList = (String[]) set.toArray(new String[set.size()]);
		StringsPreferenceValue pref = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY,
				OpenShiftUIActivator.PLUGIN_ID);
		pref.store(userList);

		Iterator<IUser> i = allUsers.values().iterator();
		IUser tmp;
		while (i.hasNext()) {
			tmp = i.next();
			setPasswordInSecureStorage(tmp.getRhlogin(), tmp.getPassword());
		}
	}

	private static final String RECENT_RHLOGIN_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN";
	private static final String RHLOGIN_LIST_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN_LIST";

	/**
	 * Get the username stored in preferences as most recently used
	 * 
	 * @return
	 */
	public String getDefaultUsername() {
		StringPreferenceValue pref = new StringPreferenceValue(RECENT_RHLOGIN_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
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
		StringPreferenceValue preference = new StringPreferenceValue(RECENT_RHLOGIN_PREFS_KEY,
				OpenShiftUIActivator.PLUGIN_ID);
		if (rhLogin != null && !rhLogin.equals(prev)) {
			preference.store(rhLogin);
			return true;
		}
		return false;
	}

	/*
	 * Return a password from secure storage, or null if platform not found, or
	 * password not stored
	 */
	public String getPasswordFromSecureStorage(final String rhLogin) {
		if (rhLogin == null)
			return null;

		SecurePasswordStore store = getSecureStore(rhLogin);
		if (store != null && rhLogin != null && !rhLogin.isEmpty()) {
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
		if (store != null && rhLogin != null && !rhLogin.isEmpty()) {
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
		if (platform == null)
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
