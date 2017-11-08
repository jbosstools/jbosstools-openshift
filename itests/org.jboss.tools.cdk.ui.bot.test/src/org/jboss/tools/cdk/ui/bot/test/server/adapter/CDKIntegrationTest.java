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

import static org.junit.Assert.assertEquals;

import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Deprecated as CDK 2.x is not supported
 * Basic Devstudio and CDK integration test
 * Requires Secure Storage disabled
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
@Deprecated
public class CDKIntegrationTest extends CDKServerAdapterAbstractTest {
	
	private static final String OS_USER_NAME = "openshift-dev"; //$NON-NLS-1$
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER;
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER;
	}
	
	@BeforeClass
	public static void setup() {
		checkVagrantfileParameters();
		addNewCDKServer(CDK_SERVER_NAME, SERVER_ADAPTER, VAGRANTFILE);
	}
	
	@Test
	public void testCDKServerAdapter() {
		// cdk start verification
		startServerAdapter();
		// OS3 and docker connection created verification
		testOpenshiftConncetion(OS_USER_NAME);
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk restart check
		getCDEServer().restart();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
<<<<<<< HEAD
		// OS and docker connection should be operational after restart
		testOpenshiftConncetion(OS_USER_NAME);
=======
	}
	
	@Test
	public void testOpenShiftConnection() {
		startServerAdapter();
		testOpenshiftConncetion(findOpenShiftConnection(null, OS_USER_NAME));
	}
	
	@Test
	public void testDockerDaemonConnection() {
		startServerAdapter();
>>>>>>> CDK discovery new itests
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk stop verification
		getCDEServer().stop();
		assertEquals(ServerState.STOPPED, getCDEServer().getLabel().getState());
	}

}
