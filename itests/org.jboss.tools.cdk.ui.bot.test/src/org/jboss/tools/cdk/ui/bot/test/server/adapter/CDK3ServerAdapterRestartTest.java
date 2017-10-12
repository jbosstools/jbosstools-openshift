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
 * Tests restart of CDK 3 server adapter
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK3ServerAdapterRestartTest extends CDKServerAdapterAbstractTest {

	@BeforeClass
	public static void setup() {
		checkMinishiftParameters();
		addNewCDK3Server(CDK3_SERVER_NAME, SERVER_ADAPTER_3, MINISHIFT_HYPERVISOR, MINISHIFT_PATH);
	}

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_3;
	}
	
	@Test
	public void testCDERestart() {
		startServerAdapter();
		getCDEServer().restart();
		assertEquals(ServerState.STARTED, getCDEServer().getLabel().getState());
	}
	
}
