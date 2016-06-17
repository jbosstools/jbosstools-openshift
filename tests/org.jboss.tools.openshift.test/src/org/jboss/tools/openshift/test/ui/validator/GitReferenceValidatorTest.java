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

import org.jboss.tools.openshift.internal.ui.validator.GitReferenceValidator;
import org.junit.Test;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class GitReferenceValidatorTest extends AbstractValidatorTest {

	public GitReferenceValidatorTest() {
		super(new GitReferenceValidator());
	}

	@Test
	public void testShouldPassIfEmpty() {
		assertPass("");
	}

	@Test
	public void testSouldPassIfValid() {
		assertPass("a");
		assertPass("@a");
		assertPass("a]");
		assertPass("abc/def8");
		assertPass("abc./def");
		assertPass("a-");
		assertPass("a+");
		assertPass("a=");
		assertPass("a(");
	}

	//Rule 1.
	@Test
	public void testShouldFailIfStartsWithDotOrEndsWithLock() {
		assertFailure(".kkk");
		assertFailure("kkk.lock");
		assertFailure("a/.kkk");
		assertFailure("kkk.lock/a");
	}

	//Rule 3.
	@Test
	public void testShouldFailIfHasTwoConsequtiveDots() {
		assertFailure("kk..k");
		assertFailure("kkk/a..a");
	}

	//Rule 4.
	@Test
	public void testShouldFailIfHasCharsDefinedInRule4() {
		assertFailure("kk k");
		assertFailure("kk~k");
		assertFailure("kk^k");
		assertFailure("kk:k");
	}

	//Rule 5.
	@Test
	public void testShouldFailIfHasCharsDefinedInRule5() {
		assertFailure("kk?k");
		assertFailure("kk^k");
		assertFailure("kk[k");
	}

	//Rule 6.a
	@Test
	public void testShouldFailIfStartsWithOrEndsWithSlash() {
		assertFailure("/kkk");
		assertFailure("kkk/");
	}

	//Rule 6.b
	@Test
	public void testShouldFailIfHasTwoConsecutiveSlashes() {
		assertFailure("k//kk");
	}

	//Rule 7
	@Test
	public void testShouldFailIfEndsWithADot() {
		assertFailure("aaa.");
	}

	//Rule 8
	@Test
	public void testShouldFailIfHasLastRefOpeningSequence() {
		assertFailure("aaa@{bbb");
	}

	//Rule 9
	@Test
	public void testShouldFailIfIsASingleAtCharacter() {
		assertFailure("@");
	}

	//Rule 10
	@Test
	public void testShouldFailIfHasBacksash() {
		assertFailure("kk\\k");
	}

}
