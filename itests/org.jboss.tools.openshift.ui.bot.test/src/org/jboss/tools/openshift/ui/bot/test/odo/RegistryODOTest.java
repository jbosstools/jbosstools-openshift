/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.odo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ODO Registry test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
public class RegistryODOTest {

	private OpenShiftApplicationExplorerView explorer;

	@Before
	public void setUp() {
		this.explorer = new OpenShiftApplicationExplorerView();
		this.explorer.open();
	}

	@Test
	public void testGetDevFileRegistries() {
		assertNotNull(this.explorer.getOpenShiftODORegistries());
	}

	@Test
	public void testGetDefaultDevfileRegistry() {
		assertNotNull(this.explorer.getOpenShiftODORegistries().getRegistry("devfile"));
	}

	@Test
	public void testAddRemoveRegistry() {
		this.explorer.getOpenShiftODORegistries().createNewRegistry("test", "https://registry.devfile.io");
		assertEquals(2, this.explorer.getOpenShiftODORegistries().getAllRegistries().size());
		this.explorer.getOpenShiftODORegistries().getRegistry("test").delete();
		assertEquals(1, this.explorer.getOpenShiftODORegistries().getAllRegistries().size());
	}
}
