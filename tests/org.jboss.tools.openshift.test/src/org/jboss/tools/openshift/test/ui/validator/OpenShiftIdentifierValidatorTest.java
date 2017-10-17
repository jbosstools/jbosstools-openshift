/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import org.jboss.tools.openshift.internal.ui.validator.OpenShiftIdentifierValidator;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftIdentifierValidatorTest extends AbstractValidatorTest {

	public OpenShiftIdentifierValidatorTest() {
		super(new OpenShiftIdentifierValidator());
	}

	@Test
	public void testShouldPassIfEmpty() {
		assertFailure("");
	}

	@Test
	public void shouldPassIfStartsWithLiteral() {
		assertPass("a");
		assertPass("A");
	}

	@Test
	public void shouldPassIfStartsWithUnderscore() {
		assertPass("_");
	}

	@Test
	public void shouldFailIfStartsWithNumeric() {
		assertFailure("4");
	}

	@Test
	public void shouldFailIfStartsWithSpecialChar() {
		assertFailure("$");
		assertFailure("!");
		assertFailure("/");
		assertFailure("\\");
		assertFailure("^");
		assertFailure("{");
		assertFailure("}");
		assertFailure("[");
		assertFailure("]");
		assertFailure("ö");
	}

	@Test
	public void shouldPassIfNumericAfterChar1() {
		assertPass("a4");
	}

	@Test
	public void shouldFailIfSpecialCharAfterChar1() {
		assertFailure("a$");
		assertFailure("a!");
		assertFailure("a/");
		assertFailure("a\\");
		assertFailure("a^");
		assertFailure("a{");
		assertFailure("a}");
		assertFailure("a[");
		assertFailure("a]");
		assertFailure("aö");
	}

	@Test
	public void shouldPassIfUnderscoreAfterChar1() {
		assertPass("a_");
	}

}
