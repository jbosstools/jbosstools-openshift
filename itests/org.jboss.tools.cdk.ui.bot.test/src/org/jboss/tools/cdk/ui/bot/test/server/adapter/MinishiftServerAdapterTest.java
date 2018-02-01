/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.adapter;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Minishift server adapter's basic functionality test class
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class MinishiftServerAdapterTest extends CDKServerAdapterAbstractTest {

	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_MINISHIFT;
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_MINISHIFT;
	}
	
	@BeforeClass
	public static void setupMinishiftServerAdapterTest() {
		checkMinishiftParameters();
		addNewMinishiftServer(SERVER_ADAPTER_MINISHIFT, MINISHIFT_HYPERVISOR, MINISHIFT, "");
	}
	
	@Test
	public void testMinishiftServerAdapter() {
		// cdk start verification
		startServerAdapter(() -> {}, false);
		// OS3 and docker connection created verification
		testOpenshiftConnection(null, OPENSHIFT_USERNAME);
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk restart check
		restartServerAdapter();
		// OS and docker connection should be operational after restart
		testOpenshiftConnection(null, OPENSHIFT_USERNAME);
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk stop verification
		stopServerAdapter();
	}

}
