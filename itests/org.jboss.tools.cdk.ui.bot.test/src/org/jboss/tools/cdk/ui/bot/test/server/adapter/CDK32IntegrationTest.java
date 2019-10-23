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

import org.eclipse.linuxtools.docker.reddeer.requirements.CleanDockerExplorerRequirement.CleanDockerExplorer;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.securestorage.SecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
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
@DisableSecureStorage
@CleanDockerExplorer
@CleanOpenShiftExplorer
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK3100,
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		useExistingBinaryInProperty="cdk32.minishift")
@RunWith(RedDeerSuite.class)
public class CDK32IntegrationTest extends CDKServerAdapterAbstractTest {
	
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
	public void testCDK32ServerAdapter() {
		serverRequirement.configureCDKServerAdapter(false);
		// cdk start verification
		startServerAdapter(getCDKServer(), () -> {}, false);
		// cdk inner rhel image was registered during starting of server adapter
		CDKTestUtils.verifyConsoleContainsRegEx("\\bRegistering.*subscription-manager\\b"); 
		// commented out due to https://issues.jboss.org/browse/CDK-270
		CDKTestUtils.verifyConsoleContainsRegEx("\\bRegistration in progress.*OK\\b"); 
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
		// verify unregistering of machine
		CDKTestUtils.verifyConsoleContainsRegEx("\\bUnregistering machine\\b"); 
	}

}
