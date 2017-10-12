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
 * Tests that openshift and docker connection are created upon successful start of CDK 3 server adapter
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK3ServerAdapterConnectionTest extends CDKServerAdapterAbstractTest {

	private static final String OPENSHIFT_USER_NAME = "developer"; //$NON-NLS-1$
	
	private static final String OPENSHIFT_PROJECT_NAME = "My Project"; //$NON-NLS-1$

	private static final String DOCKER_DAEMON_CONNECTION = SERVER_ADAPTER_3;

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_3;
	}

	@BeforeClass
	public static void setup() {
		checkMinishiftParameters();
		addNewCDK3Server(CDK3_SERVER_NAME, SERVER_ADAPTER_3, MINISHIFT_HYPERVISOR, MINISHIFT_PATH);
	}

	@Test
	public void testCDK3ServerAdapterConnection() {
		startServerAdapter();
		testOpenshiftConncetion(OPENSHIFT_PROJECT_NAME, OPENSHIFT_USER_NAME);
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
	}


}
