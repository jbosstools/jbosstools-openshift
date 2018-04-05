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

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Operate CDK 3.2+ server adapter with minishift2 profile, base test case
 * Also covers test case no. 2 from JBIDE-25378
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDK32IntegrationTest extends CDKServerAdapterAbstractTest {
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_32;
	
	private static final Logger log = Logger.getLogger(CDK32IntegrationTest.class);
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@BeforeClass
	public static void setup() {
		checkDevelopersParameters();
		checkCDK32Parameters();
		addNewCDK32Server(SERVER_ADAPTER_32, MINISHIFT_HYPERVISOR, CDK32_MINISHIFT, MINISHIFT_PROFILE); 
	}
	
	@Test
	public void testCDK32ServerAdapter() {
		// cdk start verification
		startServerAdapter(() -> {}, false);
		// cdk inner rhel image was registered during starting of server adapter
		verifyConsoleContainsRegEx("\\bRegistering.*subscription-manager\\b"); 
		// commented out due to https://issues.jboss.org/browse/CDK-270
		try {
			verifyConsoleContainsRegEx("\\bRegistration in progress.*OK\\b"); 
		} catch (AssertionError err) {
			log.error(err.getLocalizedMessage() + " because of CDK-270");
		}
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
		// verify unregistering of machine
		verifyConsoleContainsRegEx("\\bUnregistering machine\\b"); 
	}
}
