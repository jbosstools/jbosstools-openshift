/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.test.internal.core.preferences;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.junit.Before;
import org.junit.Test;

public class OpenShiftCorePreferencesTest {

	private static final String CONNECTION = "test@localtest";

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testExtPropertiesCycle() {
		Map<String, Object> ext = new HashMap<>();
		ext.put("foo","bar");
		
		OpenShiftCorePreferences.INSTANCE.saveExtProperties(CONNECTION, ext);
		
		Map<String, Object> loaded = OpenShiftCorePreferences.INSTANCE.loadExtProperties(CONNECTION);
		
		assertEquals(ext, loaded);
	}

	@Test
	public void testSaveExtPropertiesWithNulls() {
		
		OpenShiftCorePreferences.INSTANCE.saveExtProperties(CONNECTION, null);
		OpenShiftCorePreferences.INSTANCE.saveExtProperties(" ", null);
		OpenShiftCorePreferences.INSTANCE.saveExtProperties(null, new HashMap<>());
	}
	
	@Test
	public void testExtPropertyLoadOfForMissingURL() {
		String connection = String.valueOf(new Random().nextInt());
		Map<String, Object> loaded = OpenShiftCorePreferences.INSTANCE.loadExtProperties(connection);
		assertTrue(loaded.isEmpty());
	}

	@Test
	public void testExtPropertyLoadOfNullConnection() {
		Map<String, Object> loaded = OpenShiftCorePreferences.INSTANCE.loadExtProperties(null);
		assertTrue(loaded.isEmpty());
	}
	@Test
	public void testExtPropertyLoadOfEmptyConnection() {
		Map<String, Object> loaded = OpenShiftCorePreferences.INSTANCE.loadExtProperties(" ");
		assertTrue(loaded.isEmpty());
	}

}
