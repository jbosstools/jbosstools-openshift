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
package org.jboss.tools.openshift.test.ui.comparators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IProject;

@RunWith(MockitoJUnitRunner.class)
public class ProjectViewerComparatorTest {
	
	private static final int LAST = 1;
	private static final int FIRST = -1;
	private static final int EQUAL = 0;
	private ProjectViewerComparator comparator;
	@Mock
	private IProject projectOne;
	@Mock
	private IProject projectTwo;

	@Before
	public void setUp() throws Exception {
		comparator = new ProjectViewerComparator();
		when(projectOne.getName()).thenReturn("mmmm");
		when(projectTwo.getName()).thenReturn("gggg");
	}
	
	@Test
	public void testDefaultProjectShouldAppearFirst() {
		when(projectOne.getName()).thenReturn("openshift-infra");
		when(projectTwo.getName()).thenReturn("default");
		assertTrue(0  < comparator.compare(null, projectOne, projectTwo));
	}

	@Test
	public void testOpenShiftProjectsShouldAppearBeforeOthers() {
		when(projectTwo.getName()).thenReturn("openshift-infra");
		assertTrue(0 < comparator.compare(null, projectOne, projectTwo));
	}

	@Test
	public void testOtherProjectsShouldAppearInAlphabetical() {
		assertTrue(0 < comparator.compare(null, projectOne, projectTwo));
		assertTrue(0 > comparator.compare(null, projectTwo, projectOne));
	}
	
	@Test
	public void testWhenInstanceAreNotProjects() {
		assertEquals(LAST, comparator.compare(null, "first", null));
		assertEquals(LAST, comparator.compare(null, null, "second"));
	}

	@Test
	public void testWhenInstanceAreNull() {
		assertEquals(LAST, comparator.compare(null, projectOne, null));
		assertEquals(LAST, comparator.compare(null, null, projectOne));
	}

}
