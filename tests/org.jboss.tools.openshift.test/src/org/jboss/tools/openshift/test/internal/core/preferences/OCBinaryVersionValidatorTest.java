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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.core.preferences.OCBinaryVersionValidator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

public class OCBinaryVersionValidatorTest {

	private TestableOCBinaryVersionValidator validator;

	@Before
	public void before() {
		this.validator = new TestableOCBinaryVersionValidator("");
	}
	
	@Test
	public void shouldReportVersionIsPresent() {
		assertFalse(validator.parseVersion(null).isPresent());
		assertFalse(validator.parseVersion("oc vqualifier").isPresent());
		assertTrue(validator.parseVersion("oc v1-foo").isPresent());
		assertTrue(validator.parseVersion("oc v1.0-foo").isPresent());
		assertTrue(validator.parseVersion("oc v1.0-foo+fdf5432").isPresent());
		assertTrue(validator.parseVersion("oc v1.2.0-foo+fdf5432").isPresent());
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
	public void nullPathReturnsEmptyVersion() {
		OCBinaryVersionValidator validator = new OCBinaryVersionValidator(null);
		assertEquals(Version.emptyVersion, validator.getVersion(new NullProgressMonitor()));
	}

	@Test
	public void testIsCompatibleForPublishing() {
		assertFalse(validator.getValidationStatus(null, new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("1.1.0"), new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("1.1.1"), new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("1.1.2"), new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("1.4.0.rc1"), new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("3.10.0"), new NullProgressMonitor()).isOK());
		assertFalse(validator.getValidationStatus(Version.parseVersion("3.11.0.rc1"), new NullProgressMonitor()).isOK());
		assertTrue(validator.getValidationStatus(Version.parseVersion("3.11.0"), new NullProgressMonitor()).isOK());
	}

	@Test
	public void oc310OnLinuxValidatesToWarning() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator(null, Platform.OS_LINUX);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.10.0"), null);
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void oc310OnNonLinuxValidatesOK() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator(null, Platform.OS_WIN32);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.10.0"), null);
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc311OnLinuxValidatesOK() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator(null, Platform.OS_LINUX);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.11.0"), null);
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc360OnMacWithSpaceInPathValidatesToWarning() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator("/my home/bin/oc", Platform.OS_MACOSX);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.6.0"), null);
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void oc370OnMacWithSpaceInPathValidatesOK() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator("/my home/bin/oc", Platform.OS_MACOSX);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.7.0"), null);
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc360OnWindowsWithSpaceInPathValidatesOK() {
		// given
		TestableOCBinaryVersionValidator validator = new TestableOCBinaryVersionValidator("/my home/bin/oc", Platform.OS_WIN32);
		// when
		IStatus status = validator.getValidationStatus(Version.parseVersion("3.6.0"), null);
		// then
		assertThat(status.isOK()).isTrue();
	}

	private void assertSameVersion(String expectedVersion, String input) {
		Optional<Version> version = validator.parseVersion(input);
		assertTrue("Couldn't parse " + input, version.isPresent());
		assertEquals(expectedVersion, version.get().toString());
	}

	private class TestableOCBinaryVersionValidator extends OCBinaryVersionValidator {

		private String platform;

		private TestableOCBinaryVersionValidator(String path) {
			this(path, null);
		}

		private TestableOCBinaryVersionValidator(String path, String platform) {
			super(path);
			this.platform = platform;
		}

		@Override
		public Optional<Version> parseVersion(String line) {
			return super.parseVersion(line);
		}

		@Override
		protected boolean isPlatform(String platform) {
			if (!StringUtils.isEmpty(this.platform)) {
				return this.platform.equals(platform);
			} else {
				return super.isPlatform(platform);
			}
		}
	}
	
}
