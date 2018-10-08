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
package org.jboss.tools.cdk.ui.bot.test.server.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKServerAdapterType;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test class is supposed to be place as first in test suite as it verifies Runtime detection feature
 * that takes place just after devstudio start up and searches for predefined path for runtime detection files (ie. at $HOME/.minishift)
 * Also, RedDeer takes care of closing all shell before tests are run, 
 * thus we can only check for presence of CDK server adapter in Servers View.
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK32RuntimeDetectionDevstudioStartUpTest {

	@Test
	public void testCDK32PresentAfterDevstudioStart() {
		new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
		new WaitWhile(new JobIsRunning(), TimePeriod.DEFAULT, false);
		new WaitUntil(new ServerAdapterTypeExists(CDKServerAdapterType.CDK32), TimePeriod.DEFAULT);
		List<Server> servers = CDKUtils.getAllServers();
		assertFalse("There are no servers in Servers View", servers.isEmpty());
		assertTrue("There should be just one server present but there are " + servers.size(), 
				CDKUtils.getAllServers().size() == 1);
		Server server = servers.get(0);
		assertTrue("Given server " + server.getLabel().getName() +
				" is supposed to be of " + CDKServerAdapterType.CDK32.serverType(),
				CDKUtils.isServerOfType(server, CDKServerAdapterType.CDK32.serverType()));
	}
	
	private class ServerAdapterTypeExists extends AbstractWaitCondition {

		ServersView2 view;
		
		CDKServerAdapterType type;
		
		public ServerAdapterTypeExists(CDKServerAdapterType type) {
			this.type = type;
			view = new ServersView2();
			view.open();
		}
		
		@Override
		public boolean test() {
			for (Server server : view.getServers()) {
				if (CDKUtils.isServerOfType(server, type.serverType())) {
					return true;
				}
			}
			return false;
		}
		
	}
}
