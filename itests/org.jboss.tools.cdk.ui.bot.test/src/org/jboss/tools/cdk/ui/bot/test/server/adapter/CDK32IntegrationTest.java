/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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
 * Testing of CDK 3.2+ server adapter 
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDK32IntegrationTest extends CDKServerAdapterAbstractTest {
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_32;

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@BeforeClass
	public static void setup() {
		checkMinishiftProfileParameters();
		addNewCDK32Server(CDK32_SERVER_NAME, SERVER_ADAPTER_32, MINISHIFT_HYPERVISOR, MINISHIFT_PROFILE, "");
	}
	
	@Test
	public void testCDK32ServerAdapter() {
		// cdk start verification
		startServerAdapter(() -> {});
		// cdk inner rhel image was registered during starting of server adapter
		verifyConsoleContainsRegEx("\\bRegistering.*subscription-manager\\b");
		verifyConsoleContainsRegEx("\\bRegistration in progress.*OK\\b");
		// OS3 and docker connection created verification
		testOpenshiftConncetion(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk restart check
		restartServerAdapter();
		// OS and docker connection should be operational after restart
		testOpenshiftConncetion(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk stop verification
		stopServerAdapter();
		// verify unregistering of machine
		verifyConsoleContainsRegEx("\\bUnregistering machine\\b");
	}

}
