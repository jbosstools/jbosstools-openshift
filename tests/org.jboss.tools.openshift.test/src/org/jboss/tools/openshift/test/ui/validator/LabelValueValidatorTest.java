/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import org.jboss.tools.openshift.internal.ui.validator.LabelKeyValidator;
import org.jboss.tools.openshift.internal.ui.validator.LabelValueValidator;
import org.junit.Test;

public class LabelValueValidatorTest extends AbstractValidatorTest{
	
	public LabelValueValidatorTest() {
		super(new LabelValueValidator());
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
		StringBuilder b = new StringBuilder();
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

}
