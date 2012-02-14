/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class SecurePasswordStore {

	private static final String ENCODING = "UTF-8";

	public static interface IStorageKey {
		public String getKey();

		public boolean equals(IStorageKey key);
	}

	private String password;
	private IStorageKey storageKey;

	public SecurePasswordStore(IStorageKey key) {
		this.storageKey = key;
	}

	public String getPassword() throws SecurePasswordStoreException {
		try {
			return this.password = getFromPreferences(storageKey);
		} catch (Exception e) {
			throw new SecurePasswordStoreException("Could get password", e);
		}
	}

	public void setPassword(String password) throws SecurePasswordStoreException {
		update(storageKey, password);
	}

	private void update(IStorageKey key, String password) throws SecurePasswordStoreException {
		if (!storageKey.equals(key) || isPasswordChanged(password)) {
			storeInPreferences(this.password = password, this.storageKey = key);
		}
	}

	private boolean isPasswordChanged(String password) {
		if (this.password == null && password == null) {
			return false;
		} else {
			return (this.password == null && password != null) || (this.password != null && password == null)
					|| !password.equals(this.password);
		}
	}

	public void remove() throws SecurePasswordStoreException {
		try {
			ISecurePreferences node = getNode(storageKey);
			if (node == null) {
				throw new SecurePasswordStoreException("Could not remove password");
			}
			node.clear();
		} catch (Exception e) {
			throw new SecurePasswordStoreException("Could not remove password", e);
		}
	}

	private String getFromPreferences(IStorageKey key) throws StorageException, UnsupportedEncodingException {
		ISecurePreferences node = getNode(key);
		String password = node.get("password", null); //$NON-NLS-1$
		if (password == null) {
			return null;
		}
		return new String(EncodingUtils.decodeBase64(password));
	}

	private void storeInPreferences(String password, IStorageKey key) throws SecurePasswordStoreException {
		try {
			ISecurePreferences node = getNode(key);
			node.put("password", EncodingUtils.encodeBase64(password.getBytes()), true /* encrypt */); //$NON-NLS-1$
		} catch (Exception e) {
			throw new SecurePasswordStoreException("Could not store password", e);
		}
	}

	private ISecurePreferences getNode(IStorageKey key) throws UnsupportedEncodingException {
		if (key == null) {
			return null;
		}

		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		String keyString = URLEncoder.encode(key.getKey(), ENCODING);
		return root.node(keyString);
	}
}
