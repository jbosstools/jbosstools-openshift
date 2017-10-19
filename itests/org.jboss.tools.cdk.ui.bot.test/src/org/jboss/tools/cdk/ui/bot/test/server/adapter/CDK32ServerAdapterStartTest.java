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
 * Tests start/stop operation of CDK 3.2+ server adapter
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK32ServerAdapterStartTest extends CDKServerAdapterAbstractTest {

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
	public void testStartServerAdapter() {
		startServerAdapter();
		getCDEServer().stop();
		assertEquals(ServerState.STOPPED, getCDEServer().getLabel().getState());
	}	

}
