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
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
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
 * Test class for Minishift server adapter using multiple profiles
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
public class MinishiftServerAdapterProfilesTest extends CDKServerAdapterAbstractTest {

	private Server secondServer;
	
	private OpenShiftExplorerView view;
	
	private DockerExplorerView dockerView;
	
	private static final Logger log = Logger.getLogger(CDK32ServerAdapterProfilesTest.class);
	
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
	
	protected static String getSecondServerAdapter() {
		return SERVER_ADAPTER_MINISHIFT + " " + MINISHIFT_PROFILE2;
	}
	
	protected void setSecondCDKServer(Server server) {
		this.secondServer = (CDKServer) server;
	}
	
	protected Server getSecondCDKServer() {
		return this.secondServer;
	}
	
	@BeforeClass
	public static void setupServerAdapterWithMultipleProfile() {
		CDKUtils.addNewMinishiftServer(getSecondServerAdapter(), MINISHIFT_HYPERVISOR, 
				serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString(), 
				MINISHIFT_PROFILE2);
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
	public void testMinishiftServerAdapterWithMultipleProfiles() {
		// fisrt adapter start verification
		startServerAdapter(getCDKServer(), () -> {}, false);
		int conCount = view.getOpenShift3Connections().size();
		int docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected only one OS connection, got " + conCount, 1, conCount);
		assertEquals("Expected only one Docker connection, got " + docCount, 1, docCount);
		// second adapter start verification
		startServerAdapter(getSecondCDKServer(), 
				() -> {}, false);
		// check counts of connections
		conCount = view.getOpenShift3Connections().size();
		docCount = getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION).size();
		assertEquals("Expected two OS connections, got " + conCount, 2, conCount);
		assertEquals("Expectedtwo Docker connections, got " + docCount, 2, docCount);
		// check functionality of connections
		for (String docker : getDockerConnectionCreatedByCDK(dockerView, DOCKER_DAEMON_CONNECTION)) {
			CDKTestUtils.testDockerConnection(docker);
		}
		for (OpenShift3Connection conn : view.getOpenShift3Connections()) {
			CDKTestUtils.testOpenshiftConnection(conn);
		}
		// adapters stoping verification
		stopServerAdapter(getCDKServer());
		stopServerAdapter(getSecondCDKServer());
	}

}
