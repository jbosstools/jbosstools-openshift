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

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.internal.ui.validator.LabelKeyValidator;
import org.jboss.tools.openshift.internal.ui.validator.LabelValueValidator;
import org.junit.Test;

public class LabelValueValidatorTest {
	
	private static final IStatus PASS = ValidationStatus.ok();
	
	private LabelValueValidator validator = new LabelValueValidator();
	
	protected IValidator getValidator() {
		return validator;
	}

	@Test
	public void nullValueShouldBeInvalid() {
		assertCancel(null);
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
	public void valueWithSpacesShouldBeInvalid() {
		assertFailure("abc def");
	}

	@Test
	public void valueWithSlashesShouldBeInValid() {
		assertFailure("abcd.efg/a23");
	}
	
	@Test
	public void valueWithDotsDashesAndUnderScoresShouldBeValid() {
		assertPass("abcd.efg_k-123");
	}

	protected void assertFailure(String value) {
		IStatus act = getValidator().validate(value);
		assertEquals(String.format("Expected to receive ERROR status but got %s", getStatus(act.getSeverity())),IStatus.ERROR, act.getSeverity());
	}

	protected void assertCancel(String value) {
		IStatus act = getValidator().validate(value);
		assertEquals(String.format("CANCEL to receive ERROR status but got %s", getStatus(act.getSeverity())),IStatus.CANCEL, act.getSeverity());
	}

	protected void assertPass(String value) {
		assertEquals(PASS, getValidator().validate(value));
	}
	
	protected String getStatus(int severity) {
		String status = null;
		switch(severity) {
		case IStatus.OK:
			status = "OK";
			break;
		case IStatus.CANCEL:
			status = "CANCEL";
			break;
		case IStatus.ERROR:
			status = "ERROR";
			break;
		case IStatus.WARNING:
			status = "WARNING";
			break;
		case IStatus.INFO:
			status = "INFO";
			break;
		}
		return status;
	}

}
