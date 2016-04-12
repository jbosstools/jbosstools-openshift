/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.security;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

/**
 * A store that persists values in a secure way.
 * 
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class SecureStore {

	public static interface IStoreKey {
		public String getKey();
	}

	private Map<String, String> values;
	private IStoreKey storeKey;

	public SecureStore(IStoreKey key) {
		this.storeKey = key;
		this.values = new HashMap<>();
	}

	public String get(String id) throws SecureStoreException {
		try {
			String value = get(id, storeKey);
			values.put(id, value);
			return value;
		} catch (Exception e) {
			throw new SecureStoreException(NLS.bind("Could not get value {0}", id), e);
		}
	}

	public void put(String id, String value) throws SecureStoreException {
		if (isValueChanged(id, value)) {
			store(id, value, storeKey);
		}
	}

	private boolean isValueChanged(String id, String value) {
		String cachedValue = values.get(id);
		if (cachedValue == null) {
			return value != null;
		} else {
			return !cachedValue.equals(value);
		}
	}

	public void remove(String id) throws SecureStoreException {
		try {
			getNode(storeKey).remove(id);
			values.remove(id);
		} catch (Exception e) {
			throw new SecureStoreException(NLS.bind("Could not remove value {0}", id), e);
		}
	}

	public void clear() throws SecureStoreException {
		try {
			getNode(storeKey).clear();
			values.clear();
		} catch (Exception e) {
			throw new SecureStoreException(NLS.bind("Could not remove storage node {0}", storeKey.getKey()), e);
		}
	}
	
	private String get(String id, IStoreKey key) throws StorageException, UnsupportedEncodingException, SecureStoreException {
		if (StringUtils.isEmpty(id)
				|| key == null) {
			return null;
		}
		String value = getNode(key).get(id, null); //$NON-NLS-1$
		if (value == null) {
			return null;
		}
		return new String(EncodingUtils.decodeBase64(value));
	}

	private void store(String id, String value, IStoreKey key) throws SecureStoreException {
		try {
			getNode(storeKey).put(id, EncodingUtils.encodeBase64(value.getBytes()), true /* encrypt */); // $NON-NLS-1$
		} catch (Exception e) {
			throw new SecureStoreException(NLS.bind("Could not store value {0}: {1}", id, value), e);
		}
	}

	private ISecurePreferences getNode(IStoreKey key) throws UnsupportedEncodingException, SecureStoreException {
		if (key == null) {
			throw new SecureStoreException("storage key is null.");
		}

		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = root.node(key.getKey());
		if (node == null) {
			throw new SecureStoreException(NLS.bind("Could find storage node {0}", key.getKey()));
		}
		return node;

	}
}
