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

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.CleanDockerExplorerRequirement.CleanDockerExplorer;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class for multiple server adapter profiles running at once
 * @author odockal
 *
 */
@DisableSecureStorage
@CleanDockerExplorer
@CleanOpenShiftExplorer
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK3130,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		useExistingBinaryInProperty="cdk32.minishift")
@RunWith(RedDeerSuite.class)
public class CDK32ServerAdapterProfilesTest extends CDKServerAdapterAbstractTest {

	private Server secondServer;
	
	private OpenShiftExplorerView view;
	
	private DockerExplorerView dockerView;
	
	private static String DOCKER_DAEMON_CONNECTION;
	
	private static final Logger log = Logger.getLogger(CDK32ServerAdapterProfilesTest.class);
	
	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	protected static String getSecondServerAdapter() {
		return SERVER_ADAPTER_32 + " " + MINISHIFT_PROFILE2;
	}
	
	protected void setSecondCDKServer(Server server) {
		this.secondServer = (CDKServer) server;
	}
	
	protected Server getSecondCDKServer() {
		return this.secondServer;
	}
	
	@BeforeClass
	public static void setupServerAdapterWithMultipleProfile() {
		CDKUtils.addNewCDK32Server(getSecondServerAdapter(), MINISHIFT_HYPERVISOR, 
				serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString(), 
				MINISHIFT_PROFILE2, USERNAME, PASSWORD);
		DOCKER_DAEMON_CONNECTION = serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@Before
	public void setupSecondAdapter() {
		log.info("Open Servers view tab");
		setServersView(new CDKServersView());
		getServersView().open();
		log.info("Getting server object from Servers View with name: " + getSecondServerAdapter());
		setSecondCDKServer(getServersView().getServer(getSecondServerAdapter()));
		new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
		view = new OpenShiftExplorerView();
		dockerView = new DockerExplorerView();
	}

	@Test
	public void testCDK32ServerAdapterWithMultipleProfiles() {
		// fisrt adapter start verification
		serverRequirement.configureCDKServerAdapter(false);
		startServerAdapter(getCDKServer(), () -> {
			skipRegistrationViaFlag(getCDKServer(), true);
		}, false);
		new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
		new WaitWhile(new JobIsRunning(), TimePeriod.SHORT, false);
		int conCount = view.getOpenShift3Connections().size();
		int docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected only one OS connection, got " + conCount, 1, conCount);
		assertEquals("Expected only one Docker connection, got " + docCount, 1, docCount);
		// second adapter start verification
		startServerAdapter(getSecondCDKServer(), () -> { 
			skipRegistrationViaFlag(getSecondCDKServer(), true);
		}, false);
		new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
		new WaitWhile(new JobIsRunning(), TimePeriod.SHORT, false);
		// check counts of connections
		conCount = view.getOpenShift3Connections().size();
		docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected different number of OS connection.", 2, conCount);
		assertEquals("Expected different number of Docker connections.", 2, docCount);
		// check functionality of connections
		for (String docker : getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION)) {
			CDKTestUtils.testDockerConnection(docker);
		}
		for (OpenShift3Connection conn : view.getOpenShift3Connections()) {
			CDKTestUtils.testOpenshiftConnection(conn);
		}
		// adapters stopping verification
		stopServerAdapter(getCDKServer());
		stopServerAdapter(getSecondCDKServer());
	}
}
