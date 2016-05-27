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
package org.jboss.tools.openshift.test.ui.propertytester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.propertytester.OpenShiftResourcePropertyTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OpenShiftResourcePropertyTesterTest {

	@Mock 
	private IResourceUIModel uiModel;
	private String property = "isDeleting";
	private Object expectedValue = Boolean.TRUE;
	private OpenShiftResourcePropertyTester tester = new OpenShiftResourcePropertyTester();
	
	@Test
	public void testWhenAdapterDoesMatchExpectedReturnsTrue() {
		when(uiModel.isDeleting()).thenReturn(true);
		assertTrue(whenTesting());
	}

	@Test
	public void testWhenAdapterDoesNotMatchExpectedReturnsFalse() {
		when(uiModel.isDeleting()).thenReturn(false);
		assertFalse(whenTesting());
	}

	@Test
	public void testWhenPropertyIsNotDeletingReturnsFalse() {
		property = "foo";
		assertFalse(whenTesting());
	}

	@Test
	public void testWhenReceiverNotIProjectAdapterReturnsFalse() {
		assertFalse(tester.test("", property, null, expectedValue));
	}

	@Test
	public void testWhenExpectedNotBooleanReturnsFalse() {
		assertFalse(tester.test(uiModel, property, null, ""));
	}
	

	private boolean whenTesting() {
		return tester.test(uiModel, property, null, expectedValue); 
	}
}
