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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * Soon to be deprecated as CDK 3.2 will became only supported version.
 * Testing CDK 3.x server adapter with minishift using vm driver passed via system property.
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
@Deprecated
public class CDK3IntegrationTest extends CDKServerAdapterAbstractTest {
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_3;

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_3;
	}
	
	@BeforeClass
	public static void setup() {
		checkMinishiftParameters();
		addNewCDK3Server(CDK3_SERVER_NAME, SERVER_ADAPTER_3, MINISHIFT_HYPERVISOR, MINISHIFT);
	}
	
	@Test
	public void testCDK3ServerAdapter() {
		// cdk start verification
		startServerAdapter(() -> skipRegistration(getCDEServer()));
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
	}
}
