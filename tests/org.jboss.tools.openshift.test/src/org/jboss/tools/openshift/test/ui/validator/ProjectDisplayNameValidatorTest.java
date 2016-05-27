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

import org.jboss.tools.openshift.internal.ui.validator.ProjectDisplayNameValidator;
import org.junit.Test;

public class ProjectDisplayNameValidatorTest extends AbstractValidatorTest {

	public ProjectDisplayNameValidatorTest() {
		super(new ProjectDisplayNameValidator());
	}
	
	@Test
	public void validDisplayNameShouldBeValid() {
		assertPass("jboss tools dream team");
	}
	
	@Test
	public void emptyDisplayNameShouldBeValid() {
		assertPass("");
	}
	
	@Test
	public void nullDisplayNameShouldBeCanceled() {
		assertCancel(null);
	}
	
	@Test
	public void notStringShouldBeCanceled() {
		assertCancel(new Object());
	}
	
	@Test
	public void tabsShouldBeInvalid() {
		assertFailure("jboss\tools");
	}
	
	@Test
	public void newLinesShouldBeInvalid() {
		assertFailure("happy\new year");
	}
	
	@Test
	public void veryLongDisplayNameShouldBeInvalid() {
		assertFailure(new String(new char[ProjectDisplayNameValidator.DISPLAY_NAME_LENGTH_LIMIT + 1]));
	}

}
