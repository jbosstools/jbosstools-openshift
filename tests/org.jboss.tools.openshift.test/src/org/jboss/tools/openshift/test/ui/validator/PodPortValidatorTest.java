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

import java.util.Arrays;

import org.jboss.tools.openshift.internal.ui.wizard.deployimage.PodPortValidator;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.ServicePortAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PodPortValidatorTest extends AbstractValidatorTest {
	static final String CURRENT_VALUE = "999";

	static PodPortValidator createPodPortValidator() {
		ServicePortAdapter port1 = mockServicePort(1000, "3000");
		ServicePortAdapter port2 = mockServicePort(2000, "home");

		return new PodPortValidator(CURRENT_VALUE, Arrays.asList(port1, port2));
	}

	private static ServicePortAdapter mockServicePort(int port, String targetPort) {
		ServicePortAdapter port1 = Mockito.mock(ServicePortAdapter.class);
		Mockito.when(port1.getPort()).thenReturn(1000);
		Mockito.when(port1.getTargetPort()).thenReturn(targetPort);
		return port1;
	}

	public PodPortValidatorTest() {
		super(createPodPortValidator());
	}

	@Test
	public void testShouldReturnPassIfSameValue() {
		assertPass(CURRENT_VALUE);
	}

	@Test
	public void testShouldReturnPassIfConforms() {
		assertPass("30000");
		assertPass("name");
		assertPass("name5");
		assertPass("name-09-09");
	}

	@Test
	public void testShouldReturnFalseIfEmptyValue() {
		assertCancel("");
	}

	@Test
	public void testShouldReturnFalseIfLargeNumber() {
		assertFailure("100000");
	}

	@Test
	public void testShouldReturnFalseIfNegative() {
		assertFailure("-1");
	}

	@Test
	public void testShouldReturnFalseIfLong() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 63; i++) {
			sb.append("x");
		}
		assertPass(sb.toString());
		sb.append("x");
		assertFailure(sb.toString());
	}

	@Test
	public void testShouldReturnFalseIfUsedTargetPort() {
		assertFailure("3000");
		assertFailure("home");
	}

	@Test
	public void testShouldReturnFalseIfNotValidName() {
		assertFailure("-g");
		assertFailure("@x");
		assertFailure("x$x");
	}
}
