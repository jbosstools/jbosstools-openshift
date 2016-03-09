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

import org.jboss.tools.openshift.internal.ui.validator.ServiceNameValidator;
import org.junit.Test;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class ServiceNameValidatorTest extends AbstractValidatorTest {
	
	public ServiceNameValidatorTest() {
		super(new ServiceNameValidator());
	}
	
	@Test
	public void testShouldReturnPassIfConforms() {
		assertPass("spring");
		assertPass("spring09");
		assertPass("spring-09");
		assertPass("spring-09-09");
	}

	@Test
	public void testShouldReturnFalseIfBeginsWithCapital() {
		assertFailure("Spring");
	}

	@Test
	public void testShouldReturnFalseIfDoesntBeginWithAlpha() {
		assertFailure("99spring");
		assertFailure("$$spring");
	}

	@Test
	public void testShouldReturnFalseIfDoesntEndWithAlphanumeric() {
		assertFailure("spring-");
		assertFailure("spring-$");
	}

	@Test
	public void testShouldReturnFalseForGreaterThanMax() {
		assertFailure("spring-boot-helloworld-ui");
	}

	@Test
	public void testShouldReturnPForEqualToMax() {
		assertPass("spring-boot-helloworld-u");
	}
	
	@Test
	public void testShouldReturnFalseForNonString() {
		assertCancel(Boolean.FALSE);
	}

	@Test
	public void testShouldReturnFalseForNull() {
		assertCancel(null);
	}

	@Test
	public void testShouldReturnFalseForEmptyString() {
		assertCancel("");
	}

	@Test
	public void testShouldReturnFalseForBlankString() {
		assertCancel(" ");
	}

}
