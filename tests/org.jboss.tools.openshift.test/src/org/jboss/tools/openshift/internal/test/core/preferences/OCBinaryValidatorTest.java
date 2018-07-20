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
package org.jboss.tools.openshift.internal.test.core.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.internal.core.ocbinary.IOCBinaryRequirement;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryValidator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

public class OCBinaryValidatorTest {

	private TestableOCBinaryValidator validator;

	@Before
	public void before() throws IOException {
		this.validator = new TestableOCBinaryValidator("", false);

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
	public void getVersionReturnsEmptyVersionForNullPath() {
		OCBinaryValidator validator = new OCBinaryValidator(null);
		assertEquals(Version.emptyVersion, validator.getVersion(new NullProgressMonitor()));
	}

	@Test
	public void nonExistingOcValidatesKO() {
		// given
		TestableOCBinaryValidator validator = 
				new TestableOCBinaryValidator("/bollock-oc", true);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("100.0.0"));
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void nonExecutableOcValidatesKO() throws IOException {
		// given
		File ocFake = File.createTempFile("OCBinaryValidatorTest", null);
		ocFake.setExecutable(false);
		TestableOCBinaryValidator validator = new TestableOCBinaryValidator(ocFake.getAbsolutePath(), true);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("100.0.0"));
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void existsAndIsExecutableOcValidatesOK() throws IOException {
		// given
		File ocFake = File.createTempFile("OCBinaryValidatorTest", null);
		ocFake.setExecutable(true);

		TestableOCBinaryValidator validator = new TestableOCBinaryValidator(ocFake.getAbsolutePath(), true);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("100.0.0"));
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc310OnLinuxValidatesKO() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("papa-smurf", Platform.OS_LINUX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.10.0"));
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void oc310OnNonLinuxValidatesOK() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("papa-smurf", Platform.OS_WIN32, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.10.0"));
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc311OnLinuxValidatesOK() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("papa-smurf", Platform.OS_LINUX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.11.0"));
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc310OnLinuxValidatesOKIfNonRSyncRequirementsAreTested() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("papa-smurf", Platform.OS_LINUX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.10.0"), false, OCBinaryValidator.NON_RSYNC_REQUIREMENTS);
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc360OnMacWithSpaceInPathValidatesKO() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("/my home/bin/oc", Platform.OS_MACOSX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.6.0"));
		// then
		assertThat(status.isOK()).isFalse();
	}

	@Test
	public void oc360OnMacWithSpaceInPathValidatesOKIfNonRSyncRequirementsTested() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("/my home/bin/oc", Platform.OS_MACOSX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.6.0"), false, OCBinaryValidator.NON_RSYNC_REQUIREMENTS);
		// then validates ok, path with spaces is only relevant for rsync
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc370OnMacWithSpaceInPathValidatesOK() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("/my home/bin/oc", Platform.OS_MACOSX, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.7.0"));
		// then
		assertThat(status.isOK()).isTrue();
	}

	@Test
	public void oc360OnWindowsWithSpaceInPathValidatesOK() {
		// given
		OCBinaryValidator validator = new TestableOCBinaryValidator("/my home/bin/oc", Platform.OS_WIN32, false);
		// when
		IStatus status = validator.getStatus(Version.parseVersion("3.6.0"));
		// then
		assertThat(status.isOK()).isTrue();
	}

	private void assertSameVersion(String expectedVersion, String input) {
		Optional<Version> version = validator.parseVersion(input);
		assertTrue("Couldn't parse " + input, version.isPresent());
		assertEquals(expectedVersion, version.get().toString());
	}

	public class TestableOCBinaryValidator extends OCBinaryValidator {

		private String os;
		private boolean verifyIsExecutable;

		
		private TestableOCBinaryValidator(String path, boolean checkOcExists) {
			this(path, Platform.OS_WIN32, checkOcExists);
		}

		private TestableOCBinaryValidator(String path, String os, boolean verifyIsExecutable) {
			super(path);
			this.os = os;
			this.verifyIsExecutable = verifyIsExecutable;
		}

		@Override
		public Optional<Version> parseVersion(String line) {
			return super.parseVersion(line);
		}

		@Override
		public IStatus getStatus(Version version, boolean displayLinks, Collection<IOCBinaryRequirement> requirements) {
			return super.getStatus(version, displayLinks, createSpies(requirements));
		}

		private List<IOCBinaryRequirement> createSpies(Collection<IOCBinaryRequirement> requirements) {
			List<IOCBinaryRequirement> spies = new ArrayList<>();
			for (IOCBinaryRequirement requirement : requirements) {
				IOCBinaryRequirement spy = spy(requirement);

				fakeOS(os, spy);
				fakeExecutable(verifyIsExecutable, requirement, spy);

				spies.add(spy);
			}
			return spies;
		}

		private void fakeExecutable(boolean verifyIsExecutable, IOCBinaryRequirement requirement, IOCBinaryRequirement spy) {
			if (!verifyIsExecutable
					&& requirement == IOCBinaryRequirement.EXECUTABLE) {
				doReturn(true).when(spy).isFulfilled(any(Version.class), anyString());
			}
		}

		private void fakeOS(String os, IOCBinaryRequirement spy) {
			doReturn(os).when(spy).getOS();
		}
	
	}
}
