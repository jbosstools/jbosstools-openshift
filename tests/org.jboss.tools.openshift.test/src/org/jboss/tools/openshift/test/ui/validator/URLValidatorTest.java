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
package org.jboss.tools.openshift.test.ui.validator;

import static org.junit.Assert.*;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;
import org.junit.Test;

public class URLValidatorTest {

	private URLValidator validator = new URLValidator("", true); 
	
	@Test
	public void testHttpsPassValidURL() {
		assertEquals(ValidationStatus.ok(), validator.validate("https://foobar"));
	}

	@Test
	public void testHttpPassValidURL() {
		assertEquals(ValidationStatus.ok(), validator.validate("http://foobar"));
	}
	
	@Test
	public void testFailInvalidURL() {
		assertNotEquals(ValidationStatus.ok(), validator.validate("htt://foobar"));
	}
	
	@Test
	public void testAllowBlankURL() {
		validator = new URLValidator("", true);
		assertEquals(ValidationStatus.ok(), validator.validate(" "));
	}

	@Test
	public void testDoNotAllowBlankURL() {
		validator = new URLValidator("", false);
		assertNotEquals(ValidationStatus.ok(), validator.validate(" "));
	}

}
