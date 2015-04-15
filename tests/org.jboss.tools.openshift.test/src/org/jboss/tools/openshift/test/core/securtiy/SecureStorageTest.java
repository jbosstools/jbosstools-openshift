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
package org.jboss.tools.openshift.test.core.securtiy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore;
import org.jboss.tools.openshift.internal.common.core.security.SecureStore.IStoreKey;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class SecureStorageTest {

	private IStoreKey key;
	private SecureStore store;

	@Before
	public void setUp() throws CoreException {
		this.key = new OpenShiftSecureStorageKey("org.jboss.tools.openshift.test", "localhost", "foobar");
		this.store = new SecureStore(key);
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
}