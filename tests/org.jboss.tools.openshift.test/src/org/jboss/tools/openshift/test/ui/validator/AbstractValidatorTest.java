/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * 
 * @author jeff.cantrill
 *
 */
public abstract class AbstractValidatorTest {

	private static final IStatus PASS = ValidationStatus.ok();
	private IValidator validator;
	
	protected AbstractValidatorTest(IValidator validator) {
		this.validator = validator;
	}
	protected IValidator getValidator() {
		return this.validator;
	}
	
	protected void assertFailure(Object value) {
		IStatus act = getValidator().validate(value);
		assertEquals(String.format("Exp. to receive ERROR status but got %s", getStatus(act.getSeverity())),IStatus.ERROR, act.getSeverity());
	}

	protected void assertCancel(Object value) {
		IStatus act = getValidator().validate(value);
		assertEquals(String.format("Exp. to receive CANCEL status but got %s", getStatus(act.getSeverity())),IStatus.CANCEL, act.getSeverity());
	}

	protected void assertPass(Object value) {
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
