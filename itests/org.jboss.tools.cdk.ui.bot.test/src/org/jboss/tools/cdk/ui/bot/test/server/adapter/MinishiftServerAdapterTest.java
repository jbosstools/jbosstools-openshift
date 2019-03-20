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
package org.jboss.tools.cdk.ui.bot.test.server.adapter;

import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Minishift server adapter's basic functionality test class
 * @author odockal
 *
 */
@CleanOpenShiftExplorer
@ContainerRuntimeServer(
		version = CDKVersion.MINISHIFT1320,
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		useExistingBinaryInProperty="minishift")
@RunWith(RedDeerSuite.class)
public class MinishiftServerAdapterTest extends CDKServerAdapterAbstractTest {

	private static String DOCKER_DAEMON_CONNECTION;
	
	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@BeforeClass
	public static void setup() {
		DOCKER_DAEMON_CONNECTION = serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@Test
	public void testMinishiftServerAdapter() {
		// cdk start verification
		startServerAdapter(getCDKServer(), () -> {}, false);
		// OS3 and docker connection created verification
		CDKTestUtils.testOpenshiftConnection(null, OPENSHIFT_USERNAME);
		CDKTestUtils.testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk restart check
		restartServerAdapter(getCDKServer());
		// OS and docker connection should be operational after restart
		CDKTestUtils.testOpenshiftConnection(null, OPENSHIFT_USERNAME);
		CDKTestUtils.testDockerConnection(DOCKER_DAEMON_CONNECTION);
		// cdk stop verification
		stopServerAdapter(getCDKServer());
	}

}
