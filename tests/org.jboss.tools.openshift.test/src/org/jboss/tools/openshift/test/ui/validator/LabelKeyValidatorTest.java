/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import static org.junit.Assert.*;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.internal.ui.validator.LabelKeyValidator;
import org.junit.Test;

public class LabelKeyValidatorTest {
	
	private static final IStatus PASS = ValidationStatus.ok();
	
	private LabelKeyValidator validator = new LabelKeyValidator();
	private final IStatus FAIL = validator.getFailedStatus();
	
	@Test
	public void nullValueShouldBeInvalid() {
		assertFailure(null);
	}

	@Test
	public void emptyValueShouldBeInvalid() {
		assertFailure("");
	}

	@Test
	public void blankValueShouldBeInvalid() {
		assertFailure("  ");
	}

	@Test
	public void valueWithoutSubDomainThatExceedsTheMaxValueShouldBeInvalid() {
		StringBuffer b = new StringBuffer();
		for(int i=0; i < LabelKeyValidator.LABEL_MAXLENGTH + 1; i++)
			b.append("a");
		assertFailure(b.toString());
	}

	@Test
	public void valueWithSubDomainThatExceedsTheMaxValueShouldBeInvalid() {
		StringBuffer b = new StringBuffer();
		for(int i=0; i < LabelKeyValidator.SUBDOMAIN_MAXLENGTH + 1; i++)
			b.append("a");
		b.append("/abc");
		assertFailure(b.toString());
	}

	@Test
	public void valueWithSpacesShouldBeInvalid() {
		assertFailure("abc def");
	}

	@Test
	public void valueWithSubdomainWithSpacesShouldBeInvalid() {
		assertFailure("abc def/abcdef");
	}

	@Test
	public void valueWithDotsDashesAndUnderScoresShouldBeValid() {
		assertPass("abcd.efg_k-123");
	}

	@Test
	public void valueWithSubdomainWithUnderScoresShouldBeInValid() {
		assertFailure("abcd.efg_k-123/abc123");
	}

	@Test
	public void valueWithSubdomainWithDotsDashesAndUnderScoresShouldBeInValid() {
		assertFailure("abcd.efg_k-123/abc123");
	}

	@Test
	public void valueWithSubdomainWithDotsDashesShouldBeInValid() {
		assertPass("abcd.efgk-123/abc123");
	}
	
	private void assertFailure(String value) {
		assertEquals(FAIL, validator.validate(value));
	}

	private void assertPass(String value) {
		assertEquals(PASS, validator.validate(value));
	}


}
