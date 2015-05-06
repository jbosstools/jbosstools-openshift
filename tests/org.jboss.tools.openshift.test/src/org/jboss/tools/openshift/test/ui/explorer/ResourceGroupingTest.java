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
package org.jboss.tools.openshift.test.ui.explorer;

import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceGroupingTest {
	
	private @Mock IProject project;
	private ResourceGrouping grouping;
	
	@Before
	public void setUp() throws Exception {
		grouping = new ResourceGrouping(ResourceKind.Service, project);
	}

	@Test
	public void getResourcesShouldReturnResourcesOfTheKindForTheGroup() {
		when(project.getResources(any(ResourceKind.class))).thenReturn(new ArrayList<IResource>());
		grouping.getResources();
		verify(project).getResources(eq(ResourceKind.Service));
	}

}
