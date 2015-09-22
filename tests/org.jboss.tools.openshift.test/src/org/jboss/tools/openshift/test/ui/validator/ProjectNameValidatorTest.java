/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.jboss.tools.openshift.internal.ui.validator.ProjectNameValidator;
import org.junit.Test;

/**
 * @author jeff.cantrill
 */
public class ProjectNameValidatorTest extends LabelValueValidatorTest {

	private ProjectNameValidator validator = new ProjectNameValidator("default message");

	@Override
	protected IValidator getValidator() {
		return validator;
	}

	@Test
	public void singleDotNameShouldNotBeAllowed() {
		assertFailure(".");
	}

	@Test
	public void doubleDotNameShouldNotBeAllowed() {
		assertFailure(".,");
	}

	@Test
	public void slashShouldNotBeAllowed() {
		assertFailure("abc\\def");
	}

	@Test
	public void percentShouldNotBeAllowed() {
		assertFailure("abcdef%");
	}

	@Test
	public void lengthLessThan2ShouldNotBeAllowed() {
		assertFailure("a");
	}

	@Test
	public void validProjectNameShouldBeValid() {
		assertPass("projectname");
	}

	@Override
	public void emptyValueShouldBeInvalid() {
		assertCancel("");
	}

	@Override
	public void valueWithSlashesShouldBeInValid() {
		// Should be invalid, as opposed to regular LabelValueValidator behavior
		assertFailure("abcd.efg/a23");
	}

	@Override
	public void valueWithDotsDashesAndUnderScoresShouldBeValid() {
		// Should be invalid, as opposed to regular LabelValueValidator behavior
		assertFailure("abcd.efg_k-123");
	}

	@Override
	public void nullValueShouldBeInvalid() {
		assertFailure(null);
	}

	@Test
	public void startsWithDashShouldNotBeAllowed() {
		assertFailure("-abc");
	}

	@Test
	public void endsWithDashShouldNotBeAllowed() {
		assertFailure("abc-");
	}

	@Test
	public void upperCaseCharactersShouldNotBeAllowed() {
		assertFailure("aBc");
	}
}
