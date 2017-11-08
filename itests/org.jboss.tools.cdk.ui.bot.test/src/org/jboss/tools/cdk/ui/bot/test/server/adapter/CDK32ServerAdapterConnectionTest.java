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
 * Tests that openshift and docker connection are created upon successful start of CDK 3.2 server adapter
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK32ServerAdapterConnectionTest extends CDKServerAdapterAbstractTest {

	private static final String DOCKER_DAEMON_CONNECTION = "Container Development Environment 3.2";

	@Override
	protected String getServerAdapter() {
		// return SERVER_ADAPTER_32; 
		//workaround for https://github.com/eclipse/reddeer/issues/1841
		return "Container Development Environment 3.2";
	}

	@BeforeClass
	public static void setup() {
		checkMinishiftProfileParameters();
		addNewCDK3Server(CDK32_SERVER_NAME, "Container Development Environment 3.2", MINISHIFT_HYPERVISOR, MINISHIFT_PROFILE);
	}

	@Test
	public void testCDK3ServerAdapterConnection() {
		startServerAdapter();
		testOpenshiftConncetion(findOpenShiftConnection(null, OPENSHIFT_USERNAME));
		testDockerConnection(DOCKER_DAEMON_CONNECTION);
	}


}
