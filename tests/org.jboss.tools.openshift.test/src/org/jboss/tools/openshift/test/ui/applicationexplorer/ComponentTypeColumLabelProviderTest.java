/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.applicationexplorer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.ComponentTypeColumLabelProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ComponentTypeColumLabelProviderTest {

	private ComponentTypeColumLabelProvider provider;

	@Before
	public void setup() throws Exception {
		provider = new ComponentTypeColumLabelProvider();
	}
	
	@Test
	public void checkDisplayNameIsUsed() {
		DevfileRegistry registry = mock(DevfileRegistry.class);
		when(registry.getName()).thenReturn("registry name");
		DevfileComponentType type = mock(DevfileComponentType.class);
		when(type.getName()).thenReturn("myname");
		when(type.getDisplayName()).thenReturn("my display name");
		when(type.getDevfileRegistry()).thenReturn(registry);
		String result = provider.getText(type);
		assertTrue(result.equals("my display name (from registry name)"));
	}
 }
