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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.tools.openshift.internal.ui.comparators.CreationTimestampComparator;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IResource;

@RunWith(MockitoJUnitRunner.class)
public class CreationTimestampComparatorTest {
	
	private static final int AFTER = 1;
	private static final int EQUAL = 0;
	private CreationTimestampComparator comparator;
	@Mock
	private IResourceUIModel one;
	@Mock
	private IResourceUIModel two;
	@Mock
	private IResource projectOne;
	@Mock
	private IResource projectTwo;

	@Before
	public void setUp() throws Exception {
		comparator = new CreationTimestampComparator();
		when(one.getResource()).thenReturn(projectOne);
		when(projectOne.getCreationTimeStamp()).thenReturn("2016-01-15T20:24:18Z");
		when(two.getResource()).thenReturn(projectTwo);
		when(projectTwo.getCreationTimeStamp()).thenReturn("2016-02-15T20:24:18Z");
	}
	
	@Test
	public void testResourcesAreSortedFromNewestToOldest() {
		assertEquals(AFTER, comparator.compare(one, two));
	}

	@Test
	public void testResourcesAreEqualOnParseException() {
		when(projectTwo.getCreationTimeStamp()).thenReturn("aaa");
		assertEquals(EQUAL, comparator.compare(one, two));
	}
	


}
