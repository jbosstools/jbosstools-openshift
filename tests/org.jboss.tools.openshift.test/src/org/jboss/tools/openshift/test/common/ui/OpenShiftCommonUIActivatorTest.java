/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils.hideOpenShiftExplorer;
import static org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils.isOpenShiftExplorerVisible;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.test.util.UITestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpenShiftCommonUIActivatorTest {

	@BeforeClass
	public static void closeIntro() throws Exception {
		final IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart intro = introManager.getIntro();
		if (intro != null) {
			introManager.closeIntro(intro);
		}
	}	
	
	@Before
	public void setup() {
		hideOpenShiftExplorer();
	}
	
	@After
	public void tearDown() {
		hideOpenShiftExplorer();
		ConnectionsRegistrySingleton.getInstance().clear();
	}
	
	
	@Test
	public void testShowExplorerOnNewConnections() throws Exception {
		//Ensure OpenShift Explorer is hidden
		assertThat(isOpenShiftExplorerVisible()).isFalse();
		
		//Come up with a dummy connection
		Connection connection = mock(Connection.class);
		when(connection.getHost()).thenReturn("foo");
		when(connection.getUsername()).thenReturn("bar");
		
		//adding a new connection should open the OpenShift explorer
		ConnectionsRegistrySingleton.getInstance().add(connection);
		
		UITestUtils.waitForDeferredEvents();
		
		//check OpenShift explorer is visible now
		assertThat(isOpenShiftExplorerVisible()).isTrue();
	}

}
