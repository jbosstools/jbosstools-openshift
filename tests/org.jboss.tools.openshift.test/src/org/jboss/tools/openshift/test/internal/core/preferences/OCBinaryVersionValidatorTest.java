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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.openshift.internal.core.preferences.OCBinaryVersionValidator;
import org.junit.Test;
import org.osgi.framework.Version;

public class OCBinaryVersionValidatorTest {

	@Test
	public void shouldReportVersionIsPresent() {
		assertFalse(OCBinaryVersionValidator.parseVersion(null).isPresent());
		assertFalse(OCBinaryVersionValidator.parseVersion("oc vqualifier").isPresent());
		assertTrue(OCBinaryVersionValidator.parseVersion("oc v1-foo").isPresent());
		assertTrue(OCBinaryVersionValidator.parseVersion("oc v1.0-foo").isPresent());
		assertTrue(OCBinaryVersionValidator.parseVersion("oc v1.0-foo+fdf5432").isPresent());
		assertTrue(OCBinaryVersionValidator.parseVersion("oc v1.2.0-foo+fdf5432").isPresent());
	}
	
	@Test
	public void shouldParseOCGithubVersion() {
		assertSameVersion("1.2.3.foo", "oc v1.2.3-foo");
		assertSameVersion("1.2.3", "oc v1.2.3");
		assertSameVersion("1.2.0.foo", "oc v1.2-foo");
		assertSameVersion("1.0.0", "oc v1");
		assertSameVersion("1.4.0.rc1", "oc v1.4.0-rc1+b4e0954");
	}

	@Test
	public void testParseOCEnterpriseVersion() {
		assertSameVersion("3.4.1.2", "oc v3.4.1.2");
	}

	@Test
	public void testIsCompatibleForPublishing() {
		assertFalse(OCBinaryVersionValidator.isCompatibleForPublishing((Version)null));
		assertFalse(OCBinaryVersionValidator.isCompatibleForPublishing(Version.parseVersion("1.1.0")));
		assertTrue(OCBinaryVersionValidator.isCompatibleForPublishing(Version.parseVersion("1.1.1")));
		assertTrue(OCBinaryVersionValidator.isCompatibleForPublishing(Version.parseVersion("1.1.2")));
		assertTrue(OCBinaryVersionValidator.isCompatibleForPublishing(Version.parseVersion("1.4.0.rc1")));
	}
	
	private void assertSameVersion(String expectedVersion, String input) {
		Optional<Version> version = OCBinaryVersionValidator.parseVersion(input);
		assertTrue("Couldn't parse "+input, version.isPresent());
		assertEquals(expectedVersion, version.get().toString());
	}
	
	@Test
	public void testValidatorWithDefault() {
	    OCBinaryVersionValidator validator = new OCBinaryVersionValidator(null);
	    assertEquals(Version.emptyVersion, validator.getVersion(new NullProgressMonitor()));
	}
}
