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
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic Devstudio and CDK integration test
 * Requires Secure Storage disabled
 * @author odockal
 *
 */public class CDKIntegrationTest extends CDKServerAdapterAbstractTest {
	
	private static final String OPENSHIFT_USER_NAME = "openshift-dev"; //$NON-NLS-1$
	
	private static final String OPENSHIFT_PROJECT_NAME = "OpenShift sample project"; //$NON-NLS-1$
	
	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER;
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER;
	}
	
	@BeforeClass
	public static void setup() {
		checkVagrantfileParameters();
		addNewCDKServer(CDK_SERVER_NAME, SERVER_ADAPTER, VAGRANTFILE_PATH);
	}
	
	@Test
	public void testCDEStop() {
		startServerAdapter();
		getCDEServer().stop();
		assertEquals(ServerState.STOPPED, getCDEServer().getLabel().getState());
	}
	
	@Test
	public void testCDERestart() {
		startServerAdapter();
		getCDEServer().restart();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
	@Test
	public void testOpenShiftConnection() {
		startServerAdapter();
		testOpenshiftConncetion(OPENSHIFT_PROJECT_NAME, OPENSHIFT_USER_NAME);
	}
	
	@Test
	public void testDockerDaemonConnection() {
		startServerAdapter();
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
	}

}
