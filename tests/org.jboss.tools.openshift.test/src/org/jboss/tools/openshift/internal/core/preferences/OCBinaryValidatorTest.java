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
package org.jboss.tools.openshift.internal.core.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.osgi.framework.Version;

public class OCBinaryValidatorTest {

	@Test
	public void testParseVersion() {
		assertFalse(OCBinaryValidator.parseVersion(null).isPresent());
		assertFalse(OCBinaryValidator.parseVersion("foo").isPresent());
		assertSameVersion("1.2.3.foo", "oc v1.2.3-foo");
		assertSameVersion("1.2.3", "oc v1.2.3");
		assertSameVersion("1.2.0.foo", "oc v1.2-foo");
		assertSameVersion("1.0.0", "oc v1");
	}
	
	@Test
	public void testIsCompatibleForPublishing() {
		assertFalse(OCBinaryValidator.isCompatibleForPublishing((Version)null));
		assertFalse(OCBinaryValidator.isCompatibleForPublishing(Version.parseVersion("1.1.0")));
		assertTrue(OCBinaryValidator.isCompatibleForPublishing(Version.parseVersion("1.1.1")));
		assertTrue(OCBinaryValidator.isCompatibleForPublishing(Version.parseVersion("1.1.2")));
	}
	
	private void assertSameVersion(String expectedVersion, String input) {
		Optional<Version> version = OCBinaryValidator.parseVersion(input);
		assertTrue("Couldn't parse "+input, version.isPresent());
		assertEquals(expectedVersion, version.get().toString());
	}
}
