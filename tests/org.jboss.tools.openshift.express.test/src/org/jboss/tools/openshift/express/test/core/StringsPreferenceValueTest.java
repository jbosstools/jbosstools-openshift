/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.jboss.tools.openshift.internal.common.core.preferences.StringsPreferenceValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Andre Dietisheim
 */
public class StringsPreferenceValueTest {

	private StringsPreferenceValue stringsPreferenceValue;

	@Before
	public void setUp() {
		this.stringsPreferenceValue =
				new StringsPreferenceValue('|', 4, "test", "org.jboss.tools.openshift.express.test.core");
	}

	@After
	public void tearDown() throws BackingStoreException {
		stringsPreferenceValue.clear();
	}

	@Test
	public void shouldAdd1Value() {
		// pre-condition
		assertEquals(0, stringsPreferenceValue.size());

		// operation
		stringsPreferenceValue.add("42");

		// verification
		assertEquals(1, stringsPreferenceValue.size());
		assertEquals("42", stringsPreferenceValue.get()[0]);
	}

	@Test
	public void shouldAdd3Values() {
		// pre-condition
		assertEquals(0, stringsPreferenceValue.size());

		// operation
		stringsPreferenceValue.add("42");
		stringsPreferenceValue.add("128");
		stringsPreferenceValue.add("1024");

		// verification
		assertEquals(3, stringsPreferenceValue.size());
		assertArrayEquals(new String[] { "42", "128", "1024" }, stringsPreferenceValue.get());
	}

	@Test
	public void shouldDropFirstValueWhenMaximumReached() {
		// pre-condition
		assertEquals(0, stringsPreferenceValue.size());
		stringsPreferenceValue.add("42");
		stringsPreferenceValue.add("128");
		stringsPreferenceValue.add("1024");
		stringsPreferenceValue.add("4096");
		
		// operation
		stringsPreferenceValue.add("512");

		// verification
		assertEquals(4, stringsPreferenceValue.size());
		assertArrayEquals(new String[] { "128", "1024", "4096", "512" }, stringsPreferenceValue.get());
	}
	
	@Test
	public void shouldRemove1Value() {
		// pre-condition
		assertEquals(0, stringsPreferenceValue.size());
		stringsPreferenceValue.add("42");
		stringsPreferenceValue.add("128");
		stringsPreferenceValue.add("1024");

		// operation
		stringsPreferenceValue.remove("128");

		// verification
		assertEquals(2, stringsPreferenceValue.size());
		assertArrayEquals(new String[] { "42", "1024" }, stringsPreferenceValue.get());
	}

	@Test
	public void shouldRemove2Values() {
		// pre-condition
		assertEquals(0, stringsPreferenceValue.size());
		stringsPreferenceValue.add("42");
		stringsPreferenceValue.add("128");
		stringsPreferenceValue.add("1024");
		stringsPreferenceValue.add("4096");
		
		// operation
		stringsPreferenceValue.remove("128", "1024");

		// verification
		assertEquals(2, stringsPreferenceValue.size());
		assertArrayEquals(new String[] { "42", "4096" }, stringsPreferenceValue.get());
	}
	
}
