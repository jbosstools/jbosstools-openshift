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

import static org.junit.Assert.*;

import org.jboss.tools.openshift.internal.common.core.security.OpenShiftSecureStorageKey;
import org.junit.Before;
import org.junit.Test;

public class OpenShiftSecureStorageKeyTest {

	private static final String BASE = "the.base.key";
	private static final String USERNAME = "aperson@anaddress.com";
	private static final String EXP_KEY = "the.base.key/ahost/aperson@anaddress.com";

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetKeyForHostWithoutSceme() {
		assertEquals(EXP_KEY, new OpenShiftSecureStorageKey(BASE, "ahost", USERNAME).getKey());
	}

	@Test
	public void testGetKeyForHostWithSceme() {
		assertEquals(EXP_KEY, new OpenShiftSecureStorageKey(BASE, "https://ahost", USERNAME).getKey());
	}
	
	@Test
	public void testGetKeyForHostWithTrailingSlash() {
		assertEquals(EXP_KEY, new OpenShiftSecureStorageKey(BASE, "https://ahost/", USERNAME).getKey());
	}

}
