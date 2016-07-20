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
package org.jboss.tools.openshift.test.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore.IStoreKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
@Ignore
public class SecureStorageTest {

	private static IStoreKey key;
	private static SecureStore store;

	@BeforeClass
	public static void setUp() throws CoreException {
		key = new OpenShiftSecureStorageKey("org.jboss.tools.openshift.test", "localhost", "foobar");
		store = new SecureStore(key);
	}

	@After
	public void tearDown() throws SecureStoreException {
		store.clear();
	}
	
	@Test
	public void shouldRetrieveValue() throws SecureStoreException {
		// pre-condition
		String password = store.get("password");
		assertTrue(password == null);

		// operation
		store.put("password", "gingerbred");
		
		// verification
		assertEquals("gingerbred", store.get("password"));
	}

	@Test
	public void shouldRemoveValue() throws SecureStoreException {
		// pre-condition
		store.put("password", "honey");

		// operation
		store.remove("password");
		
		// verification
		assertTrue(store.get("password") == null);
	}

	@Test
	public void shouldUpdateValue() throws SecureStoreException {
		// pre-condition
		store.put("password", "chocolate");
		assertEquals("chocolate", store.get("password"));

		// operation
		store.put("password", "tabasco");

		// verification
		assertEquals("tabasco", store.get("password"));
	}

	@Test
	public void shouldStoreDistinctValues() throws SecureStoreException {
		// pre-condition
		store.put("password", "chocolate");
		
		// operation
		store.put("nonsense", "foobar");
		
		// verification
		assertEquals("foobar", store.get("nonsense"));
	}
	
	@Test
	public void shouldRemoveNode() throws SecureStoreException {
		// pre-condition
		IStoreKey key = new OpenShiftSecureStorageKey("org.jboss.tools.openshift.test", "localhost", "foobar");
		SecureStore store = new SecureStore(key);
		store.put("password", "chocolate");
		assertTrue(SecurePreferencesFactory.getDefault().nodeExists(key.getKey()));
		
		// operation
		store.removeNode();
		
		// verification
		assertFalse(SecurePreferencesFactory.getDefault().nodeExists(key.getKey()));
	}
}